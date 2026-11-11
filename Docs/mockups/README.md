# Mockups — The Royale (AC3)

Cada pantalla de la SPA está asociada a su URL y al rol que puede alcanzarla.
Las imágenes de cada mockup se guardan en esta carpeta con el nombre indicado
en la columna *Archivo*.

## Pantallas públicas

| Pantalla | URL | Archivo |
|---|---|---|
| Landing | `/` | `01-landing.png` |
| Login de cliente | `/login` | `02-login.png` |
| Registro de cliente | `/register` | `03-register.png` |
| Listado de habitaciones | `/rooms` | `04-rooms.png` |
| Detalle de tipo de habitación | `/rooms/:id` | `05-room-detalle.png` |
| Servicios en tarjetas | `/services/cards` | `06-services-cards.png` |
| Servicios en tabla | `/services/table` | `07-services-table.png` |
| Detalle de servicio | `/services/:id` | `08-service-detalle.png` |
| Reservar (consultar disponibilidad) | `/book` | `09-book.png` |
| Login de operador | `/operator/login` | `10-operator-login.png` |

## Pantallas de cliente (requieren sesión de cliente)

| Pantalla | URL | Archivo |
|---|---|---|
| Mi perfil | `/profile` | `11-profile.png` |
| Mis reservas (activas e historial) | `/my-reservations` | `12-my-reservations.png` |
| Modificar una reserva | `/book?reservaId=:id` | `13-modify-reservation.png` |

## Pantallas de operador (requieren sesión de operador)

| Pantalla | URL | Archivo |
|---|---|---|
| Todas las reservas con filtros | `/operator/reservas` | `14-operator-reservas.png` |
| Detalle de reserva, cuenta y pagos | `/operator/reservas/:id` | `15-operator-detalle.png` |

## Pantallas de administrador

| Pantalla | URL | Archivo |
|---|---|---|
| Reservas | `/admin/reservas` · `/admin/reservas/:id` | `16-admin-reservas.png` |
| Habitaciones | `/admin/habitaciones` (+ `/nuevo`, `/:id/editar`) | `17-admin-habitaciones.png` |
| Tipos de habitación | `/admin/tipos-habitacion` (+ `/nuevo`, `/:id/editar`) | `18-admin-tipos.png` |
| Servicios | `/admin/servicios` (+ `/nuevo`, `/:id/editar`) | `19-admin-servicios.png` |
| Clientes | `/admin/clientes` (+ `/nuevo`, `/:id/editar`) | `20-admin-clientes.png` |
| Operadores | `/admin/operadores` (+ `/nuevo`, `/:id/editar`) | `21-admin-operadores.png` |

## Acceso por tipo de usuario

| Pantalla | Visitante | Cliente | Operador | Administrador |
|---|:---:|:---:|:---:|:---:|
| Landing, habitaciones, servicios | ✅ | ✅ | ✅ | ✅ |
| Consultar disponibilidad (`/book`) | ✅ | ✅ | ✅ | ✅ |
| Confirmar una reserva | ❌ | ✅ | ❌ | ❌ |
| Mi perfil, mis reservas | ❌ | ✅ | ❌ | ❌ |
| Portal de operaciones (`/operator/**`) | ❌ | ❌ | ✅ | ❌ |
| Panel de administración (`/admin/**`) | ❌ | ❌ | ❌ | ✅ |

El visitante puede recorrer todo el catálogo y consultar disponibilidad sin sesión;
solo se le exige iniciar sesión al confirmar la reserva o al abrir sus datos.

## Flujo de navegación

```text
Landing (/)
├── Rooms (/rooms) ───────── Room detail (/rooms/:id) ──┐
├── Services (/services/cards | /services/table) ──────┐│
│                             └── Service detail (/services/:id)
├── Book Now ──────────────── /book
│                              ├── sin sesión → /login?redirect=/book → vuelve a /book
│                              └── con sesión → reserva confirmada → /my-reservations
├── Sign In (/login) ──────── /profile
│                              └── My Reservations (/my-reservations)
│                                   ├── Modify → /book?reservaId=:id
│                                   └── Cancel (en la misma pantalla)
├── Sign Up (/register) ───── /login
├── Admin ─────────────────── /admin/tipos-habitacion y el resto del panel
└── Operator ──────────────── /operator/login
                               └── /operator/reservas
                                    └── /operator/reservas/:id
                                         ├── Confirm · Check-in · Check-out · Cancel
                                         ├── Agregar / eliminar servicios de la cuenta
                                         └── Registrar pagos
```
