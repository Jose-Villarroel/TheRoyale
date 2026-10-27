import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Operador } from '../models/operador.model';
import { environment } from '../../environments/environment';

// Cliente HTTP del CRUD de operadores (/api/operadores) que usa el panel admin.
@Injectable({ providedIn: 'root' })
export class OperadorService {
  private readonly url = `${environment.apiUrl}/operadores`;

  constructor(private http: HttpClient) {}

  obtenerTodos(): Observable<Operador[]> {
    return this.http.get<Operador[]>(this.url);
  }

  obtenerPorId(id: number): Observable<Operador> {
    return this.http.get<Operador>(`${this.url}/${id}`);
  }

  crear(operador: Omit<Operador, 'id'>): Observable<Operador> {
    return this.http.post<Operador>(this.url, operador);
  }

  actualizar(id: number, operador: Partial<Operador>): Observable<Operador> {
    return this.http.put<Operador>(`${this.url}/${id}`, operador);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
