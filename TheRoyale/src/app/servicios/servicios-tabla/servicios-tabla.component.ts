import { Component, inject } from '@angular/core';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { CtaInteriorComponent } from '../../shared/cta-interior/cta-interior.component';
import { BarraVistasComponent } from '../secciones/barra-vistas/barra-vistas.component';
import { FilaServicioComponent } from './secciones/fila-servicio/fila-servicio.component';
import { ServicioService } from '../../services/servicio.service';
import { Servicio } from '../../models/servicio.model';

// Listado público de servicios en tabla (/services/table), el servicios-tabla.html del backend
@Component({
  selector: 'app-servicios-tabla',
  standalone: true,
  imports: [
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    CtaInteriorComponent,
    BarraVistasComponent,
    FilaServicioComponent
  ],
  templateUrl: './servicios-tabla.component.html'
})
export class ServiciosTablaComponent {
  servicios: Servicio[] = inject(ServicioService).obtenerTodos();
  menuAbierto = false;
}
