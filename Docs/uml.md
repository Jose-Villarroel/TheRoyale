# Modelo de Clases

## Diagrama de Clases

Este diagrama representa la estructura principal del sistema de hotel para el proyecto The Royale. Se organiza en tres roles principales: el cliente, que realiza reservas; el operador, que gestiona reservas, consumos y pagos; y el administrador, que administra habitaciones, servicios y personal.

La relación entre `Cliente` y `Reserva` muestra que un cliente puede tener muchas reservas, mientras que cada reserva está asociada a una habitación y puede generar varios consumos. `Habitacion` y `TipoHabitacion` definen la disponibilidad y el costo de la estancia, y `Servicio` e `ItemConsumo` permiten registrar los servicios adicionales dentro de la cuenta del huésped.

El diagrama también refleja el flujo operativo del hotel: el operador puede cancelar reservas, registrar pagos y realizar check-out, mientras que el administrador mantiene la configuración del sistema y del inventario del hotel.

```plantuml
@startuml DiagramaClasesHotelAlcaravan

skinparam classAttributeIconSize 0

class Cliente {
  -id: int
  -nombre: string
  -email: string
  -password: string
  -telefono: string
  -fechaRegistro: date
  +registrarse(datos): boolean
  +login(email, password): boolean
  +actualizarInfo(datos): void
  +verReservasActivas(): List<Reserva>
  +verHistorialReservas(): List<Reserva>
}

class Operador {
  -id: int
  -nombre: string
  -email: string
  -password: string
  +login(email, password): boolean
  +verReservas(filtro): List<Reserva>
  +cancelarReserva(idReserva): void
  +agregarServicioACuenta(idReserva, idServicio, cantidad): ItemConsumo
  +eliminarItemCuenta(idItem): void
  +registrarPago(idReserva): void
  +realizarCheckOut(idReserva): boolean
}

class Administrador {
  -id: int
  -nombre: string
  -email: string
  -password: string
  +login(email, password): boolean
  +crearOperador(datos): Operador
  +eliminarOperador(id): void
  +crearServicio(datos): Servicio
  +actualizarServicio(id, datos): void
  +eliminarServicio(id): void
  +crearHabitacion(datos): Habitacion
  +eliminarHabitacion(id): void
  +deshabilitarHabitacion(id): void
}

class TipoHabitacion {
  -id: int
  -nombre: string
  -descripcion: string
  -capacidadMaxima: int
  -precioPorNoche: decimal
}

class Habitacion {
  -id: int
  -numero: string
  -estado: string
  +estaDisponible(fechaInicio, fechaFin): boolean
  +habilitar(): void
  +deshabilitar(): void
}

class Reserva {
  -id: int
  -fechaInicio: date
  -fechaFin: date
  -cantidadPersonas: int
  -estado: string
  -fechaCreacion: date
  +cancelar(): void
  +modificar(nuevaFechaInicio, nuevaFechaFin, nuevoTipoHabitacion, nuevaCantidad): boolean
  +calcularCostoHabitacion(): decimal
  +calcularSaldoPendiente(): decimal
}

class Servicio {
  -id: int
  -nombre: string
  -descripcion: string
  -precio: decimal
  -categoria: string
  -activo: boolean
}

class ItemConsumo {
  -id: int
  -cantidad: int
  -fechaHora: datetime
  -precioUnitario: decimal
  -pagado: boolean
  +calcularSubtotal(): decimal
}

' ---- Relaciones ----
Cliente "1" -- "0..*" Reserva : realiza >
TipoHabitacion "1" -- "0..*" Habitacion : clasifica >
Habitacion "1" -- "0..*" Reserva : es reservada en >
Reserva "1" -- "0..*" ItemConsumo : genera >
Servicio "1" -- "0..*" ItemConsumo : es consumido como >
Operador "1" -- "0..*" ItemConsumo : registra >
Operador "1" -- "0..*" Reserva : cancela / gestiona >

Administrador ..> Operador : administra (CRUD)
Administrador ..> Servicio : administra (CRUD)
Administrador ..> Habitacion : administra (CRUD)
Administrador ..> TipoHabitacion : administra

@enduml

```
