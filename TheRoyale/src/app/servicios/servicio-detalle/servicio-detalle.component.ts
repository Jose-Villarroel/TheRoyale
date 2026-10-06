import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { ImagenDetalleComponent } from '../../shared/imagen-detalle/imagen-detalle.component';
import { CtaInteriorComponent } from '../../shared/cta-interior/cta-interior.component';
import { InfoServicioComponent } from './secciones/info-servicio/info-servicio.component';
import { GaleriaServicioComponent } from './secciones/galeria-servicio/galeria-servicio.component';
import { ServicioNoEncontradoComponent } from './secciones/servicio-no-encontrado/servicio-no-encontrado.component';
import { ServicioService } from '../../services/servicio.service';
import { Servicio } from '../../models/servicio.model';

// Página contenedora del detalle de un servicio (/services/:id).
// Equivale a ServicioController.mostrarDetalle: si el servicio no existe se
// muestra la pantalla de no encontrado, igual que servicio-no-encontrado.html.
@Component({
  selector: 'app-servicio-detalle',
  standalone: true,
  imports: [
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    ImagenDetalleComponent,
    CtaInteriorComponent,
    InfoServicioComponent,
    GaleriaServicioComponent,
    ServicioNoEncontradoComponent
  ],
  templateUrl: './servicio-detalle.component.html'
})
export class ServicioDetalleComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private servicioService = inject(ServicioService);

  servicio: Servicio | null = null;
  menuAbierto = false;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.servicio = this.servicioService.obtenerPorId(id) ?? null;
  }
}
