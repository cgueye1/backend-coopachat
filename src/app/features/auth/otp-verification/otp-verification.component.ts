import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthLayoutComponent } from '../auth-layout/auth-layout.component';
import { NgOtpInputModule, NgOtpInputConfig } from 'ng-otp-input';
import Swal from 'sweetalert2';
import { AuthService } from '../../../shared/services/auth.service';

/**
 * Composant de vérification OTP
 * Sert à vérifier le code envoyé par email et activer le compte utilisateur
 */
@Component({
  selector: 'app-otp-verification',
  standalone: true,
  imports: [CommonModule, AuthLayoutComponent, NgOtpInputModule],
  templateUrl: './otp-verification.component.html',
  styleUrls: ['./otp-verification.component.css']
})
export class OtpVerificationComponent implements OnInit {

  // ========================================================================
  //  Référence du champ OTP pour pouvoir le réinitialiser si erreur
  // ========================================================================
  @ViewChild('ngOtpInput') ngOtpInputRef: any;

  // ========================================================================
  //  Informations utilisateur
  // ========================================================================
  maskedEmail: string = ''; // Email masqué pour affichage (ex: j***n@exemple.com)
  userEmail: string = '';   // Email réel utilisé pour l'API

  // ========================================================================
  // Etat de l'application
  // ========================================================================
  errorMessage: string = ''; // Message d'erreur affiché à l'utilisateur
  isLoading: boolean = false; // True = appel API en cours (affiche spinner)
  isInvalid: boolean = false; // True = code OTP invalide

  // ========================================================================
  //  Gestion du renvoi de code
  // ========================================================================
  isResendDisabled: boolean = false; // True = bouton “renvoyer” désactivé
  resendCountdown: number = 0;       // Temps restant avant de pouvoir renvoyer

  // ========================================================================
  //  Code OTP saisi par l'utilisateur
  // ========================================================================
  otpValue: string = '';

  // ========================================================================
  // Configuration du champ OTP
  // ========================================================================
  otpConfig: NgOtpInputConfig = {
    length: 6, // nombre de chiffres
    isPasswordInput: false,
    disableAutoFocus: false,
    placeholder: '',
    allowNumbersOnly: true,
    inputStyles: {
      'width': '48px',
      'height': '48px',
      'border': '1px solid #D1D5DB',
      'border-radius': '16px',
      'text-align': 'center',
      'font-size': '1.25rem',
      'font-weight': '600',
      'color': '#374151',
      'outline': 'none',
      'transition': 'all 0.2s'
    },
    containerStyles: {
      'display': 'flex',
      'gap': '16px'
    }
  };

  // ========================================================================
  //  CONSTRUCTEUR
  // ========================================================================
  constructor(
    private router: Router,        // pour naviguer vers d'autres pages
    private authService: AuthService // service pour appeler les API
  ) { }

  // ========================================================================
  // INITIALISATION DU COMPOSANT
  // ========================================================================
  ngOnInit(): void {
    // On récupère l'email depuis le storage
    const email =
      sessionStorage.getItem('otpEmail') || // cas admin (login -> OTP)
      localStorage.getItem('verificationEmail') || // cas inscription
      sessionStorage.getItem('email') || // fallback session
      '';
    this.userEmail = email;

    // On masque l'email pour affichage
    if (email) {
      this.maskedEmail = this.maskEmail(email);
    } else {
      this.maskedEmail = 'votre@email.com'; // valeur par défaut
    }
  }

  // ========================================================================
  // VÉRIFICATION OTP
  // ========================================================================

  //  Vérifie si le code OTP est complet (6 chiffres)
  get isCodeComplete(): boolean {
    return this.otpValue.length === 6;
  }

  // Quand l'utilisateur modifie le code OTP
  onOtpChange(otp: string): void {
    this.otpValue = otp;
    this.errorMessage = ''; // on efface l'erreur
    this.isInvalid = false; // on réinitialise le flag d'erreur
  }

  //  Soumettre le code OTP
  onSubmit(): void {

    // Vérifie si le code est complet et qu'on n'est pas déjà en chargement
    if (this.isCodeComplete && !this.isLoading) {
      this.isLoading = true; // on affiche spinner
      this.errorMessage = '';

      // Vérifie si l'email est disponible
      if (!this.userEmail) {
        this.errorMessage = 'Email introuvable';
        this.isLoading = false; // arrêt spinner
        return;
      }

      const isAdminOtpFlow = !!sessionStorage.getItem('otpEmail');

      // Appel API : OTP admin (2FA) ou activation de compte
      const request$ = isAdminOtpFlow
        ? this.authService.verifyAdminOtp(this.userEmail, this.otpValue)
        : this.authService.verifyActivationCode(this.userEmail, this.otpValue);

      request$.subscribe({
        next: (response) => {
          this.isLoading = false; // arrêt spinner

          // Cas admin : on reçoit un token + role -> redirection dashboard
          if (isAdminOtpFlow && response?.accessToken) {
            sessionStorage.setItem('token', response.accessToken);
            sessionStorage.setItem('role', response.role);
            if (response.email) {
              sessionStorage.setItem('email', response.email);
            }
            if (response.firstName) {
              sessionStorage.setItem('firstName', response.firstName);
            }
            if (response.lastName) {
              sessionStorage.setItem('lastName', response.lastName);
            }
            sessionStorage.removeItem('otpEmail'); // nettoyer

            const role = response.role;
            if (role === 'Administrateur' || role === 'ADMINISTRATOR' || role === 'ADMIN') {
              this.router.navigate(['/admin/dashboardadmin']);
            } else if (role === 'Responsable Logistique') {
              this.router.navigate(['/log/dashboardlog']);
            } else if (role === 'Commercial') {
              this.router.navigate(['/com/dashboard']);
            } else {
              this.router.navigate(['/portail']);
            }
            return;
          }

          // Cas activation (commercial / logistique)
          this.showSuccessMessage();
        },
        error: (error) => {
          this.isLoading = false; // arrêt spinner
          this.errorMessage = error.error?.message || 'Code de vérification incorrect. Veuillez réessayer.';
          this.isInvalid = true;

          // Réinitialiser le champ OTP
          if (this.ngOtpInputRef) {
            this.ngOtpInputRef.setValue('');
          }
          this.otpValue = '';
        }
      });
    }
  }

  // ========================================================================
  // 🔄 RENVOI DE CODE
  // ========================================================================
  resendCode(): void {
    
    // Vérifier si le bouton n'est pas déjà désactivé et que l'utilisateur a un email
    if (!this.isResendDisabled && this.userEmail) {

      // Désactiver le bouton immédiatement pour éviter les clics multiples
      this.isResendDisabled = true;
      this.errorMessage = ''; // Réinitialiser tout message d'erreur précédent

      // Appel de l'API pour renvoyer le code d'activation
      this.authService.resendActivationCode(this.userEmail).subscribe({

        // -------------------
        // Succès de l'API
        // -------------------
        next: (response) => {
          // Le code a été renvoyé avec succès
          // On lance le countdown par défaut de 30 secondes pour bloquer le bouton
          this.startResendCountdown(30);
        },

        // -------------------
        // Erreur de l'API
        // -------------------
        error: (error) => {
          // Réactiver le bouton car l'envoi a échoué
          this.isResendDisabled = false;

          // Récupérer le message d'erreur du backend ou afficher un message générique
          const errorMessage = error.error?.message || 'Erreur lors du renvoi du code';
          this.errorMessage = errorMessage;

          // Vérifier si le message contient un délai de réessai (ex: "15 secondes")
          const secondsMatch = errorMessage.match(/(\d+)\s*secondes?/);
          if (secondsMatch) {
            // Extraire le nombre de secondes et lancer le countdown correspondant
            const remainingSeconds = parseInt(secondsMatch[1]);
            //utiliser le nombre extrait pour le countdown (compte à rebours)
            this.startResendCountdown(remainingSeconds);
          }
        }
      });
    }
  }


  // Countdown pour le bouton “renvoyer code”
  private startResendCountdown(seconds: number): void {
    this.resendCountdown = seconds; // nombre de secondes affiché
    const countdownInterval = setInterval(() => {
      this.resendCountdown--; // on décrémente chaque seconde

      if (this.resendCountdown <= 0) {
        this.isResendDisabled = false; // bouton réactivé
        clearInterval(countdownInterval); // on arrête le timer
      }
    }, 1000); // répété toutes les 1000ms = 1 seconde
  }


  // ========================================================================
  // GESTION DU SUCCÈS
  // ========================================================================
  showSuccessMessage(): void {
    // Affiche un message de succès via SweetAlert
    Swal.fire({
      title: 'Vérification réussie',
      html: '<p style="color: #231F20; font-size: 18px; margin: 0;">Votre code a été vérifié avec succès.</p>',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showConfirmButton: false,
      timer: 1500,
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-xl p-6',
        title: 'text-2xl font-semibold text-[#231F20]',
        icon: 'border-none'
      },
      backdrop: `rgba(0,0,0,0.2)`,
      width: '580px',
      showClass: {
        popup: 'animate__animated animate__fadeIn animate__faster'
      },
      hideClass: {
        popup: 'animate__animated animate__fadeOut animate__faster'
      }
    }).then(() => {
      this.router.navigate(['/create-password']);
    });
  }

  // ========================================================================
  //  UTILITAIRES
  // ========================================================================
  private maskEmail(email: string): string {
    // Masque les lettres de l'email sauf 1ère et dernière lettre
    const [localPart, domain] = email.split('@');
    if (localPart.length <= 2) {
      return `${localPart}***@${domain}`;
    }
    const maskedLocal = localPart.charAt(0) + '*'.repeat(localPart.length - 2) + localPart.charAt(localPart.length - 1);
    return `${maskedLocal}@${domain}`;
  }

  // Redirige vers la page de connexion
  navigateToLogin(): void {
    this.router.navigate(['/login']);
  }
}
