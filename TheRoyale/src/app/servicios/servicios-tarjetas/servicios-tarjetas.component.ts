import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { CtaInteriorComponent } from '../../shared/cta-interior/cta-interior.component';
import { BarraVistasComponent } from '../secciones/barra-vistas/barra-vistas.component';
import { TarjetaServicioItemComponent } from './secciones/tarjeta-servicio-item/tarjeta-servicio-item.component';
import { ServicioService } from '../../services/servicio.service';
import { Servicio } from '../../models/servicio.model';

// Listado público de servicios en tarjetas (/services/cards), el servicios-tarjetas.html del backend
@Component({
  selector: 'app-servicios-tarjetas',
  standalone: true,
  imports: [
    RouterLink,
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    CtaInteriorComponent,
    BarraVistasComponent,
    TarjetaServicioItemComponent
  ],
  templateUrl: './servicios-tarjetas.component.html'
})
export class ServiciosTarjetasComponent {
  servicios: Servicio[] = inject(ServicioService).obtenerTodos();
  menuAbierto = false;
}
