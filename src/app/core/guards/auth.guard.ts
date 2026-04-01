import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

/**
 * Guard d'authentification 
 *
 * Rôle : avant de charger une route protégée (admin, commercial, logistique), on vérifie
 * qu'un JWT est présent dans le navigateur. Sinon on redirige vers /login sans monter le
 * composant : ngOnInit ne lance pas d'appels HttpClient vers l'API, ce qui évite les 401
 * « Token manquant » si un utilisateur non connecté ouvre directement une URL métier.
 *
 */
export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID); //PLATFORM_ID est une constante qui contient l'identifiant de la plateforme (browser, server, etc.)

  // si on n'est pas sur le navigateur, on retourne true (on ne bloque pas le rendu de la page)
  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  // Même clé que lors du login / OTP (voir login.component et otp-verification.component).
  const token =
    sessionStorage.getItem('token') || localStorage.getItem('token');

  if (token) {
    // Jeton présent : la route peut s'activer ; l'intercepteur ajoutera Authorization.
    return true;
  }

  // Pas de jeton : navigation vers login via UrlTree(createUrlTree est une fonction géré par Angular) (remplace l'URL courante proprement).
  return router.createUrlTree(['/login']);
};
