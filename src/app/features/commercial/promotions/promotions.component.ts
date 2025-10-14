import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { CouponModalComponent, CouponFormData } from '../../../shared/components/coupon-modal/coupon-modal.component';

interface Promotion {
  id: number;
  nom: string;
  reduction: string;
  produits: string;
  validite: string;
  icon: string;
  statut: 'Actif' | 'Expiré' | 'Planifié';
  utilisations: number;
  montantGenere: string;
}

@Component({
  selector: 'app-promotions',
  standalone: true,
  imports: [CommonModule, FormsModule, MainLayoutComponent, CouponModalComponent],
  template: `
    <app-main-layout role="com">
      <div class="bg-white rounded-lg shadow-sm">
        <!-- Header Section -->
        <div class="p-6 border-b border-gray-200">
          <div class="flex justify-between items-center mb-6">
            <h1 class="text-2xl font-semibold text-gray-900">Gestion des Promotions</h1>
            <button 
              (click)="openCouponModal()"
              class="bg-[#2C2D5B] hover:bg-[#24254F] text-white px-4 py-2 rounded-lg flex items-center gap-2 transition-colors"
            >
              <svg width="17" height="16" viewBox="0 0 17 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M3.36328 8H12.6966" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M8.02979 3.33337V12.6667" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              Créer un coupon
            </button>
          </div>

          <!-- Stats Cards -->
          <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
            <div class=" bg-gray-50 p-4 rounded-lg">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-xs mb-1">Promotions actives</p>
                  <p class="text-3xl font-bold">{{ getActivePromotions() }}</p>
                </div>
                <div class="w-10 h-10 bg-green-200 rounded-full flex items-center justify-center">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M12.586 2.586C12.211 2.2109 11.7024 2.00011 11.172 2H4C3.46957 2 2.96086 2.21071 2.58579 2.58579C2.21071 2.96086 2 3.46957 2 4V11.172C2.00011 11.7024 2.2109 12.211 2.586 12.586L11.29 21.29C11.7445 21.7416 12.3592 21.9951 13 21.9951C13.6408 21.9951 14.2555 21.7416 14.71 21.29L21.29 14.71C21.7416 14.2555 21.9951 13.6408 21.9951 13C21.9951 12.3592 21.7416 11.7445 21.29 11.29L12.586 2.586Z" stroke="#16A34A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M7.5 8C7.77614 8 8 7.77614 8 7.5C8 7.22386 7.77614 7 7.5 7C7.22386 7 7 7.22386 7 7.5C7 7.77614 7.22386 8 7.5 8Z" fill="black" stroke="#16A34A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </div>
              </div>
            </div>
            
            <div class="bg-gray-50 p-4 rounded-lg">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-xs mb-1">Utilisations totales</p>
                  <p class="text-3xl font-bold">{{ getTotalUtilisations() }}</p>
                </div>
                <div class="w-10 h-10 bg-blue-200 rounded-full flex items-center justify-center">
                  <svg width="25" height="24" viewBox="0 0 25 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M8.61572 22C9.16801 22 9.61572 21.5523 9.61572 21C9.61572 20.4477 9.16801 20 8.61572 20C8.06344 20 7.61572 20.4477 7.61572 21C7.61572 21.5523 8.06344 22 8.61572 22Z" stroke="#2C2D5B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M19.6157 22C20.168 22 20.6157 21.5523 20.6157 21C20.6157 20.4477 20.168 20 19.6157 20C19.0634 20 18.6157 20.4477 18.6157 21C18.6157 21.5523 19.0634 22 19.6157 22Z" stroke="#2C2D5B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M2.66553 2.05005H4.66553L7.32553 14.47C7.42311 14.9249 7.6762 15.3315 8.04124 15.6199C8.40628 15.9083 8.86043 16.0604 9.32553 16.05H19.1055C19.5607 16.0493 20.002 15.8933 20.3566 15.6079C20.7111 15.3224 20.9577 14.9246 21.0555 14.48L22.7055 7.05005H5.73553" stroke="#2C2D5B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </div>
              </div>
            </div>

            <div class="bg-gray-50 p-4 rounded-lg ">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-xs mb-1">Montant généré</p>
                  <p class="text-3xl font-bold">{{ getTotalMontant() }}</p>
                </div>
                <div class="w-10 h-10 bg-purple-200 rounded-full flex items-center justify-center">
                 <svg width="25" height="24" viewBox="0 0 25 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M19.6157 5L5.61572 19" stroke="#9333EA" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M7.11572 9C8.49643 9 9.61572 7.88071 9.61572 6.5C9.61572 5.11929 8.49643 4 7.11572 4C5.73501 4 4.61572 5.11929 4.61572 6.5C4.61572 7.88071 5.73501 9 7.11572 9Z" stroke="#9333EA" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M18.1157 20C19.4964 20 20.6157 18.8807 20.6157 17.5C20.6157 16.1193 19.4964 15 18.1157 15C16.735 15 15.6157 16.1193 15.6157 17.5C15.6157 18.8807 16.735 20 18.1157 20Z" stroke="#9333EA" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </div>
              </div>
            </div>

            <div class=" bg-gray-50 p-4 rounded-lg">
              <div class="flex items-center justify-between">
                <div>
                  <p class="text-xs mb-1">Panier moyen</p>
                  <p class="text-3xl font-bold">{{ getPanierMoyen() }}</p>
                </div>
                <div class="w-10 h-10 bg-orange-200 rounded-full flex items-center justify-center">
                  <svg width="25" height="24" viewBox="0 0 25 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M11.6157 21.73C11.9198 21.9056 12.2646 21.998 12.6157 21.998C12.9668 21.998 13.3117 21.9056 13.6157 21.73L20.6157 17.73C20.9195 17.5547 21.1717 17.3025 21.3473 16.9989C21.5228 16.6952 21.6154 16.3508 21.6157 16V8.00002C21.6154 7.6493 21.5228 7.30483 21.3473 7.00119C21.1717 6.69754 20.9195 6.44539 20.6157 6.27002L13.6157 2.27002C13.3117 2.09449 12.9668 2.00208 12.6157 2.00208C12.2646 2.00208 11.9198 2.09449 11.6157 2.27002L4.61572 6.27002C4.31199 6.44539 4.0597 6.69754 3.88418 7.00119C3.70867 7.30483 3.61608 7.6493 3.61572 8.00002V16C3.61608 16.3508 3.70867 16.6952 3.88418 16.9989C4.0597 17.3025 4.31199 17.5547 4.61572 17.73L11.6157 21.73Z" stroke="#EA580C" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M12.6157 22V12" stroke="#EA580C" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M3.90576 7L12.6158 12L21.3258 7" stroke="#EA580C" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M8.11572 4.27002L17.1157 9.42002" stroke="#EA580C" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </div>
              </div>
            </div>
          </div>

          <!-- Search and Filters -->
          <div class="flex flex-col sm:flex-row gap-4 items-center">
            <div class="flex-1 relative">
              <input
                type="text"
                [(ngModel)]="searchTerm"
                (input)="filterPromotions()"
                placeholder="Rechercher un coupon..."
                class="w-full pl-4 pr-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-[#2C2D5B] focus:border-transparent outline-none"
              >
            </div>
            <div class="relative">
              <select 
                [(ngModel)]="selectedStatusFilter"
                (change)="filterPromotions()"
                class="appearance-none bg-white border border-gray-200 rounded-lg px-4 py-2 pr-8 focus:ring-2 focus:ring-[#2C2D5B] focus:border-transparent outline-none"
              >
                <option value="">Tous les statuts</option>
                <option value="Actif">Actif</option>
                <option value="Expiré">Expiré</option>
                <option value="Planifié">Planifié</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Table -->
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead class="bg-gray-50">
              <tr>
                <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Nom du coupon</th>
                <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Réduction</th>
                <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Produits</th>
                <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Validité</th>
                <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Statut</th>
                <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Utilisations</th>
                <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Montant généré</th>
                <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr *ngFor="let promotion of filteredPromotions" class="hover:bg-gray-50 transition-colors">
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  {{ promotion.nom }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                  {{ promotion.reduction }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                  {{ promotion.produits }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600"    >
                  <span class="inline-flex">
                        <svg width="17" height="16" viewBox="0 0 17 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                          <path d="M5.51172 1.33337V4.00004" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                          <path d="M10.8447 1.33337V4.00004" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                          <path d="M12.8449 2.66663H3.51156C2.77518 2.66663 2.17822 3.26358 2.17822 3.99996V13.3333C2.17822 14.0697 2.77518 14.6666 3.51156 14.6666H12.8449C13.5813 14.6666 14.1782 14.0697 14.1782 13.3333V3.99996C14.1782 3.26358 13.5813 2.66663 12.8449 2.66663Z" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                          <path d="M2.17822 6.66663H14.1782" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        </svg>
                  </span>
                  {{ promotion.validite }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <span 
                    class="inline-flex items-center gap-2 px-3 py-1 rounded-full text-sm font-medium whitespace-nowrap"
                    [ngClass]="{
                      'bg-green-100 text-green-800': promotion.statut === 'Actif',
                      'bg-red-100 text-red-800': promotion.statut === 'Expiré',
                      'bg-blue-100 text-blue-800': promotion.statut === 'Planifié'
                    }"
                  >
                    <img 
                      class="w-5 h-5 flex-shrink-0"
                      [src]="promotion.icon"
                    >
                    {{ promotion.statut }}
                  </span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                  {{ promotion.utilisations }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                  {{ promotion.montantGenere }}
                </td>
                <td class="flex gap-2 px-6 py-4 whitespace-nowrap text-sm text-gray-600 justify-end">
                  <button 
                    (click)="viewDetails(promotion.id)"
                    class="flex items-center gap-2 px-3 py-1.5 border hover:bg-gray-200 text-gray-700 rounded-lg transition-colors"
                  >
                  
                    <svg width="14" height="15" viewBox="0 0 14 15" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <g clip-path="url(#clip0_44_1067)">
                      <path d="M1.19491 7.70297C1.14629 7.572 1.14629 7.42793 1.19491 7.29697C1.6684 6.14888 2.47213 5.16724 3.50419 4.47649C4.53626 3.78574 5.75018 3.41699 6.99208 3.41699C8.23397 3.41699 9.44789 3.78574 10.48 4.47649C11.512 5.16724 12.3157 6.14888 12.7892 7.29697C12.8379 7.42793 12.8379 7.572 12.7892 7.70297C12.3157 8.85105 11.512 9.83269 10.48 10.5234C9.44789 11.2142 8.23397 11.5829 6.99208 11.5829C5.75018 11.5829 4.53626 11.2142 3.50419 10.5234C2.47213 9.83269 1.6684 8.85105 1.19491 7.70297Z" stroke="#374151" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M6.99219 9.25C7.95869 9.25 8.74219 8.4665 8.74219 7.5C8.74219 6.5335 7.95869 5.75 6.99219 5.75C6.02569 5.75 5.24219 6.5335 5.24219 7.5C5.24219 8.4665 6.02569 9.25 6.99219 9.25Z" stroke="#374151" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      </g>
                      <defs>
                      <clipPath id="clip0_44_1067">
                      <rect width="14" height="14" fill="white" transform="translate(-0.0078125 0.5)"/>
                      </clipPath>
                      </defs>
                    </svg>
                    Détails
                  </button>
                  <button 
                    *ngIf="promotion.statut === 'Actif'"
                    (click)="togglePromotionStatus(promotion.id, 'desactiver')"
                    class="bg-[#2C2D5B] hover:bg-[#24254F] text-white px-4 py-2 rounded-lg"
                  >
                    Désactiver
                  </button>
                  
                  <button 
                    *ngIf="promotion.statut === 'Planifié'"
                    (click)="togglePromotionStatus(promotion.id, 'activer')"
                    class="bg-[#2C2D5B] hover:bg-[#24254F] text-white px-4 py-2 rounded-lg"
                  >
                    Activer
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div class="px-6 py-4 border-t border-gray-200 flex items-center justify-between">
          <div class="text-sm text-gray-600">
            Affichage de {{ (currentPage - 1) * itemsPerPage + 1 }} à {{ Math.min(currentPage * itemsPerPage, filteredPromotions.length) }} sur {{ filteredPromotions.length }} résultats
          </div>
          <div class="flex items-center gap-2">
            <button 
              (click)="previousPage()"
              [disabled]="currentPage === 1"
              class="px-3 py-1 border border-gray-300 rounded-md text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
              </svg>
            </button>
            <span class="px-3 py-1 bg-[#2C2D5B] text-white rounded-md text-sm">{{ currentPage }}</span>
            <button 
              (click)="nextPage()"
              [disabled]="currentPage === getTotalPages()"
              class="px-3 py-1 border border-gray-300 rounded-md text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
              </svg>
            </button>
          </div>
        </div>
      </div>
    <div class="bg-gray-50 rounded-lg p-6 mt-6 border border-gray-200 shadow-sm w-full">
  <!-- Titre avec wrapper -->
  <div class="w-full pb-2 border-b border-gray-200 mb-6">
    <h3 class="text-lg font-semibold text-gray-900">
      Guide d'utilisation des coupons
    </h3>
  </div>
  <div class="space-y-6">
    <!-- Visibilité pour les salariés -->
    <div class="flex items-start space-x-4">
      <div class="flex-shrink-0 w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
        <!-- icône -->
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M13.3334 17.5V15.8333C13.3334 14.9493 12.9822 14.1014 12.3571 13.4763C11.732 12.8512 10.8841 12.5 10.0001 12.5H5.00008C4.11603 12.5 3.26818 12.8512 2.64306 13.4763C2.01794 14.1014 1.66675 14.9493 1.66675 15.8333V17.5" stroke="#2C2D5B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M13.3333 2.60645C14.048 2.79175 14.6811 3.20917 15.133 3.79316C15.5849 4.37716 15.8301 5.09469 15.8301 5.83311C15.8301 6.57154 15.5849 7.28906 15.133 7.87306C14.6811 8.45706 14.048 8.87447 13.3333 9.05978" stroke="#2C2D5B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M18.3333 17.5001V15.8334C18.3327 15.0948 18.0869 14.3774 17.6344 13.7937C17.1819 13.2099 16.5484 12.793 15.8333 12.6084" stroke="#2C2D5B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          <path d="M7.50008 9.16667C9.34103 9.16667 10.8334 7.67428 10.8334 5.83333C10.8334 3.99238 9.34103 2.5 7.50008 2.5C5.65913 2.5 4.16675 3.99238 4.16675 5.83333C4.16675 7.67428 5.65913 9.16667 7.50008 9.16667Z" stroke="#2C2D5B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <div class="flex-1">
        <h4 class="text-base font-medium text-gray-900 mb-2">Visibilité pour les salariés</h4>
        <p class="text-sm text-gray-600 leading-relaxed">
          Une fois activé, le coupon est automatiquement visible par les salariés dans leur espace personnel, dans la section "Coupons".
        </p>
      </div>
    </div>

    <!-- Application lors de la commande -->
    <div class="flex items-start space-x-4">
      <div class="flex-shrink-0 w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
        <!-- icône -->
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
          <g clip-path="url(#clip0_44_1143)">
            <path d="M6.66659 18.3332C7.12682 18.3332 7.49992 17.9601 7.49992 17.4998C7.49992 17.0396 7.12682 16.6665 6.66659 16.6665C6.20635 16.6665 5.83325 17.0396 5.83325 17.4998C5.83325 17.9601 6.20635 18.3332 6.66659 18.3332Z" stroke="#2C2D5B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M15.8333 18.3332C16.2936 18.3332 16.6667 17.9601 16.6667 17.4998C16.6667 17.0396 16.2936 16.6665 15.8333 16.6665C15.3731 16.6665 15 17.0396 15 17.4998C15 17.9601 15.3731 18.3332 15.8333 18.3332Z" stroke="#2C2D5B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M1.70825 1.7085H3.37492L5.59159 12.0585C5.6729 12.4375 5.88381 12.7764 6.18801 13.0167C6.49221 13.257 6.87067 13.3838 7.25825 13.3752H15.4083C15.7876 13.3745 16.1553 13.2446 16.4508 13.0067C16.7462 12.7688 16.9517 12.4373 17.0333 12.0668L18.4083 5.87516H4.26659" stroke="#2C2D5B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </g>
          <defs>
            <clipPath id="clip0_44_1143">
              <rect width="20" height="20" fill="white"/>
            </clipPath>
          </defs>
        </svg>
      </div>
      <div class="flex-1">
        <h4 class="text-base font-medium text-gray-900 mb-2">Application lors de la commande</h4>
        <p class="text-sm text-gray-600 leading-relaxed">
          Lorsqu'un salarié passe une commande, il peut saisir manuellement le code du coupon ou cliquer sur un bouton "Appliquer" si le coupon est proposé automatiquement. Le montant total de la commande est mis à jour avec la réduction de 5%, visible avant validation du paiement.
        </p>
      </div>
    </div>

    <!-- Suivi des performances -->
    <div class="flex items-start space-x-4">
      <div class="flex-shrink-0 w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
        <!-- icône -->
        <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
          <g clip-path="url(#clip0_44_1155)">
            <path d="M10.4884 2.15484C10.1759 1.84225 9.75207 1.6666 9.31008 1.6665H3.33341C2.89139 1.6665 2.46746 1.8421 2.1549 2.15466C1.84234 2.46722 1.66675 2.89114 1.66675 3.33317V9.30984C1.66684 9.75183 1.8425 10.1757 2.15508 10.4882L9.40842 17.7415C9.78718 18.1179 10.2995 18.3291 10.8334 18.3291C11.3674 18.3291 11.8797 18.1179 12.2584 17.7415L17.7417 12.2582C18.1181 11.8794 18.3294 11.3671 18.3294 10.8332C18.3294 10.2992 18.1181 9.78693 17.7417 9.40817L10.4884 2.15484Z" stroke="#2C2D5B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M6.24992 6.66683C6.48004 6.66683 6.66659 6.48028 6.66659 6.25016C6.66659 6.02004 6.48004 5.8335 6.24992 5.8335C6.0198 5.8335 5.83325 6.02004 5.83325 6.25016C5.83325 6.48028 6.0198 6.66683 6.24992 6.66683Z" fill="black" stroke="#2C2D5B" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </g>
          <defs>
            <clipPath id="clip0_44_1155">
              <rect width="20" height="20" fill="white"/>
            </clipPath>
          </defs>
        </svg>
      </div>
      <div class="flex-1">
        <h4 class="text-base font-medium text-gray-900 mb-2">Suivi des performances</h4>
        <p class="text-sm text-gray-600 leading-relaxed">
          Vous pouvez suivre les performances du coupon depuis votre tableau de bord : nombre d'utilisations, montant total généré via ce coupon, et panier moyen des commandes avec coupon.
        </p>
      </div>
    </div>
  </div>
</div>

      <!-- Modal de création de coupon -->
      <app-coupon-modal
        [isOpen]="isModalOpen"
        [isSubmitting]="isSubmitting"
        (close)="closeCouponModal()"
        (submitCoupon)="onSubmitCoupon($event)"
      ></app-coupon-modal>
    </app-main-layout>
  `
})
export class PromotionsManagementComponent {

  searchTerm: string = '';
  selectedStatusFilter: string = '';
  currentPage: number = 1;
  itemsPerPage: number = 4;
  Math = Math;

  // Propriétés pour le modal
  isModalOpen: boolean = false;
  isSubmitting: boolean = false;

  promotions: Promotion[] = [
    {
      id: 1,
      nom: 'Rentrée 2023',
      reduction: '5%',
      produits: 'Tous les produits',
      validite: '01/09/2023 - 15/09/2023',
      icon: "/icones/actif.svg",
      statut: 'Actif',
      utilisations: 124,
      montantGenere: '8 700 €'
    },
    {
      id: 2,
      nom: 'Été 2023',
      reduction: '5%',
      produits: 'Catégorie Électroménager',
      validite: '15/06/2023 - 31/07/2023',
      icon: "/icones/inactif.svg",
      statut: 'Expiré',
      utilisations: 215,
      montantGenere: '15 000 €'
    },
    {
      id: 3,
      nom: 'Bienvenue Entreprise ABC',
      reduction: '5%',
      produits: 'Tous les produits',
      validite: '01/07/2023 - 31/07/2023',
      icon: "/icones/inactif.svg",
      statut: 'Expiré',
      utilisations: 45,
      montantGenere: '3 200 €'
    },
    {
      id: 4,
      nom: 'Black Friday 2023',
      reduction: '5%',
      produits: 'Tous les produits',
      validite: '24/11/2023 - 27/11/2023',
      icon: "/icones/attente.svg",
      statut: 'Planifié',
      utilisations: 0,
      montantGenere: '- €'
    }
  ];

  filteredPromotions: Promotion[] = [...this.promotions];

  constructor() {
    this.filterPromotions();
  }

  // Méthodes pour le modal
  openCouponModal(): void {
    this.isModalOpen = true;
  }

  closeCouponModal(): void {
    this.isModalOpen = false;
  }

  onSubmitCoupon(couponData: CouponFormData): void {
    this.isSubmitting = true;

    // Simuler un appel API
    setTimeout(() => {
      console.log('Nouveau coupon créé:', couponData);

      // Créer une nouvelle promotion à partir des données du formulaire
      const newPromotion: Promotion = {
        id: this.promotions.length + 1,
        nom: couponData.nom,
        reduction: couponData.taux,
        produits: couponData.produits.length > 0 ? `${couponData.produits.length} produit(s) sélectionné(s)` : 'Tous les produits',
        validite: `${couponData.dateDebut} - ${couponData.dateFin}`,
        icon: '/icones/attente.svg',
        statut: 'Planifié',
        utilisations: 0,
        montantGenere: '- €'
      };

      // Ajouter la nouvelle promotion
      this.promotions.push(newPromotion);
      this.filterPromotions();

      // Fermer le modal
      this.isSubmitting = false;
      this.closeCouponModal();

      // Optionnel : afficher un message de succès
      alert('Coupon créé avec succès !');
    }, 2000);
  }

  getActivePromotions(): number {
    return this.promotions.filter(promo => promo.statut === 'Actif').length;
  }

  getTotalUtilisations(): number {
    return this.promotions.reduce((total, promo) => total + promo.utilisations, 0);
  }

  getTotalMontant(): string {
    const total = this.promotions
      .filter(promo => promo.montantGenere !== '- €')
      .reduce((sum, promo) => {
        const amount = parseInt(promo.montantGenere.replace(/[^\d]/g, ''));
        return sum + amount;
      }, 0);
    return `${total.toLocaleString()} €`;
  }

  getPanierMoyen(): string {
    return '72 €';
  }

  filterPromotions(): void {
    this.filteredPromotions = this.promotions.filter(promotion => {
      const matchesSearch = promotion.nom.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        promotion.produits.toLowerCase().includes(this.searchTerm.toLowerCase());

      const matchesStatus = !this.selectedStatusFilter || promotion.statut === this.selectedStatusFilter;

      return matchesSearch && matchesStatus;
    });

    this.currentPage = 1;
  }

  getTotalPages(): number {
    return Math.ceil(this.filteredPromotions.length / this.itemsPerPage);
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.getTotalPages()) {
      this.currentPage++;
    }
  }

  viewDetails(id: number): void {
    console.log('Afficher les détails de la promotion:', id);
    // Ici vous pouvez implémenter la navigation vers les détails
  }

  // Et ajouter cette méthode dans la classe du composant :
  togglePromotionStatus(promotionId: number, action: 'activer' | 'desactiver'): void {
    const promotion = this.promotions.find(p => p.id === promotionId);
    if (promotion) {
      if (action === 'activer') {
        promotion.statut = 'Actif';
        promotion.icon = '/icones/actif.svg';
        console.log(`Promotion "${promotion.nom}" activée`);
      } else if (action === 'desactiver') {
        promotion.statut = 'Planifié';
        promotion.icon = '/icones/attente.svg';
        console.log(`Promotion "${promotion.nom}" désactivée`);
      }

      // Rafraîchir les promotions filtrées
      this.filterPromotions();

      // Optionnel : afficher un message de confirmation
      const actionText = action === 'activer' ? 'activée' : 'désactivée';
      alert(`Promotion "${promotion.nom}" ${actionText} avec succès !`);
    }
  }
}