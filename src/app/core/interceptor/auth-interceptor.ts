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

    /**
     * Fonction utilitaire pour nettoyer les messages d'erreurs techniques du backend
     */
    const getFriendlyErrorMessage = (err: any): string | null => {
        const body = err?.error;
        const message = err?.message || "";

        // 1. Si le backend a envoyé un message explicite, on le garde
        if (body && typeof body === 'object' && body.message) {
            return body.message;
        }

        // 2. Liste des mots-clés "techniques" (Java, SQL, Angular)
        const technicalKeywords = [
            'hibernate', 'jpa', 'sql', 'jdbc', 'constraint', 'violation', 
            'optimistic', 'lock', 'staleobject', 'row was updated', 'unsaved-value',
            '.entities.', 'com.example', 'nullpointer', 'http failure', 'localhost'
        ];

        const isTechnical = technicalKeywords.some(key => 
            message.toLowerCase().includes(key.toLowerCase()) || 
            (body && typeof body === 'string' && body.toLowerCase().includes(key.toLowerCase()))
        );

        // Si c'est technique, on retourne null pour laisser ErrorHandlerService gérer le status code
        if (isTechnical) {
            return null;
        }

        return body?.message || null;// Si pas de message métier, on renvoie null aussi
    };

    // Récupérer le token stocké (sessionStorage prioritaire, sinon localStorage)
    const token = isBrowser
        ? (sessionStorage.getItem('token') || localStorage.getItem('token'))
        : null;

    // Pas de token → envoyer la requête telle quelle (ex. login, register)
    if (!token) {
        return next(req).pipe(
            catchError((err: unknown) => {
                if (!isBrowser) return throwError(() => err);
                
                let errorToReturn = err;
                if (err instanceof HttpErrorResponse) {
                    const friendlyMessage = getFriendlyErrorMessage(err);
                    errorToReturn = new HttpErrorResponse({
                        error: typeof err.error === 'object' ? { ...err.error, message: friendlyMessage } : { message: friendlyMessage },
                        status: err.status,
                        statusText: err.statusText,
                        url: err.url || undefined
                    });

                    if (!isAuthEndpoint && err.status === 401) {
                        clearSession();
                        if (router.url !== '/login') {
                            router.navigate(['/login']);
                        }
                    }
                }
                
                return throwError(() => errorToReturn);
            })
        );
    }

    // sinon on clone la requête et on ajoute le header Authorization pour l'authentification
    const authReq = req.clone({
        setHeaders: {
            Authorization: `Bearer ${token}`
        }
    });

    return next(authReq).pipe(
        catchError((err: unknown) => {
            if (!isBrowser) return throwError(() => err);
            
            let errorToReturn = err;
            if (err instanceof HttpErrorResponse) {
                const friendlyMessage = getFriendlyErrorMessage(err);
                errorToReturn = new HttpErrorResponse({
                    error: typeof err.error === 'object' ? { ...err.error, message: friendlyMessage } : { message: friendlyMessage },
                    status: err.status,
                    statusText: err.statusText,
                    url: err.url || undefined
                });

                if (!isAuthEndpoint && err.status === 401) {
                    clearSession();
                    if (router.url !== '/login') {
                        router.navigate(['/login']);
                    }
                }
            }
            
            return throwError(() => errorToReturn);
        })
    );
};
