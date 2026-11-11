import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Panel de administración (CRUD de catálogo y personal): solo para administradores.
// Un operador tampoco entra; cada rol tiene su propio panel y se entra por el login normal.
export const adminGuard: CanActivateFn = (_ruta, estado) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  return authService.esAdmin
    ? true
    : router.createUrlTree(['/login'], { queryParams: { redirect: estado.url } });
};
