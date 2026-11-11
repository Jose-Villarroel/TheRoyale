import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ActualizarResena, CrearResena, FiltrosModeracion, Resena, ResumenResenas } from '../models/resena.model';
import { environment } from '../../environments/environment';

// Cliente HTTP de las reseñas de habitaciones (/api/resenas).
// Los errores de negocio llegan como { status, message } y se leen con err.error?.message.
@Injectable({ providedIn: 'root' })
export class ResenaService {
  private readonly url = `${environment.apiUrl}/resenas`;

  constructor(private http: HttpClient) {}

  // Público: solo las visibles del tipo, de la más reciente a la más antigua
  obtenerPorTipo(tipoHabitacionId: number): Observable<Resena[]> {
    const params = new HttpParams().set('tipoHabitacionId', tipoHabitacionId);
    return this.http.get<Resena[]>(this.url, { params });
  }

  // Las del huésped, incluidas las que el hotel ocultó (RN-8)
  obtenerPorCliente(clienteId: number): Observable<Resena[]> {
    const params = new HttpParams().set('clienteId', clienteId);
    return this.http.get<Resena[]>(this.url, { params });
  }

  // Admin: todas, visibles y ocultas, con filtros opcionales por tipo y calificación
  obtenerParaModeracion(filtros: FiltrosModeracion = {}): Observable<Resena[]> {
    let params = new HttpParams().set('incluirOcultas', true);
    if (filtros.tipoHabitacionId != null) {
      params = params.set('tipoHabitacionId', filtros.tipoHabitacionId);
    }
    if (filtros.calificacion != null) {
      params = params.set('calificacion', filtros.calificacion);
    }
    return this.http.get<Resena[]>(this.url, { params });
  }

  resumen(tipoHabitacionId: number): Observable<ResumenResenas> {
    const params = new HttpParams().set('tipoHabitacionId', tipoHabitacionId);
    return this.http.get<ResumenResenas>(`${this.url}/resumen`, { params });
  }

  // Un resumen por tipo, de una sola vez, para el listado /rooms
  resumenTodos(): Observable<ResumenResenas[]> {
    return this.http.get<ResumenResenas[]>(`${this.url}/resumen`);
  }

  // 409 si la reserva no está FINALIZADA o ya tiene reseña; 403 si no es del cliente
  crear(solicitud: CrearResena): Observable<Resena> {
    return this.http.post<Resena>(this.url, solicitud);
  }

  // Solo el autor (403 si el clienteId no coincide)
  actualizar(id: number, solicitud: ActualizarResena): Observable<Resena> {
    return this.http.put<Resena>(`${this.url}/${id}`, solicitud);
  }

  eliminar(id: number, clienteId: number): Observable<void> {
    const params = new HttpParams().set('clienteId', clienteId);
    return this.http.delete<void>(`${this.url}/${id}`, { params });
  }

  // ===== Moderación (admin) =====
  ocultar(id: number): Observable<Resena> {
    return this.http.post<Resena>(`${this.url}/${id}/ocultar`, {});
  }

  mostrar(id: number): Observable<Resena> {
    return this.http.post<Resena>(`${this.url}/${id}/mostrar`, {});
  }
}
