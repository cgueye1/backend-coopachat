import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthLayoutComponent } from '../auth-layout/auth-layout.component';
// TODO: Importer le service d'authentification
// import { AuthService } from '../services/auth.service';
// TODO: Importer Router pour la navigation après connexion
// import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, AuthLayoutComponent],
  templateUrl: `login.component.html`,
  styles: []
})
export class LoginComponent {
  loginForm: FormGroup;
  showPassword = false;

  // TODO: Ajouter ces propriétés pour gérer l'état de l'API
  // isLoading = false;
  // errorMessage = '';

  constructor(
    private fb: FormBuilder
    // TODO: Injecter le service d'authentification
    // private authService: AuthService,
    // TODO: Injecter le Router pour la navigation
    // private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      rememberMe: [false]
    });
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      console.log('Form submitted:', this.loginForm.value);

      // TODO: ============ INTÉGRATION API - DÉBUT ============
      // this.isLoading = true;
      // this.errorMessage = '';

      // const credentials = {
      //   username: this.loginForm.value.username,
      //   password: this.loginForm.value.password,
      //   rememberMe: this.loginForm.value.rememberMe
      // };

      // this.authService.login(credentials).subscribe({
      //   next: (response) => {
      //     // Succès de la connexion
      //     this.isLoading = false;
      //     console.log('Connexion réussie:', response);
      //     
      //     // Stocker le token (si votre API retourne un token)
      //     // localStorage.setItem('token', response.token);
      //     // localStorage.setItem('user', JSON.stringify(response.user));
      //     
      //     // Rediriger vers le dashboard ou la page d'accueil
      //     // this.router.navigate(['/dashboard']);
      //   },
      //   error: (error) => {
      //     // Gestion des erreurs
      //     this.isLoading = false;
      //     console.error('Erreur de connexion:', error);
      //     
      //     // Afficher un message d'erreur approprié
      //     if (error.status === 401) {
      //       this.errorMessage = 'Nom d\'utilisateur ou mot de passe incorrect';
      //     } else if (error.status === 0) {
      //       this.errorMessage = 'Impossible de se connecter au serveur';
      //     } else {
      //       this.errorMessage = error.error?.message || 'Une erreur est survenue';
      //     }
      //   }
      // });
      // TODO: ============ INTÉGRATION API - FIN ============

    } else {
      console.log('Form is invalid');
      // Marquer tous les champs comme touchés pour afficher les erreurs
      Object.keys(this.loginForm.controls).forEach(key => {
        this.loginForm.get(key)?.markAsTouched();
      });
    }
  }

  // TODO: Méthode pour la connexion Google (OAuth)
  // loginWithGoogle(): void {
  //   this.authService.loginWithGoogle().subscribe({
  //     next: (response) => {
  //       // Gérer la réponse de Google OAuth
  //       console.log('Connexion Google réussie:', response);
  //       // this.router.navigate(['/dashboard']);
  //     },
  //     error: (error) => {
  //       console.error('Erreur connexion Google:', error);
  //       this.errorMessage = 'Erreur lors de la connexion avec Google';
  //     }
  //   });
  // }

  // TODO: Méthode pour la connexion Apple (OAuth)
  // loginWithApple(): void {
  //   this.authService.loginWithApple().subscribe({
  //     next: (response) => {
  //       // Gérer la réponse de Apple OAuth
  //       console.log('Connexion Apple réussie:', response);
  //       // this.router.navigate(['/dashboard']);
  //     },
  //     error: (error) => {
  //       console.error('Erreur connexion Apple:', error);
  //       this.errorMessage = 'Erreur lors de la connexion avec Apple';
  //     }
  //   });
  // }
}