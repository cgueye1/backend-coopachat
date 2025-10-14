import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { CompanyModalComponent, CompanyFormData } from '../../../shared/components/company-modal/company-modal.component';

interface Prospect {
  id: number;
  entreprise: string;
  secteur: string;
  localisation: string;
  contact: {
    nom: string;
    telephone: string;
  };
  statut: string;
  statutColor: string;
  statutBgColor: string;
  date: string;
}

@Component({
  selector: 'app-prospection',
  standalone: true,
  imports: [CommonModule, FormsModule, MainLayoutComponent, CompanyModalComponent],
  template: `
    <app-main-layout role="com">
      <div class="min-h-screen p-4 lg:p-6">
        <!-- Header Section -->
        <div class="mb-6">
          <div class="flex items-center justify-between mb-4">
            <h1 class="text-2xl font-bold text-gray-900">Gestion des Prospections</h1>
            <button 
              class="bg-indigo-900 hover:bg-indigo-800 text-white px-4 py-2 rounded-lg text-sm font-medium flex items-center transition-colors"
              (click)="isCompanyModalOpen = true"
            >
              <svg width="18" height="16" viewBox="0 0 17 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M3.48022 8H12.8136" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M8.14697 3.33337V12.6667" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span class="ml-2">Nouvelle entreprise</span>
            </button>
          </div>
        </div>

        <!-- Company Modal -->
        <app-company-modal 
          [isOpen]="isCompanyModalOpen" 
          (close)="isCompanyModalOpen = false" 
          (submit)="onCompanySubmit($event)"
        ></app-company-modal>

        <!-- Table Section -->
        <div class="bg-white rounded-xl p-4 lg:p-6">
          <!-- Search and Filters Section -->
          <div class="mb-6">
            <div class="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
              <!-- Search Bar -->
              <div class="flex-1 max-w-md w-full">
                <input
                  type="text"
                  placeholder="Rechercher une entreprise..."
                  class="w-full px-4 py-2 border border-gray-50 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none text-sm bg-white"
                  [(ngModel)]="searchTerm"
                  (input)="onSearch()"
                />
              </div>

              <!-- Filters -->
              <div class="flex items-center gap-3 flex-wrap">
                <!-- Filters Button -->
                <button class="flex items-center px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 transition-colors">
                  <svg width="17" height="16" viewBox="0 0 17 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <g clip-path="url(#clip0_83_1008)">
                      <path d="M6.94637 13.3333C6.94632 13.4572 6.98078 13.5787 7.0459 13.6841C7.11102 13.7895 7.20422 13.8746 7.31504 13.93L8.64837 14.5967C8.75004 14.6475 8.863 14.6714 8.97654 14.6663C9.09007 14.6612 9.20041 14.6271 9.29706 14.5673C9.39372 14.5075 9.47349 14.424 9.52879 14.3247C9.58409 14.2254 9.61309 14.1137 9.61304 14V9.33333C9.61319 9.00292 9.73602 8.68433 9.95771 8.43933L14.773 3.11333C14.8593 3.01771 14.9161 2.89912 14.9364 2.77192C14.9568 2.64472 14.9398 2.51435 14.8876 2.39658C14.8355 2.27881 14.7503 2.17868 14.6424 2.1083C14.5345 2.03792 14.4085 2.0003 14.2797 2H2.27971C2.15078 2.00005 2.02463 2.03748 1.91654 2.10776C1.80845 2.17804 1.72306 2.27815 1.67071 2.39598C1.61836 2.5138 1.6013 2.64427 1.62159 2.77159C1.64188 2.89892 1.69866 3.01762 1.78504 3.11333L6.60171 8.43933C6.8234 8.68433 6.94622 9.00292 6.94637 9.33333V13.3333Z" stroke="#374151" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </g>
                    <defs>
                      <clipPath id="clip0_83_1008">
                        <rect width="16" height="16" fill="white" transform="translate(0.279785)"/>
                      </clipPath>
                    </defs>
                  </svg>
                  <span class="ml-2">Filtres</span>
                </button>

                <!-- Status Filter -->
                <select 
                  class="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 bg-white focus:ring-2 focus:ring-blue-800 focus:border-transparent outline-none min-w-[150px]"
                  [(ngModel)]="selectedStatus"
                  (change)="onStatusFilter()"
                >
                  <option value="">Tous les statuts</option>
                  <option value="Intéressé">Intéressé</option>
                  <option value="À relancer">À relancer</option>
                  <option value="Rendez-vous">Rendez-vous</option>
                  <option value="Refusé">Refusé</option>
                  <option value="Premier contact">Premier contact</option>
                </select>

                <!-- Sector Filter -->
                <select 
                  class="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium text-gray-700 bg-white focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none min-w-[150px]"
                  [(ngModel)]="selectedSector"
                  (change)="onSectorFilter()"
                >
                  <option value="">Tous les secteurs</option>
                  <option value="Technologie">Technologie</option>
                  <option value="Santé">Santé</option>
                  <option value="Éducation">Éducation</option>
                </select>
              </div>
            </div>
          </div>

          <!-- Responsive container: Table for lg+, Cards for small -->
          <div class="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">

            <!-- Desktop / large screens TABLE -->
            <div class="hidden lg:block">
              <!-- Table Header - OUTSIDE the scroll container -->
              <div class="bg-gray-50 border-b border-gray-200">
                <div class="px-6 py-4">
                  <div class="grid grid-cols-12 gap-4 text-sm font-semibold text-gray-700">
                    <div class="col-span-2">Entreprise</div>
                    <div class="col-span-2">Secteur</div>
                    <div class="col-span-2">Localisation</div>
                    <div class="col-span-2">Contact</div>
                    <div class="col-span-2">Statut</div>
                    <div class="col-span-1">Date</div>
                    <div class="col-span-1">Actions</div>
                  </div>
                </div>
              </div>

              <!-- Scrollable Table Body -->
              <div class="overflow-x-auto">
                <div class="min-w-full">
                  <!-- Table Body -->
                  <div class="divide-y divide-gray-100">
                    <div 
                      *ngFor="let prospect of getCurrentPageProspects(); trackBy: trackByProspect" 
                      class="px-6 py-4 hover:bg-gray-50 transition-colors duration-200"
                    >
                      <div class="grid grid-cols-12 gap-4 items-center">
                        <!-- Entreprise -->
                        <div class="col-span-2">
                          <div class="font-medium text-gray-900 text-sm">{{ prospect.entreprise }}</div>
                        </div>

                        <!-- Secteur -->
                        <div class="col-span-2">
                          <span class="text-sm text-gray-600">{{ prospect.secteur }}</span>
                        </div>

                        <!-- Localisation -->
                        <div class="col-span-2">
                          <span class="text-sm text-gray-600">{{ prospect.localisation }}</span>
                        </div>

                        <!-- Contact -->
                        <div class="col-span-2">
                          <div class="text-sm">
                            <div class="font-medium text-gray-900">{{ prospect.contact.nom }}</div>
                            <div class="text-gray-500 text-xs">{{ prospect.contact.telephone }}</div>
                          </div>
                        </div>

                        <!-- Statut -->
                        <div class="col-span-2">
                          <span 
                            class="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium"
                            [style.background-color]="prospect.statutBgColor"
                            [style.color]="prospect.statutColor"
                          >
                            {{ prospect.statut }}
                          </span>
                        </div>

                        <!-- Date + Actions: unified flex container to ensure spacing -->
                        <div class="col-span-2">
                          <div class="flex items-center justify-between pr-4">
                            <!-- Date -->
                            <span class="text-sm text-gray-600">
                              {{ prospect.date }}
                            </span>

                            <!-- Actions -->
                            <div class="flex items-center gap-2">
                              <button 
                                class="text-gray-600 text-sm border border-gray-300 px-2 py-1.5 rounded-lg hover:bg-gray-50 transition-colors whitespace-nowrap ml-2"
                                (click)="editProspect(prospect.id)"
                              >
                                Éditer
                              </button>
                              <button 
                                class="bg-indigo-900 hover:bg-indigo-800 text-white text-sm px-3 py-1.5 rounded-lg font-medium transition-colors whitespace-nowrap"
                                (click)="viewDetails(prospect.id)"
                              >
                                Détails
                              </button>
                            </div>
                          </div>
                        </div>

                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Empty State for desktop -->
              <div *ngIf="filteredProspects.length === 0" class="px-6 py-12 text-center">
                <div class="text-gray-400 mb-4">
                  <svg class="mx-auto h-16 w-16" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </div>
                <h3 class="text-lg font-medium text-gray-900 mb-2">Aucun résultat trouvé</h3>
                <p class="text-gray-600 text-sm">Essayez de modifier vos critères de recherche ou filtres.</p>
              </div>
            </div>

            <!-- Mobile / small screens CARDS -->
            <div class="block lg:hidden">
              <div *ngIf="filteredProspects.length === 0" class="px-4 py-8 text-center">
                <div class="text-gray-400 mb-4">
                  <svg class="mx-auto h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                </div>
                <h3 class="text-base font-medium text-gray-900 mb-2">Aucun résultat trouvé</h3>
                <p class="text-gray-600 text-sm">Essayez de modifier vos critères de recherche ou filtres.</p>
              </div>

              <div class="space-y-3 px-2">
                <div *ngFor="let prospect of getCurrentPageProspects(); trackBy: trackByProspect" class="border border-gray-200 rounded-lg p-3 bg-white shadow-sm">
                  <div class="flex items-start justify-between">
                    <div>
                      <div class="font-semibold text-sm text-gray-900">{{ prospect.entreprise }}</div>
                      <div class="text-xs text-gray-500">{{ prospect.secteur }} • {{ prospect.localisation }}</div>
                    </div>
                    <div>
                      <span class="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium" [style.background-color]="prospect.statutBgColor" [style.color]="prospect.statutColor">{{ prospect.statut }}</span>
                    </div>
                  </div>

                  <div class="mt-3 flex items-center justify-between text-xs text-gray-600">
                    <div>
                      <div class="font-medium text-gray-900">{{ prospect.contact.nom }}</div>
                      <div class="text-gray-500">{{ prospect.contact.telephone }}</div>
                    </div>
                    <div class="text-right">
                      <div class="text-gray-500 mb-2">{{ prospect.date }}</div>
                      <div class="mt-2 flex gap-2 justify-end">
                        <button class="text-blue-600 hover:text-blue-800 text-sm font-medium" (click)="editProspect(prospect.id)">Éditer</button>
                        <button class="bg-indigo-900 hover:bg-indigo-800 text-white px-3 py-1 rounded text-xs font-medium" (click)="viewDetails(prospect.id)">Détails</button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

          </div>

          <!-- Pagination Section -->
          <div class="flex items-center justify-between mt-6 pb-8">
            <div class="text-sm text-gray-600">
              Affichage de <span class="font-medium">{{ getDisplayStart() }}</span> à 
              <span class="font-medium">{{ getDisplayEnd() }}</span> sur 
              <span class="font-medium">{{ totalResults }}</span> résultats
            </div>
            
            <div class="flex items-center gap-1">
              <button 
                class="px-3 py-2 text-sm font-medium text-gray-600 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                [disabled]="currentPage === 1"
                (click)="previousPage()"
              >
                <svg width="21" height="20" viewBox="0 0 21 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12.7332 5.50024C12.8657 5.5003 12.993 5.55304 13.0867 5.64673C13.1802 5.74047 13.2332 5.86779 13.2332 6.00024C13.2331 6.13272 13.1803 6.26005 13.0867 6.35376L9.44019 10.0002L13.0798 13.6399V13.6409C13.1709 13.7352 13.2216 13.8613 13.2205 13.9924C13.2192 14.1234 13.1666 14.2484 13.074 14.3411C12.9814 14.4337 12.8563 14.4863 12.7253 14.4875C12.5942 14.4887 12.4671 14.438 12.3728 14.3469L8.37964 10.3538C8.28595 10.26 8.23321 10.1327 8.23315 10.0002C8.23315 9.86769 8.28594 9.74049 8.37964 9.64673L12.3796 5.64673C12.4734 5.55303 12.6006 5.50024 12.7332 5.50024Z" fill="black" stroke="#6B7280"/>
                </svg>
              </button>
              
              <div class="flex items-center gap-1">
                <button 
                  *ngFor="let page of getPageNumbers()" 
                  class="w-9 h-9 md:w-10 md:h-10 text-sm font-medium rounded-lg transition-colors"
                  [class.bg-green-100]="page === currentPage"
                  [class.text-green-700]="page === currentPage"
                  [class.border]="page === currentPage"
                  [class.border-green-200]="page === currentPage"
                  [class.text-gray-600]="page !== currentPage"
                  [class.bg-white]="page !== currentPage"
                  [class.border]="page !== currentPage"
                  [class.border-gray-300]="page !== currentPage"
                  [class.hover:bg-gray-50]="page !== currentPage"
                  (click)="goToPage(page)"
                >
                  {{ page }}
                </button>
              </div>
              
              <button 
                class="px-3 py-2 text-sm font-medium text-gray-600 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                [disabled]="currentPage === totalPages"
                (click)="nextPage()"
              >
                <svg width="21" height="20" viewBox="0 0 21 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M8.62378 5.51233C8.75467 5.51121 8.8811 5.56115 8.97534 5.65198L12.9695 9.64612C13.0631 9.73979 13.1159 9.8672 13.116 9.99963C13.116 10.132 13.063 10.2594 12.9695 10.3531L8.96948 14.3531C8.87577 14.4468 8.74847 14.4996 8.61597 14.4996C8.48338 14.4996 8.35621 14.4469 8.26245 14.3531C8.16881 14.2594 8.11597 14.1321 8.11597 13.9996C8.11607 13.8672 8.16881 13.7398 8.26245 13.6461L11.9089 9.99963L8.26245 6.35315C8.17531 6.25968 8.12669 6.13642 8.12769 6.00842C8.12882 5.87741 8.18158 5.7515 8.27417 5.65881C8.36687 5.56611 8.49268 5.51347 8.62378 5.51233Z" fill="black" stroke="#6B7280"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </app-main-layout>
  `,
  styles: [`
    /* Custom styles for better visual consistency */
    .table-row:hover {
      background-color: #f9fafb;
    }
    
    /* Focus styles for accessibility */
    input:focus, select:focus, button:focus {
      outline: 2px solid transparent;
      outline-offset: 2px;
    }
    
    /* Smooth transitions */
    * {
      transition-property: background-color, border-color, color, fill, stroke;
      transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
      transition-duration: 200ms;
    }

    /* Scrollbar styles (applied to elements with overflow-x-auto) */
    .overflow-x-auto {
      scrollbar-width: thin;
      scrollbar-color: #cbd5e1 #f8fafc;
      position: relative;
    }
    
    .overflow-x-auto::-webkit-scrollbar {
      height: 8px;
    }
    
    .overflow-x-auto::-webkit-scrollbar-track {
      background: #f8fafc;
      border-radius: 4px;
    }
    
    .overflow-x-auto::-webkit-scrollbar-thumb {
      background: #cbd5e1;
      border-radius: 4px;
    }
    
    .overflow-x-auto::-webkit-scrollbar-thumb:hover {
      background: #94a3b8;
    }

    /* Responsive tweaks: ensure grids collapse nicely on small screens */
    @media (max-width: 1024px) {
      .grid-cols-12 { grid-template-columns: repeat(12, minmax(0, 1fr)); }
    }

    /* Improve touch target sizes on small screens */
    @media (max-width: 640px) {
      button, .px-4, .py-2 {
        min-height: 40px;
      }
    }

    /* Ensure cards and table cells wrap text properly */
    .whitespace-nowrap { white-space: nowrap; }
  `]
})
export class ProspectionComponent {
  searchTerm = '';
  selectedStatus = '';
  selectedSector = '';
  currentPage = 1;
  itemsPerPage = 5;
  isCompanyModalOpen = false;

  prospects: Prospect[] = [
    { id: 1, entreprise: 'Entreprise LMN', secteur: 'Technologie', localisation: 'Paris', contact: { nom: 'Marie Dupont', telephone: '01 23 45 67 89' }, statut: 'Intéressé', statutColor: '#065f46', statutBgColor: '#d1fae5', date: '15/07/2023' },
    { id: 2, entreprise: 'Société PQR', secteur: 'Santé', localisation: 'Lyon', contact: { nom: 'Jean Martin', telephone: '01 98 76 54 32' }, statut: 'À relancer', statutColor: '#92400e', statutBgColor: '#fef3c7', date: '22/07/2023' },
    { id: 3, entreprise: 'Groupe 456', secteur: 'Éducation', localisation: 'Marseille', contact: { nom: 'Sophie Lefebvre', telephone: '04 56 78 90 12' }, statut: 'Rendez-vous', statutColor: '#1e40af', statutBgColor: '#dbeafe', date: '28/07/2023' },
    { id: 4, entreprise: 'Tech Innovate', secteur: 'Technologie', localisation: 'Toulouse', contact: { nom: 'Pierre Durand', telephone: '05 43 21 98 76' }, statut: 'Refusé', statutColor: '#dc2626', statutBgColor: '#fee2e2', date: '10/07/2023' },
    { id: 5, entreprise: 'Médical Plus', secteur: 'Santé', localisation: 'Bordeaux', contact: { nom: 'Isabelle Moreau', telephone: '05 67 89 01 23' }, statut: 'Premier contact', statutColor: '#7c3aed', statutBgColor: '#ede9fe', date: '05/07/2023' },
    { id: 6, entreprise: 'StartupXYZ', secteur: 'Technologie', localisation: 'Nice', contact: { nom: 'Lucas Bernard', telephone: '04 93 12 34 56' }, statut: 'Intéressé', statutColor: '#065f46', statutBgColor: '#d1fae5', date: '01/08/2023' },
    { id: 7, entreprise: 'Clinique Santé+', secteur: 'Santé', localisation: 'Strasbourg', contact: { nom: 'Dr. Anne Durand', telephone: '03 88 45 67 89' }, statut: 'Rendez-vous', statutColor: '#1e40af', statutBgColor: '#dbeafe', date: '12/08/2023' },
    { id: 8, entreprise: 'École Future', secteur: 'Éducation', localisation: 'Nantes', contact: { nom: 'Marie-Claire Petit', telephone: '02 40 11 22 33' }, statut: 'Premier contact', statutColor: '#7c3aed', statutBgColor: '#ede9fe', date: '20/08/2023' },
    { id: 9, entreprise: 'Digital Solutions', secteur: 'Technologie', localisation: 'Lille', contact: { nom: 'Thomas Leroy', telephone: '03 20 55 66 77' }, statut: 'À relancer', statutColor: '#92400e', statutBgColor: '#fef3c7', date: '25/08/2023' },
    { id: 10, entreprise: 'Pharma Conseil', secteur: 'Santé', localisation: 'Montpellier', contact: { nom: 'Sylvie Roux', telephone: '04 67 89 12 34' }, statut: 'Refusé', statutColor: '#dc2626', statutBgColor: '#fee2e2', date: '30/08/2023' },
    { id: 11, entreprise: 'Université Privée', secteur: 'Éducation', localisation: 'Rennes', contact: { nom: 'Prof. Michel Blanc', telephone: '02 99 44 55 66' }, statut: 'Intéressé', statutColor: '#065f46', statutBgColor: '#d1fae5', date: '05/09/2023' },
    { id: 12, entreprise: 'Innovation Lab', secteur: 'Technologie', localisation: 'Grenoble', contact: { nom: 'Sarah Martin', telephone: '04 76 12 34 56' }, statut: 'Rendez-vous', statutColor: '#1e40af', statutBgColor: '#dbeafe', date: '10/09/2023' }
  ];

  filteredProspects: Prospect[] = [...this.prospects];

  // totalResults is dynamic now
  get totalResults(): number {
    return this.filteredProspects.length;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalResults / this.itemsPerPage));
  }

  // Retourne seulement les prospects de la page courante
  getCurrentPageProspects(): Prospect[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredProspects.slice(startIndex, endIndex);
  }

  trackByProspect(index: number, prospect: Prospect): number {
    return prospect.id;
  }

  onSearch(): void {
    this.filterProspects();
  }

  onStatusFilter(): void {
    this.filterProspects();
  }

  onSectorFilter(): void {
    this.filterProspects();
  }

  filterProspects(): void {
    let filtered = [...this.prospects];

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase().trim();
      filtered = filtered.filter(prospect =>
        prospect.entreprise.toLowerCase().includes(term) ||
        prospect.contact.nom.toLowerCase().includes(term) ||
        prospect.localisation.toLowerCase().includes(term)
      );
    }

    if (this.selectedStatus) {
      filtered = filtered.filter(prospect => prospect.statut === this.selectedStatus);
    }

    if (this.selectedSector) {
      filtered = filtered.filter(prospect => prospect.secteur === this.selectedSector);
    }

    this.filteredProspects = filtered;
    this.currentPage = 1; // Reset à la première page après filtrage
  }

  editProspect(id: number): void {
    console.log('Éditer prospect:', id);
    // Implémentation de l'édition
  }

  viewDetails(id: number): void {
    console.log('Voir détails prospect:', id);
    // Implémentation de la vue détaillée
  }

  getDisplayStart(): number {
    if (this.totalResults === 0) return 0;
    return (this.currentPage - 1) * this.itemsPerPage + 1;
  }

  getDisplayEnd(): number {
    return Math.min(this.currentPage * this.itemsPerPage, this.totalResults);
  }

  getPageNumbers(): number[] {
    const pages = [];
    const maxVisiblePages = 5;
    let startPage = Math.max(1, this.currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(this.totalPages, startPage + maxVisiblePages - 1);

    if (endPage - startPage + 1 < maxVisiblePages) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    return pages;
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  onCompanySubmit(data: CompanyFormData): void {
    console.log('Nouvelle entreprise ajoutée:', data);
    // Ajouter la logique pour traiter les données soumises (par ex. ajouter à prospects)
    this.isCompanyModalOpen = false;
  }
}
