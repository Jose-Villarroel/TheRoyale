import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TipoHabitacion } from '../models/tipo-habitacion.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TipoHabitacionService {
  private readonly url = `${environment.apiUrl}/tipos-habitacion`;

  constructor(private http: HttpClient) {}

  obtenerTodos(): Observable<TipoHabitacion[]> {
    return this.http.get<TipoHabitacion[]>(this.url);
  }

  listarTodos(): Observable<TipoHabitacion[]> {
    return this.obtenerTodos();
  }

  obtenerPorId(id: number): Observable<TipoHabitacion> {
    return this.http.get<TipoHabitacion>(`${this.url}/${id}`);
  }

  crear(tipo: Omit<TipoHabitacion, 'id'>): Observable<TipoHabitacion> {
    return this.http.post<TipoHabitacion>(this.url, tipo);
  }

  actualizar(id: number, tipo: TipoHabitacion): Observable<TipoHabitacion> {
    return this.http.put<TipoHabitacion>(`${this.url}/${id}`, tipo);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
