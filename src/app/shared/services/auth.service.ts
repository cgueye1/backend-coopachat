import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserDto } from '../models/user.model';

@Injectable({
    providedIn: 'root'
})
export class AuthService {

    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) { }

    register(userData: UserDto): Observable<any> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        return this.http.post(
            `${this.apiUrl}/auth/users`,
            userData,
            {
                headers,
                responseType: 'text' as 'json' // Accepter la réponse comme texte
            }
        );
    }

    // Méthode pour vérifier le code OTP envoyé par email
    verifyActivationCode(email: string, code: string): Observable<any> {

        // Crée les headers HTTP pour indiquer qu'on envoie des données JSON
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        // Envoie une requête POST au backend pour vérifier le code OTP
        return this.http.post(
            // URL de l'API qui vérifie le code d'activation
            `${this.apiUrl}/auth/verify-activation-code`,
            // Corps de la requête : email et code OTP
            { email, code },
            {
                headers, // on envoie les headers définis juste au-dessus
                responseType: 'text' as 'json' // Le serveur renvoie du texte plutôt qu'un objet JSON
            }
        );
    }

    // Vérifier le code OTP admin (2FA) et récupérer le token
    verifyAdminOtp(email: string, code: string): Observable<any> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        return this.http.post(
            `${this.apiUrl}/auth/admin/verify-otp`,
            { email, code },
            { headers }
        );
    }

    // Créer le mot de passe après vérification du code
    setPassword(email: string, password: string, confirmPassword: string): Observable<any> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        return this.http.post(
            `${this.apiUrl}/auth/set-password`,
            { email, password, confirmPassword },
            {
                headers,
                responseType: 'text' as 'json'
            }
        );
    }

    // Envoyer un code d'activation par email
    sendActivationCode(email: string): Observable<any> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        return this.http.post(
            `${this.apiUrl}/auth/send-activation-code`,
            { email },
            {
                headers,
                responseType: 'text' as 'json'
            }
        );
    }


    // Renvoyer le code d'activation
    resendActivationCode(email: string): Observable<any> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        return this.http.post(
            `${this.apiUrl}/auth/resend-activation-code`,
            { email },
            {
                headers,
                responseType: 'text' as 'json'
            }
        );
    }

    // Connexion via Google OAuth (idToken)
    loginWithGoogle(idToken: string): Observable<any> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        return this.http.post(
            `${this.apiUrl}/auth/login/google`,
            { idToken },
            { headers }
        );
    }

    // Connexion (un seul endpoint pour tous)
    login(email: string, password: string): Observable<any> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        return this.http.post(
            `${this.apiUrl}/auth/login`,
            { email, password },
            {
                headers
            }
        );
    }

    // Déconnexion (invalidation du token)
    logout(token: string): Observable<any> {
        return this.http.post(
            `${this.apiUrl}/auth/logout?token=${encodeURIComponent(token)}`,
            {},
            {
                responseType: 'text' as 'json'
            }
        );
    }
}
