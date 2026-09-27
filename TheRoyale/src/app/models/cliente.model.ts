export interface Cliente {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  password: string;
  telefono: string;
  fechaRegistro: string; // formato ISO, ej. "2025-01-15"
}