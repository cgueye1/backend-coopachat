import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserDto } from '../models/user.model';
import { UserDetailsDTO } from './admin.service';

/** Réponse PUT /api/auth/me (profil + nouveau JWT si email modifié). */
export interface ProfileUpdateResponseDTO {
    profile: UserDetailsDTO;
    accessToken: string;
}

@Injectable({
    providedIn: 'root'
})
export class AuthService {

    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) { }

    private persistAuthAfterProfileUpdate(res: ProfileUpdateResponseDTO): void {
        if (!res?.accessToken) return;
        const store = (key: string, value: string) => {
            sessionStorage.setItem(key, value);
            localStorage.setItem(key, value);
        };
        store('token', res.accessToken);
        const p = res.profile;
        if (p?.email) store('email', p.email);
        if (p.firstName != null) store('firstName', p.firstName);
        if (p.lastName != null) store('lastName', p.lastName);
        if (p.profilePhotoUrl != null && p.profilePhotoUrl !== '') {
            store('profilePhotoUrl', p.profilePhotoUrl);
        }
    }

    //----------------------------------------
    // INSCRIPTION
    //----------------------------------------

    /**
     * Inscription d'un nouvel utilisateur.
     * Envoie les données vers POST /api/auth/users.
     */
    register(userData: UserDto): Observable<any> {

        // Les headers indiquent que le corps de la requête est en JSON. et responseType: 'text' indique que le backend renvoie une simple chaîne de caractères (message de succès ou d'erreur) au lieu d'un objet JSON.
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        return this.http.post(
            `${this.apiUrl}/auth/users`, userData,{headers, responseType: 'text' as 'json'
            }
        );
    }

    //----------------------------------------
    // ACTIVATION DU COMPTE (CODE OTP)
    //----------------------------------------

    /**
     * Vérifie le code OTP reçu par email après inscription.
     * Si le code est valide, l'utilisateur peut créer son mot de passe.
     */
    verifyActivationCode(email: string, code: string): Observable<any> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        return this.http.post(
            `${this.apiUrl}/auth/verify-activation-code`,
            { email, code },
            {
                headers,
                responseType: 'text' as 'json'
            }
        );
    }

    /**
     * Vérifie le code OTP pour les admins (authentification à deux facteurs).
     * Retourne le token JWT si le code est correct.
     */
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

    //----------------------------------------
    // CRÉATION / GESTION DU MOT DE PASSE
    //----------------------------------------

    /**
     * Demande un lien de réinitialisation par email (flux web : channel WEB).
     */
    forgotPassword(email: string): Observable<string> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });
        return this.http.post(
            `${this.apiUrl}/auth/forgot-password`,{ email, channel: 'WEB' },
            { headers, responseType: 'text' }
        );
    }

    /**
     * Nouveau mot de passe via le token du lien email (mot de passe oublié).
     */
    resetPassword(token: string, newPassword: string, confirmPassword: string): Observable<string> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });
        return this.http.post(
            `${this.apiUrl}/auth/reset-password`,
            { token, newPassword, confirmPassword },
            { headers, responseType: 'text' }
        );
    }

    /**
     * Crée le mot de passe après vérification du code OTP.
     * Étape finale de l'inscription avant la connexion.
     */
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

    //----------------------------------------
    // ENVOI ET RENVOI DU CODE D'ACTIVATION
    //----------------------------------------

    /**
     * Demande l'envoi d'un nouveau code d'activation par email.
     * Utilisé quand l'utilisateur n'a pas reçu le premier envoi.
     */
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

    /**
     * Renvoie le code OTP par email (par ex. si le précédent a expiré).
     * Limité par un délai pour éviter les abus.
     */
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

    /**
     * Connexion classique avec email et mot de passe.
     * Même endpoint pour tous les rôles (Admin, Commercial, Salarié, etc.).
     */
    login(email: string, password: string): Observable<any> {
        const headers = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        return this.http.post(
            `${this.apiUrl}/auth/login`,
            { email, password },
            { headers }
        );
    }

    //----------------------------------------
    // DÉCONNEXION
    //----------------------------------------

    /**
     * Déconnexion : invalide le token côté serveur (blacklist).
     * À appeler avant de supprimer le token du navigateur.
     */
    logout(token: string): Observable<any> {
        return this.http.post(
            `${this.apiUrl}/auth/logout?token=${encodeURIComponent(token)}`,
            {},
            {
                responseType: 'text' as 'json'
            }
        );
    }

    //----------------------------------------
    // PROFIL UTILISATEUR
    //----------------------------------------

    /**
     * Récupère le profil de l'utilisateur connecté (tous rôles).
     * Requiert le token JWT dans le header Authorization.
     */
    getCurrentUserProfile(): Observable<UserDetailsDTO> {
        return this.http.get<UserDetailsDTO>(`${this.apiUrl}/auth/me`);
    }

    /**
     * PUT /api/auth/me — commercial et responsable logistique uniquement.
     * Met à jour le stockage local si un nouveau JWT est renvoyé.
     */
    updateMyProfile(body: {
        firstName?: string;
        lastName?: string;
        email?: string;
        phoneNumber?: string;
    }): Observable<ProfileUpdateResponseDTO> {
        return this.http.put<ProfileUpdateResponseDTO>(`${this.apiUrl}/auth/me`, body);
    }

    /**
     * Après {@link #updateMyProfile}, persiste le token et les champs du profil.
     */
    applyProfileUpdateResponse(res: ProfileUpdateResponseDTO): void {
        this.persistAuthAfterProfileUpdate(res);
    }

    updateMyProfilePhoto(file: File): Observable<string> {
        const form = new FormData();
        form.append('file', file);
        return this.http.put(`${this.apiUrl}/auth/me/profile-photo`, form, { responseType: 'text' });
    }

    removeMyProfilePhoto(): Observable<string> {
        return this.http.delete(`${this.apiUrl}/auth/me/profile-photo`, { responseType: 'text' });
    }
}
