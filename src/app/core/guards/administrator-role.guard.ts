import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

/**
 * Réservé aux routes /admin/** : un commercial ou un responsable logistique ne doit pas
 * y accéder (même avec un JWT valide). Redirection vers leur tableau de bord métier.
 *
 * Les libellés de rôle sont alignés sur le login (sessionStorage / localStorage `role`).
 */
export const administratorRoleGuard: CanActivateFn = () => {
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  const role =
    sessionStorage.getItem('role') || localStorage.getItem('role') || '';

  if (role === 'Administrateur' || role === 'Admin') {
    return true;
  }

  if (role === 'Responsable Logistique') {
    return router.createUrlTree(['/log/dashboardlog']);
  }
  if (role === 'Commercial') {
    return router.createUrlTree(['/com/dashboard']);
  }

  return router.createUrlTree(['/portail']);
};
