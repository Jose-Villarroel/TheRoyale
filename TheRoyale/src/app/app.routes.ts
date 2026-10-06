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

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'rooms/:id', component: RoomDetalleComponent },
  // Las rutas fijas van antes de 'services/:id' o se intentarian resolver como un id
  { path: 'services/table', component: ServiciosTablaComponent },
  { path: 'services/cards', component: ServiciosTarjetasComponent },
  { path: 'services/:id', component: ServicioDetalleComponent },
  { path: 'admin/tipos-habitacion', component: TiposHabitacionListaComponent },
  { path: 'admin/tipos-habitacion/nuevo', component: TiposHabitacionFormularioComponent },
  { path: 'admin/tipos-habitacion/:id/editar', component: TiposHabitacionFormularioComponent },
  { path: 'admin/servicios', component: ServiciosListaComponent },
  { path: 'admin/servicios/nuevo', component: ServiciosFormularioComponent },
  { path: 'admin/servicios/:id/editar', component: ServiciosFormularioComponent }
];
