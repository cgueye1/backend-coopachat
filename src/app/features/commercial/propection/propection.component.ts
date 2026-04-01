import { Component, OnInit, NgZone, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { CompanyModalComponent, CompanyFormData, CompanySubmitPayload } from '../../../shared/components/company-modal/company-modal.component';
import { CommercialService } from '../../../shared/services/commercial.service';
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
  logo?: string; // URL pour afficher le logo (ex. /api/files/companies/uuid.png)
  employeeCount?: number;
  orderCount?: number;
}

//Interfaces pour les statistiques 
interface MetricCard {
  title: string;
  value: string;
  icon: string;
}

@Component({
  selector: 'app-prospection',
  standalone: true,
  imports: [CommonModule, FormsModule, MainLayoutComponent, CompanyModalComponent, HeaderComponent],
  templateUrl: `./propection.component.html`,
  styles: []
})
export class ProspectionComponent implements OnInit {

  // --- Variables de filtres ---
  searchTerm = ''; // Terme de recherche dans le tableau
  selectedStatus = ''; // Filtre Actif/Inactif (page partenaires)
  selectedSector = ''; // Filtre secteur d'activité
  selectedProspectionStatus = ''; // Filtre état prospection (page prospects)
  selectedCompanyType = ''; // Type de vue (prospects ou partenaires)
  routeMode: 'prospects' | 'partenaires' = 'prospects'; // Mode page courant

  // --- Pagination ---
  currentPage  = 1;
  itemsPerPage = 6;
  totalElements = 0; // Nombre total renvoyé par le backend

  // --- Visibilité des menus déroulants ---
  showStatusDropdown            = false;
  showCompanyTypeDropdown       = false;
  showProspectionStatusDropdown = false;
  showSectorDropdown            = false;

  // --- Modales (détails / création / modification) ---
  showProspectModal = false;
  selectedProspect: Prospect | null = null; // Prospect affiché dans la modale détails
  isCreateModalOpen    = false;
  isEditModalOpen      = false;
  isCompanySubmitting  = false; // Loader pendant l'enregistrement
  editingCompanyId: string | null   = null;
  editingCompanyData: CompanyFormData | null = null;
  editingCompanyLogoUrl: string | null = null; // Logo existant en mode édition
  private successPopupTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private submitSequence = 0;

  // --- Données et listes ---
  /** KPI en-tête (prospects ou partenaires selon routeMode). */
  loadingStats: boolean = false;
  /** Tableau paginé (liste entreprises / prospects). */
  loadingCompanyList: boolean = false;
  metricsData: MetricCard[] = []; // Cartes stats en haut
  prospects: Prospect[] = [];
  filteredProspects: Prospect[] = []; // Liste affichée (avec pagination)

  uniqueStatuses = ['Tous les statuts', 'Actif', 'Inactif'];
  prospectionStatusOptions = [
    { value: '',           label: 'Tous les statuts prospection' },
    { value: 'PENDING',    label: 'En attente' },
    { value: 'RELAUNCHED', label: 'Relancée' },
    { value: 'INTERESTED', label: 'Intéressée' },
    { value: 'REFUSED',    label: 'Refusée' }
  ];


  // Ferme tous les dropdowns au clic en dehors
  @HostListener('document:click')
  onDocumentClick(): void {
    this.showStatusDropdown = false;
    this.showSectorDropdown = false;
    this.showCompanyTypeDropdown = false;
    this.showProspectionStatusDropdown = false;
  }

  // Secteurs d'activité 
  /** Secteurs chargés depuis l'API (référentiel admin). */
  sectorOptions: { id: number; name: string }[] = [];
  /** Pour le filtre : libellé affiché + option "Tous les secteurs". */
  get uniqueSectors(): string[] {
    return ['Tous les secteurs', ...this.sectorOptions.map(s => s.name)];
  }

  // ---------- BADGES (couleurs selon statut prospection) ----------
  get selectedProspectionStatusLabel(): string {
    return this.prospectionStatusOptions.find(o => o.value === this.selectedProspectionStatus)?.label
      ?? 'Tous les statuts prospection';
  }

  /** Couleurs des badges selon statut prospection */
  getProspectionStatusClasses(status: string | undefined): string {
    if (!status) return 'bg-gray-50 text-gray-600';
    const s = status.toLowerCase();
    if (s.includes('attente')    || s === 'pending')    return 'bg-gray-50 text-gray-600';
    if (s.includes('relanc')     || s === 'relaunched') return 'bg-blue-50 text-blue-600';
    if (s.includes('intéress')   || s === 'interested') return 'bg-emerald-50 text-emerald-600';
    if (s.includes('rendez-vous')|| s.includes('meeting')) return 'bg-violet-50 text-violet-600';
    if (s.includes('refus')      || s === 'refused')    return 'bg-red-50 text-red-600';
    if (s.includes('signé')      || s.includes('signed')) return 'bg-green-50 text-green-600';
    return 'bg-gray-50 text-gray-600';
  }

  getProspectionStatusDotClasses(status: string | undefined): string {
    if (!status) return 'bg-gray-400';
    const s = status.toLowerCase();
    if (s.includes('attente')    || s === 'pending')    return 'bg-gray-400';
    if (s.includes('relanc')     || s === 'relaunched') return 'bg-blue-500';
    if (s.includes('intéress')   || s === 'interested') return 'bg-emerald-500';
    if (s.includes('rendez-vous')|| s.includes('meeting')) return 'bg-violet-500';
    if (s.includes('refus')      || s === 'refused')    return 'bg-red-500';
    if (s.includes('signé')      || s.includes('signed')) return 'bg-green-500';
    return 'bg-gray-400';
  }

  // ---------- INIT ----------
  constructor(
    private commercialService: CommercialService,
    private route: ActivatedRoute,
    private router: Router,
    private ngZone: NgZone
  ) {}

  ngOnInit(): void {
    const mode = this.route.snapshot.data['mode'] as 'prospects' | 'partenaires' | undefined;
    this.routeMode = mode === 'partenaires' ? 'partenaires' : 'prospects';
    this.selectedCompanyType = this.routeMode;

    this.commercialService.getCompanySectors().subscribe({
      next: (list) => { this.sectorOptions = list.map(s => ({ id: s.id, name: s.name })); },
      error: () => { this.sectorOptions = []; }
    });
    this.loadCompanies();
    this.loadCompanyStats();

    this.route.queryParams.subscribe((params) => {
      if (params['id']) this.openProspectDetailById(params['id']);
    });
  }

  // ---------- CHARGEMENT API (liste + stats) ----------
  /** ID du secteur sélectionné pour le filtre (null = tous). */
  selectedSectorId: number | null = null;

  loadCompanies(): void {
    const page   = this.currentPage - 1;
    const size   = this.itemsPerPage;
    const search = this.searchTerm;

    this.loadingCompanyList = true;
    const request = this.routeMode === 'prospects'
      ? this.commercialService.getProspects(page, size, search, this.selectedSectorId ?? undefined, this.selectedProspectionStatus || undefined)
      : this.commercialService.getCompanies(page, size, search, this.selectedSectorId ?? undefined, this.getIsActiveFilter());

    request.subscribe({
      next: (response) => {
        const companies        = response?.content ?? [];
        this.prospects         = companies.map((c: any) => this.mapCompanyToProspect(c));
          this.filteredProspects = [...this.prospects];
        this.totalElements     = response?.totalElements ?? this.prospects.length;
        this.loadingCompanyList = false;
      },
      error: (error) => {
        console.error('Erreur chargement:', error);
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
            { title: 'Total prospects', value: String(stats?.total      ?? 0), icon: '/icones/entreprise.svg' },
            { title: 'En attente',      value: String(stats?.enAttente  ?? 0), icon: '/icones/entreprise.svg' },
            { title: 'Relancés',        value: String(stats?.relancer   ?? 0), icon: '/icones/entreprise.svg' },
            { title: 'Intéressés',      value: String(stats?.interesses ?? 0), icon: '/icones/actif.svg' },
            { title: 'Signés',          value: String(stats?.signes     ?? 0), icon: '/icones/inactif.svg' }
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
            { title: 'Total partenaires', value: String(stats?.totalCompanies    ?? 0), icon: '/icones/entreprise.svg' },
            { title: 'Actives',           value: String(stats?.activeCompanies   ?? 0), icon: '/icones/actif.svg' },
            { title: 'Inactives',         value: String(stats?.inactiveCompanies ?? 0), icon: '/icones/inactif.svg' }
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

  // ---------- MODALE CRÉATION ----------
  openCreateModal(): void {
    this.isEditModalOpen   = false;
    this.isCreateModalOpen = true;
  }

  onCloseCreateModal(): void {
    this.isCreateModalOpen = false;
  }

  onCompanyCreate(payload: CompanySubmitPayload): void {
    this.submitCompany(payload, false);
  }

  // ---------- MODALE MODIFICATION ----------
  editProspect(id: string): void {
    this.isCreateModalOpen = false;
    this.commercialService.getCompanyDetails(id).subscribe({
      next: (details) => {
        this.editingCompanyId    = id;
        this.editingCompanyData  = this.mapCompanyDetailsToFormData(details);
        this.editingCompanyLogoUrl = details?.logo
          ? `${environment.imageServerUrl}/files/${details.logo}`
          : null;
        this.isEditModalOpen     = true;
      },
      error: (error) => console.error('Erreur chargement pour modification:', error)
    });
  }

  // Méthode qui permet de : ......
  onCloseEditModal(): void {
    this.isEditModalOpen      = false;
    this.editingCompanyId     = null;
    this.editingCompanyData   = null;
    this.editingCompanyLogoUrl = null;
  }


  onCompanyEdit(payload: CompanySubmitPayload): void {
    if (!this.editingCompanyId) return;
    this.submitCompany(payload, true);
  }

  // ---------- MODALE DÉTAILS ----------
  viewDetails(prospect: Prospect): void {
    this.commercialService.getCompanyDetails(prospect.id).subscribe({
      next: (details) => {
        this.selectedProspect  = this.mapCompanyDetailsToProspect(details);
        this.showProspectModal = true;
      },
      error: () => {
        this.selectedProspect  = prospect;
        this.showProspectModal = true;
      }
    });
  }

  closeProspectModal(): void {
    this.showProspectModal = false;
    this.selectedProspect  = null;
  }

  modifierProspect(): void {
    if (!this.selectedProspect) return;
    const id = this.selectedProspect.id;
    this.closeProspectModal();
    this.editProspect(id);
  }

  annulerProspect(): void {
    this.closeProspectModal();
  }

  private openProspectDetailById(id: string): void {
    this.commercialService.getCompanyDetails(id).subscribe({
      next: (details) => {
        this.selectedProspect  = this.mapCompanyDetailsToProspect(details);
        this.showProspectModal = true;
      },
      error: () => {
        this.selectedProspect  = {
          id, companyCode: '', entreprise: '', secteur: '',
          localisation: '', contact: { nom: '', telephone: '' },
          statut: 'Inactif', date: '', initials: ''
        };
        this.showProspectModal = true;
      }
    });
  }

  // ---------- ACTIVER / DÉSACTIVER ENTREPRISE ----------
  async toggleProspectStatus(prospect: Prospect): Promise<void> {
    const nextIsActive = prospect.statut !== 'Actif';

    const result = await Swal.fire({
      title: nextIsActive ? 'Activer cette entreprise partenaire ?' : 'Désactiver cette entreprise partenaire ?',
      text: nextIsActive
        ? 'Les membres auront accès à leurs actions (commandes, promotions, etc.).'
        : 'Les salariés ne pourront plus se connecter ni effectuer de commandes.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: nextIsActive ? '#22C55E' : '#EF4444',
      cancelButtonColor: '#6B7280',
      confirmButtonText: nextIsActive ? 'Activer' : 'Désactiver',
      cancelButtonText: 'Annuler'
    });

    if (!result.isConfirmed) return;

    this.ngZone.run(() => {
    this.commercialService.updateCompanyStatus(prospect.id, nextIsActive).subscribe({
      next: () => {
        prospect.statut = nextIsActive ? 'Actif' : 'Inactif';
        this.loadCompanies();
          this.loadCompanyStats();
          Swal.fire({
            title: nextIsActive ? 'Entreprise activée' : 'Entreprise désactivée',
            icon: 'success', timer: 2000, showConfirmButton: false
          });
      },
      error: (error) => {
          const msg = error?.error?.message || 'Impossible de modifier le statut.';
          Swal.fire({ title: 'Erreur', text: typeof msg === 'string' ? msg : 'Impossible de modifier le statut.', icon: 'error' });
        }
      });
    });
  }

  // ---------- SOUMISSION FORMULAIRE (création + modification) ----------
  private submitCompany(payload: CompanySubmitPayload, isEdit: boolean): void {
    const data = payload.formData;
    const submitId = ++this.submitSequence;
    this.isCompanySubmitting = true;
    Swal.close();

    if (this.successPopupTimeoutId) {
      clearTimeout(this.successPopupTimeoutId);
      this.successPopupTimeoutId = null;
    }

    const fallbackData = this.editingCompanyData;
    const resolveValue = (value?: string, fallback?: string): string => {
      const trimmed = value?.trim();
      return trimmed ? trimmed : (fallback?.trim() ?? '');
    };

    const secteurLabel = resolveValue(data.secteur, fallbackData?.secteur) || undefined;
    const sectorId = secteurLabel ? this.sectorOptions.find(s => s.name === secteurLabel)?.id : undefined;
    const apiPayload = {
      name:         resolveValue(data.nom,          fallbackData?.nom),
      sectorId,
      location:     resolveValue(data.localisation, fallbackData?.localisation),
      contactName:  resolveValue(data.contact,      fallbackData?.contact),
      contactEmail: resolveValue(data.email,        fallbackData?.email)        || undefined,
      contactPhone: resolveValue(data.telephone,    fallbackData?.telephone),
      status:       resolveValue(data.statut,       fallbackData?.statut),
      note:         (data.note ?? fallbackData?.note ?? '').trim()
    };

    const request$ = isEdit && this.editingCompanyId
      ? this.commercialService.updateCompany(this.editingCompanyId, apiPayload)
      : this.commercialService.createCompany({ ...apiPayload, logo: payload.logo });

    const isPartnerSigned = (apiPayload.status === 'Partenaire signé' || apiPayload.status === 'PARTNER_SIGNED');
    const shouldRedirectToPartners = this.routeMode === 'prospects' && isPartnerSigned;

    request$.subscribe({
      next: () => {
        if (isEdit) {
          const logoFile = payload.logo;
          const deleteLogo = payload.deleteLogo;
          if (logoFile) {
            this.commercialService.uploadCompanyLogo(this.editingCompanyId!, logoFile).subscribe({
              next: () => this.finishSubmitSuccess(true, shouldRedirectToPartners, this.editingCompanyId ?? undefined),
              error: () => {
                this.isCompanySubmitting = false;
                Swal.fire({
                  icon: 'warning',
                  title: 'Logo non enregistré',
                  text: 'L\'entreprise a été modifiée mais l\'upload du logo a échoué.',
                  confirmButtonText: 'OK'
                });
                this.finishSubmitSuccess(true, shouldRedirectToPartners, this.editingCompanyId ?? undefined);
              }
            });
          } else if (deleteLogo) {
            this.commercialService.deleteCompanyLogo(this.editingCompanyId!).subscribe({
              next: () => this.finishSubmitSuccess(true, shouldRedirectToPartners, this.editingCompanyId ?? undefined),
              error: () => {
                this.isCompanySubmitting = false;
                Swal.fire({
                  icon: 'warning',
                  title: 'Logo non supprimé',
                  text: 'L\'entreprise a été modifiée mais la suppression du logo a échoué.',
                  confirmButtonText: 'OK'
                });
                this.finishSubmitSuccess(true, shouldRedirectToPartners, this.editingCompanyId ?? undefined);
              }
            });
          } else {
            this.finishSubmitSuccess(true, shouldRedirectToPartners, this.editingCompanyId ?? undefined);
          }
        } else {
          this.finishSubmitSuccess(false, shouldRedirectToPartners);
        }
      },
      error: (error) => {
        this.isCompanySubmitting = false;
        if (this.successPopupTimeoutId) {
          clearTimeout(this.successPopupTimeoutId);
          this.successPopupTimeoutId = null;
        }
        Swal.close();

        const rawMessage = error?.error?.message || 'Erreur lors de l\'enregistrement';
        let message = rawMessage;
        if (rawMessage.includes('email') && rawMessage.includes('existe déjà')) {
          message = 'Cet email de contact existe déjà. Utilise un autre email.';
        } else if (rawMessage.includes('téléphone') && rawMessage.includes('existe déjà')) {
          message = 'Ce numéro de téléphone existe déjà. Utilise un autre numéro.';
        } else if (rawMessage.includes('Duplicata')) {
          message = 'Cet email de contact existe déjà. Utilise un autre email.';
        }

        Swal.fire({ icon: 'error', title: isEdit ? 'Modification échouée' : 'Création échouée', text: message });
      }
    });
  }

  private finishSubmitSuccess(isEdit: boolean, redirectToPartners = false, updatedCompanyId?: string): void {
    const idToRefetch = isEdit ? (updatedCompanyId ?? this.editingCompanyId ?? undefined) : undefined;
    this.isCompanySubmitting = false;
    if (isEdit) {
      this.isEditModalOpen      = false;
      this.editingCompanyId     = null;
      this.editingCompanyData   = null;
      this.editingCompanyLogoUrl = null;
    } else {
      this.isCreateModalOpen = false;
          }
          Swal.fire({
            title: isEdit ? 'Entreprise modifiée avec succès' : 'Entreprise créée avec succès',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
            showConfirmButton: false,
      timer: 3000,
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
    if (redirectToPartners) {
      this.router.navigate(['/com/entreprises']);
    } else {
      this.loadCompanies();
      this.loadCompanyStats();
      // Rafraîchir les détails de l’entreprise modifiée pour mettre à jour le secteur (et la note) dans la liste
      if (idToRefetch) {
        this.commercialService.getCompanyDetails(idToRefetch).subscribe({
          next: (details) => {
            const updated = this.mapCompanyDetailsToProspect(details);
            const idx = this.prospects.findIndex(p => p.id === idToRefetch || p.id === updated.id);
            if (idx !== -1) {
              this.prospects[idx] = { ...this.prospects[idx], ...updated };
              this.filteredProspects = [...this.prospects];
            }
          }
        });
      }
    }
  }

  // ---------- FILTRES (toggle + select → recharge) ----------
  toggleStatusDropdown():            void { this.showStatusDropdown = !this.showStatusDropdown; this.showSectorDropdown = this.showCompanyTypeDropdown = this.showProspectionStatusDropdown = false; }
  toggleSectorDropdown():            void { this.showSectorDropdown = !this.showSectorDropdown; this.showStatusDropdown = this.showCompanyTypeDropdown = this.showProspectionStatusDropdown = false; }
  toggleCompanyTypeDropdown():       void { this.showCompanyTypeDropdown = !this.showCompanyTypeDropdown; this.showSectorDropdown = this.showStatusDropdown = this.showProspectionStatusDropdown = false; }
  toggleProspectionStatusDropdown(): void { this.showProspectionStatusDropdown = !this.showProspectionStatusDropdown; this.showSectorDropdown = this.showStatusDropdown = this.showCompanyTypeDropdown = false; }

  selectStatus(status: string): void {
    this.selectedStatus     = status === 'Tous les statuts' ? '' : status;
    this.showStatusDropdown = false;
    this.currentPage        = 1;
    this.loadCompanies();
  }

  selectSector(sector: string): void {
    this.selectedSector     = sector === 'Tous les secteurs' ? '' : sector;
    this.selectedSectorId   = sector === 'Tous les secteurs' ? null : (this.sectorOptions.find(s => s.name === sector)?.id ?? null);
    this.showSectorDropdown = false;
    this.currentPage        = 1;
    this.loadCompanies();
  }

  selectCompanyType(value: string): void {
    this.selectedCompanyType         = value;
    this.selectedProspectionStatus   = '';
    this.showCompanyTypeDropdown     = false;
    this.currentPage                 = 1;
    this.loadCompanies();
  }

  selectProspectionStatus(value: string): void {
    this.selectedProspectionStatus        = value;
    this.showProspectionStatusDropdown    = false;
    this.currentPage                      = 1;
    this.loadCompanies();
  }

  onSearch(): void {
    this.currentPage = 1;
    this.loadCompanies();
  }

  // ---------- PAGINATION (backend gère la tranche) ----------
  get totalResults(): number {
    return this.totalElements > 0 ? this.totalElements : this.filteredProspects.length;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalResults / this.itemsPerPage));
  }

  getCurrentPageProspects(): Prospect[] {
    return this.filteredProspects;
  }

  trackByProspect(index: number, prospect: Prospect): string {
    return prospect.id;
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
    const maxVisible = 5;
    let start = Math.max(1, this.currentPage - Math.floor(maxVisible / 2));
    let end   = Math.min(this.totalPages, start + maxVisible - 1);
    if (end - start + 1 < maxVisible) start = Math.max(1, end - maxVisible + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  previousPage(): void { if (this.currentPage > 1)                  { this.currentPage--; this.loadCompanies(); } }
  nextPage():     void { if (this.currentPage < this.totalPages)     { this.currentPage++; this.loadCompanies(); } }
  goToPage(page: number): void { if (page >= 1 && page <= this.totalPages) { this.currentPage = page; this.loadCompanies(); } }

  // ---------- TRANSFORMATIONS API → FRONT (tableau, détails, formulaire) ----------
  private mapCompanyToProspect(company: any): Prospect {
    const name     = company?.name ?? '';
    const initials = name.split(' ')
      .filter((w: string) => w)
      .map((w: string) => w[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();

    const logoUrl = company?.logo
      ? `${environment.imageServerUrl}/files/${company.logo}`
      : undefined;
    return {
      id:                company?.id?.toString() ?? initials,
      entreprise:        name,
      secteur:           company?.sector ?? 'Non défini',
      localisation:      company?.location ?? '',
      contact: {
        nom:       company?.contactName  ?? '',
        email:     company?.contactEmail ?? '',
        telephone: company?.contactPhone ?? ''
      },
      statut:            this.deriveStatutFromCompany(company),
      prospectionStatus: company?.status ?? '',
      date:              this.formatCreatedAt(company?.createdAt),
      initials,
      logo:              logoUrl
    };
  }

  private mapCompanyDetailsToProspect(details: any): Prospect {
    const name     = details?.name ?? '';
    const initials = name.split(' ')
      .filter((w: string) => w)
      .map((w: string) => w[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();

    const logoUrl = details?.logo
      ? `${environment.imageServerUrl}/files/${details.logo}`
      : undefined;
    return {
      id:                details?.id?.toString() ?? initials,
      companyCode:       details?.companyCode ?? '',
      entreprise:        name,
      secteur:           details?.sectorLabel ?? 'Non défini',
      note:              details?.note        ?? '',
      localisation:      details?.location    ?? '',
      contact: {
        nom:       details?.contactName  ?? '',
        email:     details?.contactEmail ?? '',
        telephone: details?.contactPhone ?? ''
      },
      statut:            this.deriveStatutFromCompany(details),
      prospectionStatus: details?.status ?? '',
      date:              this.formatCreatedAt(details?.createdAt),
      initials,
      logo:              logoUrl,
      employeeCount:     details?.employeeCount ?? undefined,
      orderCount:        details?.orderCount    ?? undefined
    };
  }

  private mapCompanyDetailsToFormData(details: any): CompanyFormData {
    return {
      nom:          details?.name          ?? '',
      secteur:      details?.sectorLabel   ?? '',
      localisation: details?.location      ?? '',
      statut:       this.normalizeStatusForForm(details?.status),
      contact:      details?.contactName   ?? '',
      email:        details?.contactEmail  ?? '',
      telephone:    details?.contactPhone  ?? '',
      note:         details?.note          ?? ''
    };
  }

  private deriveStatutFromCompany(company: any): 'Actif' | 'Inactif' {
    if (company?.isActive === true)  return 'Actif';
    if (company?.isActive === false) return 'Inactif';
    if (company?.active === true)    return 'Actif';
    if (company?.active === false)   return 'Inactif';
    const status = typeof company?.status === 'string' ? company.status.trim().toUpperCase() : '';
    if (status === 'INACTIVE' || status === 'INACTIF') return 'Inactif';
    return 'Actif';
  }

  private getIsActiveFilter(): boolean | undefined {
    if (this.selectedStatus === 'Actif')   return true;
    if (this.selectedStatus === 'Inactif') return false;
    return undefined;
  }

  private normalizeStatusForForm(rawStatus: string | undefined): string {
    const allowed = ['En attente', 'Relancée', 'Intéressée', 'Refusée', 'Partenaire signé'];
    if (rawStatus && allowed.includes(rawStatus)) return rawStatus;
    const mapping: Record<string, string> = {
      PENDING: 'En attente',   RELAUNCHED: 'Relancée',
      INTERESTED: 'Intéressée', REFUSED: 'Refusée',
      PARTNER_SIGNED: 'Partenaire signé'
    };
    return mapping[rawStatus ?? ''] || 'En attente';
  }

  private formatCreatedAt(createdAt: string | undefined): string {
    if (!createdAt) return '';
    return createdAt.split(' ')[0] ?? createdAt;
  }

  // ---------- BADGES Actif/Inactif (tableau) ----------
  getStatusClass(status: string): string {
    return status === 'Actif' ? 'bg-[#0A97480F] text-[#0A9748]' : 'bg-red-50 text-[#FF0909]';
  }

  getStatusDotClass(status: string): string {
    return status === 'Actif' ? 'bg-[#0A9748]' : 'bg-[#FF0909]';
  }
}