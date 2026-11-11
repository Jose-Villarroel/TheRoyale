import { Component, inject, OnInit } from '@angular/core';
import { CabeceraComponent } from '../../shared/cabecera/cabecera.component';
import { MenuMovilComponent } from '../../shared/menu-movil/menu-movil.component';
import { PiePaginaComponent } from '../../shared/pie-pagina/pie-pagina.component';
import { HeroInteriorComponent } from '../../shared/hero-interior/hero-interior.component';
import { AlertaErrorComponent } from '../../shared/alerta-error/alerta-error.component';
import { NavAdminComponent } from '../secciones/nav-admin/nav-admin.component';
import { EncabezadoClientesComponent } from './secciones/encabezado-clientes/encabezado-clientes.component';
import { GridClientesComponent } from './secciones/grid-clientes/grid-clientes.component';
import { ClienteService } from '../../services/cliente.service';
import { Cliente } from '../../models/cliente.model';

// Página contenedora: trae los datos del servicio y maneja el borrado.
// La parte visual vive en los componentes de secciones/.
@Component({
  selector: 'app-clientes-lista',
  standalone: true,
  imports: [
    CabeceraComponent,
    MenuMovilComponent,
    PiePaginaComponent,
    HeroInteriorComponent,
    AlertaErrorComponent,
    NavAdminComponent,
    EncabezadoClientesComponent,
    GridClientesComponent
  ],
  templateUrl: './clientes-lista.component.html'
})
export class ClientesListaComponent implements OnInit {
  private clienteService = inject(ClienteService);

  clientes: Cliente[] = [];
  error: string | null = null;
  menuAbierto = false;

  ngOnInit(): void {
    this.cargarClientes();
  }

  eliminar(cliente: Cliente): void {
    if (!confirm('Delete this client?')) {
      return;
    }
    this.clienteService.eliminar(cliente.id).subscribe({
      next: () => {
        this.error = null;
        this.cargarClientes();
      },
      error: (err) => {
        // 409 si el cliente tiene reservas asociadas
        this.error = err.error?.message || err.message || 'Error deleting client';
      }
    });
  }

  private cargarClientes(): void {
    this.clienteService.obtenerTodos().subscribe({
      next: (clientes) => {
        this.clientes = clientes;
      },
      error: (err) => {
        this.error = err.error?.message || err.message || 'Error loading clients';
      }
    });
  }
}
