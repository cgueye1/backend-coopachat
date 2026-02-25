import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { EmployeeModalComponent, EmployeeFormData, Company } from '../../../shared/components/employee-modal/employee-modal.component';
import { CommercialService } from '../../../shared/services/commercial.service';
import Swal from 'sweetalert2';

// ==================================================
// INTERFACES D'AFFICHAGE
// ==================================================
interface Employee {
  id: string;
  nom: string;
  email: string;
  telephone: string;
  adresse: string;
  entreprise: string;
  companyId?: string;
  statut: 'Actif' | 'Inactif';
  dateInscription: string;
  initials: string;
  code: string;
}

interface MetricCard {
  title: string;
  value: string;
  icon: string;
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
export class EmployeeManagementComponent implements OnInit {
  // ==================================================
  // VARIABLES D'ETAT
  // ==================================================
  searchTerm = '';
  selectedCompanyFilter = '';
  selectedCompanyId = '';
  selectedStatusFilter = '';
  currentPage = 1;
  itemsPerPage = 6;
  isModalOpen = false;
  isSubmitting = false;
  showCompanyDropdown = false;
  showStatusDropdown = false;
  showEmployeeModal = false;
  selectedEmployee: Employee | null = null;

  uniqueStatuses = ['Tous les statuts', 'Actif', 'Inactif'];

  metricsData: MetricCard[] = [];
  employees: Employee[] = [];
  filteredEmployees: Employee[] = [];
  totalElements = 0;

  companies: Company[] = [];
  uniqueCompanies: string[] = [];

  editingEmployeeId: string | null = null;
  editingEmployeeData: EmployeeFormData | null = null;

  // Service API
  constructor(private commercialService: CommercialService) {}

  // ==================================================
  // INITIALISATION
  // ==================================================
  ngOnInit(): void {
    this.loadCompaniesForEmployees();
    this.loadEmployees();
    this.loadEmployeeStats();
  }

  // ==================================================
  // MODAL CREATE / EDIT
  // ==================================================
  openModal(): void {
    this.editingEmployeeId = null;
    this.editingEmployeeData = null;
    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
    this.editingEmployeeId = null;
    this.editingEmployeeData = null;
  }

  handleSubmit(formData: EmployeeFormData): void {
    if (this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;

    const isEdit = !!this.editingEmployeeId;
    const payload = this.mapFormDataToPayload(formData);

    const request$ = isEdit
      ? this.commercialService.updateEmployee(this.editingEmployeeId as string, payload)
      : this.commercialService.createEmployee(payload);

    request$.subscribe({
      next: () => {
        this.isSubmitting = false;
        this.isModalOpen = false;
        this.editingEmployeeId = null;
        this.editingEmployeeData = null;
        this.loadEmployees();
        this.loadEmployeeStats();
        Swal.close();
        this.showEmployeeSuccessMessage(isEdit);
      },
      error: (error) => {
        this.isSubmitting = false;
        const message = error?.error?.message || "Erreur lors de l'enregistrement";

        Swal.fire({
          icon: 'error',
          title: isEdit ? 'Modification échouée' : 'Création échouée',
          text: message,
          confirmButtonText: 'OK'
        });
      }
    });
  }

  // ==================================================
  // LISTE & STATS
  // ==================================================
  loadEmployees(): void {
    this.commercialService
      .getEmployees(
        this.currentPage - 1,
        this.itemsPerPage,
        this.searchTerm,
        this.selectedCompanyId || undefined,
        this.getIsActiveFilter()
      )
      .subscribe({
        next: (response) => {
          const items = response?.content ?? [];
          this.employees = items.map((item: any) => this.mapEmployeeListItemToEmployee(item));
          this.filteredEmployees = [...this.employees];
          this.totalElements = response?.totalElements ?? this.employees.length;
        },
        error: (error) => {
          console.error('Erreur lors du chargement des salariés:', error);
        }
      });
  }

  loadEmployeeStats(): void {
    this.commercialService.getEmployeeStats().subscribe({
      next: (stats) => {
        this.metricsData = [
          {
            title: 'Total des salariés',
            value: String(stats?.totalEmployees ?? 0),
            icon: '/icones/utilisateurs.svg'
          },
          {
            title: 'Salariés actifs',
            value: String(stats?.activeEmployees ?? 0),
            icon: '/icones/GreenUser.svg'
          },
          {
            title: "En attente d'activation",
            value: String(stats?.pendingEmployees ?? 0),
            icon: '/icones/exclamUser.svg'
          }
        ];
      },
      error: (error) => {
        console.error('Erreur lors du chargement des statistiques salariés:', error);
        this.metricsData = [];
      }
    });
  }

  loadCompaniesForEmployees(): void {
    this.commercialService.getCompanies(0, 1000).subscribe({
      next: (response) => {
        const list = response?.content ?? [];
        this.companies = list.map((company: any) => ({
          id: String(company?.id ?? ''),
          name: company?.name ?? ''
        }));
        this.uniqueCompanies = ['Toutes les entreprises', ...this.companies.map(c => c.name)];
      },
      error: (error) => {
        console.error('Erreur lors du chargement des entreprises:', error);
        this.uniqueCompanies = ['Toutes les entreprises'];
        this.companies = [];
      }
    });
  }

  // ==================================================
  // DETAILS / EDITION
  // ==================================================
  viewDetails(employee: Employee): void {
    this.commercialService.getEmployeeDetails(employee.id).subscribe({
      next: (details) => {
        this.selectedEmployee = this.mapEmployeeDetailsToEmployee(details);
        this.showEmployeeModal = true;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des détails salarié:', error);
        this.selectedEmployee = employee;
        this.showEmployeeModal = true;
      }
    });
  }

  closeEmployeeModal(): void {
    this.showEmployeeModal = false;
    this.selectedEmployee = null;
  }

  editEmployee(id: string): void {
    this.commercialService.getEmployeeDetails(id).subscribe({
      next: (details) => {
        this.editingEmployeeId = id;
        this.editingEmployeeData = this.mapEmployeeDetailsToFormData(details);
        this.isModalOpen = true;
      },
      error: (error) => {
        console.error("Erreur lors du chargement pour modification:", error);
      }
    });
  }

  modifierEmployee(): void {
    if (!this.selectedEmployee) return;
    this.closeEmployeeModal();
    this.editEmployee(this.selectedEmployee.id);
  }

  annulerEmployee(): void {
    this.closeEmployeeModal();
  }

  toggleEmployeeStatus(employee: Employee): void {
    const nextIsActive = employee.statut !== 'Actif';
    this.commercialService.updateEmployeeStatus(employee.id, nextIsActive).subscribe({
      next: () => {
        employee.statut = nextIsActive ? 'Actif' : 'Inactif';
        this.loadEmployees();
        this.loadEmployeeStats();
      },
      error: (error) => {
        console.error("Erreur lors de la mise à jour du statut salarié:", error);
      }
    });
  }

  // ==================================================
  // FILTRES & RECHERCHE
  // ==================================================
  toggleCompanyDropdown(): void {
    this.showCompanyDropdown = !this.showCompanyDropdown;
    this.showStatusDropdown = false;
  }

  toggleStatusDropdown(): void {
    this.showStatusDropdown = !this.showStatusDropdown;
    this.showCompanyDropdown = false;
  }

  selectCompany(company: string): void {
    if (company === 'Toutes les entreprises') {
      this.selectedCompanyFilter = '';
      this.selectedCompanyId = '';
    } else {
      this.selectedCompanyFilter = company;
      const match = this.companies.find(c => c.name === company);
      this.selectedCompanyId = match?.id ?? '';
    }
    this.showCompanyDropdown = false;
    this.currentPage = 1;
    this.loadEmployees();
  }

  selectStatus(status: string): void {
    this.selectedStatusFilter = status === 'Tous les statuts' ? '' : status;
    this.showStatusDropdown = false;
    this.currentPage = 1;
    this.loadEmployees();
  }

  onSearch(): void {
    this.currentPage = 1;
    this.loadEmployees();
  }

  // ==================================================
  // PAGINATION
  // ==================================================
  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadEmployees();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadEmployees();
    }
  }

  getCurrentPageEmployees(): Employee[] {
    // Les donnees sont deja paginees par le backend
    return this.filteredEmployees;
  }

  get totalResults(): number {
    return this.totalElements > 0 ? this.totalElements : this.filteredEmployees.length;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalResults / this.itemsPerPage));
  }

  // ==================================================
  // HELPERS D'AFFICHAGE
  // ==================================================
  getStatusClass(status: string): string {
    return status === 'Actif'
      ? 'bg-[#0A97480F] text-[#0A9748]'
      : 'bg-red-50 text-[#FF0909]';
  }

  getStatusDotClass(status: string): string {
    return status === 'Actif' ? 'bg-[#0A9748]' : 'bg-[#FF0909]';
  }

  private mapEmployeeListItemToEmployee(item: any): Employee {
    const firstName = item?.firstName ?? '';
    const lastName = item?.lastName ?? '';
    const fullName = `${firstName} ${lastName}`.trim();
    const initials = `${firstName.charAt(0) || ''}${lastName.charAt(0) || ''}`.toUpperCase();

    return {
      id: String(item?.id ?? ''),
      nom: fullName,
      email: item?.email ?? '',
      telephone: '',
      adresse: '',
      entreprise: item?.companyName ?? '',
      companyId: undefined,
      statut: this.normalizeStatus(item?.status),
      dateInscription: this.formatCreatedAt(item?.createdAt),
      initials,
      code: item?.employeeCode ?? ''
    };
  }

  private mapEmployeeDetailsToEmployee(details: any): Employee {
    const firstName = details?.firstName ?? '';
    const lastName = details?.lastName ?? '';
    const fullName = `${firstName} ${lastName}`.trim();
    const initials = `${firstName.charAt(0) || ''}${lastName.charAt(0) || ''}`.toUpperCase();

    return {
      id: String(details?.id ?? ''),
      nom: fullName,
      email: details?.email ?? '',
      telephone: details?.phone ?? '',
      adresse: details?.address ?? '',
      entreprise: details?.companyName ?? '',
      companyId: details?.companyId ? String(details.companyId) : undefined,
      statut: this.normalizeStatus(details?.status),
      dateInscription: this.formatCreatedAt(details?.createdAt),
      initials,
      code: details?.employeeCode ?? ''
    };
  }

  private mapEmployeeDetailsToFormData(details: any): EmployeeFormData {
    return {
      prenom: details?.firstName ?? '',
      nom: details?.lastName ?? '',
      email: details?.email ?? '',
      telephone: details?.phone ?? '',
      adresse: details?.address ?? '',
      entreprise: details?.companyId ? String(details.companyId) : ''
    };
  }

  private mapFormDataToPayload(data: EmployeeFormData) {
    return {
      firstName: data.prenom?.trim(),
      lastName: data.nom?.trim(),
      email: data.email?.trim(),
      phone: data.telephone?.trim(),
      address: data.adresse?.trim(),
      companyId: data.entreprise
    };
  }

  private normalizeStatus(status: string | undefined): 'Actif' | 'Inactif' {
    if (!status) return 'Actif';
    const normalized = status.toLowerCase();
    if (normalized.includes('inactif') || normalized === 'inactive' || normalized === 'false') {
      return 'Inactif';
    }
    return 'Actif';
  }

  private formatCreatedAt(createdAt: string | undefined): string {
    if (!createdAt) return '';
    const parts = createdAt.split(' ');
    return parts[0] ?? createdAt;
  }

  private getIsActiveFilter(): boolean | undefined {
    if (this.selectedStatusFilter === 'Actif') return true;
    if (this.selectedStatusFilter === 'Inactif') return false;
    return undefined;
  }

  private showEmployeeSuccessMessage(isEdit: boolean): void {
    Swal.fire({
      title: isEdit ? 'Salarié modifié avec succès' : 'Salarié créé avec succès',
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
}