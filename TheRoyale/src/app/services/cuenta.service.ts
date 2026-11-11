import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AgregarConsumoRequest, Cuenta, RegistrarPagoRequest } from '../models/cuenta.model';
import { environment } from '../../environments/environment';

// La cuenta cuelga de su reserva: /api/reservas/{reservaId}/cuenta
@Injectable({ providedIn: 'root' })
export class CuentaService {
  private readonly url = `${environment.apiUrl}/reservas`;

  constructor(private http: HttpClient) {}

  // 404 cuando la reserva todavía no tiene cuenta (se abre al confirmarla)
  obtenerPorReserva(reservaId: number): Observable<Cuenta> {
    return this.http.get<Cuenta>(`${this.url}/${reservaId}/cuenta`);
  }

  agregarConsumo(reservaId: number, solicitud: AgregarConsumoRequest): Observable<Cuenta> {
    return this.http.post<Cuenta>(`${this.url}/${reservaId}/cuenta/consumos`, solicitud);
  }

  eliminarConsumo(reservaId: number, itemId: number): Observable<Cuenta> {
    return this.http.delete<Cuenta>(`${this.url}/${reservaId}/cuenta/consumos/${itemId}`);
  }

  registrarPago(reservaId: number, solicitud: RegistrarPagoRequest): Observable<Cuenta> {
    return this.http.post<Cuenta>(`${this.url}/${reservaId}/cuenta/pagos`, solicitud);
  }
}
