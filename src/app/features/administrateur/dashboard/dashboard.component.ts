import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [CommonModule, MainLayoutComponent],
  template: `
    <!-- Utilisation du MainLayout avec role='admin' pour afficher le sidebar admin -->
    <app-main-layout [role]="role">
      <!-- Contenu principal de votre page admin -->
      <div class="min-h-screen bg-gray-50">
        <!-- Header Section -->
        <div class="bg-white border-b border-gray-200 px-6 py-4 mb-8">
          <div class="flex flex-col space-y-4 sm:flex-row sm:items-center sm:justify-between sm:space-y-0">
            <div>
              <h1 class="text-2xl sm:text-3xl font-bold text-gray-900">Administration</h1>
              <p class="text-sm text-gray-600 mt-1">Gestion et configuration du système</p>
            </div>
            <div class="flex items-center space-x-3">
              <button class="bg-gray-100 hover:bg-gray-200 text-gray-700 px-4 py-2 rounded-lg text-sm font-medium transition-colors">
                Paramètres
              </button>
              <button class="bg-orange-500 hover:bg-orange-600 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors">
                Nouvelle action
              </button>
            </div>
          </div>
        </div>

        <!-- Zone de contenu principal -->
        <div class="px-6 pb-8">
          <!-- Vous pouvez ajouter votre contenu ici -->
          <div class="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
            
            <!-- Card exemple 1 -->
            <div class="bg-white rounded-lg p-6 border border-gray-200 hover:shadow-md transition-shadow">
              <div class="flex items-center justify-between mb-4">
                <h3 class="text-lg font-semibold text-gray-900">Gestion des utilisateurs</h3>
                <div class="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                  <svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-9a2.5 2.5 0 11-5 0 2.5 2.5 0 015 0z" />
                  </svg>
                </div>
              </div>
              <p class="text-gray-600 text-sm mb-4">Gérer les comptes utilisateurs et leurs permissions</p>
              <button class="w-full bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors">
                Accéder
              </button>
            </div>

            <!-- Card exemple 2 -->
            <div class="bg-white rounded-lg p-6 border border-gray-200 hover:shadow-md transition-shadow">
              <div class="flex items-center justify-between mb-4">
                <h3 class="text-lg font-semibold text-gray-900">Configuration système</h3>
                <div class="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
                  <svg class="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                </div>
              </div>
              <p class="text-gray-600 text-sm mb-4">Paramètres généraux et configuration avancée</p>
              <button class="w-full bg-green-500 hover:bg-green-600 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors">
                Configurer
              </button>
            </div>

            <!-- Card exemple 3 -->
            <div class="bg-white rounded-lg p-6 border border-gray-200 hover:shadow-md transition-shadow">
              <div class="flex items-center justify-between mb-4">
                <h3 class="text-lg font-semibold text-gray-900">Rapports et analytics</h3>
                <div class="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
                  <svg class="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                  </svg>
                </div>
              </div>
              <p class="text-gray-600 text-sm mb-4">Visualiser les statistiques et générer des rapports</p>
              <button class="w-full bg-purple-500 hover:bg-purple-600 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors">
                Voir les rapports
              </button>
            </div>

          </div>

          <!-- Section additionnelle -->
          <div class="mt-8">
            <div class="bg-white rounded-lg p-6 border border-gray-200">
              <h2 class="text-xl font-semibold text-gray-900 mb-4">Espace de travail</h2>
              <div class="bg-gray-50 rounded-lg p-8 text-center">
                <svg class="w-16 h-16 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                </svg>
                <h3 class="text-lg font-medium text-gray-900 mb-2">Zone de contenu vide</h3>
                <p class="text-gray-600 mb-4">Cette zone est prête pour accueillir votre contenu personnalisé.</p>
                <button class="bg-orange-500 hover:bg-orange-600 text-white px-6 py-2 rounded-lg text-sm font-medium transition-colors">
                  Ajouter du contenu
                </button>
              </div>
            </div>
          </div>

        </div>
      </div>
    </app-main-layout>
  `,
  styles: [`
    /* Styles personnalisés si nécessaire */
    .hover\:shadow-md:hover {
      box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
    }
    
    /* Animation pour les boutons */
    button {
      transition: all 0.2s ease-in-out;
    }
    
    /* Responsive improvements */
    @media (max-width: 640px) {
      .grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class AdminPageComponent {
  // Définir le rôle comme 'admin' pour utiliser le sidebar admin
  role: 'admin' = 'admin';
  
  constructor() {
    // Initialisations si nécessaire
  }

  // Méthodes du composant peuvent être ajoutées ici
  onNewAction(): void {
    console.log('Nouvelle action déclenchée');
  }

  onSettingsClick(): void {
    console.log('Paramètres cliqués');
  }
}