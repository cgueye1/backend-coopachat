import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

/**
 * Intercepteur HTTP : ajoute automatiquement le token JWT à toutes les requêtes sortantes.
 * Exécuté par Angular avant chaque appel HTTP (appels via HttpClient).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {

  
    const platformId = inject(PLATFORM_ID);//ON injecte le platformId
    const isBrowser = isPlatformBrowser(platformId);//ON vérifie si on est côté navigateur
    const router = inject(Router);//ON injecte le router

    // Endpoints d'auth (login/OTP/reset/inscription) : ne jamais déclencher de logout/redirect automatique dessus,
    // sinon on peut boucler vers /login pendant une connexion ou un flux de mot de passe.
    const isAuthEndpoint =
        req.url.includes('/auth/login') ||
        req.url.includes('/auth/admin/verify-otp') ||
        req.url.includes('/auth/verify-activation-code') ||
        req.url.includes('/auth/set-password') ||
        req.url.includes('/auth/forgot-password') ||
        req.url.includes('/auth/reset-password') ||
        req.url.includes('/auth/send-activation-code') ||
        req.url.includes('/auth/resend-activation-code') ||
        req.url.includes('/auth/users');

    // Fonction pour nettoyer la session
    const clearSession = () => {
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('role');
        sessionStorage.removeItem('firstName');
        sessionStorage.removeItem('lastName');
        sessionStorage.removeItem('email');
        sessionStorage.removeItem('otpEmail');
        sessionStorage.removeItem('verificationEmail');
        sessionStorage.removeItem('profilePhotoUrl');

        localStorage.removeItem('token');
        localStorage.removeItem('role');
        localStorage.removeItem('firstName');
        localStorage.removeItem('lastName');
        localStorage.removeItem('email');
        localStorage.removeItem('profilePhotoUrl');
    };

    // Récupérer le token stocké (sessionStorage prioritaire, sinon localStorage)
    const token = isBrowser
        ? (sessionStorage.getItem('token') || localStorage.getItem('token'))
        : null;

    // Pas de token → envoyer la requête telle quelle (ex. login, register)
    if (!token) {
        return next(req).pipe(
            catchError((err: unknown) => {
                //SI on n'est pas côté navigateur(on est côté serveur), on retourne l'erreur
                if (!isBrowser) return throwError(() => err);
                // Sans token : on redirige seulement sur 401 (token manquant), jamais sur 403 (interdit / rôle).
                // Et on ne touche pas au flux des endpoints d'auth (login, otp, reset, etc.).
                if (!isAuthEndpoint && err instanceof HttpErrorResponse && err.status === 401) {
                    clearSession();
                    if (router.url !== '/login') {
                        router.navigate(['/login']);
                    }
                }
                return throwError(() => err);
            })
        );
    }

    // sinon on clone la requête et on ajoute le header Authorization pour l'authentification
    const authReq = req.clone({
        setHeaders: {
            Authorization: `Bearer ${token}`
        }
    });

    return next(authReq).pipe(//ON envoie la requête avec le header Authorization
        //SI on a une erreur, on vérifie si on est côté navigateur ou côté serveur
        catchError((err: unknown) => {
            //SI on n'est pas côté navigateur(on est côté serveur), on retourne l'erreur
            if (!isBrowser) return throwError(() => err);
           //SI on n'est pas sur un endpoint d'auth et que l'erreur est une erreur 401, on nettoie la session et on redirige vers /login
            if (!isAuthEndpoint && err instanceof HttpErrorResponse && err.status === 401) {
                clearSession();//ON nettoie la session
                if (router.url !== '/login') {//SI on n'est pas sur /login, on redirige vers /login
                    router.navigate(['/login']);//ON redirige vers /login
                }
            }
            return throwError(() => err);//ON retourne l'erreur
        })
    );
};
