import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  tipo: string;
  id: number;
  nombre: string;
  apellido: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly url = `${environment.apiUrl}/auth`;
  private _usuario: LoginResponse | null = null;

  constructor(private http: HttpClient) {
    // Recupera sesión de localStorage si existe
    const guardado = localStorage.getItem('theroyale_usuario');
    if (guardado) {
      try { this._usuario = JSON.parse(guardado); } catch { this._usuario = null; }
    }
  }

  login(credenciales: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.url}/login`, credenciales).pipe(
      tap(usuario => {
        this._usuario = usuario;
        localStorage.setItem('theroyale_usuario', JSON.stringify(usuario));
      })
    );
  }

  logout(): void {
    this._usuario = null;
    localStorage.removeItem('theroyale_usuario');
  }

  get usuario(): LoginResponse | null {
    return this._usuario;
  }

  get estaAutenticado(): boolean {
    return this._usuario !== null;
  }
}
