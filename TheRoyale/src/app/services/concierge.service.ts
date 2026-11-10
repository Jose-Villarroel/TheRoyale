import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChatResponse, ItinerarioRequest, ItinerarioResponse } from '../models/concierge.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ConciergeService {
  private readonly url = `${environment.apiUrl}/concierge`;

  constructor(private http: HttpClient) {}

  enviarMensaje(mensaje: string, conversacionId?: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.url}/chat`, { mensaje, conversacionId });
  }

  generarItinerario(request: ItinerarioRequest): Observable<ItinerarioResponse> {
    return this.http.post<ItinerarioResponse>(`${this.url}/itinerario`, request);
  }
}
