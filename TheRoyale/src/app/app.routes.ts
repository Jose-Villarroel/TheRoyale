import { Routes } from '@angular/router';
import { LandingComponent } from './landing/landing.component';
import { TiposHabitacionListaComponent } from './admin/tipos-habitacion-lista/tipos-habitacion-lista.component';
import { TiposHabitacionFormularioComponent } from './admin/tipos-habitacion-formulario/tipos-habitacion-formulario.component';
import { RoomDetalleComponent } from './rooms/room-detalle/room-detalle.component';
import { ServiciosListaComponent } from './admin/servicios-lista/servicios-lista.component';
import { ServiciosFormularioComponent } from './admin/servicios-formulario/servicios-formulario.component';

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'rooms/:id', component: RoomDetalleComponent },
  { path: 'admin/tipos-habitacion', component: TiposHabitacionListaComponent },
  { path: 'admin/tipos-habitacion/nuevo', component: TiposHabitacionFormularioComponent },
  { path: 'admin/tipos-habitacion/:id/editar', component: TiposHabitacionFormularioComponent },
  { path: 'admin/servicios', component: ServiciosListaComponent },
  { path: 'admin/servicios/nuevo', component: ServiciosFormularioComponent },
  { path: 'admin/servicios/:id/editar', component: ServiciosFormularioComponent }
];
