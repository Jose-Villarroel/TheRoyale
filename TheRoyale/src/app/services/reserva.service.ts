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

  crear(solicitud: CrearReservaRequest): Observable<Reserva> {
    return this.http.post<Reserva>(this.url, solicitud);
  }
}
