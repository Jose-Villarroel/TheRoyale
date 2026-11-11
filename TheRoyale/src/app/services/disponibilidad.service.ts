import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DisponibilidadTipo } from '../models/reserva.model';
import { environment } from '../../environments/environment';

// Consulta cuántas habitaciones libres quedan por tipo entre dos fechas (paso 02 de la reserva).
@Injectable({ providedIn: 'root' })
export class DisponibilidadService {
  private readonly url = `${environment.apiUrl}/disponibilidad`;

  constructor(private http: HttpClient) {}

  // Las fechas van en formato yyyy-MM-dd, que es lo que lee LocalDate en el backend
  consultar(fechaInicio: string, fechaFin: string): Observable<DisponibilidadTipo[]> {
    const params = new HttpParams()
      .set('fechaInicio', fechaInicio)
      .set('fechaFin', fechaFin);
    return this.http.get<DisponibilidadTipo[]>(this.url, { params });
  }
}
