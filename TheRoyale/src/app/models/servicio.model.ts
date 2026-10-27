// Servicio del hotel (spa, restaurante...) que se muestra en la web y se carga a la cuenta de una estadía.
export interface Servicio {
  id: number;
  nombre: string;
  descripcion: string;
  precio: number;
  imagenUrl: string;
  caracteristicas: string[];
  galeriaUrls: string[];
}