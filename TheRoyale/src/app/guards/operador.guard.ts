import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// El portal de operador tiene su propia puerta de entrada (/operator/login), separada
// de la del cliente: no basta con estar autenticado, hace falta ser personal del hotel.
export const operadorGuard: CanActivateFn = (_ruta, estado) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  return authService.esOperador
    ? true
    : router.createUrlTree(['/operator/login'], { queryParams: { redirect: estado.url } });
};
