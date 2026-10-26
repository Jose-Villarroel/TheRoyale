import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CrearReservaRequest, EstadoReserva, Reserva } from '../models/reserva.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ReservaService {
  private readonly url = `${environment.apiUrl}/reservas`;

  constructor(private http: HttpClient) {}

  // Sin estado trae todas las reservas, no solo las del cliente autenticado
  obtenerTodas(estado?: EstadoReserva | null): Observable<Reserva[]> {
    const params = estado ? new HttpParams().set('estado', estado) : undefined;
    return this.http.get<Reserva[]>(this.url, { params });
  }

  obtenerPorId(id: number): Observable<Reserva> {
    return this.http.get<Reserva>(`${this.url}/${id}`);
  }

  // Busca la reserva activa (CONFIRMADA o EN_CURSO) por numero de habitacion.
  // El backend devuelve 404 si no hay ninguna activa para ese numero.
  buscarPorHabitacion(numero: string): Observable<Reserva> {
    const params = new HttpParams().set('numero', numero);
    return this.http.get<Reserva>(`${this.url}/buscar-habitacion`, { params });
  }

  // activas = true -> vigentes (AC16); false -> historial (AC17)
  obtenerPorCliente(clienteId: number, activas: boolean): Observable<Reserva[]> {
    const params = new HttpParams()
      .set('clienteId', clienteId)
      .set('activas', activas);
    return this.http.get<Reserva[]>(this.url, { params });
  }

  crear(solicitud: CrearReservaRequest): Observable<Reserva> {
    return this.http.post<Reserva>(this.url, solicitud);
  }

  // Reescribe la reserva: el backend solo la mueve si hay habitación libre con los datos nuevos
  modificar(id: number, solicitud: CrearReservaRequest): Observable<Reserva> {
    return this.http.put<Reserva>(`${this.url}/${id}`, solicitud);
  }

  // ===== Transiciones del ciclo de vida (409 si el estado o el saldo no lo permiten) =====
  confirmar(id: number): Observable<Reserva> {
    return this.http.post<Reserva>(`${this.url}/${id}/confirmar`, {});
  }

  cancelar(id: number): Observable<Reserva> {
    return this.http.post<Reserva>(`${this.url}/${id}/cancelar`, {});
  }

  checkIn(id: number): Observable<Reserva> {
    return this.http.post<Reserva>(`${this.url}/${id}/check-in`, {});
  }

  checkOut(id: number): Observable<Reserva> {
    return this.http.post<Reserva>(`${this.url}/${id}/check-out`, {});
  }
}
