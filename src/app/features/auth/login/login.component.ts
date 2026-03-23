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

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, AuthLayoutComponent],
  templateUrl: `login.component.html`,
  styles: []
})
export class LoginComponent implements OnInit {

  loginForm: FormGroup;
  showPassword = false;
  isLoading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]],
      rememberMe: [false]
    });
  }

  ngOnInit(): void {
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  loginWithGoogle(): void {
    // TODO: implémenter la connexion OAuth Google
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';

      const email = this.loginForm.value.email;
      const password = this.loginForm.value.password;

      this.authService.login(email, password).subscribe({
        next: (response) => {
          this.isLoading = false;

          if (response.requiresOtp) {
            sessionStorage.setItem('otpEmail', email);
            this.router.navigate(['/otp-verification']);
            return;
          }

          if (response.accessToken) {
            const store = (key: string, value: string) => {
              sessionStorage.setItem(key, value);
              localStorage.setItem(key, value);
            };
            store('token', response.accessToken);
            store('role', response.role);
            if (response.email || email) {
              store('email', response.email || email);
            }
            if (response.firstName) {
              store('firstName', response.firstName);
            }
            if (response.lastName) {
              store('lastName', response.lastName);
            }
            if (response.profilePhotoUrl != null && response.profilePhotoUrl !== '') {
              store('profilePhotoUrl', response.profilePhotoUrl);
            }

            const role = response.role;
            if (role === 'Administrateur' || role === 'Admin') {
              this.router.navigate(['/admin/dashboardadmin']);
            } else if (role === 'Responsable Logistique') {
              this.router.navigate(['/log/dashboardlog']);
            } else if (role === 'Commercial') {
              this.router.navigate(['/com/dashboard']);
            } else {
              this.router.navigate(['/portail']);
            }
          }
        },

        error: (error) => {
          this.isLoading = false;
          const backendMessage = error?.error?.message;
          if (backendMessage) {
            if (backendMessage.includes('email OTP') || backendMessage.includes('OTP')) {
              this.errorMessage = 'Le serveur utilise encore l\'ancienne version (envoi OTP). Rebuild et redéployez le backend sur le VPS pour activer la connexion directe sans OTP.';
            } else {
              this.errorMessage = backendMessage;
            }
          } else if (error.status === 401) {
            this.errorMessage = 'Email ou mot de passe incorrect';
          } else if (error.status === 0) {
            this.errorMessage = 'Serveur inaccessible (timeout ou backend non démarré). Vérifiez l\'URL dans environment et que le backend tourne sur le port indiqué.';
          } else if (error.status === 500) {
            this.errorMessage = 'Erreur interne du serveur (500). Consultez les logs du backend sur le VPS pour en identifier la cause.';
          } else {
            this.errorMessage = 'Une erreur est survenue';
          }
        }
      });
    } else {
      Object.keys(this.loginForm.controls).forEach(key => {
        this.loginForm.get(key)?.markAsTouched();
      });
    }
  }

}
