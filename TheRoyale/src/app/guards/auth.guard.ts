import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Reemplaza el "redirect:/login" que hacía el controlador Thymeleaf cuando no había clienteId:
// las páginas del cliente solo se abren con sesión iniciada.
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  return authService.estaAutenticado ? true : router.createUrlTree(['/login']);
};
