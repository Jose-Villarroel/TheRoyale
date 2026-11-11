import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Portal del operador (reservas, cuentas, check-in/out): solo para operadores.
// Un admin tampoco entra; cada rol tiene su propio panel y se entra por el login normal.
export const operadorGuard: CanActivateFn = (_ruta, estado) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  return authService.esOperador
    ? true
    : router.createUrlTree(['/login'], { queryParams: { redirect: estado.url } });
};
