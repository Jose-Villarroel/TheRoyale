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

  // La sesión se guarda en sessionStorage: sin esto, recargar la página cerraba la sesión
  // y cualquier recorrido de varios pasos (reservar, cobrar, hacer check-out) se perdía.
  constructor(private http: HttpClient) {
    this._usuario = this.leerSesion();
  }

  login(credenciales: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.url}/login`, credenciales).pipe(
      tap(usuario => this.guardarSesion(usuario))
    );
  }

  // Tras editar el perfil se refrescan nombre/email guardados, si no la cabecera mostraría los viejos
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
      // Almacenamiento bloqueado (ventana privada): basta con haberlo borrado de memoria
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

  // Admin y operador son roles separados: el admin gestiona el catálogo, no la operación diaria
  get esAdmin(): boolean {
    return this._usuario?.tipo === 'ADMIN';
  }

  private guardarSesion(usuario: LoginResponse): void {
    this._usuario = usuario;
    try {
      sessionStorage.setItem(CLAVE_SESION, JSON.stringify(usuario));
    } catch {
      // Si no se puede persistir, la sesión sigue viva en memoria hasta recargar
    }
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
