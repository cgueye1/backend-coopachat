import { HttpInterceptorFn } from '@angular/common/http';
import { PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/**
 * Intercepteur HTTP : ajoute automatiquement le token JWT à toutes les requêtes sortantes.
 * Exécuté par Angular avant chaque appel HTTP (appels via HttpClient).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {

    // Vérifier si on est côté navigateur 
    const platformId = inject(PLATFORM_ID);
    const isBrowser = isPlatformBrowser(platformId);

    // Récupérer le token stocké (sessionStorage prioritaire, sinon localStorage)
    const token = isBrowser
        ? (sessionStorage.getItem('token') || localStorage.getItem('token'))
        : null;

    // Pas de token → envoyer la requête telle quelle (ex. login, register)
    if (!token) {
        return next(req);
    }

    // Cloner la requête et ajouter le header Authorization pour l'authentification
    const authReq = req.clone({
        setHeaders: {
            Authorization: `Bearer ${token}`
        }
    });

    return next(authReq);
};
