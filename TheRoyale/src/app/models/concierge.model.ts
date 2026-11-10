export interface MensajeChat {
  remitente: 'usuario' | 'concierge';
  texto: string;
  hora: string;
  sugerencias?: string[];
  enlaceAccion?: string;
  textoAccion?: string;
}

export interface ChatResponse {
  respuesta: string;
  sugerencias: string[];
  enlaceAccion: string;
  textoAccion: string;
}

export interface DiaItinerario {
  numeroDia: number;
  tituloDia: string;
  manana: string;
  tarde: string;
  noche: string;
  recomendacionHotel: string;
}

export interface ItinerarioResponse {
  titulo: string;
  resumen: string;
  estilo: string;
  totalDias: number;
  dias: DiaItinerario[];
}

export interface ItinerarioRequest {
  dias: number;
  estilo: string;
  acompanantes: string;
  preferencias: string;
}
