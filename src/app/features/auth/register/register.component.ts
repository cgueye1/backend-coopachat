import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule, ReactiveFormsModule, 
         FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthLayoutComponent } from '../auth-layout/auth-layout.component';
import { AuthService } from '../../../shared/services/auth.service';
import Swal from 'sweetalert2';
import { UserDto } from '../../../shared/models/user.model';

// ============================================================
// CE QUE FAIT CE FICHIER
// ============================================================
// C'est la page "Créer un compte".
// Elle permet à un Commercial ou un Responsable Logistique
// de s'inscrire sur CoopAchat.
//
// Le flux est :
// 1. L'utilisateur remplit le formulaire
// 2. Il clique sur "Créer un compte"
// 3. On envoie les données au backend (AuthService)
// 4. Si succès → on envoie un code OTP par email
// 5. On redirige vers la page de vérification OTP
// ============================================================

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,           // *ngIf, *ngFor etc.
    FormsModule,            // [(ngModel)]
    ReactiveFormsModule,    // formGroup, formControlName
    AuthLayoutComponent     // le layout commun aux pages auth
  ],
  templateUrl: './register.component.html'
})
export class RegisterComponent {

  // ============================================================
  // 📦 LES VARIABLES DE CE COMPOSANT
  // ============================================================

  // Le formulaire avec tous ses champs et règles de validation
  registerForm!: FormGroup;

  // Les 2 types d'utilisateurs qui peuvent s'inscrire
  // value = ce qu'on envoie au backend
  // label = ce qu'on affiche à l'écran
  userTypes = [
    { value: 'commercial',  label: 'Commercial' },
    { value: 'logistique',  label: 'Logistique' }
  ];

  // true = on affiche le spinner de chargement
  // false = formulaire normal
  isLoading = false;

  // ============================================================
  // CONSTRUCTEUR
  // ============================================================
  // Le constructeur s'exécute quand la page se charge.
  // On injecte 3 services :
  //   fb          → pour créer le formulaire
  //   router      → pour naviguer entre les pages
  //   authService → pour appeler l'API d'inscription
  // ============================================================

  constructor(
    private fb: FormBuilder,     
    private router: Router,       
    private authService: AuthService 
  ) {
    this.initializeForm(); // on crée le formulaire dès le départ
  }

  // ============================================================
  // 📝 CRÉATION DU FORMULAIRE
  // ============================================================

  private initializeForm(): void {
    // fb.group() crée un formulaire avec plusieurs champs.
    // Chaque champ a :
    //   - une valeur par défaut ('' = vide)
    //   - des règles de validation (Validators)

    this.registerForm = this.fb.group({

      // Choix Commercial ou Logistique — obligatoire
      userType:  ['', [Validators.required]],

      // Prénom — obligatoire, minimum 2 lettres
      firstName: ['', [Validators.required, Validators.minLength(2)]],

      // Nom — obligatoire, minimum 2 lettres
      lastName:  ['', [Validators.required, Validators.minLength(2)]],

      // Email — obligatoire, format email valide
      email:     ['', [Validators.required, Validators.email]],

      // Téléphone — obligatoire, entre 8 et 15 chiffres
      // Le pattern accepte : +221 77 123 45 ** ou 77 123 45 **
      phone: ['', [
        Validators.required,
        Validators.pattern(/^[+]?[0-9\s\-\(\)]{8,15}$/)
      ]],

      // Entreprise — obligatoire seulement pour Commercial
      // La validation required est gérée côté backend
      company: ['', [Validators.minLength(2)]]
    });
  }

  // ============================================================
  //  QUAND L'UTILISATEUR CHOISIT SON TYPE
  // ============================================================

  // Appelée quand on clique sur "Commercial" ou "Logistique"
  // Met à jour le champ userType dans le formulaire
  selectUserType(type: string): void {
    this.registerForm.patchValue({ userType: type });
  }

  // ============================================================
  //  QUAND L'UTILISATEUR CLIQUE SUR "CRÉER UN COMPTE"
  // ============================================================

  onSubmit(event?: Event): void {
    // Empêche la page de se recharger (comportement par défaut des formulaires HTML)
    if (event) event.preventDefault();

    if (this.registerForm.valid) {
      // Formulaire OK → on envoie les données
      this.registerUser();
    } else {
      // Formulaire incomplet → on affiche les erreurs en rouge
      this.markFormFieldsAsTouched();
    }
  }

  // ============================================================
  // APPEL AU BACKEND
  // ============================================================

  private registerUser(): void {
    this.isLoading = true; // affiche le spinner

    // Convertit les données du formulaire au format attendu par l'API
    const userDto = this.mapFormDataToUserDto();

    // Envoie la requête au backend
    this.authService.register(userDto).subscribe({
      next:  (response) => this.handleRegistrationSuccess(response),
      error: (error)    => this.handleRegistrationError(error)
    });
  }

  // ============================================================
  // 🔄 CONVERSION FORMULAIRE → FORMAT API
  // ============================================================
  // Le formulaire utilise des noms simples (phone, userType)
  // L'API attend des noms précis (phoneNumber, role)
  // Cette méthode fait la traduction
  // ============================================================

  private mapFormDataToUserDto(): UserDto {
    return {
      email:     this.registerForm.value.email,
      firstName: this.registerForm.value.firstName,
      lastName:  this.registerForm.value.lastName,

      // phone → phoneNumber
      phoneNumber: this.registerForm.value.phone,

      // 'commercial' → 'Commercial'
      // 'logistique' → 'Responsable Logistique'
      role: this.registerForm.value.userType === 'commercial'
        ? 'Commercial'
        : 'Responsable Logistique',

      companyCommercial: this.registerForm.value.company
    };
  }

  // ============================================================
  // ✅ SI L'INSCRIPTION RÉUSSIT
  // ============================================================

  private handleRegistrationSuccess(response: any): void {
    // On sauvegarde l'email dans le navigateur pour le réutiliser sur la page OTP
    const email = this.registerForm.get('email')?.value;
    localStorage.setItem('verificationEmail', email);

    // On envoie le code de vérification par email
    this.authService.sendActivationCode(email).subscribe({
      next: () => {
        this.isLoading = false; // loader s'arrête juste avant le modal
        // Modal succès
        Swal.fire({
          title: 'Inscription réussie',
          text: 'Votre compte a été créé. Un code de vérification vous a été envoyé.',
          iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
          showConfirmButton: false,
          timer: 4000,
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
          this.router.navigate(['/otp-verification'], { queryParams: { email } });
        });
      },
      error: (error) => {
        this.isLoading = false;
        const message = error?.error?.message
          || 'Impossible d\'envoyer le code de vérification.';
        Swal.fire({ icon: 'error', title: 'Erreur', text: message });
      }
    });
  }

  // ============================================================
  // ❌ SI L'INSCRIPTION ÉCHOUE
  // ============================================================
  // On affiche un message d'erreur selon le code HTTP reçu :
  //   409 → email ou téléphone déjà utilisé
  //   400 → données invalides
  //   0   → serveur inaccessible
  // ============================================================

  private handleRegistrationError(error: any): void {
    this.isLoading = false;

    let message = 'Une erreur est survenue lors de l\'inscription';

    if (error.status === 409) {
      message = 'Cet email ou ce téléphone est déjà utilisé';
    } else if (error.status === 400) {
      message = error.error?.message || 'Données invalides';
    } else if (error.status === 0) {
      message = 'Impossible de se connecter au serveur';
    } else if (error.error?.message) {
      message = error.error.message;
    }

    Swal.fire({ icon: 'error', title: 'Erreur', text: message });
  }

  // ============================================================
  // 🔴 AFFICHER LES ERREURS DE VALIDATION
  // ============================================================
  // Quand on soumet un formulaire incomplet,
  // Angular n'affiche les erreurs que sur les champs "touchés".
  // Cette méthode force tous les champs à être "touchés"
  // pour que toutes les erreurs s'affichent d'un coup.
  // ============================================================

  private markFormFieldsAsTouched(): void {
    Object.keys(this.registerForm.controls).forEach(key => {
      this.registerForm.get(key)?.markAsTouched();
    });
  }
}
