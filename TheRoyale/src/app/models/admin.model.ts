// Usuario administrador del panel (/admin), tal como lo devuelve el API.
export interface Admin {
  id: number;
  nombre: string;
  email: string;
  password: string;
}