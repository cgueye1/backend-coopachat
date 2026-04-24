import { Component, OnInit, NgZone, HostListener, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { CompanyModalComponent, CompanyFormData, CompanySubmitPayload } from '../../../shared/components/company-modal/company-modal.component';
import { CommercialService, ProspectStats, CompanyStats } from '../../../shared/services/commercial.service';
import { environment } from '../../../../environments/environment';
import Swal from 'sweetalert2';

/** 2 pages en 1 : Prospections (à convaincre) ou Entreprises (partenaires). Mode lu via route.data['mode'] */
interface Prospect {
  id: string;
  companyCode?: string;
  entreprise: string;
  secteur: string;
  note?: string;
  localisation: string;
  contact: {
    nom: string;
    email?: string;
    telephone: string;
  };
  statut: 'Actif' | 'Inactif';
  prospectionStatus?: string;
  date: string;
  initials: string;
  logo?: string; // URL pour afficher le logo
  employeeCount?: number;
  orderCount?: number;
}

// Interfaces pour les statistiques 
interface MetricCard {
  title: string;
  value: string;
  icon: string;
}

@Component({
  selector: 'app-propection',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MainLayoutComponent, CompanyModalComponent, HeaderComponent],
  templateUrl: './propection.component.html',
  styleUrls: ['./propection.component.css']
})
export class ProspectionComponent implements OnInit, OnDestroy {

  // --- Variables de filtres ---
  searchTerm = ''; 
  selectedStatus = ''; 
  selectedSector = ''; 
  selectedProspectionStatus = ''; 
  selectedCompanyType = ''; 
  routeMode: 'prospects' | 'partenaires' = 'prospects'; 
  private routeSub?: Subscription;

  // --- Pagination ---
  currentPage  = 1;
  itemsPerPage = 10;
  totalElements = 0; 
  totalPages = 1;

  // --- Visibilité des menus déroulants ---
  showStatusDropdown            = false;
  showCompanyTypeDropdown       = false;
  showProspectionStatusDropdown = false;
  showSectorDropdown            = false;

  // --- Modales (création / modification) ---
  isCreateModalOpen    = false;
  isEditModalOpen      = false;
  isCompanySubmitting  = false; 
  editingCompanyId: string | null   = null;
  editingCompanyData: CompanyFormData | null = null;
  editingCompanyLogoUrl: string | null = null; 
  private successPopupTimeoutId: any = null;
  private submitSequence = 0;

  // --- Données et listes ---
  loadingStats: boolean = false;
  loadingCompanyList: boolean = false;
  metricsData: MetricCard[] = []; 
  prospects: Prospect[] = [];
  filteredProspects: Prospect[] = []; 

  uniqueStatuses = ['Tous les statuts', 'Actif', 'Inactif'];
  prospectionStatusOptions = [
    { value: '',           label: 'Tous les états' },
    { value: 'PENDING',    label: 'En attente' },
    { value: 'RELAUNCHED', label: 'Relancée' },
    { value: 'INTERESTED', label: 'Intéressée' },
    { value: 'PARTNER_SIGNED', label: 'Partenaire signé' }
  ];

  /** Libellés API / affichage (alignés sur CompanyStatus côté backend). */
  readonly prospectionTableStatusLabels: string[] = [
    'En attente',
    'Relancée',
    'Intéressée',
    'Partenaire signé'
  ];

  /** Ligne en cours de mise à jour (désactive le select). */
  updatingProspectionStatusId: string | null = null;

  private commercialService = inject(CommercialService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private ngZone = inject(NgZone);

  // Ferme tous les dropdowns au clic en dehors
  @HostListener('document:click')
  onDocumentClick(): void {
    this.showStatusDropdown = false;
    this.showSectorDropdown = false;
    this.showCompanyTypeDropdown = false;
    this.showProspectionStatusDropdown = false;
  }

  // Secteurs d'activité 
  sectorOptions: { id: number; name: string }[] = [];
  uniqueSectors: string[] = [];

  // ---------- BADGES (couleurs selon statut prospection) ----------
  get selectedProspectionStatusLabel(): string {
    return this.prospectionStatusOptions.find(o => o.value === this.selectedProspectionStatus)?.label
      ?? 'Tous les états';
  }

  /** Couleurs des badges selon statut prospection */
  getProspectionStatusClasses(status: string | undefined): string {
    if (!status) return 'bg-gray-50 text-gray-600';
    const s = status.toLowerCase();
    if (s.includes('attente')    || s === 'pending')    return 'bg-gray-50 text-gray-600';
    if (s.includes('relanc')     || s === 'relaunched') return 'bg-blue-50 text-blue-600';
    if (s.includes('intéress')   || s === 'interested') return 'bg-emerald-50 text-emerald-600';
    if (s.includes('signé')      || s.includes('signed')) return 'bg-green-50 text-green-600';
    return 'bg-gray-50 text-gray-600';
  }

  getProspectionStatusDotClasses(status: string | undefined): string {
    if (!status) return 'bg-gray-400';
    const s = status.toLowerCase();
    if (s.includes('attente')    || s === 'pending')    return 'bg-gray-400';
    if (s.includes('relanc')     || s === 'relaunched') return 'bg-blue-500';
    if (s.includes('intéress')   || s === 'interested') return 'bg-emerald-500';
    if (s.includes('signé')      || s.includes('signed')) return 'bg-green-500';
    return 'bg-gray-400';
  }

  /** Valeur du select : toujours un libellé autorisé. */
  prospectionRowSelectModel(prospect: Prospect): string {
    return this.canonicalProspectionStatus(prospect.prospectionStatus);
  }

  async onProspectionStatusChange(prospect: Prospect, event: Event): Promise<void> {
    const el = event.target as HTMLSelectElement;
    const newStatus = el.value;
    const previousDisplay = this.prospectionRowSelectModel(prospect);
    if (newStatus === previousDisplay) {
      return;
    }

    el.value = previousDisplay;

    const result = await Swal.fire({
      title: 'Modifier l\'état prospection ?',
      text: `« ${prospect.entreprise} » passera de « ${previousDisplay} » à « ${newStatus} ».`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Confirmer',
      cancelButtonText: 'Annuler',
      focusCancel: true,
      confirmButtonColor: '#2B3674',
      cancelButtonColor: '#6B7280'
    });

    if (!result.isConfirmed) {
      return;
    }

    el.value = newStatus;
    this.updatingProspectionStatusId = prospect.id;

    this.ngZone.run(() => {
      this.commercialService.updateCompanyProspectionStatus(prospect.id, newStatus).subscribe({
        next: () => {
          this.updatingProspectionStatusId = null;
          const redirectPartners = this.routeMode === 'prospects' && newStatus === 'Partenaire signé';
          
          Swal.fire({
            toast: true,
            position: 'top-end',
            icon: 'success',
            title: 'Statut mis à jour',
            showConfirmButton: false,
            timer: 3000
          });

          if (redirectPartners) {
            this.router.navigate(['/com/entreprises']);
          } else {
            this.loadCompanies();
            this.loadCompanyStats();
          }
        },
        error: (err) => {
          this.updatingProspectionStatusId = null;
          el.value = this.prospectionRowSelectModel(prospect);
          const msg = err?.error?.message ?? 'Impossible de modifier l\'état prospection.';
          Swal.fire({
            icon: 'error',
            title: 'Erreur',
            text: msg
          });
        }
      });
    });
  }

  // ---------- INIT ----------
  ngOnInit(): void {
    this.routeSub = this.route.data.subscribe(data => {
      this.routeMode = data['mode'] || 'prospects';
      this.selectedCompanyType = this.routeMode;
      this.resetFiltersAndLoad();
    });
    this.loadSectors();
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  private resetFiltersAndLoad(): void {
    this.currentPage = 1;
    this.searchTerm = '';
    this.selectedSector = '';
    this.selectedStatus = '';
    this.selectedProspectionStatus = '';
    this.loadCompanies();
    this.loadCompanyStats();
  }

  private loadSectors(): void {
    this.commercialService.getCompanySectors().subscribe({
      next: (list) => {
        this.sectorOptions = list.map(s => ({ id: s.id, name: s.name }));
        this.uniqueSectors = ['Tous les secteurs', ...this.sectorOptions.map(s => s.name)];
      },
      error: () => { this.sectorOptions = []; }
    });
  }

  // ---------- CHARGEMENT API (liste + stats) ----------
  selectedSectorId: number | null = null;

  loadCompanies(): void {
    const page   = this.currentPage - 1;
    const size   = this.itemsPerPage;
    const search = this.searchTerm;

    this.loadingCompanyList = true;
    const sectorId = this.selectedSectorId || undefined;
    
    const request = this.routeMode === 'prospects'
      ? this.commercialService.getProspects(page, size, search, sectorId, this.selectedProspectionStatus || undefined)
      : this.commercialService.getCompanies(page, size, search, sectorId, this.getIsActiveFilter());

    request.subscribe({
      next: (response) => {
        const companies        = response?.content ?? [];
        this.prospects         = companies.map((c: any) => this.mapCompanyToProspect(c));
        this.filteredProspects = [...this.prospects];
        this.totalElements     = response?.totalElements ?? this.prospects.length;
        this.totalPages        = response?.totalPages ?? 1;
        this.loadingCompanyList = false;
      },
      error: () => {
        this.loadingCompanyList = false;
      }
    });
  }

  loadCompanyStats(): void {
    this.loadingStats = true;
    if (this.routeMode === 'prospects') {
      this.commercialService.getProspectStats().subscribe({
        next: (stats: any) => {
          this.metricsData = [
            { title: 'Total Prospects', value: String(stats?.total ?? 0), icon: '/icones/users.svg' },
            { title: 'En attente',      value: String(stats?.enAttente ?? 0), icon: '/icones/attente.svg' },
            { title: 'Relancés',        value: String(stats?.relancer ?? 0), icon: '/icones/prospection.svg' },
            { title: 'Intéressés',      value: String(stats?.interesses ?? 0), icon: '/icones/actif.svg' },
            { title: 'Signés',          value: String(stats?.signes ?? 0), icon: '/icones/GreenUser.svg' }
          ];
          this.loadingStats = false;
        },
        error: () => {
          this.metricsData = [];
          this.loadingStats = false;
        }
      });
    } else {
      this.commercialService.getPartnerStats().subscribe({
        next: (stats: any) => {
          this.metricsData = [
            { title: 'Total Partenaires', value: String(stats?.totalCompanies ?? 0), icon: '/icones/entreprise.svg' },
            { title: 'Entreprises Actives', value: String(stats?.activeCompanies ?? 0), icon: '/icones/actif.svg' },
            { title: 'Entreprises Inactives', value: String(stats?.inactiveCompanies ?? 0), icon: '/icones/inactif.svg' }
          ];
          this.loadingStats = false;
        },
        error: () => {
          this.metricsData = [];
          this.loadingStats = false;
        }
      });
    }
  }

  // ---------- MODALES ----------
  openCreateModal(): void {
    this.isCreateModalOpen = true;
  }

  onCloseCreateModal(): void {
    this.isCreateModalOpen = false;
  }

  onCompanyCreate(payload: CompanySubmitPayload): void {
    this.submitCompany(payload, false);
  }

  editProspect(id: string): void {
    this.commercialService.getCompanyDetails(id).subscribe({
      next: (details) => {
        this.editingCompanyId = id;
        this.editingCompanyData = this.mapCompanyDetailsToFormData(details);
        this.editingCompanyLogoUrl = details?.logo ? `${environment.imageServerUrl}/files/${details.logo}` : null;
        this.isEditModalOpen = true;
      }
    });
  }

  onCloseEditModal(): void {
    this.isEditModalOpen = false;
    this.editingCompanyId = null;
    this.editingCompanyData = null;
    this.editingCompanyLogoUrl = null;
  }

  onCompanyEdit(payload: CompanySubmitPayload): void {
    if (!this.editingCompanyId) return;
    this.submitCompany(payload, true);
  }

  // ---------- ACTIONS ----------
  async toggleProspectStatus(prospect: Prospect): Promise<void> {
    const nextIsActive = prospect.statut !== 'Actif';
    const result = await Swal.fire({
      title: nextIsActive ? 'Activer cette entreprise ?' : 'Désactiver cette entreprise ?',
      text: nextIsActive ? 'Elle sera visible et ses salariés pourront commander.' : 'Les salariés ne pourront plus commander.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Confirmer',
      cancelButtonText: 'Annuler'
    });

    if (result.isConfirmed) {
      this.commercialService.updateCompanyStatus(prospect.id, nextIsActive).subscribe({
        next: () => {
          this.loadCompanies();
          this.loadCompanyStats();
          Swal.fire({ icon: 'success', title: 'Statut mis à jour', timer: 2000, showConfirmButton: false });
        }
      });
    }
  }

  private submitCompany(payload: CompanySubmitPayload, isEdit: boolean): void {
    this.isCompanySubmitting = true;
    const data = payload.formData;
    const sectorId = this.sectorOptions.find(s => s.name === data.secteur)?.id;
    
    const apiPayload = {
      name: data.nom,
      sectorId,
      location: data.localisation,
      contactName: data.contact,
      contactEmail: data.email || undefined,
      contactPhone: data.telephone,
      status: data.statut,
      note: data.note
    };

    const request$ = isEdit && this.editingCompanyId
      ? this.commercialService.updateCompany(this.editingCompanyId, apiPayload)
      : this.commercialService.createCompany({ ...apiPayload, logo: payload.logo });

    request$.subscribe({
      next: () => {
        this.isCompanySubmitting = false;
        if (isEdit) this.onCloseEditModal();
        else this.onCloseCreateModal();
        
        Swal.fire({
          title: isEdit ? 'Entreprise modifiée' : 'Entreprise créée',
          icon: 'success',
          timer: 3000,
          showConfirmButton: false
        });
        
        this.loadCompanies();
        this.loadCompanyStats();
      },
      error: (err) => {
        this.isCompanySubmitting = false;
        const msg = err?.error?.message || 'Une erreur est survenue';
        Swal.fire('Erreur', msg, 'error');
      }
    });
  }

  // ---------- FILTRES ----------
  toggleStatusDropdown(): void { this.showStatusDropdown = !this.showStatusDropdown; this.closeOthers('status'); }
  toggleSectorDropdown(): void { this.showSectorDropdown = !this.showSectorDropdown; this.closeOthers('sector'); }
  toggleProspectionStatusDropdown(): void { this.showProspectionStatusDropdown = !this.showProspectionStatusDropdown; this.closeOthers('prospection'); }

  private closeOthers(except: string): void {
    if (except !== 'status') this.showStatusDropdown = false;
    if (except !== 'sector') this.showSectorDropdown = false;
    if (except !== 'prospection') this.showProspectionStatusDropdown = false;
  }

  selectStatus(status: string): void {
    this.selectedStatus = status === 'Tous les statuts' ? '' : status;
    this.showStatusDropdown = false;
    this.currentPage = 1;
    this.loadCompanies();
  }

  selectSector(sector: string): void {
    this.selectedSector = sector === 'Tous les secteurs' ? '' : sector;
    this.selectedSectorId = sector === 'Tous les secteurs' ? null : (this.sectorOptions.find(s => s.name === sector)?.id ?? null);
    this.showSectorDropdown = false;
    this.currentPage = 1;
    this.loadCompanies();
  }

  selectProspectionStatus(value: string): void {
    this.selectedProspectionStatus = value;
    this.showProspectionStatusDropdown = false;
    this.currentPage = 1;
    this.loadCompanies();
  }

  onSearch(): void {
    this.currentPage = 1;
    this.loadCompanies();
  }

  // ---------- PAGINATION ----------
  getCurrentPageProspects(): Prospect[] { return this.filteredProspects; }
  trackByProspect(index: number, prospect: Prospect): string { return prospect.id; }
  previousPage(): void { if (this.currentPage > 1) { this.currentPage--; this.loadCompanies(); } }
  nextPage(): void { if (this.currentPage < this.totalPages) { this.currentPage++; this.loadCompanies(); } }
  goToPage(page: number): void { this.currentPage = page; this.loadCompanies(); }
  getPageNumbers(): number[] {
    const pages = [];
    for (let i = 1; i <= this.totalPages; i++) pages.push(i);
    return pages;
  }

  // ---------- MAPPING & UTILS ----------
  private mapCompanyToProspect(c: any): Prospect {
    const initials = c.name ? c.name.split(' ').map((n: any) => n[0]).join('').toUpperCase().substring(0, 2) : '??';
    return {
      id: c.id?.toString(),
      entreprise: c.name,
      secteur: c.sectorLabel || c.sector || '—',
      localisation: c.location || '—',
      contact: { nom: c.contactName || '—', telephone: c.contactPhone || '—', email: c.contactEmail },
      date: this.formatCreatedAt(c.createdAt),
      prospectionStatus: this.canonicalProspectionStatus(c.status),
      statut: c.isActive ? 'Actif' : 'Inactif',
      logo: c.logo ? `${environment.imageServerUrl}/files/${c.logo}` : undefined,
      initials
    };
  }

  private mapCompanyDetailsToFormData(details: any): CompanyFormData {
    return {
      nom: details.name,
      secteur: details.sectorLabel || details.sector,
      localisation: details.location,
      statut: this.canonicalProspectionStatus(details.status),
      contact: details.contactName,
      email: details.contactEmail,
      telephone: details.contactPhone,
      note: details.note
    };
  }

  private mapCompanyDetailsToProspect(details: any): Prospect {
    return this.mapCompanyToProspect(details);
  }

  private getIsActiveFilter(): boolean | undefined {
    if (this.selectedStatus === 'Actif') return true;
    if (this.selectedStatus === 'Inactif') return false;
    return undefined;
  }

  private canonicalProspectionStatus(raw: string | undefined): string {
    const mapping: any = { 'PENDING': 'En attente', 'RELAUNCHED': 'Relancée', 'INTERESTED': 'Intéressée', 'PARTNER_SIGNED': 'Partenaire signé' };
    return mapping[raw!] || raw || 'En attente';
  }

  private formatCreatedAt(createdAt: any): string {
    if (!createdAt) return '—';
    let d: Date;
    if (Array.isArray(createdAt)) {
      d = new Date(createdAt[0], createdAt[1] - 1, createdAt[2], createdAt[3] || 0, createdAt[4] || 0);
    } else {
      // Gère le format ISO ou dd-MM-yyyy (si jamais le backend renvoie encore l'ancien format)
      let dateStr = String(createdAt);
      if (dateStr.includes('-') && dateStr.indexOf('-') === 2) {
        const parts = dateStr.split(' ')[0].split('-');
        d = new Date(+parts[2], +parts[1] - 1, +parts[0]);
      } else {
        d = new Date(dateStr);
      }
    }
    if (isNaN(d.getTime())) return '—';
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  getStatusClass(status: string): string {
    return status === 'Actif' ? 'bg-[#0A97480F] text-[#0A9748]' : 'bg-red-50 text-[#FF0909]';
  }

  getStatusDotClass(status: string): string {
    return status === 'Actif' ? 'bg-[#0A9748]' : 'bg-[#FF0909]';
  }
}