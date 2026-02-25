import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormsModule,
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators
} from '@angular/forms';
import { AuthLayoutComponent } from '../auth-layout/auth-layout.component';
import { AuthService } from '../../../shared/services/auth.service';
import { Router } from '@angular/router';
import { environment } from '../../../../environments/environment';

declare const google: any;

@Component({
  selector: 'app-login',
  standalone: true,
  // Modules nécessaires au template (formulaires, layout, directives Angular)
  imports: [CommonModule, FormsModule, ReactiveFormsModule, AuthLayoutComponent],
  templateUrl: `login.component.html`,
  styles: []
})
export class LoginComponent implements OnInit {

  // Formulaire réactif de connexion
  loginForm: FormGroup;

  // Permet d’afficher ou masquer le mot de passe
  showPassword = false;

  // Indique si l’appel API est en cours (sert à afficher un loader)
  isLoading = false;

  // Message d’erreur affiché dans le template si la connexion échoue
  errorMessage = '';

  // Client ID Google OAuth (chargé depuis environment)
  googleClientId = environment.googleClientId;

  constructor(
    private fb: FormBuilder,        // Sert à construire le FormGroup
    private authService: AuthService, // Service pour appeler l’API de login
    private router: Router           // Sert à naviguer après la connexion
  ) {
    // Initialisation du formulaire avec ses champs et validations
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]], // email obligatoire et valide
      password: ['', [Validators.required]],                // mot de passe obligatoire
      rememberMe: [false]                                   // option "se souvenir de moi"
    });
  }

  ngOnInit(): void {
    this.loadGoogleScript();
  }

  // Change l’état showPassword pour afficher/masquer le mot de passe
  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  // Méthode appelée lors de la soumission du formulaire
  onSubmit(): void {

    // On vérifie d’abord si le formulaire est valide
    if (this.loginForm.valid) {

      // Activation du loader et reset du message d’erreur
      this.isLoading = true;
      this.errorMessage = '';

      // Récupération des valeurs saisies
      const email = this.loginForm.value.email;
      const password = this.loginForm.value.password;

      // Appel du backend pour la connexion
      this.authService.login(email, password).subscribe({

        // Cas succès
        next: (response) => {
          this.isLoading = false;

          // Cas particulier : l’admin doit valider un OTP
          if (response.requiresOtp) {
            sessionStorage.setItem('otpEmail', email);
            this.router.navigate(['/otp-verification']);
            return;// on sort de la méthode ici
          }

          // Connexion classique avec token
          if (response.accessToken) {

            // Stockage des informations de session
            sessionStorage.setItem('token', response.accessToken);
            sessionStorage.setItem('role', response.role);
            if (response.email || email) {
              sessionStorage.setItem('email', response.email || email);
            }
            if (response.firstName) {
              sessionStorage.setItem('firstName', response.firstName);
            }
            if (response.lastName) {
              sessionStorage.setItem('lastName', response.lastName);
            }

            // Redirection selon le rôle de l’utilisateur (labels renvoyés par le backend)
            if (response.role === 'Responsable Logistique') {
              this.router.navigate(['/log/dashboardlog']);
            } else if (response.role === 'Commercial') {
              this.router.navigate(['/com/dashboard']);
            } else {
              this.router.navigate(['/portail']);
            }
          }
        },

        // Cas erreur (mauvais identifiants, serveur indisponible, etc.)
        error: (error) => {
          this.isLoading = false;

          const backendMessage = error?.error?.message;
          if (backendMessage) {
            // Si le backend renvoie un message clair (ex: compte non actif)
            this.errorMessage = backendMessage;
          } else if (error.status === 401) {
            this.errorMessage = 'Email ou mot de passe incorrect';
          } else if (error.status === 0) {
            this.errorMessage = 'Impossible de se connecter au serveur';
          } else {
            this.errorMessage = 'Une erreur est survenue';
          }
        }
      });

    } else {
      // Si le formulaire est invalide, on force l’affichage des erreurs
      Object.keys(this.loginForm.controls).forEach(key => {
        this.loginForm.get(key)?.markAsTouched();
      });
    }
  }

  // Lancer la connexion Google
  loginWithGoogle(): void { 

    // Vérifie que la librairie Google est bien chargée dans le navigateur
    // Si ce n’est pas le cas, on ne peut pas lancer la connexion
    if (!(window as any).google?.accounts?.id) {
      this.errorMessage = 'Google n’est pas encore chargé';
      return;
    }

    // Réinitialise les messages d’erreur précédents
    this.errorMessage = '';

    // Active le loader (spinner)
    this.isLoading = true;

    // Ouvre la popup Google pour que l’utilisateur choisisse son compte
    //  À CE MOMENT, Google va gérer l’authentification
    google.accounts.id.prompt();
  }

  //Charge le script de google si ce n'est pas déjà fait
  private loadGoogleScript(): void {

    // Si Google est déjà chargé, on initialise directement
    if ((window as any).google?.accounts?.id) {
      this.initializeGoogle();
      return;
    }

    // Évite de charger le script plusieurs fois
    if (document.getElementById('google-identity')) {
      return;
    }

    // Création dynamique du script Google Identity
    const script = document.createElement('script');
    script.id = 'google-identity';
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;

    // Une fois le script chargé, on initialise Google
    script.onload = () => this.initializeGoogle();

    // Ajoute le script dans le <head> de la page
    document.head.appendChild(script);
  }
  //Initialise Google pour notre application 
  private initializeGoogle(): void {

    // Sécurité : on vérifie encore que Google est bien disponible
    if (!(window as any).google?.accounts?.id) {
      return;
    }

    // Initialisation de Google Login
    google.accounts.id.initialize({

      // Identifiant unique de notre application Google , Google saura que la connexion vient de notre app
      client_id: this.googleClientId,

      // Fonction appelée automatiquement par Google , après que l’utilisateur s’est connecté
      callback: (response: any) => this.handleGoogleCredential(response)
    });
  }


  // =================================================
  // RÉCEPTION DU TOKEN GOOGLE APRÈS CONNEXION UTILISATEUR
  // =================================================
  private handleGoogleCredential(response: any): void {

    // Google renvoie un token signé (preuve d’identité)
    const idToken = response?.credential;

    // Si aucun token n’est reçu → erreur
    if (!idToken) {
      this.isLoading = false;
      this.errorMessage = 'Token Google introuvable';
      return;
    }

    // Envoie le token au BACKEND
    //  Le backend va :
    // - vérifier le token chez Google
    // - retrouver l’utilisateur
    // - générer SON PROPRE JWT
    this.authService.loginWithGoogle(idToken).subscribe({

      next: (loginResponse) => {
        this.isLoading = false;

        // Cas ADMIN : OTP obligatoire
        if (loginResponse.requiresOtp) {
          sessionStorage.setItem('otpEmail', loginResponse.email || '');
          this.router.navigate(['/otp-verification']);
          return;
        }

        // Connexion réussie : le backend renvoie SON token JWT
        if (loginResponse.accessToken) {

          // Sauvegarde du token et du rôle
          sessionStorage.setItem('token', loginResponse.accessToken);
          sessionStorage.setItem('role', loginResponse.role);
          if (loginResponse.email) {
            sessionStorage.setItem('email', loginResponse.email);
          }
          if (loginResponse.firstName) {
            sessionStorage.setItem('firstName', loginResponse.firstName);
          }
          if (loginResponse.lastName) {
            sessionStorage.setItem('lastName', loginResponse.lastName);
          }

          // Redirection selon le rôle (labels renvoyés par le backend)
          if (loginResponse.role === 'Responsable Logistique') {
            this.router.navigate(['/log/dashboardlog']);
          } else if (loginResponse.role === 'Commercial') {
            this.router.navigate(['/com/dashboard']);
          } else {
            this.router.navigate(['/portail']);
          }
        }
      },

      // Gestion des erreurs backend
      error: (error) => {
        this.isLoading = false;
        this.errorMessage =
          error.error?.message || 'Erreur lors de la connexion Google';
      }
    });
  }

}
