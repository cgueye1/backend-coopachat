import { Component, OnInit, ViewChild, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthLayoutComponent } from '../auth-layout/auth-layout.component';
import { NgOtpInputModule, NgOtpInputConfig } from 'ng-otp-input';
import Swal from 'sweetalert2';
import { AuthService } from '../../../shared/services/auth.service';
import { getUserFacingHttpErrorMessage } from '../../../shared/utils/http-error-message';

// ============================================================
// CE QUE FAIT CE FICHIER
// ============================================================
// Page OTP à deux usages : 
// inscription (Commercial/Logistique) → création de mot de passe ;
// connexion Admin (2FA) → accès au dashboard. 
// Le cas est choisi selon la présence de otpEmail dans sessionStorage.
// ============================================================

@Component({
  selector: 'app-otp-verification',
  standalone: true,
  imports: [
    CommonModule,        // *ngIf, *ngFor etc.
    AuthLayoutComponent, // cadre commun aux pages auth
    NgOtpInputModule     // bibliothèque pour le champ OTP à 6 cases
  ],
  templateUrl: './otp-verification.component.html',
  styleUrls: ['./otp-verification.component.css']
})
export class OtpVerificationComponent implements OnInit {

  // ============================================================
  //  LES VARIABLES DE CE COMPOSANT
  // ============================================================

  // @ViewChild → permet d'accéder directement au champ OTP dans le HTML depuis le TypeScript
  // Utilisé pour vider le champ si le code est incorrect
  @ViewChild('ngOtpInput') ngOtpInputRef: any;

  // Email affiché à l'écran (masqué pour la confidentialité)
  maskedEmail: string = '';

  // Email réel (non masqué) utilisé pour appeler l'API
  userEmail: string = '';

  // Message d'erreur affiché sous les cases OTP
  errorMessage: string = '';

  // true = appel API en cours → on affiche le spinner
  // false = pas d'appel en cours → formulaire normal
  isLoading: boolean = false;

  // true = le code saisi est incorrect 
  // false = pas d'erreur
  isInvalid: boolean = false;

  // true = bouton "Renvoyer le code" grisé et non cliquable
  // false = bouton actif (l'utilisateur peut demander un nouveau code)
  isResendDisabled: boolean = false;

  // Compte à rebours affiché sur le bouton "Renvoyer"
  // Quand il atteint 0 → bouton réactivé
  resendCountdown: number = 0;

  // Le code OTP saisi par l'utilisateur 
  otpValue: string = '';

  // ============================================================
  // ⚙️ CONFIGURATION DU CHAMP OTP
  // ============================================================
  get otpConfig(): NgOtpInputConfig {
    const border = this.isInvalid ? '2px solid #EF4444' : '1px solid #D1D5DB';
    const backgroundColor = this.isInvalid ? '#FEF2F2' : 'transparent';
    return {
      length: 6,
      isPasswordInput: false,
      disableAutoFocus: false,
      placeholder: '',
      allowNumbersOnly: true,
      inputStyles: {
        'width': '48px',
        'height': '48px',
        'border': border,
        'background-color': backgroundColor,
        'border-radius': '16px',
        'text-align': 'center',
        'font-size': '1.25rem',
        'font-weight': '600',
        'color': '#374151',
        'outline': 'none',
        'transition': 'all 0.2s'
      },
      containerStyles: { 'display': 'flex', 'gap': '16px' }
    };
  }

  // ============================================================
  // 🔧 CONSTRUCTEUR
  // ============================================================
  // On injecte 3 services :
  //   router      → pour naviguer vers une autre page
  //   route       → pour lire l'email dans l'URL (priorité inscription)
  //   authService → pour appeler les API (vérifier OTP, renvoyer code)
  // ============================================================
  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private authService: AuthService,
    @Inject(PLATFORM_ID) private platformId: object
  ) { }

  // ============================================================
  // 🚀 INITIALISATION — s'exécute quand la page se charge
  // ============================================================
  ngOnInit(): void {

    // On cherche l'email dans plusieurs endroits possibles
    // car il peut venir de 2 flux différents (inscription ou admin)
    // Priorité : query param (inscription) > otpEmail (admin) > verificationEmail
    const emailFromUrl = this.route.snapshot.queryParams['email'];
    const fromStorage = isPlatformBrowser(this.platformId)
      ? sessionStorage.getItem('otpEmail') ||
        localStorage.getItem('verificationEmail') ||
        sessionStorage.getItem('email') ||
        ''
      : '';
    const email = emailFromUrl || fromStorage || '';

    this.userEmail = email; // email réel pour l'API

    // On masque l'email pour l'affichage
    this.maskedEmail = email
      ? this.maskEmail(email)
      : 'votre@email.com'; // valeur par défaut si email introuvable
  }

  // ============================================================
  // ✅ PROPRIÉTÉ CALCULÉE — le code est-il complet ?
  // ============================================================
  // get = propriété calculée automatiquement,retourne true seulement si les 6 cases sont remplies
  // Utilisé pour activer/désactiver le bouton "Vérifier"
  get isCodeComplete(): boolean {
    return this.otpValue.length === 6;
  }

  // ============================================================
  // ⌨️ QUAND L'UTILISATEUR TAPE DANS LES CASES OTP
  // ============================================================
  // Appelée automatiquement à chaque changement dans le champ OTP
  onOtpChange(otp: string): void {
    this.otpValue = otp;       // on met à jour le code saisi
    this.errorMessage = '';     // on efface l'erreur précédente
    this.isInvalid = false;     // on remet les cases en normal (pas rouge)
  }

  // ============================================================
  // 🚀 QUAND L'UTILISATEUR CLIQUE "VÉRIFIER"
  // ============================================================
  onSubmit(): void {

    // Sécurité : on vérifie que le code est complet
    // et qu'on n'est pas déjà en train d'appeler l'API
    if (!this.isCodeComplete || this.isLoading) return;

    this.isLoading = true;
    this.errorMessage = '';

    // Sécurité : on vérifie qu'on a bien un email
    if (!this.userEmail) {
      this.errorMessage = 'Email introuvable';
      this.isLoading = false;
      return;
    }

    // On détecte dans quel cas on est :
    const isAdminOtpFlow =
      isPlatformBrowser(this.platformId) && !!sessionStorage.getItem('otpEmail');
    // !! = convertit en booléen (null → false, "email@..." → true)

    // On choisit le bon appel API selon le cas
    const request$ = isAdminOtpFlow
      ? this.authService.verifyAdminOtp(this.userEmail, this.otpValue)
      // CAS ADMIN  → vérifie le code 2FA et retourne un token
      : this.authService.verifyActivationCode(this.userEmail, this.otpValue);
      // CAS INSCRIPTION → active le compte et ne retourne pas de token

    // On envoie la requête
    request$.subscribe({

      // ✅ SI LE CODE EST CORRECT
      next: (response) => {
        this.isLoading = false;

        // CAS ADMIN : on reçoit un token → on le stocke → redirection
        if (isAdminOtpFlow && response?.accessToken) {

          // On stocke toutes les infos de l'utilisateur (session + local pour persistance au rechargement)
          const store = (key: string, value: string) => {
            if (!isPlatformBrowser(this.platformId)) {
              return;
            }
            sessionStorage.setItem(key, value);
            localStorage.setItem(key, value);
          };
          store('token', response.accessToken);
          store('role', response.role);
          if (response.email) store('email', response.email);
          if (response.firstName) store('firstName', response.firstName);
          if (response.lastName) store('lastName', response.lastName);
          if (response.profilePhotoUrl) store('profilePhotoUrl', response.profilePhotoUrl);

          if (isPlatformBrowser(this.platformId)) {
            sessionStorage.removeItem('otpEmail');
          }

          // On redirige selon le rôle de l'utilisateur
          const role = response.role;
          if (role === 'Administrateur' || role === 'ADMIN') {
            this.router.navigate(['/admin/dashboardadmin']);
          } else if (role === 'Responsable Logistique') {
            this.router.navigate(['/log/dashboardlog']);
          } else if (role === 'Commercial') {
            this.router.navigate(['/com/dashboard']);
          } else {
            this.router.navigate(['/portail']); // rôle inconnu → page d'accueil
          }
          return; // on sort, pas besoin de continuer
        }

        // CAS INSCRIPTION : pas de token → juste un message de succès
        // puis redirection vers "Créer un mot de passe"
        this.showSuccessMessage();
      },

      // ❌ SI LE CODE EST INCORRECT
      error: (error) => {
        this.isLoading = false;

        this.errorMessage = getUserFacingHttpErrorMessage(
          error,
          'Code de vérification incorrect. Veuillez réessayer.'
        );

        // On marque les cases en rouge (le code reste affiché pour que l'utilisateur puisse le corriger)
        this.isInvalid = true;
      }
    });
  }

  // ============================================================
  // 🔄 RENVOYER UN NOUVEAU CODE
  // ============================================================
  resendCode(): void {

    // Sécurité : bouton déjà désactivé ou email manquant → on ne fait rien
    if (this.isResendDisabled || !this.userEmail) return;

    // On désactive le bouton immédiatement pour éviter les clics multiples
    this.isResendDisabled = true;
    this.errorMessage = '';

    // Appel API : envoie un nouveau code par email
    this.authService.resendActivationCode(this.userEmail).subscribe({

      // ✅ CODE RENVOYÉ AVEC SUCCÈS
      next: () => {
        // On lance un compte à rebours de 30 secondes pendant lequel le bouton reste désactivé
        this.startResendCountdown(30);
      },

      // ❌ ERREUR LORS DU RENVOI
      error: (error) => {
        this.isResendDisabled = false; // on réactive le bouton

        const errorMessage = getUserFacingHttpErrorMessage(
          error,
          'Erreur lors du renvoi du code'
        );
        this.errorMessage = errorMessage;

        // Le backend peut envoyer un message comme : "Veuillez attendre 15 secondes avant de réessayer"
        // On extrait le nombre de secondes avec une regex
        const secondsMatch = errorMessage.match(/(\d+)\s*secondes?/);
        if (secondsMatch) {
          // On lance le countdown avec le délai exact du backend
          const remainingSeconds = parseInt(secondsMatch[1]);
          this.startResendCountdown(remainingSeconds);
        }
      }
    });
  }

  // ============================================================
  // ⏱️ COMPTE À REBOURS DU BOUTON "RENVOYER"
  // ============================================================
  private startResendCountdown(seconds: number): void {
    this.resendCountdown = seconds;

    // setInterval = exécute la fonction toutes les 1000ms (1 seconde)
    const countdownInterval = setInterval(() => {
      this.resendCountdown--;

      if (this.resendCountdown <= 0) {
        this.isResendDisabled = false;      // bouton réactivé
        clearInterval(countdownInterval);   // on arrête le timer
      }
    }, 1000);
  }

  // ============================================================
  // 🎉 POPUP DE SUCCÈS (même style que les modals admin produits)
  // ============================================================
  showSuccessMessage(): void {
    Swal.fire({
      title: 'Vérification réussie',
      text: 'Votre code a été vérifié avec succès.',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showConfirmButton: false,
      timer: 3000,
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-3xl p-6',
        title: 'text-xl font-medium text-gray-900',
        htmlContainer: 'text-base text-gray-600',
        icon: 'border-none'
      },
      backdrop: 'rgba(0,0,0,0.2)',
      width: '580px',
      showClass: { popup: 'animate__animated animate__fadeIn animate__faster' },
      hideClass: { popup: 'animate__animated animate__fadeOut animate__faster' }
    }).then(() => {
      this.router.navigate(['/create-password']);
    });
  }

  // ============================================================
  // 🔒 MASQUER L'EMAIL (utilitaire)
  // ============================================================
  private maskEmail(email: string): string {
    const [localPart, domain] = email.split('@');
    if (localPart.length <= 2) {
      return `${localPart}***@${domain}`;
    }
    const maskedLocal =
      localPart.charAt(0) +                        
      '*'.repeat(localPart.length - 2) +           
      localPart.charAt(localPart.length - 1); 

    return `${maskedLocal}@${domain}`;
    
  }

  // Redirige vers la page de connexion
  navigateToLogin(): void {
    this.router.navigate(['/login']);
  }
}
