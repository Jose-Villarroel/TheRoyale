# Especificación — Plan de visita con IA

**Proyecto:** The Royale · **Estado:** propuesta · **Fecha:** 2026-11-11

## 1. Objetivo

Que un huésped con una reserva reciba un itinerario día por día para su estadía en Nueva York. El plan combina servicios y restaurantes del hotel con eventos reales de la ciudad y tiene en cuenta el clima de esas fechas.

La IA (Gemini, el mismo cliente del chatbot) solo **ordena y explica**. Los eventos, lugares y servicios del plan salen de fuentes verificables: la base de datos del hotel y APIs públicas. Cada elemento que no venga de una de esas fuentes se descarta antes de llegar al huésped.

## 2. Ubicación del hotel (ficticia)

La dirección actual en `chatbot/informacion-hotel.md` ("Fifth Avenue, Midtown Manhattan") no es suficiente para calcular distancias. Se fija esta dirección **inventada para el proyecto**:

| Campo | Valor |
|---|---|
| Dirección | **The Royale, 640 Fifth Avenue, New York, NY 10019** (Fifth Avenue con West 51st Street, Midtown) |
| Coordenadas | `40.7590, -73.9772` |
| Metro más cercano | 5 Av/53 St (E, M) y 47–50 Sts–Rockefeller Ctr (B, D, F, M) |
| A pie | Rockefeller Center y St. Patrick's Cathedral (3 min), MoMA (6 min), Central Park South (10 min), Times Square (12 min), Bryant Park (12 min) |

> Es una dirección ficticia: el número coincide con un edificio real de la Quinta Avenida, pero The Royale no existe. Si el proyecto se presenta en público, conviene aclararlo en el pie de página.

**Dónde se configura:**
- `application.properties`: `hotel.direccion`, `hotel.latitud`, `hotel.longitud`, leídos con una clase `HotelProperties`, igual que `GeminiProperties`.
- `chatbot/informacion-hotel.md`: actualizar la línea *Address* para que el chatbot dé la misma dirección.
- Pie de página del frontend: mostrar la dirección.

## 3. Alcance

**Incluye**
- Formulario de preferencias y generación del plan para una reserva propia.
- Clima por día, eventos de NYC Open Data y Ticketmaster, catálogo curado de lugares y servicios del hotel.
- Validación de cada elemento contra sus fuentes.
- Regenerar un día, con una indicación opcional ("algo más tranquilo").
- "Add to my stay": cargar un servicio del hotel a la cuenta de la reserva.
- Actualización automática del clima cuando la llegada está cerca.

**No incluye (sección 15)**
- Reservar restaurantes externos o comprar entradas: se enlaza a la fuente oficial.
- Mapa interactivo.
- Consultar el plan desde el chatbot.
- Alertas del metro en tiempo real.

## 4. Flujo

```
Huésped                Angular                          Spring Boot                                APIs / Gemini
  │  "Plan my visit"     │                                   │                                          │
  │ ───────────────────► │  formulario de preferencias       │                                          │
  │  envía preferencias  │ ── POST /api/reservas/{id}/plan ► │ 1. valida dueño, estado y fechas          │
  │                      │                                   │ 2. junta el contexto EN PARALELO ───────► │ NWS / Open-Meteo
  │                      │                                   │    (timeout 5 s por fuente)              │ NYC Open Data
  │                      │                                   │                                          │ Ticketmaster
  │                      │                                   │    + servicios, restaurantes y lugares (BD)│
  │                      │                                   │ 3. pide el plan en JSON con esquema ────► │ Gemini
  │                      │                                   │ 4. valida cada bloque contra el contexto  │
  │                      │                                   │ 5. guarda PlanVisita y responde           │
  │  ve la línea de      │ ◄──────────── 201 PlanVisitaResponse                                         │
  │  tiempo por día      │                                   │                                          │
```

Si una API externa falla o tarda, el plan se genera sin esa fuente y la respuesta la lista en `fuentesNoDisponibles`. Solo Gemini es imprescindible: sin él responde 503.

## 5. Fuentes de datos

### 5.1 Del hotel (base de datos)

| Fuente | Qué aporta | Identificador en el plan |
|---|---|---|
| `Servicio` + `caracteristicas` (Wellness, Dining, Business, Concierge, Transportation) | Spa, tours privados de museos, entradas a Broadway, traslados, etc. | `servicioId` + `caracteristica` (texto exacto de la lista) |
| `PlatoMenu` + `RestauranteHotel` | Comidas en el hotel con los horarios de `informacion-hotel.md` | `restaurante` (enum) |
| **`LugarNyc` (nuevo, catálogo curado)** | Lugares emblemáticos con coordenadas, si es bajo techo, horario típico y duración sugerida | `lugarId` |

**Catálogo `LugarNyc` (entidad nueva, sembrada en `DataLoader`).** Unos 25 lugares: Central Park, The Met, MoMA, Top of the Rock, Rockefeller Center, St. Patrick's Cathedral, Bryant Park, New York Public Library, Grand Central Terminal, Times Square, High Line, Vessel and Hudson Yards, Chelsea Market, 9/11 Memorial & Museum, One World Observatory, Brooklyn Bridge, DUMBO, Statue of Liberty Ferry, American Museum of Natural History, Guggenheim, Fifth Avenue shopping, Greenwich Village, SoHo, Little Island y Summit One Vanderbilt.

Campos: `id`, `nombre`, `categoria` (MUSEO, PARQUE, MIRADOR, PASEO, COMPRAS, LANDMARK), `latitud`, `longitud`, `bajoTecho`, `duracionMinutos`, `horarioReferencia` (texto), `urlOficial`, `activo`.

Con este catálogo la IA no puede inventar lugares: solo elige entre los que el hotel aprobó. El admin podría editarlo después (sección 15).

### 5.2 APIs públicas

| API | Uso | Endpoint | Clave | Caché |
|---|---|---|---|---|
| **National Weather Service** | Pronóstico oficial para los días dentro de los próximos 7 | `GET https://api.weather.gov/points/40.7590,-73.9772` → `properties.forecast` → `GET` esa URL (periodos de día y noche) | No. Exige un `User-Agent` que identifique la app, p. ej. `TheRoyaleCourseProject/1.0` | 1 h |
| **Open-Meteo** | Pronóstico de los días 8 a 16 y **clima típico** para fechas más lejanas (promedio de los mismos días en los 3 años anteriores) | `GET https://api.open-meteo.com/v1/forecast?latitude=…&longitude=…&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=America/New_York&forecast_days=16` · histórico: `https://archive-api.open-meteo.com/v1/archive` | No | 6 h |
| **NYC Open Data** (Socrata SODA) | Eventos con permiso de la ciudad en Manhattan durante la estadía: ferias, mercados, desfiles, eventos en parques | `GET https://data.cityofnewyork.us/resource/tvpp-9vvx.json` (*NYC Permitted Event Information*) con `$where=start_date_time between '…' and '…' AND event_borough='Manhattan'` y `$limit=200` | Opcional: `X-App-Token` gratis para subir el límite | 6 h |
| **Ticketmaster Discovery API v2** | Conciertos, teatro, deportes y familia cerca del hotel en esas fechas, con enlace oficial | `GET https://app.ticketmaster.com/discovery/v2/events.json?apikey=…&latlong=40.7590,-73.9772&radius=5&unit=miles&startDateTime=…Z&endDateTime=…Z&size=50&sort=date,asc` | Sí, gratis (crear app en developer.ticketmaster.com) | 6 h |

> **Antes de implementar, verificar en la documentación oficial:** el identificador y los nombres de campo del dataset de NYC Open Data (se pueden ver en la ficha del dataset), los límites de uso de cada API y las condiciones de atribución de Ticketmaster.

**Normalización.** Cada fuente se convierte a un formato interno común:
```java
record EventoCandidato(String id, FuenteEvento fuente, String titulo, LocalDateTime inicio,
                       LocalDateTime fin, String lugar, Double latitud, Double longitud,
                       String categoria, String url) {}
enum FuenteEvento { NYC_OPEN_DATA, TICKETMASTER }

record ClimaDia(LocalDate fecha, String resumen, int maxC, int minC,
                Integer probLluvia, TipoClima tipo) {}
enum TipoClima { PRONOSTICO_NWS, PRONOSTICO_OPEN_METEO, CLIMA_TIPICO }
```

**Filtros antes de mandar a la IA** (para no gastar tokens ni darle opciones absurdas):
- Eventos solo dentro de la estadía, a menos de 5 millas, hasta 15 por día y 60 en total, ordenados por cercanía.
- Se descartan los eventos de NYC Open Data de tipo construcción, filmación o cierre de calle; solo quedan los que un turista puede visitar.
- Los lugares se mandan con su distancia y tiempo a pie desde el hotel, calculados con Haversine a 80 m/min.

## 6. Preferencias del huésped

| Campo | Tipo | Valores |
|---|---|---|
| `intereses` | lista, 1 a 5 | `MUSEOS`, `TEATRO`, `GASTRONOMIA`, `PARQUES`, `COMPRAS`, `VIDA_NOCTURNA`, `BIENESTAR`, `FAMILIA` |
| `ritmo` | enum | `TRANQUILO` (2 bloques al día), `EQUILIBRADO` (3), `INTENSO` (4) |
| `presupuesto` | enum | `MODERADO`, `ALTO`, `SIN_LIMITE` |
| `dieta` | lista opcional | `VEGANO`, `VEGETARIANO`, `SIN_GLUTEN` (mismo `EtiquetaPlato`) |
| `movilidadReducida` | boolean | Prioriza lugares cercanos y el traslado privado del hotel |
| `conNinos` | boolean | Evita vida nocturna y prioriza `FAMILIA` |
| `notas` | texto opcional, máximo 300 | "Es nuestro aniversario", "Llegamos tarde el primer día" |

## 7. Generación con Gemini

**Cambio en `GeminiClient`:** agregar un método no transmitido (sin streaming) que pida JSON con esquema:
```java
String generarJson(SolicitudGemini solicitud, Map<String, Object> esquemaRespuesta);
// En GeminiRestClient: generationConfig.responseMimeType = "application/json"
// y generationConfig.responseSchema = esquema. Reutiliza los modelos de respaldo y el timeout.
```

**Instrucción de sistema** (`resources/plan/instruccion-plan.md`):
- Usa solo elementos de las listas `SERVICIOS`, `RESTAURANTES`, `LUGARES` y `EVENTOS`, con su identificador exacto. Si no hay nada adecuado, usa `TIEMPO_LIBRE`.
- Respeta los horarios de los restaurantes y de los eventos. No pongas dos bloques que se crucen.
- Con lluvia (probabilidad de 60 % o más) o con máximas bajo 2 °C, prioriza lugares bajo techo.
- Agrupa por zona: no cruces Manhattan dos veces en un mismo día.
- El día de llegada empieza después del check-in (15:00) y el de salida termina antes del check-out (12:00).
- El `motivo` de cada bloque es una frase corta en el idioma del huésped que explica por qué está ahí.

**Datos que se le mandan.** Fechas, número de huéspedes, preferencias, clima por día y las cuatro listas con sus IDs. **No** se mandan el nombre, el email, la cédula ni el teléfono del huésped.

**Esquema de respuesta:**
```json
{
  "resumen": "string",
  "dias": [{
    "fecha": "2026-12-13",
    "titulo": "Museos y Central Park",
    "bloques": [{
      "inicio": "10:00", "fin": "12:30",
      "tipo": "LUGAR | EVENTO | SERVICIO_HOTEL | RESTAURANTE_HOTEL | TIEMPO_LIBRE",
      "referenciaId": "string (lugarId, id del evento, servicioId o enum del restaurante)",
      "caracteristica": "string, solo para SERVICIO_HOTEL",
      "titulo": "string",
      "motivo": "string"
    }]
  }]
}
```

## 8. Validación (`ValidadorPlanService`)

Cada bloque se revisa contra el contexto **que se le mandó a Gemini en esa misma generación**, no contra la base completa.

| Regla | Si falla |
|---|---|
| `LUGAR`: `referenciaId` está en la lista de lugares enviada y el lugar está activo | Se elimina el bloque |
| `EVENTO`: el ID existe en los candidatos, su fecha es la del día y el bloque cae dentro de su horario | Se elimina |
| `SERVICIO_HOTEL`: `servicioId` existe y `caracteristica` es exactamente una de sus características | Se elimina |
| `RESTAURANTE_HOTEL`: el restaurante existe y el bloque está dentro de su horario | Se elimina |
| Horas en formato `HH:mm`, `inicio < fin`, sin cruces con otro bloque del mismo día | Se elimina el bloque que se cruza |
| El día de llegada no empieza antes de las 15:00 y el de salida no termina después de las 12:00 | Se elimina |
| Fechas: todos los días de la estadía, ni uno más ni uno menos | Faltan días: se completan con `TIEMPO_LIBRE`. Sobran: se eliminan |
| Más del 40 % de los bloques eliminados | Se reintenta la generación una vez. Si vuelve a pasar, 502 `"We could not build a reliable plan. Please try again."` |

Después de validar, el servidor **enriquece** cada bloque con los datos reales de la fuente: nombre oficial, URL, distancia a pie y precio del servicio. No se usa lo que haya escrito la IA para esos campos, solo su `motivo`.

## 9. Modelo de datos

### Entidad `PlanVisita` (tabla `plan_visita`)

| Campo | Tipo | Restricciones |
|---|---|---|
| `id` | `Long` | PK |
| `reserva` | `Reserva` | `@OneToOne`, único, no nulo (un plan vigente por reserva) |
| `preferencias` | `String` (JSON) | `@Lob`, no nulo |
| `contenido` | `String` (JSON del plan ya validado y enriquecido) | `@Lob`, no nulo |
| `fuentesUsadas` | `String` | p. ej. `"NWS,OPEN_METEO,NYC_OPEN_DATA,TICKETMASTER"` |
| `requiereRevision` | `boolean` | `true` si el clima cambió y un bloque al aire libre cae en día de lluvia |
| `modeloIa` | `String` | Modelo que respondió (principal o de respaldo) |
| `fechaGeneracion` / `fechaActualizacion` | `LocalDateTime` | |
| `generacionesHoy` / `fechaUltimaGeneracion` | `int` / `LocalDate` | Límite de 5 generaciones por reserva por día |

Se guarda como JSON porque el plan se lee y se reemplaza entero o por día, nunca se consulta bloque por bloque. Cada bloque lleva un `id` (UUID) para poder referirse a él desde "Add to my stay".

**Entidad `LugarNyc`** (sección 5.1).

**Regla de borrado:** `ReservaService.eliminar()` borra también el plan, igual que la cuenta y la reseña.

## 10. API REST

Las rutas cuelgan de la reserva, como `CuentaRestController`. Mientras no haya JWT, `clienteId` va en la petición y se valida que la reserva sea suya (403 con `AccesoDenegadoException`, la misma de las reseñas).

| Método | Ruta | Uso | Respuestas |
|---|---|---|---|
| POST | `/api/reservas/{reservaId}/plan` | Genera el plan, o lo reemplaza si ya existe. Cuerpo: `{ clienteId, preferencias }` | `201` / `400` / `403` / `404` / `409` / `429` / `502` / `503` |
| GET | `/api/reservas/{reservaId}/plan?clienteId=` | Obtiene el plan | `200` / `403` / `404` |
| POST | `/api/reservas/{reservaId}/plan/dias/{fecha}/regenerar` | Regenera un solo día. Cuerpo: `{ clienteId, indicacion? }` (máximo 200 caracteres) | `200` / `400` / `403` / `404` / `429` |
| POST | `/api/reservas/{reservaId}/plan/bloques/{bloqueId}/agregar-a-cuenta` | Carga un `SERVICIO_HOTEL` a la cuenta con `CuentaService` (cantidad 1, operador predeterminado) | `201 CuentaResponse` / `400` (el bloque no es un servicio) / `409` (la cuenta no está abierta) |
| DELETE | `/api/reservas/{reservaId}/plan?clienteId=` | Borra el plan | `204` / `403` / `404` |

**Reglas de la reserva:**
- **Estado:** solo se puede planificar una reserva `PENDIENTE`, `CONFIRMADA` o `EN_CURSO` (409 en otro caso). En `EN_CURSO` solo se generan los días que faltan.
- **Duración:** máximo 14 noches (400).
- **"Add to my stay":** solo funciona con la cuenta abierta, es decir, con la reserva `CONFIRMADA` o `EN_CURSO`. Es la regla que ya aplica `CuentaService`. En `PENDIENTE` el botón aparece deshabilitado con el texto "Available once your reservation is confirmed".

**`PlanVisitaResponse`** (resumido):
```json
{
  "reservaId": 12, "resumen": "...", "requiereRevision": false,
  "fuentesUsadas": ["NWS", "NYC_OPEN_DATA", "TICKETMASTER"], "fuentesNoDisponibles": [],
  "dias": [{
    "fecha": "2026-12-13", "titulo": "Museums and Central Park",
    "clima": { "resumen": "Rain likely", "maxC": 6, "minC": 1, "probLluvia": 70, "tipo": "PRONOSTICO_NWS" },
    "bloques": [{
      "id": "6f1c…", "inicio": "10:00", "fin": "12:30", "tipo": "LUGAR",
      "titulo": "The Metropolitan Museum of Art", "motivo": "Indoors on a rainy morning.",
      "aPie": "35 min", "bajoTecho": true, "url": "https://www.metmuseum.org",
      "fuente": "The Royale curated places"
    }, {
      "id": "a90b…", "inicio": "19:30", "fin": "22:00", "tipo": "EVENTO",
      "titulo": "…", "lugar": "…", "url": "https://www.ticketmaster.com/…", "fuente": "Ticketmaster"
    }, {
      "id": "c47e…", "inicio": "16:00", "fin": "17:30", "tipo": "SERVICIO_HOTEL",
      "servicioId": 1, "titulo": "Signature Massage", "precio": 45.0, "agregable": true, "fuente": "The Royale"
    }]
  }]
}
```

**Tarea programada:** un `@Scheduled` diario (06:00, hora de Nueva York) busca los planes de reservas que llegan en los próximos 7 días. Para cada uno vuelve a pedir el pronóstico a NWS, actualiza el `clima` de cada día y marca `requiereRevision = true` si un día con bloques al aire libre pasa a tener 60 % o más de probabilidad de lluvia. **No** regenera el plan solo: el huésped decide.

## 11. Backend: clases nuevas o modificadas

| Archivo | Cambio |
|---|---|
| `config/HotelProperties.java` | Dirección y coordenadas |
| `config/PlanVisitaProperties.java` | Claves y opciones: `plan.ticketmaster.api-key=${TICKETMASTER_API_KEY:}`, `plan.nyc-open-data.app-token=${NYC_OPEN_DATA_APP_TOKEN:}`, `plan.nws.user-agent`, `plan.timeout-fuente-segundos=5`, `plan.fuentes.habilitadas=true` |
| `model/PlanVisita.java`, `model/LugarNyc.java` + repositorios | Nuevos |
| `service/plan/fuentes/ClimaClient.java` (+ `NwsClimaClient`, `OpenMeteoClimaClient`) | Interfaz y dos implementaciones |
| `service/plan/fuentes/EventosClient.java` (+ `NycOpenDataEventosClient`, `TicketmasterEventosClient`) | Interfaz y dos implementaciones |
| `service/plan/ContextoPlanService.java` | Llama a las fuentes en paralelo (`CompletableFuture` con timeout), filtra y arma las listas con sus IDs |
| `service/plan/GeneradorPlanService.java` | Prompt, esquema, llamada a `generarJson` y un reintento |
| `service/plan/ValidadorPlanService.java` | Reglas de la sección 8 y enriquecimiento |
| `service/plan/PlanVisitaService.java` | Orquesta, guarda, regenera días, aplica el límite diario y la tarea de clima |
| `service/chat/GeminiClient.java`, `GeminiRestClient.java` | Método `generarJson` |
| `api/PlanVisitaRestController.java` | Endpoints de la sección 10 |
| `dto/plan/*` | `GenerarPlanRequest`, `RegenerarDiaRequest`, `PlanVisitaResponse`, `DiaPlanResponse`, `BloquePlanResponse`, `ClimaDiaResponse` |
| `service/ReservaService.java` | Borrar el plan al borrar la reserva |
| `BackendApplication.java` | `@EnableScheduling` |
| `DataLoader.java` | Catálogo de lugares y un plan de ejemplo ya validado, para que la demo funcione sin claves |
| `.env.example` | `TICKETMASTER_API_KEY=` y `NYC_OPEN_DATA_APP_TOKEN=` |
| `chatbot/informacion-hotel.md` | Nueva dirección |

Las fuentes son interfaces. Así, en el perfil `test` y cuando `plan.fuentes.habilitadas=false`, se usan implementaciones falsas con datos fijos y las pruebas nunca llaman a internet.

## 12. Frontend (Angular)

Textos en inglés. Estilo: el del mockup del concierge (carbón, dorado, off-white, sin imágenes).

| Archivo | Descripción |
|---|---|
| `models/plan-visita.model.ts` | Interfaces de la respuesta y las preferencias |
| `services/plan-visita.service.ts` | `generar`, `obtener`, `regenerarDia`, `agregarACuenta`, `eliminar` |
| `reservas/plan-visita/plan-visita.component.*` | Ruta `my-reservations/:id/plan` con `clienteGuard`. Sin plan muestra el formulario; con plan, la línea de tiempo |
| `…/secciones/form-preferencias/` | Intereses como píldoras seleccionables, ritmo y presupuesto como grupos de opciones, interruptores de dieta, movilidad y niños, y campo de notas con contador |
| `…/secciones/generando-plan/` | Estado de carga con los pasos reales: "Checking the weather", "Finding events in the city", "Building your days". La generación tarda de 10 a 25 s |
| `…/secciones/dia-plan/` | Encabezado con fecha, título y clima (con la etiqueta "Typical weather" si es `CLIMA_TIPICO`), bloques en orden y botón "Rebuild this day" |
| `…/secciones/bloque-plan/` | Hora, título, motivo, tiempo a pie, "Indoors" si aplica, enlace "Official page" y fuente. En servicios del hotel, precio y "Add to my stay" |
| `…/secciones/aviso-revision/` | Banda visible si `requiereRevision`: "The forecast changed for Dec 13. Rebuild that day?" |
| `…/secciones/fuentes-plan/` | Pie con las fuentes usadas y aviso de IA: "Plan suggested by AI with data from …. Check times on each official page." |
| `reservas/mis-reservas/secciones/tarjeta-mi-reserva/` | Botón "Plan my visit" o "View my plan" en reservas `PENDIENTE`, `CONFIRMADA` o `EN_CURSO` |
| `reservas/reserva-formulario/secciones/reserva-confirmada/` | Llamado "Plan your days in New York" con enlace al plan |
| `app.routes.ts` | Nueva ruta |

**IDs para Selenium:** `btn-plan-{reservaId}`, `form-preferencias`, `pref-interes-{VALOR}`, `pref-ritmo-{VALOR}`, `btn-generar-plan`, `plan-dia-{fecha}`, `plan-bloque-{id}`, `btn-agregar-{id}`, `btn-regenerar-{fecha}`, `plan-fuentes`.

## 13. Criterios de aceptación

1. **Generar:** *dado* un cliente con una reserva CONFIRMADA del 12 al 15 de diciembre, *cuando* elige "Museums" y "Theater" con ritmo equilibrado y envía, *entonces* ve 4 días (llegada, dos completos y salida), cada uno con su clima y bloques que no se cruzan.
2. **Llegada y salida:** el primer día no tiene bloques antes de las 15:00 y el último ninguno después de las 12:00.
3. **Sin inventos:** todo bloque `LUGAR`, `EVENTO` o `SERVICIO_HOTEL` corresponde a un elemento real de su fuente, y su título y URL vienen de ella. Prueba: un Gemini falso que devuelve un evento con ID inexistente produce un plan sin ese bloque.
4. **Clima:** en un día con 70 % de probabilidad de lluvia, ningún bloque `LUGAR` está al aire libre.
5. **Fuente caída:** si Ticketmaster no responde en 5 s, el plan se genera igual y la respuesta incluye `"fuentesNoDisponibles": ["TICKETMASTER"]`.
6. **IA no disponible:** sin `GEMINI_API_KEY`, el botón muestra "The planner is unavailable right now" y el `POST` responde 503.
7. **Regenerar día:** "Rebuild this day" con la indicación "something quieter" cambia solo ese día y deja los demás iguales.
8. **Add to my stay:** en una reserva CONFIRMADA, agregar "Signature Massage" crea un consumo en la cuenta por el precio del servicio. En una PENDIENTE el botón está deshabilitado y el `POST` responde 409.
9. **Dueño:** pedir el plan de una reserva ajena responde 403.
10. **Límite:** la sexta generación del día para la misma reserva responde 429.
11. **Privacidad:** el contenido enviado a Gemini no incluye el nombre, email, cédula ni teléfono del huésped (se comprueba capturando la solicitud en el Gemini falso).
12. **Clima actualizado:** tras la tarea diaria, un plan cuyo día cambió a lluvia muestra el aviso de revisión.

## 14. Pruebas

| Tipo | Archivo | Casos |
|---|---|---|
| Unitarias | `ValidadorPlanServiceTest` | Cada regla de la sección 8, el enriquecimiento y el reintento al superar el 40 % |
| Unitarias | `ContextoPlanServiceTest` | Fuentes en paralelo, timeout de una fuente, filtros de distancia y fecha, elección NWS / Open-Meteo / clima típico según los días que faltan |
| Unitarias | `NwsClimaClientTest`, `TicketmasterEventosClientTest`, `NycOpenDataEventosClientTest` | Parseo de respuestas guardadas en `src/test/resources/plan/*.json` (sin red) |
| Unitarias | `PlanVisitaServiceTest` | Estados de la reserva, 14 noches como máximo, límite diario, regenerar un día, tarea de clima |
| Controlador | `PlanVisitaRestControllerTest` | Códigos 201/200/204/400/403/404/409/429/503 |
| E2E (Selenium) | `PlanVisitaE2ETest` (caso 4) | Login → My reservations → Plan my visit → preferencias → ver días → Add to my stay → ver el cargo en la cuenta. Con Gemini y fuentes falsas del perfil `test` |

## 15. Extensiones futuras

- **Desde el chat:** herramienta `consultarPlanVisita` para que el concierge responda "¿qué tengo mañana?".
- **Metro:** estaciones cercanas a cada bloque con los datos estáticos de MTA (GTFS) y alertas en tiempo real el mismo día.
- **Citi Bike:** estaciones con bicicletas disponibles cerca del hotel (feed GBFS público) en días de buen clima.
- **The Met Collection API:** tres obras destacadas para ver en la visita al MET.
- **Catálogo editable:** CRUD de `LugarNyc` en el panel de admin.
- **Exportar:** "Copy my plan" como texto y "Add to calendar" (archivo .ics).
- **Solicitud en vez de cargo directo:** que "Add to my stay" cree una solicitud que el operador confirma antes de cargarla, para servicios con cupo (spa, helicóptero).
