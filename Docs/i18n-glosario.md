# Glosario de traducción EN → ES

El inglés es el idioma base de la SPA (`src/assets/i18n/en.json`): toda clave nueva se crea ahí
primero y después se traduce en `es.json`. `npm run i18n:check` falla si una clave existe en un
idioma y no en el otro.

**Registro**: español neutro latinoamericano, formal de hotel de lujo. Se trata al huésped de
**usted**, nunca de "tú", y se evita el imperativo seco: "Elija sus fechas", no "Elige tus fechas".
**Alcance**: se traduce lo que ve un huésped. El panel de administración (`/admin/**`) y el portal
del operador (`/operator/**`) se quedan en inglés por decisión de producto, y esas rutas **fuerzan**
el idioma a inglés, para que no aparezca una pantalla mitad y mitad a través de los componentes de
`shared/` (que sí están traducidos).

Los **códigos de error** que el backend devuelve y el frontend traduce tienen su propio contrato en
[i18n-codigos-error.md](i18n-codigos-error.md). Ese archivo manda sobre este para todo lo que sea un
mensaje de error del servidor.

## Términos

| Inglés | Español | Nota |
|---|---|---|
| Guest | Huésped | Nunca "invitado" ni "cliente" en texto de cara al huésped |
| Client (admin) | Cliente | Solo en el panel de admin, donde es la entidad del CRUD |
| Booking / Reservation | Reserva | Un solo término para los dos |
| Book (verbo) | Reservar | "Book Now" → "Reservar" |
| Book Your Stay | Reserve su estadía | |
| Stay | Estadía | No "estancia" (peninsular) |
| Room | Habitación | |
| Room Type | Tipo de habitación | |
| Suite | Suite | |
| Review | Reseña | No "opinión" ni "valoración" |
| Rating | Calificación | |
| Check-in / Check-out | Check-in / Check-out | **Se mantienen en inglés**: es el uso del sector |
| Arrival / Departure | Llegada / Salida | En las fechas de la reserva |
| Night / Nights | Noche / Noches | |
| Sign In | Iniciar sesión | |
| Sign Up | Crear cuenta | No "Registrarse" |
| Sign Out | Cerrar sesión | |
| My Profile | Mi perfil | |
| My Reservations | Mis reservas | |
| Dashboard | Panel | No "tablero" ni "dashboard" |
| Occupancy | Ocupación | |
| Pricing | Tarifas | En el menú del admin |
| Rate (sustantivo) | Tarifa | |
| Operator | Operador | |
| Operations | Operaciones | |
| Account (de la estadía) | Cuenta | |
| Charge | Consumo | El cargo que se suma a la cuenta |
| Payment | Pago | |
| Outstanding balance | Saldo pendiente | |
| Availability | Disponibilidad | |
| Menu / Dish | Menú / Plato | |
| Service | Servicio | |
| Experience | Experiencia | |
| Virtual Tour | Tour virtual | |
| Concierge | Concierge | **Se mantiene**: es el nombre del asistente |
| Visit plan | Plan de visita | |
| Newsletter | Newsletter | **Se mantiene** |
| Save / Cancel / Delete / Edit | Guardar / Cancelar / Eliminar / Editar | |
| Back | Volver | |
| Loading… | Cargando… | |

## No se traduce

- La marca: **The Royale**.
- Los nombres de tipo de habitación y de servicio tal como están en la base de datos:
  "Presidential Suite", "Executive Room", "Wellness". Si llevan versión en español, sale del campo
  `nombreEs` de la entidad, no de los archivos de i18n.
- Los espacios del tour virtual ("Rooftop Pool", "Spa & Hammam", "Art Gallery").
- Topónimos y nombres propios: "Fifth Avenue", "Broadway", "Central Park", "Midtown Manhattan",
  "MoMA", "Rockefeller Center", "Art Déco".
- La moneda: los precios se muestran en formato en-US (`$1,250.00`) en los dos idiomas, por decisión
  de producto. Solo las fechas, los números y los porcentajes siguen el idioma activo.
