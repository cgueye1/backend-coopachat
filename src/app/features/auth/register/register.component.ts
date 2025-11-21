import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

// TODO: Importer le service d'inscription
// import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './register.component.html',
  styles: [`
    .animate-float {
      animation: float  ease-in-out infinite;
    }
    
    @keyframes float {
      0%, 100% { transform: translateY(0px); }
      50% { transform: translateY(-20px); }
    }
  `]
})
export class RegisterComponent {
  registerForm: FormGroup;
  
  // TODO: Ajouter ces propriétés pour gérer l'état de l'API
  // isLoading = false;
  // errorMessage = '';
  // successMessage = '';

  constructor(
    private fb: FormBuilder, 
    private router: Router
    // TODO: Injecter le service d'authentification
    // private authService: AuthService
  ) {
    this.registerForm = this.fb.group({
      userType: ['', [Validators.required]],
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^[+]?[0-9\s\-\(\)]{8,15}$/)]],
      company: ['', [Validators.required, Validators.minLength(2)]]
    });
  }

  selectUserType(type: string): void {
    this.registerForm.patchValue({ userType: type });
  }

  onSubmit(): void {
    if (this.registerForm.valid) {
      console.log('Form submitted:', this.registerForm.value);
      
      // TODO: ============ INTÉGRATION API - DÉBUT ============
      // this.isLoading = true;
      // this.errorMessage = '';
      // this.successMessage = '';
      
      // const registrationData = {
      //   userType: this.registerForm.value.userType,
      //   firstName: this.registerForm.value.firstName,
      //   lastName: this.registerForm.value.lastName,
      //   email: this.registerForm.value.email,
      //   phone: this.registerForm.value.phone,
      //   company: this.registerForm.value.company
      // };
      
      // this.authService.register(registrationData).subscribe({
      //   next: (response) => {
      //     // Succès de l'inscription
      //     this.isLoading = false;
      //     console.log('Inscription réussie:', response);
      //     
      //     this.successMessage = 'Inscription réussie ! Redirection vers la vérification...';
      //     
      //     // Stocker l'email pour la page OTP
      //     const email = this.registerForm.get('email')?.value;
      //     localStorage.setItem('verificationEmail', email);
      //     
      //     // Si l'API retourne un userId ou token temporaire, le stocker aussi
      //     // localStorage.setItem('tempUserId', response.userId);
      //     
      //     // Rediriger vers la page OTP après un court délai
      //     setTimeout(() => {
      //       this.router.navigate(['/otp-verification'], {
      //         queryParams: { 
      //           email: email,
      //           type: 'registration'
      //         }
      //       });
      //     }, 1500);
      //   },
      //   error: (error) => {
      //     // Gestion des erreurs
      //     this.isLoading = false;
      //     console.error('Erreur d\'inscription:', error);
      //     
      //     // Afficher un message d'erreur approprié
      //     if (error.status === 409) {
      //       this.errorMessage = 'Cet email est déjà utilisé';
      //     } else if (error.status === 400) {
      //       this.errorMessage = error.error?.message || 'Données invalides';
      //     } else if (error.status === 0) {
      //       this.errorMessage = 'Impossible de se connecter au serveur';
      //     } else {
      //       this.errorMessage = error.error?.message || 'Une erreur est survenue lors de l\'inscription';
      //     }
      //   }
      // });
      // TODO: ============ INTÉGRATION API - FIN ============
      
      // Code de simulation actuel (à supprimer après intégration API)
      setTimeout(() => {
        const email = this.registerForm.get('email')?.value;
        localStorage.setItem('verificationEmail', email);
        
        this.router.navigate(['/otp-verification'], {
          queryParams: { 
            email: email,
            type: 'registration'
          }
        });
      }, 2000);
      
    } else {
      console.log('Form is invalid');
      // Marquer tous les champs comme touchés pour afficher les erreurs
      Object.keys(this.registerForm.controls).forEach(key => {
        const control = this.registerForm.get(key);
        control?.markAsTouched();
      });
    }
  }

  // TODO: Méthode pour vérifier si l'email existe déjà (optionnel)
  // checkEmailExists(): void {
  //   const email = this.registerForm.get('email')?.value;
  //   if (email && this.registerForm.get('email')?.valid) {
  //     this.authService.checkEmailExists(email).subscribe({
  //       next: (exists) => {
  //         if (exists) {
  //           this.registerForm.get('email')?.setErrors({ emailTaken: true });
  //           this.errorMessage = 'Cet email est déjà utilisé';
  //         }
  //       },
  //       error: (error) => {
  //         console.error('Erreur lors de la vérification de l\'email:', error);
  //       }
  //     });
  //   }
  // }

  // TODO: Méthode pour resend OTP si nécessaire
  // resendOTP(): void {
  //   const email = localStorage.getItem('verificationEmail');
  //   if (email) {
  //     this.authService.resendOTP(email).subscribe({
  //       next: (response) => {
  //         console.log('OTP renvoyé avec succès');
  //         this.successMessage = 'Un nouveau code a été envoyé à votre email';
  //       },
  //       error: (error) => {
  //         console.error('Erreur lors du renvoi du OTP:', error);
  //         this.errorMessage = 'Impossible de renvoyer le code';
  //       }
  //     });
  //   }
  // }
}