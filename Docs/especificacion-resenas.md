# Especificación — Reseñas de habitaciones

**Proyecto:** The Royale · **Estado:** propuesta · **Fecha:** 2026-11-10

## 1. Objetivo

Que los huéspedes que ya terminaron su estadía puedan calificar (1 a 5 estrellas) y comentar el tipo de habitación en que se hospedaron, y que cualquier visitante vea esas opiniones en la página del tipo de habitación. El administrador puede ocultar reseñas inapropiadas.

## 2. Alcance

**Incluye**
- Crear, editar y eliminar la propia reseña (cliente).
- Ver reseñas y el resumen (promedio, total, distribución por estrellas) en `/rooms/:id` y el promedio en `/rooms` (público).
- Moderación: listar, ocultar y volver a mostrar reseñas (admin).
- Datos de ejemplo en `DataLoader`.

**No incluye (posibles extensiones, sección 11)**
- Respuesta del hotel a una reseña.
- Calificaciones por categoría (limpieza, comodidad, servicio).
- Fotos en las reseñas.
- Resumen generado con IA.

## 3. Actores y permisos

| Acción | Visitante | Cliente | Operador | Admin |
|---|---|---|---|---|
| Ver reseñas visibles y resumen | ✔ | ✔ | ✔ | ✔ |
| Crear reseña de una reserva propia FINALIZADA | | ✔ | | |
| Editar / eliminar su propia reseña | | ✔ | | |
| Ver reseñas ocultas, ocultar / mostrar | | | | ✔ |

> El operador no participa: es coherente con la separación actual de roles (el admin gestiona catálogo y contenido, el operador la operación diaria).

## 4. Reglas de negocio

| # | Regla | Error si se incumple |
|---|---|---|
| RN-1 | Solo se puede reseñar una reserva en estado `FINALIZADA` (check-out hecho). | 409 `"Only completed stays can be reviewed."` |
| RN-2 | La reserva debe pertenecer al cliente que crea la reseña. | 403 `"This reservation does not belong to you."` |
| RN-3 | Una sola reseña por reserva. Un cliente con varias estadías en el mismo tipo puede dejar varias reseñas (una por estadía). | 409 `"This reservation already has a review."` |
| RN-4 | `calificacion` es un entero entre 1 y 5. | 400 |
| RN-5 | `comentario` es obligatorio, de 10 a 1000 caracteres (tras `trim`). `titulo` es opcional, máximo 100. | 400 |
| RN-6 | La reseña se asocia al **tipo de habitación** de la reserva, tomado en el momento de crearla. Es lo que se muestra al público (las páginas públicas son por tipo, no por habitación). | — |
| RN-7 | Solo el autor puede editar o eliminar su reseña. Editar actualiza `fechaModificacion`; no cambia la reserva ni el tipo. | 403 |
| RN-8 | Una reseña oculta (`visible = false`) no aparece en listados públicos ni cuenta para el promedio. El autor sí la sigue viendo en *My reservations*, marcada como oculta. | — |
| RN-9 | Al eliminar una reserva (`ReservaService.eliminar`) se elimina también su reseña. Un cliente con reservas ya no se puede eliminar, así que sus reseñas no quedan huérfanas. | — |
| RN-10 | El promedio se redondea a 1 decimal. Sin reseñas visibles, el resumen devuelve `promedio: null` y `total: 0`. | — |

> **Autenticación:** mientras no haya JWT, el `clienteId` viaja en el cuerpo de la petición (igual que hoy en las reservas) y el backend valida RN-2 y RN-7 comparándolo con el dueño de la reserva. Cuando se implemente JWT, el `clienteId` saldrá del token y desaparecerá del cuerpo.

## 5. Modelo de datos

### Entidad `Resena` (tabla `resena`)

> Sin "ñ" en clases, tablas y rutas para evitar problemas de codificación; en la interfaz se muestra "Review".

| Campo | Tipo | Restricciones |
|---|---|---|
| `id` | `Long` | PK, autogenerado |
| `reserva` | `Reserva` | `@OneToOne`, `reserva_id` **único**, no nulo |
| `cliente` | `Cliente` | `@ManyToOne`, no nulo |
| `tipoHabitacion` | `TipoHabitacion` | `@ManyToOne`, no nulo (copia de `reserva.habitacion.tipoHabitacion`) |
| `calificacion` | `Integer` | no nulo, 1–5 |
| `titulo` | `String` | longitud 100, nulo permitido |
| `comentario` | `String` | longitud 1000, no nulo |
| `visible` | `boolean` | no nulo, por defecto `true` |
| `fechaCreacion` | `LocalDateTime` | no nulo |
| `fechaModificacion` | `LocalDateTime` | nulo permitido |

`cliente` y `tipoHabitacion` se guardan aunque se puedan derivar de la reserva: los listados por tipo y el promedio no tienen que hacer join con reserva y habitación.

**Cambios en entidades existentes**
- `Reserva`: lado inverso `@OneToOne(mappedBy = "reserva") @JsonIgnore private Resena resena;`, con el mismo patrón que `cuenta`.
- Actualizar `Docs/diagrama_clases_hotel.puml` y `Docs/diagrama_er_hotel.puml`.

### `ResenaRepository`
```java
List<Resena> findByTipoHabitacionIdAndVisibleTrueOrderByFechaCreacionDesc(Long tipoId);
List<Resena> findByClienteIdOrderByFechaCreacionDesc(Long clienteId);
Optional<Resena> findByReservaId(Long reservaId);
boolean existsByReservaId(Long reservaId);

// Resumen: [calificacion, cantidad] para armar promedio y distribución
@Query("select r.calificacion, count(r) from Resena r " +
       "where r.tipoHabitacion.id = :tipoId and r.visible = true group by r.calificacion")
List<Object[]> contarPorCalificacion(Long tipoId);

// Promedio y total de todos los tipos de una vez (para /rooms)
@Query("select r.tipoHabitacion.id, avg(r.calificacion), count(r) from Resena r " +
       "where r.visible = true group by r.tipoHabitacion.id")
List<Object[]> resumenPorTipo();
```

## 6. API REST (`/api/resenas`)

Sigue las convenciones de `ReservaRestController`: respuestas aplanadas en un `record` y errores `{ "status", "message" }` vía `ApiExceptionHandler`.

| Método | Ruta | Uso | Respuesta |
|---|---|---|---|
| GET | `/api/resenas?tipoHabitacionId={id}` | Público: reseñas visibles de un tipo, de la más reciente a la más antigua | `200 ResenaResponse[]` |
| GET | `/api/resenas?clienteId={id}` | Cliente: sus reseñas, incluidas las ocultas | `200 ResenaResponse[]` |
| GET | `/api/resenas?incluirOcultas=true` (+ filtros opcionales `tipoHabitacionId`, `calificacion`) | Admin: moderación | `200 ResenaResponse[]` |
| GET | `/api/resenas/resumen?tipoHabitacionId={id}` | Público: resumen de un tipo | `200 ResumenResenasResponse` |
| GET | `/api/resenas/resumen` | Público: resumen de todos los tipos (para `/rooms`) | `200 ResumenResenasResponse[]` |
| GET | `/api/resenas/{id}` | Detalle | `200` / `404` |
| POST | `/api/resenas` | Crear | `201` / `400` / `403` / `404` / `409` |
| PUT | `/api/resenas/{id}` | Editar (autor) | `200` / `400` / `403` / `404` |
| DELETE | `/api/resenas/{id}?clienteId={id}` | Eliminar (autor) | `204` / `403` / `404` |
| POST | `/api/resenas/{id}/ocultar` | Admin | `200 ResenaResponse` |
| POST | `/api/resenas/{id}/mostrar` | Admin | `200 ResenaResponse` |

### DTOs

```java
// POST: los cuatro campos. PUT: solo calificacion, titulo, comentario (+ clienteId para validar autoría)
public record CrearResenaRequest(Long reservaId, Long clienteId, Integer calificacion,
                                 String titulo, String comentario) {}

public record ResenaResponse(
        Long id, Integer calificacion, String titulo, String comentario, boolean visible,
        LocalDateTime fechaCreacion, LocalDateTime fechaModificacion,
        Long reservaId, LocalDate fechaInicioEstadia, LocalDate fechaFinEstadia,
        Long clienteId, String clienteNombre,   // nombre + inicial del apellido: "Laura M."
        Long tipoHabitacionId, String tipoHabitacionNombre) {
    public static ResenaResponse from(Resena r) { ... }
}

public record ResumenResenasResponse(
        Long tipoHabitacionId, Double promedio, long total,
        Map<Integer, Long> distribucion) {}   // claves 1..5 siempre presentes, con 0 si no hay
```

> **Privacidad:** el público solo ve el nombre y la inicial del apellido. Nunca se exponen el email, la cédula ni el teléfono del autor.

**Cambio en `ReservaResponse`:** se agrega `Long resenaId` (nulo si no hay reseña). Así *My reservations* sabe si mostrar "Write a review" o "Edit review" sin otra petición.

**Nueva excepción:** `AccesoDenegadoException`, mapeada a **403** en `ApiExceptionHandler`, para RN-2 y RN-7.

### Ejemplo

```http
POST /api/resenas
{ "reservaId": 12, "clienteId": 8, "calificacion": 5,
  "titulo": "Perfect city view", "comentario": "Spotless room and very attentive staff." }

201 Created
{ "id": 3, "calificacion": 5, "titulo": "Perfect city view", "comentario": "Spotless room...",
  "visible": true, "fechaCreacion": "2026-11-10T14:02:11", "fechaModificacion": null,
  "reservaId": 12, "fechaInicioEstadia": "2026-11-03", "fechaFinEstadia": "2026-11-06",
  "clienteId": 8, "clienteNombre": "Laura M.", "tipoHabitacionId": 4, "tipoHabitacionNombre": "Luxury" }
```

## 7. Backend: clases nuevas o modificadas

| Archivo | Cambio |
|---|---|
| `model/Resena.java` | Nueva entidad |
| `model/Reserva.java` | Lado inverso `resena` |
| `repository/ResenaRepository.java` | Nuevo |
| `service/ResenaService.java` | Nuevo: `crear`, `actualizar`, `eliminar`, `ocultar`, `mostrar`, `listarPorTipo`, `listarPorCliente`, `listarParaModeracion`, `resumen`, `resumenTodos`, `eliminarPorReserva` |
| `service/ReservaService.java` | `eliminar()` llama a `resenaService.eliminarPorReserva(reserva)` antes de borrar |
| `api/ResenaRestController.java` | Nuevo, con `@CrossOrigin` igual que los demás |
| `dto/CrearResenaRequest.java`, `dto/ResenaResponse.java`, `dto/ResumenResenasResponse.java` | Nuevos |
| `dto/ReservaResponse.java` | Campo `resenaId` |
| `errors/AccesoDenegadoException.java`, `errors/ApiExceptionHandler.java` | 403 |
| `DataLoader.java` | Reseñas para las reservas FINALIZADA existentes (`finalizada104`, `finalizada501`) y algunas más, para que los tipos muestren promedio |

## 8. Frontend (Angular)

Los textos de la interfaz van en inglés, como el resto del sitio.

| Archivo | Descripción |
|---|---|
| `models/resena.model.ts` | Interfaces `Resena`, `ResumenResenas`, `CrearResena` |
| `models/reserva.model.ts` | Agregar `resenaId: number \| null` |
| `services/resena.service.ts` | Métodos `obtenerPorTipo`, `obtenerPorCliente`, `obtenerParaModeracion`, `resumen`, `resumenTodos`, `crear`, `actualizar`, `eliminar`, `ocultar`, `mostrar` |
| `shared/estrellas/` | Componente reutilizable: modo lectura (`[valor]`, admite medias estrellas para el promedio) y modo entrada (`[(valor)]`, accesible con teclado y `aria-label="Rate N of 5"`) |
| `rooms/room-detalle/secciones/resenas-room/` | Sección debajo de la disponibilidad: promedio grande, total, barras de distribución 5→1 y lista de reseñas (autor, fecha de estadía, estrellas, título, comentario). Sin reseñas: "No reviews yet". Muestra las 5 más recientes y un botón "Show all" |
| `rooms/room-detalle/secciones/info-room/` | Junto al precio: estrellas del promedio y "(N reviews)", con enlace que baja a la sección |
| `rooms/rooms-lista/secciones/room-lista-item/` | Promedio y total por tipo, cargados una sola vez con `resumenTodos()` en `rooms-lista` |
| `reservas/mis-reservas/secciones/tarjeta-mi-reserva/` | Si `estado === 'FINALIZADA'`: botón "Write a review" (sin `resenaId`) o "Edit review" con las estrellas dadas. Nuevos `output` `resenar` y `editarResena` |
| `reservas/mis-reservas/secciones/form-resena/` | Formulario reactivo (estrellas, título, comentario con contador `n/1000`), validaciones de RN-4 y RN-5 con `campo-error`, errores del backend con `alerta-error`. Botones "Submit" / "Delete review" (con confirmación) / "Cancel" |
| `admin/resenas-lista/` | Ruta `admin/resenas` con `adminGuard`. Filtros por tipo, calificación y estado (visible / oculta, reutilizando `filtros-estado`). Cada tarjeta tiene "Hide" / "Show" |
| `admin/secciones/nav-admin/` | Enlace "Reviews" |
| `app.routes.ts` | `{ path: 'admin/resenas', component: ResenasListaComponent, canActivate: [adminGuard] }` |

### IDs para las pruebas E2E (Selenium)

Siguen la convención existente (`mi-reserva-{id}`):
`btn-resenar-{reservaId}`, `form-resena`, `resena-estrella-{1..5}`, `resena-titulo`, `resena-comentario`, `btn-enviar-resena`, `resenas-room`, `resenas-promedio`, `resenas-total`, `resena-{id}`, `admin-resena-{id}`, `btn-ocultar-resena-{id}`.

## 9. Criterios de aceptación

1. **Crear:** *Dado* un cliente con una reserva FINALIZADA sin reseña, *cuando* entra a *My reservations* y envía 5 estrellas con un comentario válido, *entonces* la tarjeta pasa a mostrar "Edit review" y la reseña aparece primero en `/rooms/{tipo}` con el promedio actualizado.
2. **Estado no válido:** *Dada* una reserva PENDIENTE, CONFIRMADA, EN_CURSO o CANCELADA, la tarjeta no muestra el botón y un `POST` directo responde 409.
3. **Duplicado:** un segundo `POST` para la misma reserva responde 409.
4. **Ajena:** un `POST` con un `clienteId` que no es el dueño de la reserva responde 403.
5. **Validación:** calificación 0 o 6, comentario de menos de 10 caracteres o de más de 1000 → 400, y el formulario no deja enviar.
6. **Editar:** el autor cambia de 5 a 3 estrellas; el promedio del tipo se recalcula y la reseña muestra "Edited".
7. **Eliminar:** el autor elimina su reseña; desaparece del tipo y la tarjeta vuelve a "Write a review".
8. **Moderar:** el admin oculta una reseña; deja de verse en `/rooms/{tipo}` y no cuenta para el promedio, pero el autor la sigue viendo marcada como "Hidden by the hotel". "Show" la restaura.
9. **Sin reseñas:** un tipo sin reseñas visibles muestra "No reviews yet" y el resumen devuelve `promedio: null, total: 0`.
10. **Privacidad:** ninguna respuesta pública incluye el email, la cédula ni el teléfono del autor.
11. **Cascada:** al eliminar (admin) una reserva FINALIZADA con reseña, la reseña también se elimina y no hay error de integridad.

## 10. Pruebas

| Tipo | Archivo | Casos |
|---|---|---|
| Unitarias (Mockito) | `service/ResenaServiceTest.java` | RN-1 a RN-10: crea ok; rechaza los 4 estados no FINALIZADA; duplicado; reserva ajena; rango de calificación; longitud del comentario; editar/eliminar de otro cliente; resumen con y sin ocultas; resumen vacío |
| Repositorio (`@DataJpaTest`) | `repository/ResenaRepositoryTest.java` | `contarPorCalificacion` y `resumenPorTipo` excluyen ocultas; unicidad de `reserva_id` |
| Controlador (MockMvc) | `api/ResenaRestControllerTest.java` | Códigos 201/200/204/400/403/404/409 y forma del JSON (sin datos privados) |
| Integración | `service/ReservaServiceIntegrationTest.java` | Eliminar una reserva con reseña |
| E2E (Selenium) | `e2e/ResenaHuespedE2ETest.java` (caso 3) | Login de un cliente con una reserva FINALIZADA → reseñar → verla en `/rooms/{tipo}` → login del admin → ocultarla → ya no aparece |

Todas con `@ActiveProfiles("test")`, como las existentes.

## 11. Extensiones futuras

- **Respuesta del hotel:** campos `respuesta` y `fechaRespuesta`, editables por el admin.
- **Subcalificaciones:** limpieza, comodidad, ubicación y servicio, con promedio por categoría.
- **Resumen con IA:** "Lo que dicen los huéspedes", generado con Claude a partir de las reseñas visibles y cacheado por tipo. Reutiliza el servicio de IA del chatbot.
- **Dashboard del admin:** calificación promedio global y por tipo, y evolución mensual.
- **Invitación:** al hacer check-out, mostrar al huésped un aviso para reseñar su estadía.
