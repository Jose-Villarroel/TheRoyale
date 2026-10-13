import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Habitacion } from '../models/habitacion.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class HabitacionService {
  private readonly url = `${environment.apiUrl}/habitaciones`;

  constructor(private http: HttpClient) {}

  obtenerTodos(): Observable<Habitacion[]> {
    return this.http.get<Habitacion[]>(this.url);
  }

  obtenerPorTipoHabitacionId(tipoHabitacionId: number): Observable<Habitacion[]> {
    return this.http.get<Habitacion[]>(`${this.url}?tipoHabitacionId=${tipoHabitacionId}`);
  }

  obtenerPorId(id: number): Observable<Habitacion> {
    return this.http.get<Habitacion>(`${this.url}/${id}`);
  }

  crear(habitacion: Omit<Habitacion, 'id'>): Observable<Habitacion> {
    return this.http.post<Habitacion>(this.url, habitacion);
  }

  actualizar(id: number, habitacion: Habitacion): Observable<Habitacion> {
    return this.http.put<Habitacion>(`${this.url}/${id}`, habitacion);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
