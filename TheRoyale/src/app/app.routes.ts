import { Routes } from '@angular/router';
import { LandingComponent } from './landing/landing.component';
import { TiposHabitacionListaComponent } from './admin/tipos-habitacion-lista/tipos-habitacion-lista.component';
import { TiposHabitacionFormularioComponent } from './admin/tipos-habitacion-formulario/tipos-habitacion-formulario.component';
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
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'profile', component: PerfilComponent, canActivate: [authGuard] },
  // Consultar disponibilidad es público; la sesión solo hace falta para confirmar
  { path: 'book', component: ReservaFormularioComponent },

  { path: 'rooms/:id', component: RoomDetalleComponent },
  // Las rutas fijas van antes de 'services/:id' o se intentarian resolver como un id
  { path: 'services/table', component: ServiciosTablaComponent },
  { path: 'services/cards', component: ServiciosTarjetasComponent },
  { path: 'services/:id', component: ServicioDetalleComponent },
  // Ver todas las reservas es gestión, no algo que el cliente necesite desde el sitio
  { path: 'admin/reservas', component: ReservasListaComponent },
  { path: 'admin/reservas/:id', component: ReservaDetalleComponent },
  { path: 'admin/habitaciones', component: HabitacionesListaComponent },
  { path: 'admin/habitaciones/nuevo', component: HabitacionesFormularioComponent },
  { path: 'admin/habitaciones/:id/editar', component: HabitacionesFormularioComponent },
  { path: 'admin/tipos-habitacion', component: TiposHabitacionListaComponent },
  { path: 'admin/tipos-habitacion/nuevo', component: TiposHabitacionFormularioComponent },
  { path: 'admin/tipos-habitacion/:id/editar', component: TiposHabitacionFormularioComponent },
  { path: 'admin/servicios', component: ServiciosListaComponent },
  { path: 'admin/servicios/nuevo', component: ServiciosFormularioComponent },
  { path: 'admin/servicios/:id/editar', component: ServiciosFormularioComponent },
  { path: 'admin/clientes', component: ClientesListaComponent },
  { path: 'admin/clientes/nuevo', component: ClientesFormularioComponent },
  { path: 'admin/clientes/:id/editar', component: ClientesFormularioComponent }
];
