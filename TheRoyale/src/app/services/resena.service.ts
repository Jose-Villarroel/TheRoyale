import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CrearResenaRequest, Resena, ResumenResenas } from '../models/resena.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ResenaService {
  private readonly url = `${environment.apiUrl}/resenas`;

  constructor(private http: HttpClient) {}

  obtenerResumenPorTipo(tipoId: number): Observable<ResumenResenas> {
    return this.http.get<ResumenResenas>(`${this.url}/tipo/${tipoId}`);
  }

  obtenerRecientes(): Observable<Resena[]> {
    return this.http.get<Resena[]>(`${this.url}/recientes`);
  }

  crearResena(request: CrearResenaRequest): Observable<Resena> {
    return this.http.post<Resena>(this.url, request);
  }
}
