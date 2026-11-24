# Contrato de códigos de error (backend ↔ frontend)

Este archivo es **el contrato entre las dos partes**. Se fija antes de que cualquiera de las dos
dependa de él, y sólo cambia editándolo aquí primero.

## Forma de la respuesta

`ApiExceptionHandler` responde siempre:

```json
{ "status": 409, "codigo": "reserva.cancelar.saldo-pendiente", "message": "..." }
```

- **`status`** — el HTTP que ya devolvía antes. No cambia.
- **`codigo`** — estable, en inglés, **nunca se traduce ni se muestra**. Es la identidad del error.
  `null` para las excepciones internas que no se migran.
- **`message`** — resuelto por `MessageSource` según `Accept-Language`. Es el texto de respaldo:
  lo que el usuario ve si el frontend no conoce el código.

## Quién manda

**El backend es el dueño de los códigos.** El frontend los refleja como claves de i18n añadiendo el
prefijo `errores.`:

| Código del backend | Clave en `assets/i18n/{en,es}.json` |
|---|---|
| `reserva.cancelar.saldo-pendiente` | `errores.reserva.cancelar.saldo-pendiente` |

`ErrorApiService` (`src/app/services/error-api.service.ts`) elige en este orden:

1. `errores.<codigo>` si el frontend tiene esa clave traducida;
2. el `message` del servidor, que ya viene en el idioma pedido;
3. la clave de respaldo que pasa la pantalla.

Por el paso 2, **el frontend nunca se rompe por un código que no conozca**: muestra el texto del
servidor. Eso quiere decir que las dos partes pueden avanzar sin bloquearse, y que añadir un código
nuevo en el backend no obliga a tocar el frontend el mismo día.

## Convención del nombre

`<dominio>.<accion-o-campo>.<motivo>`, todo en minúsculas, separando palabras con `-` dentro de cada
segmento. Dominios: `auth`, `cliente`, `reserva`, `resena`, `cuenta`, `plan`, `chat`, `pago`.

Los parámetros van posicionales, estilo `MessageFormat`: `{0}`, `{1}`. En los JSON del frontend el
mismo parámetro va **con nombre**, porque Transloco no usa posicionales:

```properties
# messages.properties
reserva.cancelar.fecha-pasada=Cannot cancel: the arrival date ({0}) has already passed.
```

```json
"reserva": { "cancelar": { "fecha-pasada": "Cannot cancel: the arrival date ({{fecha}}) has already passed." } }
```

Cuando un código lleva parámetros, este archivo los nombra en la columna *Params*, **en el orden en
que van los posicionales del backend**, y ese nombre es el que usa el frontend.
`npm run i18n:check` comprueba que `en` y `es` coincidan en sus `{{...}}`.

## Alcance

Se migran **sólo los errores que puede provocar un huésped**. El panel de administración y el portal
del operador se quedan en inglés por decisión de producto, así que sus mensajes conservan el literal
y responden `codigo: null`.

Igual de fuera quedan los mensajes internos (Gemini, NWS, Open-Meteo, Ticketmaster, NYC Open Data,
Wompi, modelo de ocupación): van a los logs del servidor, no a una pantalla. De esos sólo se corrige
que estén escritos en inglés.

Para dimensionar: en `src/main/java` hay **174** `new …Exception("` con literal (**154** mensajes
distintos). De ellos, **148** son de los tipos de dominio que llegan al cliente como 4xx y **32**
están en los paquetes internos. La tabla de abajo es el subconjunto alcanzable por un huésped.

## Códigos

Prioridad **1** = lo que un huésped toca a diario; si hay que parar antes de tiempo, se para después
de la 1.

### `auth` — iniciar sesión y crear cuenta (prioridad 1)

| Código | HTTP | Params | Inglés (base) |
|---|---|---|---|
| `auth.credenciales-invalidas` | 401 | — | Incorrect email or password. |
| `auth.email-requerido` | 400 | — | Email is required. |
| `auth.password-requerido` | 400 | — | Password is required. |
| `auth.google-token-invalido` | 401 | — | Your Google session could not be verified. Please try again. |
| `auth.usuario-no-encontrado` | 404 | `username` | No account found for {{username}}. |
| `auth.solo-huespedes` | 403 | — | Google sign-in is available to guests only. Staff must sign in with their credentials. |
| `auth.sesion-cliente-requerida` | 403 | — | You must sign in as a guest to continue. |
| `auth.sin-cliente-asociado` | 403 | — | This account is not linked to a guest profile. |
| `auth.datos-ajenos` | 403 | — | You can only access your own data. |

### `cliente` — registro y perfil del huésped (prioridad 1)

| Código | HTTP | Params | Inglés (base) |
|---|---|---|---|
| `cliente.email-duplicado` | 409 | `email` | A client with email {{email}} already exists. |
| `cliente.cedula-duplicada` | 409 | `cedula` | A client with ID number {{cedula}} already exists. |
| `cliente.datos-requeridos` | 400 | — | First name, last name and email are required. |
| `cliente.cedula-requerida` | 400 | — | ID number is required. |
| `cliente.password-requerido` | 400 | — | Password is required. |
| `cliente.email-invalido` | 400 | `email` | The email {{email}} is not valid. |
| `cliente.requerido` | 400 | — | Client is required. |
| `cliente.no-encontrado` | 404 | `id` | Client not found: {{id}} |
| `cliente.tiene-reservas` | 409 | — | This client has reservations and cannot be deleted. |
| `cliente.email-en-uso` | 409 | `email` | The email {{email}} is already used by another account. |

### `reserva` — reservar, modificar y cancelar (prioridad 1)

| Código | HTTP | Params | Inglés (base) |
|---|---|---|---|
| `reserva.fechas-requeridas` | 400 | — | Both the check-in and check-out dates are required. |
| `reserva.fecha-inicio-pasada` | 400 | — | The check-in date cannot be in the past. |
| `reserva.fecha-fin-anterior` | 400 | — | The check-out date must be after the check-in date. |
| `reserva.huespedes-minimo` | 400 | — | The number of guests must be at least 1. |
| `reserva.datos-requeridos-crear` | 400 | — | Some required data is missing to create the reservation. |
| `reserva.datos-requeridos-modificar` | 400 | — | Some required data is missing to modify the reservation. |
| `reserva.no-encontrada` | 404 | `id` | Reservation not found: {{id}} |
| `reserva.cancelar.fecha-pasada` | 409 | `fecha` | Cannot cancel: the arrival date ({{fecha}}) has already passed. |
| `reserva.cancelar.saldo-pendiente` | 409 | `saldo` | Cannot cancel: the account has an outstanding balance of {{saldo}}. |
| `reserva.sin-cuenta` | 409 | — | This reservation has no associated account. |
| `reserva.ajena` | 403 | — | You can only access your own reservations. |
| `reserva.sin-disponibilidad` | 409 | `tipo`, `desde`, `hasta` | No {{tipo}} rooms are available from {{desde}} to {{hasta}}. |
| `reserva.cancelar.estado-invalido` | 409 | `estado` | A reservation with status {{estado}} cannot be cancelled. |
| `reserva.modificar.estado-invalido` | 409 | `estado` | A reservation with status {{estado}} cannot be modified. |

### `resena` — reseñas del huésped (prioridad 1)

| Código | HTTP | Params | Inglés (base) |
|---|---|---|---|
| `resena.solo-finalizadas` | 409 | — | Only completed stays can be reviewed. |
| `resena.ya-existe` | 409 | — | This reservation already has a review. |
| `resena.reserva-ajena` | 403 | — | This reservation does not belong to you. |
| `resena.solo-autor` | 403 | — | Only the author can edit or delete this review. |
| `resena.calificacion-rango` | 400 | `min`, `max` | The rating must be a whole number between {{min}} and {{max}}. |
| `resena.comentario-longitud` | 400 | `min`, `max` | The comment is required and must be between {{min}} and {{max}} characters. |
| `resena.titulo-longitud` | 400 | `max` | The title cannot exceed {{max}} characters. |
| `resena.no-encontrada` | 404 | `id` | Review not found: {{id}} |
| `resena.datos-requeridos-crear` | 400 | — | Some required data is missing to create the review. |
| `resena.datos-requeridos-actualizar` | 400 | — | Some required data is missing to update the review. |
| `resena.filtro-requerido` | 400 | — | Specify tipoHabitacionId, clienteId or incluirOcultas=true. |

### `plan` — plan de visita con IA (prioridad 2)

| Código | HTTP | Params | Inglés (base) |
|---|---|---|---|
| `plan.presupuesto-requerido` | 400 | — | Choose a budget. |
| `plan.ritmo-requerido` | 400 | — | Choose a pace for your days. |
| `plan.intereses-rango` | 400 | `min`, `max` | Choose between {{min}} and {{max}} interests. |
| `plan.intereses-repetidos` | 400 | — | Each interest can be chosen only once. |
| `plan.dieta-invalida` | 400 | — | Diet options are VEGANO, VEGETARIANO and SIN_GLUTEN, each at most once. |
| `plan.descripcion-longitud` | 400 | `min`, `max` | Tell us about your trip in {{min}} to {{max}} characters. |
| `plan.ajuste-longitud` | 400 | `min`, `max` | Tell the concierge what to change in {{min}} to {{max}} characters. |
| `plan.estado-reserva-invalido` | 409 | — | Only pending, confirmed or in-progress reservations can have a visit plan. |
| `plan.estadia-terminada` | 409 | — | This stay has already ended. |
| `plan.estadia-demasiado-larga` | 409 | `max` | Visit plans are available for stays of up to {{max}} nights. |
| `plan.dia-pasado` | 409 | — | That day has already passed. |
| `plan.dia-fuera-del-plan` | 400 | — | That day is not part of your plan. |
| `plan.limite-diario` | 429 | `max` | You have reached today's limit of {{max}} plans. |
| `plan.solo-servicios-hotel` | 400 | — | Only hotel services can be added to your stay. |
| `plan.bloque-no-encontrado` | 404 | `id` | Plan item not found: {{id}} |
| `plan.no-disponible` | 503 | — | The planner is unavailable right now. |
| `plan.ilegible` | 502 | — | The saved plan could not be read. |
| `plan.no-confiable` | 502 | — | We could not build a reliable plan. Please try again. |
| `plan.preferencias-ilegibles` | 502 | — | The saved preferences could not be read. |
| `plan.no-encontrado` | 404 | — | No plan yet for this reservation. |
| `plan.reserva-ajena` | 403 | — | This reservation does not belong to you. |
| `plan.cliente-requerido` | 400 | — | Some required data is missing. |
| `plan.datos-requeridos-interpretar` | 400 | — | Some required data is missing to read your description. |
| `plan.datos-requeridos-generar` | 400 | — | Some required data is missing to build your plan. |
| `plan.datos-requeridos-ajustar` | 400 | — | Some required data is missing to adjust your plan. |
| `plan.datos-requeridos-regenerar` | 400 | — | Some required data is missing to rebuild this day. |
| `plan.datos-requeridos-agregar` | 400 | — | Some required data is missing to add this to your stay. |
| `plan.notas-longitud` | 400 | `max` | Notes cannot exceed {{max}} characters. |

### `cuenta` — cargos y pagos que ve el huésped (prioridad 2)

Un huésped no registra consumos ni pagos, pero sí **ve** su cuenta y choca con estas reglas al
cancelar o al añadir un servicio desde el plan de visita.

| Código | HTTP | Params | Inglés (base) |
|---|---|---|---|
| `cuenta.no-existe` | 409 | — | This reservation does not have an account yet: confirm it first. |
| `cuenta.cerrada` | 409 | — | The account for this reservation is already closed. |
| `cuenta.estado-reserva-invalido` | 409 | — | Services can only be charged to confirmed or in-progress reservations. |
| `cuenta.cantidad-minima` | 400 | — | Quantity must be at least 1. |
| `cuenta.servicio-requerido` | 400 | — | Service is required. |

### `chat` — concierge (prioridad 2)

| Código | HTTP | Params | Inglés (base) |
|---|---|---|---|
| `chat.no-disponible` | 503 | — | The concierge is unavailable right now. |
| `chat.limite-mensajes` | 429 | `max` | You have reached today's limit of {{max}} messages. |
| `chat.respuesta-incompleta` | 502 | — | The concierge could not finish this answer. |
| `chat.mensaje-vacio` | 400 | — | The message cannot be empty. |
| `chat.mensaje-longitud` | 400 | `max` | The message cannot exceed {{max}} characters. |
| `chat.rol-invalido` | 400 | — | Invalid role in the history: use "usuario" or "asistente". |

### `pago` — pasarela Wompi (prioridad 3)

| Código | HTTP | Params | Inglés (base) |
|---|---|---|---|
| `pago.datos-incompletos` | 400 | — | Some required data is missing to prepare the payment. |
| `pago.transaccion-requerida` | 400 | — | The Wompi transaction id is required. |
| `pago.no-aprobado` | 409 | `estado` | The payment was not approved (status: {{estado}}). |

## Lo que NO lleva código

Responden `codigo: null` y conservan su literal en inglés:

- todo lo que sólo alcanza `/api/admin/**`, `/api/operadores/**`, `/api/habitaciones` (POST/PUT/
  DELETE), `/api/menu` (POST/PUT/DELETE), `/api/servicios` (POST/PUT/DELETE), `/api/precios/**`;
- los mensajes de los clientes externos y del modelo de ocupación;
- `DataIntegrityViolationException` y los errores de forma de la petición, que ya tienen un texto
  genérico en `ApiExceptionHandler`.

## Si un código de la tabla no existe en el código actual

La tabla se escribió leyendo los mensajes que hoy lanzan `ReservaService`, `ResenaService`,
`ClienteService`, `PlanVisitaService`, `CuentaService` y los de autenticación. Si al migrar aparece
un mensaje alcanzable por un huésped que no está aquí, **se añade a este archivo primero** y después
se usa. No se inventan códigos en el código fuente sin dejarlos escritos aquí.

## Añadidos al migrar el backend

Al recorrer los `throw` de los servicios aparecieron mensajes alcanzables por un huésped que la
tabla original no cubría. Siguiendo la regla de la sección anterior se escribieron **primero aquí** y
después en `messages.properties`. Ya están en las tablas de arriba; se listan juntos para que el
frontend sepa qué claves nuevas reflejar:

- `auth.solo-huespedes`, `auth.sesion-cliente-requerida`, `auth.sin-cliente-asociado`,
  `auth.datos-ajenos` — los 403 de `UsuarioActualService` y del login con Google.
- `cliente.email-en-uso` — editar el perfil con el email de otra cuenta (distinto de
  `cliente.email-duplicado`, que es el alta).
- `reserva.ajena`, `reserva.sin-disponibilidad`, `reserva.cancelar.estado-invalido`,
  `reserva.modificar.estado-invalido` — el huésped crea, modifica y cancela sus reservas.
- `resena.filtro-requerido` — `GET /api/resenas` sin filtros (endpoint público).
- `plan.no-confiable` — `PlanNoConfiableException` (la IA devolvio dos planes invalidos seguidos).
- `plan.no-encontrado`, `plan.reserva-ajena`, `plan.cliente-requerido`,
  `plan.preferencias-ilegibles`, `plan.notas-longitud` y la familia
  `plan.datos-requeridos-*` (interpretar, generar, ajustar, regenerar, agregar).
- `chat.mensaje-vacio`, `chat.mensaje-longitud`, `chat.rol-invalido` — validaciones de
  `POST /api/chat` previas a abrir el stream.

Dos notas sobre códigos que ya estaban en la tabla:

- `cliente.email-invalido` no tiene hoy ningún `throw` que lo lance: el backend no valida el
  formato del email. La clave existe en `messages.properties` para cuando se añada esa validación.
- `chat.respuesta-vacia` **no es un codigo de error**: es el texto que el stream emite cuando el
  modelo no escribio nada visible. Vive en `messages[_es].properties` porque hay que traducirlo,
  pero nunca viaja en el campo `codigo` de una respuesta.
- `chat.respuesta-incompleta` no viaja como HTTP 502: cuando falla a mitad del streaming la
  respuesta ya salió con 200, así que va en el evento SSE `error` como
  `{ "codigo": "chat.respuesta-incompleta", "message": "..." }`.
