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

  // El usuario vive solo en memoria: al recargar la página hay que volver a iniciar sesión
  constructor(private http: HttpClient) {}

  login(credenciales: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.url}/login`, credenciales).pipe(
      tap(usuario => this._usuario = usuario)
    );
  }

  // Tras editar el perfil se refrescan nombre/email guardados, si no la cabecera mostraría los viejos
  actualizarUsuario(datos: Pick<LoginResponse, 'nombre' | 'apellido' | 'email'>): void {
    if (!this._usuario) {
      return;
    }
    this._usuario = { ...this._usuario, ...datos };
  }

  logout(): void {
    this._usuario = null;
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
}
