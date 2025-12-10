import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { EmployeeModalComponent, EmployeeFormData, Company } from '../../../shared/components/employee-modal/employee-modal.component';
import Swal from 'sweetalert2';

interface Employee {
  id: number;
  nom: string;
  email: string;
  telephone: string;
  entreprise: string;
  statut: 'Actif' | 'Inactif';
  dateInscription: string;
  initials: string;
  code: string;
}

@Component({
  selector: 'app-salaries',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MainLayoutComponent,
    HeaderComponent,
    EmployeeModalComponent
  ],
  templateUrl: './salaries.component.html'
})
export class EmployeeManagementComponent {
  searchTerm: string = '';
  selectedCompanyFilter: string = '';
  selectedStatusFilter: string = '';
  currentPage: number = 1;
  itemsPerPage: number = 5;
  isModalOpen: boolean = false;
  Math = Math;
  showCompanyDropdown = false;
  showStatusDropdown = false;
  uniqueStatuses = ['Tous les statuts', 'Actif', 'Inactif'];
  showEmployeeModal = false;
  selectedEmployee: Employee | null = null;

  metricsData = [
    {
      title: 'Total des salariés',
      value: '5',
      icon: '/icones/utilisateurs.svg'
    },
    {
      title: 'Salariés actifs',
      value: '3',
      icon: '/icones/GreenUser.svg'
    },
    {
      title: "En attente d'activation",
      value: '1',
      icon: '/icones/exclamUser.svg'
    }
  ];

  employees: Employee[] = [
    {
      id: 1,
      nom: 'Jean Dupont',
      email: 'jean.dupont@abc.com',
      telephone: '01 23 45 67 89',
      entreprise: 'Entreprise ABC',
      statut: 'Actif',
      dateInscription: '15/06/2023',
      initials: 'JD',
      code: 'US-2025-01'
    },
    {
      id: 2,
      nom: 'Marie Martin',
      email: 'marie.martin@xyz.com',
      telephone: '01 98 76 54 32',
      entreprise: 'Société XYZ',
      statut: 'Actif',
      dateInscription: '22/06/2023',
      initials: 'MM',
      code: 'US-2025-02'
    },
    {
      id: 3,
      nom: 'Pierre Durand',
      email: 'pierre.durand@123.com',
      telephone: '04 56 78 90 12',
      entreprise: 'Groupe 123',
      statut: 'Actif',
      dateInscription: '05/07/2023',
      initials: 'PD',
      code: 'US-2025-03'
    },
    {
      id: 4,
      nom: 'Sophie Lefebvre',
      email: 'sophie.lefebvre@tech.com',
      telephone: '05 43 21 98 76',
      entreprise: 'Tech Solutions',
      statut: 'Inactif',
      dateInscription: '10/07/2023',
      initials: 'SL',
      code: 'US-2025-04'
    },
    {
      id: 5,
      nom: 'Thomas Moreau',
      email: 'thomas.moreau@abc.com',
      telephone: '01 67 89 01 23',
      entreprise: 'Entreprise ABC',
      statut: 'Actif',
      dateInscription: '18/07/2023',
      initials: 'TM',
      code: 'US-2025-05'
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
    const prenom = formData.prenom;
    const nom = formData.nom;
    const initials = `${prenom.charAt(0)}${nom.charAt(0)}`.toUpperCase();
    const nextId = this.employees.length + 1;
    const code = `US-2025-${nextId.toString().padStart(2, '0')}`;

    const newEmployee: Employee = {
      id: nextId,
      nom: `${prenom} ${nom}`,
      email: formData.email,
      telephone: formData.telephone,
      entreprise: this.companies.find(c => c.id === formData.entreprise)?.name || '',
      statut: 'Actif',
      dateInscription: new Date().toLocaleDateString('fr-FR'),
      initials: initials,
      code: code
    };
    this.employees.push(newEmployee);
    this.filterEmployees();
    this.closeModal();
    this.showCreateSuccessMessage();
  }

  showCreateSuccessMessage(): void {
    Swal.fire({
      title: 'Salarié créé avec succès',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showConfirmButton: false,
      timer: 1500,
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-3xl p-6',
        title: 'text-xl font-medium text-gray-900',
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
    });
  }

  getTotalEmployees(): number {
    return this.employees.length;
  }

  getActiveEmployees(): number {
    return this.employees.filter(emp => emp.statut === 'Actif').length;
  }

  getPendingEmployees(): number {
    return 0; // Plus de statut "En attente"
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

  viewDetails(employee: Employee): void {
    this.selectedEmployee = employee;
    this.showEmployeeModal = true;
  }

  closeEmployeeModal(): void {
    this.showEmployeeModal = false;
    this.selectedEmployee = null;
  }

  modifierEmployee(): void {
    console.log('Modifier salarié:', this.selectedEmployee);
    // Implémentation à venir - ouverture du modal d'édition
    this.closeEmployeeModal();
  }

  annulerEmployee(): void {
    this.closeEmployeeModal();
  }

  editEmployee(id: number): void {
    console.log('Éditer salarié:', id);
    // Implémentation à venir - ouverture du modal d'édition
  }

  toggleEmployeeStatus(employee: Employee): void {
    employee.statut = employee.statut === 'Actif' ? 'Inactif' : 'Actif';
  }

  toggleCompanyDropdown(): void {
    this.showCompanyDropdown = !this.showCompanyDropdown;
    this.showStatusDropdown = false;
  }

  toggleStatusDropdown(): void {
    this.showStatusDropdown = !this.showStatusDropdown;
    this.showCompanyDropdown = false;
  }

  selectCompany(company: string): void {
    this.selectedCompanyFilter = company === 'Toutes les entreprises' ? '' : company;
    this.showCompanyDropdown = false;
    this.filterEmployees();
  }

  selectStatus(status: string): void {
    this.selectedStatusFilter = status === 'Tous les statuts' ? '' : status;
    this.showStatusDropdown = false;
    this.filterEmployees();
  }

  getStatusClass(status: string): string {
    return status === 'Actif'
      ? 'bg-[#0A97480F] text-[#0A9748]'
      : 'bg-red-50 text-[#FF0909]';
  }

  getStatusDotClass(status: string): string {
    return status === 'Actif' ? 'bg-[#0A9748]' : 'bg-[#FF0909]';
  }

  getCurrentPageEmployees(): Employee[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredEmployees.slice(startIndex, endIndex);
  }

  get totalResults(): number {
    return this.filteredEmployees.length;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalResults / this.itemsPerPage));
  }

  getDisplayStart(): number {
    if (this.totalResults === 0) return 0;
    return (this.currentPage - 1) * this.itemsPerPage + 1;
  }

  getDisplayEnd(): number {
    return Math.min(this.currentPage * this.itemsPerPage, this.totalResults);
  }
}