import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ServicioService } from '../../services/servicio.service';
import { Servicio } from '../../models/servicio.model';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../secciones/alerta-error/alerta-error.component';

@Component({
  selector: 'app-servicios-formulario',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent
  ],
  templateUrl: './servicios-formulario.component.html'
})
export class ServiciosFormularioComponent implements OnInit {

  // Si id es null el guardado crea; si tiene id, actualiza
  id: number | null = null;
  error: string | null = null;
  menuAbierto = false;

  // ===== Reactive Form: caracteristicas/galeriaUrls viajan como texto, una línea por ítem =====
  formulario: FormGroup;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private servicioService: ServicioService
  ) {
    this.formulario = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3)]],
      descripcion: ['', [Validators.required, Validators.minLength(10)]],
      precio: [null, [Validators.required, Validators.min(1)]],
      imagenUrl: [''],
      caracteristicasTexto: [''],
      galeriaUrlsTexto: ['']
    });
  }

  // ===== Getters cortos para usar en el template sin repetir formulario.get(...) =====
  get nombre() { return this.formulario.get('nombre')!; }
  get descripcion() { return this.formulario.get('descripcion')!; }
  get precio() { return this.formulario.get('precio')!; }

  ngOnInit(): void {
    const parametroId = this.route.snapshot.paramMap.get('id');
    if (parametroId === null) {
      return;
    }

    const servicio = this.servicioService.obtenerPorId(Number(parametroId));
    if (!servicio) {
      this.router.navigate(['/admin/servicios']);
      return;
    }

    this.id = servicio.id;
    this.formulario.patchValue({
      nombre: servicio.nombre,
      descripcion: servicio.descripcion,
      precio: servicio.precio,
      imagenUrl: servicio.imagenUrl,
      caracteristicasTexto: servicio.caracteristicas.join('\n'),
      galeriaUrlsTexto: servicio.galeriaUrls.join('\n')
    });
  }

  guardar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const valores = this.formulario.value;
    const datos = {
      nombre: valores.nombre,
      descripcion: valores.descripcion,
      precio: Number(valores.precio),
      imagenUrl: valores.imagenUrl ?? '',
      caracteristicas: this.lineasAArray(valores.caracteristicasTexto),
      galeriaUrls: this.lineasAArray(valores.galeriaUrlsTexto)
    };

    try {
      if (this.id === null) {
        this.servicioService.crear(datos);
      } else {
        const servicio: Servicio = { id: this.id, ...datos };
        this.servicioService.actualizar(servicio);
      }
      this.router.navigate(['/admin/servicios']);
    } catch (ex) {
      this.error = (ex as Error).message;
    }
  }

  private lineasAArray(texto: string): string[] {
    return (texto ?? '')
      .split('\n')
      .map(linea => linea.trim())
      .filter(linea => linea !== '');
  }
}