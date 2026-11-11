import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Páginas propias del huésped (perfil, mis reservas): no basta con tener sesión, un operador
// no tiene perfil de cliente. El destino viaja en ?redirect= para volver tras el login.
export const clienteGuard: CanActivateFn = (_ruta, estado) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  return authService.esCliente
    ? true
    : router.createUrlTree(['/login'], { queryParams: { redirect: estado.url } });
};
