export interface StripeIntentRequest {
  reservaId: number;
  monto: number;
  moneda?: string;
}

export interface StripeIntentResponse {
  clientSecret: string;
  paymentIntentId: string;
  monto: number;
  moneda: string;
  status: string;
  publishableKey: string;
}

export interface ConfirmarStripeRequest {
  reservaId: number;
  paymentIntentId: string;
  monto: number;
}
