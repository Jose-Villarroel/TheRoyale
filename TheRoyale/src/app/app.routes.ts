import { Routes } from '@angular/router';
import { LandingComponent } from './landing/landing.component';
import { TiposHabitacionListaComponent } from './admin/tipos-habitacion-lista/tipos-habitacion-lista.component';
import { TiposHabitacionFormularioComponent } from './admin/tipos-habitacion-formulario/tipos-habitacion-formulario.component';

export const routes: Routes = [
  { path: '', component: LandingComponent },
  { path: 'admin/tipos-habitacion', component: TiposHabitacionListaComponent },
  { path: 'admin/tipos-habitacion/nuevo', component: TiposHabitacionFormularioComponent },
  { path: 'admin/tipos-habitacion/:id/editar', component: TiposHabitacionFormularioComponent }
];
