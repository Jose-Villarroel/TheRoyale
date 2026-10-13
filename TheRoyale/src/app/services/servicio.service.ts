import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Servicio } from '../models/servicio.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ServicioService {
  private readonly url = `${environment.apiUrl}/servicios`;

  constructor(private http: HttpClient) {}

  obtenerTodos(): Observable<Servicio[]> {
    return this.http.get<Servicio[]>(this.url);
  }

  obtenerPorId(id: number): Observable<Servicio> {
    return this.http.get<Servicio>(`${this.url}/${id}`);
  }

  crear(servicio: Omit<Servicio, 'id'>): Observable<Servicio> {
    return this.http.post<Servicio>(this.url, servicio);
  }

  actualizar(id: number, servicio: Servicio): Observable<Servicio> {
    return this.http.put<Servicio>(`${this.url}/${id}`, servicio);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}