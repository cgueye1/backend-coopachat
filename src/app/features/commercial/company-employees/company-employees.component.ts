import { Component, OnInit, HostListener, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, finalize } from 'rxjs';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { EmployeeModalComponent, EmployeeFormData } from '../../../shared/components/employee-modal/employee-modal.component';
import { CommercialService } from '../../../shared/services/commercial.service';
import { getUserFacingHttpErrorMessage } from '../../../shared/utils/http-error-message';
import { environment } from '../../../../environments/environment';
import Swal from 'sweetalert2';

/** Affichage date inscription (API : chaîne `dd-MM-yyyy HH:mm:ss` ou ISO). Comme la page Utilisateurs (fr-FR, jour seulement). */
function formatRegistrationDate(value: unknown): string {
  if (value == null || value === '') return '—';
  if (typeof value === 'string') {
    const s = value.trim();
    const m = /^(\d{2})-(\d{2})-(\d{4})(?: (\d{2}):(\d{2}):(\d{2}))?$/.exec(s);
    if (m) {
      const d = new Date(+m[3], +m[2] - 1, +m[1], m[4] != null ? +m[4] : 0, m[5] != null ? +m[5] : 0, m[6] != null ? +m[6] : 0);
      if (!Number.isNaN(d.getTime())) {
        return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
      }
    }
    const parsed = new Date(s);
    if (!Number.isNaN(parsed.getTime())) {
      return parsed.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
    }
    return s;
  }
  return '—';
}

/** Ligne tableau — alignée sur EmployeeListItemDTO API. */
interface EmployeeRow {
  id: string;
  displayName: string;
  email: string;
  registeredAt: string;
  statut: 'Actif' | 'Inactif';
  initials: string;
  profilePhotoUrl?: string;
}

/** Détail salarié — même modèle que la page Gestion des salariés (modal centré). */
interface EmployeeDetailView {
  id: string;
  nom: string;
  email: string;
  telephone: string;
  entreprise: string;
  companyId?: string;
  statut: 'Actif' | 'Inactif';
  dateInscription: string;
  initials: string;
  code: string;
  profilePhotoUrl?: string;
}

@Component({
  selector: 'app-company-employees',
  standalone: true,
  imports: [CommonModule, FormsModule, MainLayoutComponent, EmployeeModalComponent],
  templateUrl: './company-employees.component.html',
  styles: []
})
export class CompanyEmployeesComponent implements OnInit {
  companyId = '';

  loadingCompany = false;
  companyName = 'Entreprise';
  companyCode = '';
  sectorLabel = '';
  location = '';
  contactName = '';
  contactEmail = '';
  contactPhone = '';
  isCompanyActive = true;
  logoUrl: string | undefined;
  initials = 'EN';

  kpiInscrits = 0;
  kpiActifs = 0;
  kpiEnAttente = 0;
  loadingKpis = false;

  activeTab: 'salaries' | 'import' = 'salaries';

  searchTerm = '';
  selectedStatusFilter = '';
  showStatusDropdown = false;

  currentPage = 1;
  readonly itemsPerPage = 6;

  pagedRows: EmployeeRow[] = [];
  totalElements = 0;
  totalPages = 1;
  loadingEmployeeList = false;

  importDragOver = false;
  importFileName: string | null = null;
  isImporting = false;

  loadingExport = false;

  isEmployeeModalOpen = false;
  isSubmittingEmployee = false;
  editingEmployeeId: string | null = null;
  editingEmployeeData: EmployeeFormData | null = null;

  /** Modal détail salarié. */
  showEmployeeDetailModal = false;
  selectedEmployeeDetail: EmployeeDetailView | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly commercialService: CommercialService,
    @Inject(PLATFORM_ID) private readonly platformId: object
  ) {}

  ngOnInit(): void {
    this.companyId = this.route.snapshot.paramMap.get('companyId') ?? '';
    this.loadCompanyHeader();
  }

  get breadcrumb(): string {
    return `Pages / Entreprises / ${this.companyName}`;
  }

  get listedCount(): number {
    return this.totalElements;
  }

  private loadCompanyHeader(): void {
    if (!this.companyId) return;
    this.loadingCompany = true;
    this.commercialService.getCompanyDetails(this.companyId).subscribe({
      next: (details: any) => {
        this.loadingCompany = false;
        const name = details?.name ?? '';
        this.companyName = name || 'Entreprise';
        this.companyCode = details?.companyCode ?? '';
        this.sectorLabel = details?.sectorLabel ?? '—';
        this.location = details?.location ?? '—';
        this.contactName = details?.contactName ?? '—';
        this.contactEmail = details?.contactEmail ?? '—';
        this.contactPhone = details?.contactPhone ?? '—';
        this.isCompanyActive = details?.isActive === true;
        if (details?.logo) {
          this.logoUrl = `${environment.imageServerUrl}/files/${details.logo}`;
        } else {
          this.logoUrl = undefined;
        }
        this.initials =
          name
            .split(/\s+/)
            .filter(Boolean)
            .map((w: string) => w[0])
            .join('')
            .substring(0, 2)
            .toUpperCase() || 'EN';

        this.loadKpis();
        this.loadEmployees();
      },
      error: () => {
        this.loadingCompany = false;
        this.companyName = `Entreprise #${this.companyId}`;
      }
    });
  }

  /** Compteurs par entreprise (total / actifs / inactifs) via totalElements. */
  private loadKpis(): void {
    if (!this.companyId) return;
    this.loadingKpis = true;
    forkJoin({
      total: this.commercialService.getEmployees(0, 1, this.companyId),
      actifs: this.commercialService.getEmployees(0, 1, this.companyId, undefined, true),
      inactifs: this.commercialService.getEmployees(0, 1, this.companyId, undefined, false)
    }).subscribe({
      next: (r) => {
        this.kpiInscrits = r.total?.totalElements ?? 0;
        this.kpiActifs = r.actifs?.totalElements ?? 0;
        this.kpiEnAttente = r.inactifs?.totalElements ?? 0;
        this.loadingKpis = false;
      },
      error: () => {
        this.loadingKpis = false;
      }
    });
  }

  loadEmployees(): void {
    if (!this.companyId) {
      return;
    }
    this.loadingEmployeeList = true;
    this.commercialService
      .getEmployees(
        this.currentPage - 1,
        this.itemsPerPage,
        this.companyId,
        this.searchTerm,
        this.getIsActiveFilter()
      )
      .subscribe({
        next: (response) => {
          const items = response?.content ?? [];
          this.pagedRows = items.map((item: any) => this.mapListItemToRow(item));
          this.totalElements = response?.totalElements ?? 0;
          this.totalPages = Math.max(1, response?.totalPages ?? 1);
          this.loadingEmployeeList = false;
        },
        error: () => {
          this.loadingEmployeeList = false;
          this.pagedRows = [];
          this.totalElements = 0;
          this.totalPages = 1;
        }
      });
  }

  private mapListItemToRow(item: any): EmployeeRow {
    const firstName = item?.firstName ?? '';
    const lastName = item?.lastName ?? '';
    const displayName = `${firstName} ${lastName}`.trim() || '—';
    const initials = `${firstName.charAt(0) || ''}${lastName.charAt(0) || ''}`.toUpperCase() || '?';
    return {
      id: String(item?.id ?? ''),
      displayName,
      email: item?.email ?? '—',
      registeredAt: formatRegistrationDate(item?.createdAt),
      statut: this.normalizeStatus(item?.status),
      initials
    };
  }

  private normalizeStatus(status: string | undefined): 'Actif' | 'Inactif' {
    if (!status) return 'Inactif';
    const n = status.toLowerCase();
    if (n.includes('inactif') || n === 'inactive' || n === 'false') return 'Inactif';
    return 'Actif';
  }

  private getIsActiveFilter(): boolean | undefined {
    if (this.selectedStatusFilter === 'Actif') return true;
    if (this.selectedStatusFilter === 'Inactif') return false;
    return undefined;
  }

  goBack(): void {
    void this.router.navigate(['/com/entreprises']);
  }

  openEditCompany(): void {
    void this.router.navigate(['/com/entreprises'], { queryParams: { openEdit: this.companyId } });
  }

  exportEmployees(): void {
    if (!isPlatformBrowser(this.platformId) || this.loadingExport || !this.companyId) {
      return;
    }
    this.loadingExport = true;
    this.commercialService
      .exportEmployees(this.companyId, this.searchTerm.trim() || undefined, this.getIsActiveFilter())
      .pipe(finalize(() => { this.loadingExport = false; }))
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `salaries_${new Date().toISOString().slice(0, 10)}.xlsx`;
          a.click();
          window.URL.revokeObjectURL(url);
        },
        error: () => {
          void Swal.fire({
            title: 'Export impossible',
            text: 'Une erreur est survenue lors du téléchargement.',
            icon: 'error'
          });
        }
      });
  }

  setTab(tab: 'salaries' | 'import'): void {
    this.activeTab = tab;
  }

  toggleStatusDropdown(): void {
    this.showStatusDropdown = !this.showStatusDropdown;
  }

  selectStatusFilter(label: string): void {
    this.selectedStatusFilter = label === 'Tous les statuts' ? '' : label;
    this.showStatusDropdown = false;
    this.currentPage = 1;
    this.loadEmployees();
  }

  get statusFilterButtonLabel(): string {
    return this.selectedStatusFilter || 'Tous les statuts';
  }

  onSearchInput(): void {
    this.currentPage = 1;
    this.loadEmployees();
  }

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

  onImportZoneClick(fileInput: HTMLInputElement): void {
    if (this.isImporting) return;
    fileInput.click();
  }

  private isExcelFile(file: File): boolean {
    const n = file.name.toLowerCase();
    return n.endsWith('.xlsx') || n.endsWith('.xls');
  }

  /** Envoie le fichier vers POST /commercial/employees/import. */
  private runExcelImport(file: File): void {
    if (!this.companyId) {
      void Swal.fire({
        icon: 'warning',
        title: 'Entreprise manquante',
        text: "Impossible de déterminer l'entreprise cible."
      });
      return;
    }
    if (!this.isExcelFile(file)) {
      void Swal.fire({
        icon: 'warning',
        title: 'Format invalide',
        text: 'Utilisez un fichier .xlsx ou .xls.'
      });
      return;
    }
    this.isImporting = true;
    this.importFileName = file.name;
    this.commercialService.importEmployeesFromExcel(file, this.companyId).subscribe({
      next: (msg) => {
        this.isImporting = false;
        this.loadKpis();
        this.loadEmployees();
        void Swal.fire({
          icon: 'success',
          title: 'Import',
          text: (msg || 'Import terminé avec succès.').trim(),
          timer: 2200,
          showConfirmButton: false
        });
      },
      error: (err: unknown) => {
        this.isImporting = false;
        const raw = this.extractApiErrorMessage(err) || '';
        const msg = this.formatImportErrorForDisplay(raw);
        void Swal.fire({
          icon: 'error',
          title: 'Import des salariés',
          text: msg || "L'import n'a pas pu être effectué. Réessayez ou vérifiez le fichier.",
          confirmButtonText: 'OK'
        });
      }
    });
  }

  onImportFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (file) {
      this.runExcelImport(file);
    } else {
      this.importFileName = null;
    }
  }

  onImportDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.importDragOver = true;
  }

  onImportDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.importDragOver = false;
  }

  onImportDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.importDragOver = false;
    if (this.isImporting) return;
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.runExcelImport(file);
    }
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.showStatusDropdown = false;
  }

  openInscribeModal(): void {
    this.editingEmployeeId = null;
    this.editingEmployeeData = null;
    this.isEmployeeModalOpen = true;
  }

  closeEmployeeModal(): void {
    this.isEmployeeModalOpen = false;
    this.editingEmployeeId = null;
    this.editingEmployeeData = null;
  }

  closeDetailModal(): void {
    this.showEmployeeDetailModal = false;
    this.selectedEmployeeDetail = null;
  }

  getEmployeePhotoUrl(profilePhotoUrl: string | undefined): string {
    if (!profilePhotoUrl) return '';
    const base = environment.imageServerUrl ?? '';
    return base ? base.replace(/\/$/, '') + '/files/' + profilePhotoUrl : '/files/' + profilePhotoUrl;
  }

  modifierFromDetail(): void {
    if (!this.selectedEmployeeDetail) return;
    const id = this.selectedEmployeeDetail.id;
    this.closeDetailModal();
    this.openEditEmployeeById(id);
  }

  annulerFromDetail(): void {
    this.closeDetailModal();
  }

  toggleDetailEmployeeStatus(): void {
    if (this.selectedEmployeeDetail) {
      this.toggleEmployeeStatusSalariesStyle(this.selectedEmployeeDetail);
    }
  }

  handleEmployeeSubmit(formData: EmployeeFormData): void {
    if (this.isSubmittingEmployee || !this.companyId) return;
    this.isSubmittingEmployee = true;
    const isEdit = !!this.editingEmployeeId;
    const payload = {
      firstName: formData.prenom?.trim(),
      lastName: formData.nom?.trim(),
      email: formData.email?.trim(),
      phone: formData.telephone?.trim(),
      address: '—',
      companyId: this.companyId
    };
    const req$ = isEdit
      ? this.commercialService.updateEmployee(this.editingEmployeeId!, payload)
      : this.commercialService.createEmployee(payload);

    req$.subscribe({
      next: () => {
        this.isSubmittingEmployee = false;
        this.closeEmployeeModal();
        this.loadKpis();
        this.loadEmployees();
        Swal.fire({
          icon: 'success',
          title: isEdit ? 'Salarié modifié' : 'Salarié créé',
          timer: 1500,
          showConfirmButton: false
        });
      },
      error: (err: unknown) => {
        this.isSubmittingEmployee = false;
        const msg = this.extractApiErrorMessage(err) || 'Une erreur est survenue.';
        Swal.fire({ icon: 'error', title: 'Erreur', text: msg });
      }
    });
  }

  rowActionView(row: EmployeeRow): void {
    this.commercialService.getEmployeeDetails(row.id).subscribe({
      next: (d: any) => {
        const v = this.mapEmployeeDetailsToView(d);
        if (row.profilePhotoUrl) {
          v.profilePhotoUrl = row.profilePhotoUrl;
        }
        this.selectedEmployeeDetail = v;
        this.showEmployeeDetailModal = true;
      },
      error: (err: unknown) => {
        Swal.fire({
          icon: 'error',
          title: 'Impossible de charger le détail',
          text: this.extractApiErrorMessage(err) ?? ''
        });
      }
    });
  }

  rowActionEdit(row: EmployeeRow): void {
    this.openEditEmployeeById(row.id);
  }

  private openEditEmployeeById(id: string): void {
    this.commercialService.getEmployeeDetails(id).subscribe({
      next: (details: any) => {
        this.editingEmployeeId = id;
        this.editingEmployeeData = {
          prenom: details?.firstName ?? '',
          nom: details?.lastName ?? '',
          email: details?.email ?? '',
          telephone: details?.phone ?? ''
        };
        this.isEmployeeModalOpen = true;
      },
      error: (err: unknown) => {
        Swal.fire({
          icon: 'error',
          title: 'Impossible de charger le salarié',
          text: this.extractApiErrorMessage(err) ?? ''
        });
      }
    });
  }

  rowToggleActive(row: EmployeeRow): void {
    this.toggleEmployeeStatusSalariesStyle(row);
  }

  /**
   * Même flux de confirmation que la page Gestion des salariés (icône alerte, boutons stylés).
   */
  private toggleEmployeeStatusSalariesStyle(employee: { id: string; statut: 'Actif' | 'Inactif' }): void {
    const nextIsActive = employee.statut !== 'Actif';
    const titleText = nextIsActive ? 'Activer ce salarié' : 'Désactiver ce salarié ?';
    const descriptionText = nextIsActive
      ? 'Le salarié pourra se connecter au portail.'
      : 'Le salarié ne pourra plus se connecter.';
    const confirmButtonText = nextIsActive ? 'Activer' : 'Oui';
    const confirmButtonClass = nextIsActive
      ? 'bg-[#16A34A] hover:bg-[#16A34A] text-white px-8 py-3 rounded-lg font-medium text-base shadow-none border-none'
      : 'bg-[#EF4444] hover:bg-[#DC2626] text-white px-8 py-3 rounded-lg font-medium text-base shadow-none border-none';

    void Swal.fire({
      title: titleText,
      text: descriptionText,
      iconHtml: `<img src="/icones/alerte.svg" alt="alert" style="margin: 0 auto;" />`,
      showCancelButton: true,
      confirmButtonText,
      cancelButtonText: 'Annuler',
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-3xl p-6',
        title: 'text-2xl font-medium text-gray-900',
        htmlContainer: 'text-lg text-gray-600',
        confirmButton: confirmButtonClass,
        cancelButton:
          'bg-[#F3F4F6] hover:bg-gray-200 text-gray-700 px-8 py-3 rounded-lg font-medium text-base shadow-none border-none',
        actions: 'flex justify-center w-full gap-2',
        icon: 'border-none'
      },
      backdrop: 'rgba(0,0,0,0.2)',
      width: '580px',
      showClass: { popup: 'animate__animated animate__fadeIn animate__faster' }
    }).then((result) => {
      if (!result.isConfirmed) return;
      this.commercialService.updateEmployeeStatus(employee.id, nextIsActive).subscribe({
        next: () => {
          const next: 'Actif' | 'Inactif' = nextIsActive ? 'Actif' : 'Inactif';
          employee.statut = next;
          if (this.selectedEmployeeDetail?.id === employee.id) {
            this.selectedEmployeeDetail = { ...this.selectedEmployeeDetail, statut: next };
          }
          this.loadKpis();
          this.loadEmployees();
          this.showToggleSuccessMessage(next);
        },
        error: (err: unknown) => {
          const msg =
            this.extractApiErrorMessage(err) || 'Impossible de mettre à jour le statut pour le moment.';
          void Swal.fire({
            title: 'Activation impossible',
            text: msg,
            icon: 'error',
            confirmButtonText: 'OK'
          });
        }
      });
    });
  }

  private showToggleSuccessMessage(newStatus: 'Actif' | 'Inactif'): void {
    void Swal.fire({
      title: newStatus === 'Inactif' ? 'Le salarié a été désactivé' : 'Le salarié a été activé',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showConfirmButton: false,
      timer: 2000,
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-3xl p-6',
        title: 'text-xl font-medium text-gray-900',
        icon: 'border-none'
      },
      backdrop: 'rgba(0,0,0,0.2)',
      width: '580px',
      showClass: { popup: 'animate__animated animate__fadeIn animate__faster' },
      hideClass: { popup: 'animate__animated animate__fadeOut animate__faster' }
    });
  }

  private mapEmployeeDetailsToView(details: any): EmployeeDetailView {
    const firstName = details?.firstName ?? '';
    const lastName = details?.lastName ?? '';
    const fullName = `${firstName} ${lastName}`.trim();
    const initials = `${firstName.charAt(0) || ''}${lastName.charAt(0) || ''}`.toUpperCase() || '?';
    return {
      id: String(details?.id ?? ''),
      nom: fullName,
      email: details?.email ?? '',
      telephone: details?.phone ?? '—',
      entreprise: details?.companyName ?? '—',
      companyId: details?.companyId != null ? String(details.companyId) : undefined,
      statut: this.normalizeStatus(details?.status),
      dateInscription: formatRegistrationDate(details?.createdAt),
      initials,
      code: details?.employeeCode ?? '—',
      profilePhotoUrl: details?.profilePhotoUrl ?? undefined
    };
  }

  private extractApiErrorMessage(err: unknown): string {
    return getUserFacingHttpErrorMessage(
      err,
      'Une erreur est survenue. Veuillez réessayer dans quelques instants.'
    );
  }

  /** Retire le préfixe technique historique et les bruits JDBC pour un libellé lisible. */
  private formatImportErrorForDisplay(raw: string): string {
    let t = raw.trim();
    const legacy = "Erreur lors de l'import du fichier :";
    if (t.startsWith(legacy)) {
      t = t.slice(legacy.length).trim();
    }
    if (t.includes('could not execute statement') && t.includes('Duplicate entry')) {
      const m = /Duplicate entry '([^']+)'/.exec(t);
      if (m) {
        const v = m[1];
        if (v.includes('@')) {
          return `L'adresse e-mail « ${v} » est déjà utilisée. Retirez la ligne en doublon ou utilisez un autre e-mail.`;
        }
        return `Le numéro « ${v} » est déjà enregistré. Chaque téléphone doit être unique dans l'application.`;
      }
      return 'Une donnée du fichier existe déjà (e-mail ou téléphone en double). Vérifiez le fichier.';
    }
    return t;
  }
}
