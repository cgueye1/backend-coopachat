import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthLayoutComponent } from '../auth-layout/auth-layout.component';
import { AuthService } from '../../../shared/services/auth.service';
import Swal from 'sweetalert2';
import { UserDto } from '../../../shared/models/user.model';

/**
 * Composant d'inscription
 * Permet aux utilisateurs (Commercial, Responsable Logistique) de créer un compte
 */
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, AuthLayoutComponent],
  templateUrl: './register.component.html'
})
export class RegisterComponent {

  // ============================================================================
  // 📋 PROPRIÉTÉS
  // ============================================================================

  registerForm!: FormGroup; // Initialisé dans initializeForm() appelé dans le constructeur

  // Types d'utilisateurs disponibles pour l'inscription
  userTypes = [
    { value: 'commercial', label: 'Commercial' },
    { value: 'logistique', label: 'Logistique' }
  ];

  // État de l'application
  isLoading = false;

  // ============================================================================
  // 🔧 CONSTRUCTEUR
  // ============================================================================

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    this.initializeForm();
  }

  // ============================================================================
  // 📝 INITIALISATION DU FORMULAIRE
  // ============================================================================

  /**
   * Initialise le formulaire d'inscription avec les validations
   */
  private initializeForm(): void {
    this.registerForm = this.fb.group({
      userType: ['', [Validators.required]],
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^[+]?[0-9\s\-\(\)]{8,15}$/)]],
      company: ['', [Validators.minLength(2)]]
      // Note: La validation 'required' pour 'company' est gérée côté backend
    });
  }

  // ============================================================================
  // 🎯 GESTION DU FORMULAIRE
  // ============================================================================

  /**
   * Sélectionne le type d'utilisateur
   */
  selectUserType(type: string): void {
    this.registerForm.patchValue({ userType: type });
  }

  /**
   * Soumet le formulaire d'inscription
   */
  onSubmit(event?: Event): void {
    // Empêcher le comportement par défaut du formulaire (soumission GET au lieu de POST )
    if (event) {
      event.preventDefault();
    }

    //si le formulaire est valide, on enregistre l'utilisateur sinon on marque les champs comme touchés pour afficher les erreurs
    if (this.registerForm.valid) {
      this.registerUser();
    } else {
      this.markFormFieldsAsTouched();
    }
  }

  // ============================================================================
  // 🔐 INSCRIPTION
  // ============================================================================

  /**
   * Enregistre un nouvel utilisateur via l'API
   */
  private registerUser(): void {
    // Initialiser l'état de chargement
    this.isLoading = true;
    

    // Mapper les données du formulaire vers le format API
    const userDto = this.mapFormDataToUserDto();

    // Appeler l'API d'inscription
    this.authService.register(userDto).subscribe({
      next: (response) => this.handleRegistrationSuccess(response),
      error: (error) => this.handleRegistrationError(error)
    });
  }

  /**
   * Mappe les données du formulaire vers UserDto
   * Convertit les noms de champs et les valeurs pour correspondre à l'API
   */
  private mapFormDataToUserDto(): UserDto {
    return {
      email: this.registerForm.value.email,
      firstName: this.registerForm.value.firstName,
      lastName: this.registerForm.value.lastName,
      phoneNumber: this.registerForm.value.phone,
      role: this.registerForm.value.userType === 'commercial'
        ? 'Commercial'
        : 'Responsable Logistique', 
      companyCommercial: this.registerForm.value.company 
    };
  }

  /**
   * Gère le succès de l'inscription
   */
  private handleRegistrationSuccess(response: any): void {
    this.isLoading = false;
    // Stocker l'email pour la page de vérification OTP
    const email = this.registerForm.get('email')?.value;
    localStorage.setItem('verificationEmail', email);
    
    // si inscription réussie, envoie du code d'activation par email, si succès -> popup si clique sur continue -> redirection vers la page de vérification OTP
    this.authService.sendActivationCode(email).subscribe({
      next: () => {
        Swal.fire({
          title: 'Inscription réussie',
          html: '<p style="color: #231F20; font-size: 18px; font-weight: 400; margin: 0;">Votre compte a été créé. Un code de vérification vous a été envoyé.</p>',
          iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
          showConfirmButton: true,
          confirmButtonText: 'Continuer',
          buttonsStyling: false,
          customClass: {
            popup: 'rounded-xl p-6',
            title: 'text-2xl font-semibold text-[#231F20]',
            icon: 'border-none',
            confirmButton: 'w-full bg-gradient-to-r from-[#FF6B00] to-[#FF914D] text-white py-3 px-4 rounded-md hover:from-orange-600 hover:to-orange-700 font-medium text-base transition-all duration-200 transform hover:scale-[1.02] mt-4'
          },
          backdrop: 'rgba(0,0,0,0.2)',
          width: '580px',
          showClass: { popup: 'animate__animated animate__fadeIn animate__faster' },
          hideClass: { popup: 'animate__animated animate__fadeOut animate__faster' }
        }).then(() => {
          this.router.navigate(['/otp-verification'], {
            queryParams: {
              email: email
            }
          });
        });
      },
      error: (error) => {
        const message = error?.error?.message || 'Impossible d\'envoyer le code de vérification.';
        Swal.fire({
          icon: 'error',
          title: 'Erreur',
          text: message
        });
      }
    });
  }

  /**
   * Gère les erreurs lors de l'inscription
   */
  private handleRegistrationError(error: any): void {
    this.isLoading = false;

    // Déterminer le message d'erreur selon le code HTTP
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

    Swal.fire({
      icon: 'error',
      title: 'Erreur',
      text: message
    });
  }

  // ============================================================================
  // ✅ VALIDATION
  // ============================================================================

  /**
   * Marque tous les champs du formulaire comme touchés
   * Permet d'afficher les messages de validation
   */
  private markFormFieldsAsTouched(): void {
    Object.keys(this.registerForm.controls).forEach(key => {
      const control = this.registerForm.get(key);
      control?.markAsTouched();
    });
  }
}
