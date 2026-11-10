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

const CLAVE_SESION = 'theroyale.usuario';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly url = `${environment.apiUrl}/auth`;
  private _usuario: LoginResponse | null = null;

  constructor(private http: HttpClient) {
    this._usuario = this.leerSesion();
  }

  login(credenciales: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.url}/login`, credenciales).pipe(
      tap(usuario => this.guardarSesion(usuario))
    );
  }

  loginConGoogle(email: string, nombre: string, apellido: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.url}/google`, { email, nombre, apellido }).pipe(
      tap(usuario => this.guardarSesion(usuario))
    );
  }

  actualizarUsuario(datos: Pick<LoginResponse, 'nombre' | 'apellido' | 'email'>): void {
    if (!this._usuario) {
      return;
    }
    this.guardarSesion({ ...this._usuario, ...datos });
  }

  logout(): void {
    this._usuario = null;
    try {
      sessionStorage.removeItem(CLAVE_SESION);
    } catch {
      // Almacenamiento bloqueado
    }
  }

  get usuario(): LoginResponse | null {
    return this._usuario;
  }

  get estaAutenticado(): boolean {
    return this._usuario !== null;
  }

  get esCliente(): boolean {
    return this._usuario?.tipo === 'CLIENTE';
  }

  get esOperador(): boolean {
    return this._usuario?.tipo === 'OPERADOR';
  }

  get esAdmin(): boolean {
    return this._usuario?.tipo === 'ADMIN';
  }

  private guardarSesion(usuario: LoginResponse): void {
    this._usuario = usuario;
    try {
      sessionStorage.setItem(CLAVE_SESION, JSON.stringify(usuario));
    } catch {}
  }

  private leerSesion(): LoginResponse | null {
    try {
      const guardado = sessionStorage.getItem(CLAVE_SESION);
      return guardado ? JSON.parse(guardado) as LoginResponse : null;
    } catch {
      return null;
    }
  }
}
