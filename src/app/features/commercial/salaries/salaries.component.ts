import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { EmployeeModalComponent, EmployeeFormData, Company } from '../../../shared/components/employee-modal/employee-modal.component';

interface Employee {
  id: number;
  nom: string;
  email: string;
  telephone: string;
  entreprise: string;
  statut: 'Actif' | 'En attente' | 'Inactif';
  dateInscription: string;
}

@Component({
  selector: 'app-salaries',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MainLayoutComponent,
    EmployeeModalComponent // Importez le composant modal
  ],
  template: `
    <app-main-layout role="com">
      <div class="bg-white rounded-lg shadow-sm">
        <!-- Header Section -->
        <div class="p-6 border-b border-gray-200">
          <div class="flex justify-between items-center mb-6">
            <h1 class="text-2xl font-semibold text-gray-900">Gestion des Salariés</h1>
            <button
              (click)="openModal()"
              class="bg-[#2C2D5B] hover:bg-[#3B3C7A] text-white px-4 py-2 rounded-lg flex items-center gap-2 transition-colors"
            >
              <svg width="17" height="16" viewBox="0 0 17 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M3.36328 8H12.6966" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M8.02979 3.33337V12.6667" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              Inscrire un salarié
            </button>
          </div>

          <!-- Stats Cards -->
          <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
            <div class="bg-gray-50 p-4 rounded-lg">
              <p class="text-sm text-gray-600 mb-1">Total des salariés</p>
              <p class="text-3xl font-bold text-gray-900">{{ getTotalEmployees() }}</p>
            </div>
            <div class="bg-gray-50 p-4 rounded-lg">
              <p class="text-sm text-gray-600 mb-1">Salariés actifs</p>
              <p class="text-3xl font-bold text-gray-900">{{ getActiveEmployees() }}</p>
            </div>
            <div class="bg-gray-50 p-4 rounded-lg">
              <p class="text-sm text-gray-600 mb-1">En attente d'activation</p>
              <p class="text-3xl font-bold text-gray-900">{{ getPendingEmployees() }}</p>
            </div>
            <div class="bg-gray-50 p-4 rounded-lg">
              <p class="text-sm text-gray-600 mb-1">Entreprises représentées</p>
              <p class="text-3xl font-bold text-gray-900">{{ getUniqueCompanies() }}</p>
            </div>
          </div>

          <!-- Search and Filters -->
          <div class="flex flex-col sm:flex-row gap-4 items-center">
            <div class="flex-1 relative">
              <input
                type="text"
                [(ngModel)]="searchTerm"
                (input)="filterEmployees()"
                placeholder="Rechercher un salarié..."
                class="w-full pl-4 pr-4 py-2 border border-gray-100 rounded-lg focus:ring-2 focus:ring-[#4F46E5] focus:border-transparent outline-none"
              >
            </div>
            <div class="flex gap-2">
              <button class="flex items-center gap-2 px-4 py-2 border border-0 rounded-lg hover:bg-gray-50 transition-colors">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z"></path>
                </svg>
                Filtres
              </button>
              <div class="relative">
                <select 
                  [(ngModel)]="selectedCompanyFilter"
                  (change)="filterEmployees()"
                  class="appearance-none bg-white border border-0 rounded-lg px-4 py-2 pr-8 focus:ring-2 focus:ring-[#4F46E5] focus:border-transparent outline-none"
                >
                  <option value="">Toutes les entreprises</option>
                  <option *ngFor="let company of uniqueCompanies" [value]="company">{{ company }}</option>
                </select>
              </div>
              <div class="relative">
                <select 
                  [(ngModel)]="selectedStatusFilter"
                  (change)="filterEmployees()"
                  class="appearance-none bg-white pr-8 border-0 rounded-lg px-4 py-2 pr-8 focus:ring-2 focus:ring-[#4F46E5] focus:border-transparent outline-none"
                >
                  <option value="">Tous les statuts</option>
                  <option value="Actif">Actif</option>
                  <option value="En attente">En attente</option>
                  <option value="Inactif">Inactif</option>
                </select>
              </div>
            </div>
          </div>
        </div>

        <!-- Table -->
        <div class="overflow-x-auto">
          <table class="w-full">
            <thead class="bg-gray-50">
              <tr>
                <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Salarié</th>
                <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Email</th>
                <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Téléphone</th>
                <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Entreprise</th>
                <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Statut</th>
                <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Date d'inscription</th>
                <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr *ngFor="let employee of filteredEmployees" class="hover:bg-gray-50 transition-colors">
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  {{ employee.nom }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                  {{ employee.email }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                  {{ employee.telephone }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                  {{ employee.entreprise }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                 <div
                    class="inline-flex items-center gap-2 px-3 py-1 rounded-full text-sm font-medium whitespace-nowrap"
                    [ngClass]="{
                      'bg-green-100 text-green-800': employee.statut === 'Actif',
                      'bg-yellow-100 text-yellow-800': employee.statut === 'En attente',
                      'bg-red-100 text-red-800': employee.statut === 'Inactif'
                    }"
                  >
                    <img
                      class="w-5 h-5 flex-shrink-0"
                      [src]="employee.statut === 'Actif' ? '/icones/actif.svg' :
                              employee.statut === 'En attente' ? '/icones/attente.svg' :
                              '/icones/inactif.svg'"
                      alt="Statut"
                    />
                    <span class="leading-none">{{ employee.statut }}</span>
                  </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                  {{ employee.dateInscription }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                  <div class="flex items-center justify-end gap-2">
                    <button 
                      *ngIf="employee.statut === 'En attente'"
                      class="flex items-center px-4 py-2 border border-gray-300 rounded-lg bg-white text-gray-600 hover:bg-gray-100 hover:text-gray-800 transition-colors"
                    >
                      <svg width="15" height="14" viewBox="0 0 15 14" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <g clip-path="url(#clip0_83_1919)">
                          <path d="M13.6286 4.08337L8.38383 7.42412C8.20585 7.5275 8.00369 7.58195 7.79787 7.58195C7.59205 7.58195 7.38989 7.5275 7.21191 7.42412L1.96191 4.08337" stroke="#374151" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                          <path d="M12.4619 2.33337H3.12858C2.48425 2.33337 1.96191 2.85571 1.96191 3.50004V10.5C1.96191 11.1444 2.48425 11.6667 3.12858 11.6667H12.4619C13.1062 11.6667 13.6286 11.1444 13.6286 10.5V3.50004C13.6286 2.85571 13.1062 2.33337 12.4619 2.33337Z" stroke="#374151" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        </g>
                        <defs>
                          <clipPath id="clip0_83_1919">
                            <rect width="14" height="14" fill="white" transform="translate(0.79541)"/>
                          </clipPath>
                        </defs>
                      </svg>
                      <span class="ml-2">Relancer</span>
                    </button>
                    <button 
                      class="flex items-center px-4 py-2 border border-gray-300 rounded-lg bg-white text-gray-600 hover:bg-gray-100 hover:text-gray-800 transition-colors"
                    >
                      <svg width="15" height="14" viewBox="0 0 15 14" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M2.08447 7C2.08447 5.60761 2.6376 4.27226 3.62216 3.28769C4.60673 2.30312 5.94209 1.75 7.33447 1.75C8.80217 1.75552 10.2109 2.32821 11.2661 3.34833L12.5845 4.66667" stroke="#374151" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M12.5846 1.75V4.66667H9.66797" stroke="#374151" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M12.5845 7C12.5845 8.39239 12.0313 9.72774 11.0468 10.7123C10.0622 11.6969 8.72686 12.25 7.33447 12.25C5.86678 12.2445 4.45804 11.6718 3.40281 10.6517L2.08447 9.33333" stroke="#374151" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                        <path d="M5.00114 9.33337H2.08447V12.25" stroke="#374151" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      </svg>
                      <span class="ml-2">Réinitialiser</span>
                    </button>
                    <button 
                      (click)="deleteEmployee(employee.id)"
                      class="bg-[#FF0007] hover:bg-[#CC0006] text-white px-4 py-2 rounded-lg flex items-center gap-2 transition-colors"
                    >
                      <svg width="15" height="15" viewBox="0 0 15 15" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <g clip-path="url(#clip0_1_950)">
                            <path d="M2.67969 4H13.1797" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            <path d="M12.013 4V12.1667C12.013 12.75 11.4296 13.3333 10.8463 13.3333H5.01298C4.42965 13.3333 3.84631 12.75 3.84631 12.1667V4" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            <path d="M5.59631 3.99984V2.83317C5.59631 2.24984 6.17965 1.6665 6.76298 1.6665H9.09631C9.67965 1.6665 10.263 2.24984 10.263 2.83317V3.99984" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            </g>
                            <defs>
                            <clipPath id="clip0_1_950">
                            <rect width="14" height="14" fill="white" transform="translate(0.929688 0.5)"/>
                            </clipPath>
                            </defs>
                      </svg>
                      Supprimer
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <!-- Pagination -->
        <div class="px-6 py-4 border-t border-gray-200 flex items-center justify-between">
          <div class="text-sm text-gray-600">
            Affichage de {{ (currentPage - 1) * itemsPerPage + 1 }} à {{ Math.min(currentPage * itemsPerPage, filteredEmployees.length) }} sur {{ filteredEmployees.length }} résultats
          </div>
          <div class="flex items-center gap-2">
            <button 
              (click)="previousPage()"
              [disabled]="currentPage === 1"
              class="px-3 py-1 border border-gray-300 rounded-md text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
              </svg>
            </button>
            <span class="px-3 py-1 bg-[#4F46E5] text-white rounded-md text-sm">{{ currentPage }}</span>
            <button 
              (click)="nextPage()"
              [disabled]="currentPage === getTotalPages()"
              class="px-3 py-1 border border-gray-300 rounded-md text-sm text-gray-600 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
              </svg>
            </button>
          </div>
        </div>
      </div>
      <!-- Intégration du modal -->
      <app-employee-modal
        [isOpen]="isModalOpen"
        [companies]="companies"
        (close)="closeModal()"
        (submit)="handleSubmit($event)"
      ></app-employee-modal>
    </app-main-layout>
  `
})
export class EmployeeManagementComponent {
  searchTerm: string = '';
  selectedCompanyFilter: string = '';
  selectedStatusFilter: string = '';
  currentPage: number = 1;
  itemsPerPage: number = 5;
  isModalOpen: boolean = false; // Propriété pour contrôler l'affichage du modal
  Math = Math;

  employees: Employee[] = [
    {
      id: 1,
      nom: 'Jean Dupont',
      email: 'jean.dupont@abc.com',
      telephone: '01 23 45 67 89',
      entreprise: 'Entreprise ABC',
      statut: 'Actif',
      dateInscription: '15/06/2023'
    },
    {
      id: 2,
      nom: 'Marie Martin',
      email: 'marie.martin@xyz.com',
      telephone: '01 98 76 54 32',
      entreprise: 'Société XYZ',
      statut: 'En attente',
      dateInscription: '22/06/2023'
    },
    {
      id: 3,
      nom: 'Pierre Durand',
      email: 'pierre.durand@123.com',
      telephone: '04 56 78 90 12',
      entreprise: 'Groupe 123',
      statut: 'Actif',
      dateInscription: '05/07/2023'
    },
    {
      id: 4,
      nom: 'Sophie Lefebvre',
      email: 'sophie.lefebvre@tech.com',
      telephone: '05 43 21 98 76',
      entreprise: 'Tech Solutions',
      statut: 'Inactif',
      dateInscription: '10/07/2023'
    },
    {
      id: 5,
      nom: 'Thomas Moreau',
      email: 'thomas.moreau@abc.com',
      telephone: '01 67 89 01 23',
      entreprise: 'Entreprise ABC',
      statut: 'Actif',
      dateInscription: '18/07/2023'
    }
  ];

  filteredEmployees: Employee[] = [...this.employees];
  uniqueCompanies: string[] = [];
  companies: Company[] = []; // Liste des entreprises pour le modal

  constructor() {
    this.uniqueCompanies = [...new Set(this.employees.map(emp => emp.entreprise))];
    // Transformez uniqueCompanies en format Company pour le modal
    this.companies = this.uniqueCompanies.map((name, index) => ({
      id: `company-${index + 1}`,
      name
    }));
    this.filterEmployees();
  }

  // Ouvre le modal
  openModal() {
    this.isModalOpen = true;
  }

  // Ferme le modal
  closeModal() {
    this.isModalOpen = false;
  }

  // Gère la soumission du formulaire du modal
  handleSubmit(formData: EmployeeFormData) {
    const newEmployee: Employee = {
      id: this.employees.length + 1,
      nom: `${formData.prenom} ${formData.nom}`,
      email: formData.email,
      telephone: formData.telephone,
      entreprise: this.companies.find(c => c.id === formData.entreprise)?.name || '',
      statut: 'En attente',
      dateInscription: new Date().toLocaleDateString('fr-FR')
    };
    this.employees.push(newEmployee);
    this.filterEmployees();
    this.closeModal();
  }

  getTotalEmployees(): number {
    return this.employees.length;
  }

  getActiveEmployees(): number {
    return this.employees.filter(emp => emp.statut === 'Actif').length;
  }

  getPendingEmployees(): number {
    return this.employees.filter(emp => emp.statut === 'En attente').length;
  }

  getUniqueCompanies(): number {
    return this.uniqueCompanies.length;
  }

  filterEmployees(): void {
    this.filteredEmployees = this.employees.filter(employee => {
      const matchesSearch =
        employee.nom.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        employee.email.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        employee.entreprise.toLowerCase().includes(this.searchTerm.toLowerCase());

      const matchesCompany = !this.selectedCompanyFilter || employee.entreprise === this.selectedCompanyFilter;
      const matchesStatus = !this.selectedStatusFilter || employee.statut === this.selectedStatusFilter;

      return matchesSearch && matchesCompany && matchesStatus;
    });

    this.currentPage = 1;
  }

  getTotalPages(): number {
    return Math.ceil(this.filteredEmployees.length / this.itemsPerPage);
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

  deleteEmployee(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce salarié ?')) {
      this.employees = this.employees.filter(emp => emp.id !== id);
      this.filterEmployees();
    }
  }
}