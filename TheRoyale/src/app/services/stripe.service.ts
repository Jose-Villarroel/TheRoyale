import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ConfirmarStripeRequest, StripeIntentRequest, StripeIntentResponse } from '../models/stripe.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class StripeService {
  private readonly url = `${environment.apiUrl}/pagos/stripe`;

  constructor(private http: HttpClient) {}

  crearPaymentIntent(request: StripeIntentRequest): Observable<StripeIntentResponse> {
    return this.http.post<StripeIntentResponse>(`${this.url}/crear-intent`, request);
  }

  confirmarPago(request: ConfirmarStripeRequest): Observable<any> {
    return this.http.post<any>(`${this.url}/confirmar`, request);
  }
}
