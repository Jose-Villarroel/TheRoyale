import { Routes } from '@angular/router';
import { LandingComponent } from './landing/landing.component';
import { TiposHabitacionListaComponent } from './admin/tipos-habitacion-lista/tipos-habitacion-lista.component';
import { TiposHabitacionFormularioComponent } from './admin/tipos-habitacion-formulario/tipos-habitacion-formulario.component';
import { RoomsListaComponent } from './rooms/rooms-lista/rooms-lista.component';
import { RoomDetalleComponent } from './rooms/room-detalle/room-detalle.component';
import { ServiciosListaComponent } from './admin/servicios-lista/servicios-lista.component';
import { ServiciosFormularioComponent } from './admin/servicios-formulario/servicios-formulario.component';
import { ServiciosTablaComponent } from './servicios/servicios-tabla/servicios-tabla.component';
import { ServiciosTarjetasComponent } from './servicios/servicios-tarjetas/servicios-tarjetas.component';
import { ServicioDetalleComponent } from './servicios/servicio-detalle/servicio-detalle.component';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { PerfilComponent } from './perfil/perfil/perfil.component';
import { ReservaFormularioComponent } from './reservas/reserva-formulario/reserva-formulario.component';
import { ReservasListaComponent } from './admin/reservas-lista/reservas-lista.component';
import { ReservaDetalleComponent } from './admin/reserva-detalle/reserva-detalle.component';
import { HabitacionesListaComponent } from './admin/habitaciones-lista/habitaciones-lista.component';
import { HabitacionesFormularioComponent } from './admin/habitaciones-formulario/habitaciones-formulario.component';
import { ClientesListaComponent } from './admin/clientes-lista/clientes-lista.component';
import { ClientesFormularioComponent } from './admin/clientes-formulario/clientes-formulario.component';
import { OperadoresListaComponent } from './admin/operadores-lista/operadores-lista.component';
import { OperadoresFormularioComponent } from './admin/operadores-formulario/operadores-formulario.component';
import { MisReservasComponent } from './reservas/mis-reservas/mis-reservas.component';
import { LoginOperadorComponent } from './operador/login-operador/login-operador.component';
import { ReservasOperadorComponent } from './operador/reservas/reservas-operador.component';
import { ReservaDetalleOperadorComponent } from './operador/reserva-detalle/reserva-detalle-operador.component';
import { clienteGuard } from './guards/cliente.guard';
import { operadorGuard } from './guards/operador.guard';
import { BuscarHabitacionComponent } from './operador/buscar-habitacion/buscar-habitacion.component';

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'profile', component: PerfilComponent, canActivate: [clienteGuard] },
  { path: 'my-reservations', component: MisReservasComponent, canActivate: [clienteGuard] },
  // Consultar disponibilidad es público; la sesión solo hace falta para confirmar
  { path: 'book', component: ReservaFormularioComponent },

  { path: 'rooms', component: RoomsListaComponent },
  { path: 'rooms/:id', component: RoomDetalleComponent },
  // Las rutas fijas van antes de 'services/:id' o se intentarian resolver como un id
  { path: 'services/table', component: ServiciosTablaComponent },
  { path: 'services/cards', component: ServiciosTarjetasComponent },
  { path: 'services/:id', component: ServicioDetalleComponent },
  // Ver todas las reservas es gestión, no algo que el cliente necesite desde el sitio.
  // No hay login de administrador: el personal del hotel (operador) es quien administra.
  { path: 'admin/reservas', component: ReservasListaComponent, canActivate: [operadorGuard] },
  { path: 'admin/reservas/:id', component: ReservaDetalleComponent, canActivate: [operadorGuard] },
  { path: 'admin/habitaciones', component: HabitacionesListaComponent, canActivate: [operadorGuard] },
  { path: 'admin/habitaciones/nuevo', component: HabitacionesFormularioComponent, canActivate: [operadorGuard] },
  { path: 'admin/habitaciones/:id/editar', component: HabitacionesFormularioComponent, canActivate: [operadorGuard] },
  { path: 'admin/tipos-habitacion', component: TiposHabitacionListaComponent, canActivate: [operadorGuard] },
  { path: 'admin/tipos-habitacion/nuevo', component: TiposHabitacionFormularioComponent, canActivate: [operadorGuard] },
  { path: 'admin/tipos-habitacion/:id/editar', component: TiposHabitacionFormularioComponent, canActivate: [operadorGuard] },
  { path: 'admin/servicios', component: ServiciosListaComponent, canActivate: [operadorGuard] },
  { path: 'admin/servicios/nuevo', component: ServiciosFormularioComponent, canActivate: [operadorGuard] },
  { path: 'admin/servicios/:id/editar', component: ServiciosFormularioComponent, canActivate: [operadorGuard] },
  { path: 'admin/clientes', component: ClientesListaComponent, canActivate: [operadorGuard] },
  { path: 'admin/clientes/nuevo', component: ClientesFormularioComponent, canActivate: [operadorGuard] },
  { path: 'admin/clientes/:id/editar', component: ClientesFormularioComponent, canActivate: [operadorGuard] },
  { path: 'admin/operadores', component: OperadoresListaComponent, canActivate: [operadorGuard] },
  { path: 'admin/operadores/nuevo', component: OperadoresFormularioComponent, canActivate: [operadorGuard] },
  { path: 'admin/operadores/:id/editar', component: OperadoresFormularioComponent, canActivate: [operadorGuard] },

  // ===== Portal del operador: entra por su propia puerta, no por el login de huéspedes =====
  { path: 'operator/login', component: LoginOperadorComponent },
  { path: 'operator/reservas', component: ReservasOperadorComponent, canActivate: [operadorGuard] },
  { path: 'operator/reservas/:id', component: ReservaDetalleOperadorComponent, canActivate: [operadorGuard] },
  { path: 'operator/buscar-habitacion', component: BuscarHabitacionComponent, canActivate: [operadorGuard] },
  { path: 'operator', redirectTo: 'operator/reservas', pathMatch: 'full' },

  { path: '**', redirectTo: '' }
];
