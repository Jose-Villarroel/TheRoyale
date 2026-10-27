import { Component, inject, OnInit } from '@angular/core';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../shared/alerta-error/alerta-error.component';
import { NavAdminComponent } from '../secciones/nav-admin/nav-admin.component';
import { EncabezadoOperadoresComponent } from './secciones/encabezado-operadores/encabezado-operadores.component';
import { GridOperadoresComponent } from './secciones/grid-operadores/grid-operadores.component';
import { OperadorService } from '../../services/operador.service';
import { Operador } from '../../models/operador.model';

// Página contenedora: trae los datos del servicio y maneja el borrado.
// La parte visual vive en los componentes de secciones/.
@Component({
  selector: 'app-operadores-lista',
  standalone: true,
  imports: [
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    NavAdminComponent,
    EncabezadoOperadoresComponent,
    GridOperadoresComponent
  ],
  templateUrl: './operadores-lista.component.html'
})
export class OperadoresListaComponent implements OnInit {
  private operadorService = inject(OperadorService);

  operadores: Operador[] = [];
  error: string | null = null;
  menuAbierto = false;

  ngOnInit(): void {
    this.cargarOperadores();
  }

  eliminar(operador: Operador): void {
    if (!confirm('Delete this operator?')) {
      return;
    }
    this.operadorService.eliminar(operador.id).subscribe({
      next: () => {
        this.error = null;
        this.cargarOperadores();
      },
      error: (err) => {
        // 409 si el operador tiene consumos o pagos registrados
        this.error = err.error?.message || err.message || 'Error deleting operator';
      }
    });
  }

  private cargarOperadores(): void {
    this.operadorService.obtenerTodos().subscribe({
      next: (operadores) => {
        this.operadores = operadores;
      },
      error: (err) => {
        this.error = err.error?.message || err.message || 'Error loading operators';
      }
    });
  }
}
