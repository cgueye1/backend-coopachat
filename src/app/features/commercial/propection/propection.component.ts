import { Component, OnInit, OnDestroy, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { CompanyModalComponent, CompanySubmitPayload, CompanyFormData } from '../../../shared/components/company-modal/company-modal.component';
import { CommercialService, ProspectStats, CompanyStats } from '../../../shared/services/commercial.service';
import { environment } from '../../../../environments/environment';
import Swal from 'sweetalert2';

interface Prospect {
  id: string;
  entreprise: string;
  secteur: string;
  localisation: string;
  contact: {
    nom: string;
    telephone: string;
  };
  date: string;
  prospectionStatus: string;
  statut: 'Actif' | 'Inactif';
  logo?: string;
  initials: string;
}

@Component({
  selector: 'app-propection',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MainLayoutComponent,
    HeaderComponent,
    CompanyModalComponent
  ],
  templateUrl: './propection.component.html',
  styleUrls: ['./propection.component.css']
})
export class ProspectionComponent implements OnInit, OnDestroy {
  routeMode: 'prospects' | 'partenaires' = 'prospects';
  private routeSub?: Subscription;

  // Modals
  isCreateModalOpen = false;
  isEditModalOpen = false;
  isCompanySubmitting = false;
  editingCompanyData: CompanyFormData | null = null;
  editingCompanyId: string | null = null;
  editingCompanyLogoUrl: string | null = null;

  // Stats
  loadingStats = false;
  metricsData: any[] = [];

  // Table & Filters
  searchTerm = '';
  loadingCompanyList = false;
  companyList: Prospect[] = [];
  
  currentPage = 1;
  totalPages = 1;
  totalElements = 0;
  readonly itemsPerPage = 10;

  // Sectors
  showSectorDropdown = false;
  selectedSector = '';
  sectorOptions: { id: number; name: string }[] = [];
  uniqueSectors: string[] = [];

  // Status (Active/Inactive)
  showStatusDropdown = false;
  selectedStatus = '';
  uniqueStatuses = ['Tous les statuts', 'Actif', 'Inactif'];

  // Prospection Status
  showProspectionStatusDropdown = false;
  selectedProspectionStatus = '';
  prospectionStatusOptions = [
    { label: 'Tous les états', value: '' },
    { label: 'En attente', value: 'PENDING' },
    { label: 'Relancée', value: 'RELAUNCHED' },
    { label: 'Intéressée', value: 'INTERESTED' },
    { label: 'Partenaire signé', value: 'PARTNER_SIGNED' }
  ];

  prospectionTableStatusLabels = ['En attente', 'Relancée', 'Intéressée', 'Partenaire signé'];
  updatingProspectionStatusId: string | null = null;

  private commercialService = inject(CommercialService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  ngOnInit(): void {
    this.routeSub = this.route.data.subscribe(data => {
      this.routeMode = data['mode'] || 'prospects';
      this.resetFiltersAndLoad();
    });
    this.loadSectors();
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.showSectorDropdown = false;
    this.showStatusDropdown = false;
    this.showProspectionStatusDropdown = false;
  }

  private resetFiltersAndLoad(): void {
    this.currentPage = 1;
    this.searchTerm = '';
    this.selectedSector = '';
    this.selectedStatus = '';
    this.selectedProspectionStatus = '';
    this.loadStats();
    this.loadData();
  }

  loadStats(): void {
    this.loadingStats = true;
    if (this.routeMode === 'prospects') {
      this.commercialService.getProspectStats().subscribe({
        next: (stats: ProspectStats) => {
          this.metricsData = [
            { title: 'Total Prospects', value: stats.total, icon: '/icones/users.svg' },
            { title: 'En attente', value: stats.enAttente, icon: '/icones/attente.svg' },
            { title: 'Intéressés', value: stats.interesses, icon: '/icones/prospection.svg' }
          ];
          this.loadingStats = false;
        },
        error: () => this.loadingStats = false
      });
    } else {
      this.commercialService.getPartnerStats().subscribe({
        next: (stats: CompanyStats) => {
          this.metricsData = [
            { title: 'Total Partenaires', value: stats.totalCompanies, icon: '/icones/entreprise.svg' },
            { title: 'Entreprises Actives', value: stats.activeCompanies, icon: '/icones/actif.svg' },
            { title: 'Entreprises Inactives', value: stats.inactiveCompanies, icon: '/icones/inactif.svg' }
          ];
          this.loadingStats = false;
        },
        error: () => this.loadingStats = false
      });
    }
  }

  loadSectors(): void {
    this.commercialService.getCompanySectors().subscribe({
      next: (sectors) => {
        this.sectorOptions = sectors;
        this.uniqueSectors = ['Tous les secteurs', ...sectors.map(s => s.name)];
      }
    });
  }

  loadData(): void {
    this.loadingCompanyList = true;
    const sectorId = this.sectorOptions.find(s => s.name === this.selectedSector)?.id;
    
    if (this.routeMode === 'prospects') {
      this.commercialService.getProspects(
        this.currentPage - 1,
        this.itemsPerPage,
        this.searchTerm,
        sectorId,
        this.selectedProspectionStatus || undefined
      ).subscribe({
        next: (res) => {
          this.processResponse(res);
        },
        error: () => this.loadingCompanyList = false
      });
    } else {
      const isActive = this.selectedStatus === 'Actif' ? true : (this.selectedStatus === 'Inactif' ? false : undefined);
      this.commercialService.getCompanies(
        this.currentPage - 1,
        this.itemsPerPage,
        this.searchTerm,
        sectorId,
        isActive
      ).subscribe({
        next: (res) => {
          this.processResponse(res);
        },
        error: () => this.loadingCompanyList = false
      });
    }
  }

  private processResponse(res: any): void {
    this.companyList = (res.content || []).map((c: any) => this.mapToProspect(c));
    this.totalElements = res.totalElements || 0;
    this.totalPages = res.totalPages || 1;
    this.loadingCompanyList = false;
  }

  private mapToProspect(c: any): Prospect {
    const initials = c.name ? c.name.split(' ').map((n: string) => n[0]).join('').toUpperCase().substring(0, 2) : '??';
    
    // Parsing robuste de la date (gère les strings ISO et les tableaux Jackson [y,m,d])
    let displayDate = '—';
    if (c.createdAt) {
      const d = new Date(c.createdAt);
      if (!isNaN(d.getTime())) {
        displayDate = d.toLocaleDateString('fr-FR');
      } else if (Array.isArray(c.createdAt) && c.createdAt.length >= 3) {
        const [y, m, day] = c.createdAt;
        const dateObj = new Date(y, m - 1, day);
        if (!isNaN(dateObj.getTime())) {
          displayDate = dateObj.toLocaleDateString('fr-FR');
        }
      }
    }

    return {
      id: c.id,
      entreprise: c.name,
      secteur: c.sectorLabel || '—',
      localisation: c.location || '—',
      contact: {
        nom: c.contactName || '—',
        telephone: c.contactPhone || '—'
      },
      date: displayDate,
      prospectionStatus: c.statusLabel || c.status || 'En attente',
      statut: c.isActive ? 'Actif' : 'Inactif',
      logo: c.logo ? `${environment.imageServerUrl}/files/${c.logo}` : undefined,
      initials
    };
  }

  onSearch(): void {
    this.currentPage = 1;
    this.loadData();
  }

  toggleSectorDropdown(): void {
    this.showSectorDropdown = !this.showSectorDropdown;
  }

  selectSector(sector: string): void {
    this.selectedSector = sector === 'Tous les secteurs' ? '' : sector;
    this.showSectorDropdown = false;
    this.currentPage = 1;
    this.loadData();
  }

  toggleStatusDropdown(): void {
    this.showStatusDropdown = !this.showStatusDropdown;
  }

  selectStatus(status: string): void {
    this.selectedStatus = status === 'Tous les statuts' ? '' : status;
    this.showStatusDropdown = false;
    this.currentPage = 1;
    this.loadData();
  }

  toggleProspectionStatusDropdown(): void {
    this.showProspectionStatusDropdown = !this.showProspectionStatusDropdown;
  }

  selectProspectionStatus(value: string): void {
    this.selectedProspectionStatus = value;
    this.showProspectionStatusDropdown = false;
    this.currentPage = 1;
    this.loadData();
  }

  get selectedProspectionStatusLabel(): string {
    return this.prospectionStatusOptions.find(o => o.value === this.selectedProspectionStatus)?.label || 'Tous les états';
  }

  onProspectionStatusChange(prospect: Prospect, event: any): void {
    const newStatusLabel = event.target.value;
    this.updatingProspectionStatusId = prospect.id;
    
    this.commercialService.updateCompanyProspectionStatus(prospect.id, newStatusLabel)
      .pipe(finalize(() => this.updatingProspectionStatusId = null))
      .subscribe({
        next: () => {
          prospect.prospectionStatus = newStatusLabel;
          if (newStatusLabel === 'Partenaire signé') {
            Swal.fire({
              title: 'Statut changé avec succès !',
              text: 'L\'entreprise est désormais partenaire. L\'utilisateur peut maintenant activer son compte via l\'email envoyé.',
              icon: 'success',
              confirmButtonColor: '#2B3674'
            });
            // Si on est en mode prospects, on rafraîchit car l'entreprise devrait disparaître de cette liste
            if (this.routeMode === 'prospects') {
              this.loadData();
              this.loadStats();
            }
          } else {
            Swal.fire({
              toast: true,
              position: 'top-end',
              icon: 'success',
              title: 'Statut mis à jour',
              showConfirmButton: false,
              timer: 3000
            });
          }
        },
        error: (err) => {
          Swal.fire('Erreur', 'Impossible de mettre à jour le statut', 'error');
        }
      });
  }

  getProspectionStatusClasses(status: string): string {
    switch (status) {
      case 'Partenaire signé': return 'bg-green-50 text-green-700';
      case 'Intéressée': return 'bg-blue-50 text-blue-700';
      case 'Relancée': return 'bg-orange-50 text-orange-700';
      default: return 'bg-gray-50 text-gray-700';
    }
  }

  getProspectionStatusDotClasses(status: string): string {
    switch (status) {
      case 'Partenaire signé': return 'bg-green-500';
      case 'Intéressée': return 'bg-blue-500';
      case 'Relancée': return 'bg-orange-500';
      default: return 'bg-gray-500';
    }
  }

  openCreateModal(): void {
    this.isCreateModalOpen = true;
  }

  onCloseCreateModal(): void {
    this.isCreateModalOpen = false;
  }

  onCompanyCreate(payload: CompanySubmitPayload): void {
    this.isCompanySubmitting = true;
    const sectorId = this.sectorOptions.find(s => s.name === payload.formData.secteur)?.id;
    
    this.commercialService.createCompany({
      name: payload.formData.nom,
      sectorId,
      location: payload.formData.localisation,
      contactName: payload.formData.contact,
      contactEmail: payload.formData.email,
      contactPhone: payload.formData.telephone,
      status: payload.formData.statut,
      note: payload.formData.note,
      logo: payload.logo
    }).pipe(finalize(() => this.isCompanySubmitting = false))
      .subscribe({
        next: () => {
          this.onCloseCreateModal();
          this.loadData();
          this.loadStats();
          Swal.fire('Succès', 'Entreprise créée avec succès', 'success');
        },
        error: (err) => {
          Swal.fire('Erreur', 'Impossible de créer l\'entreprise', 'error');
        }
      });
  }

  editProspect(id: string): void {
    this.loadingCompanyList = true;
    this.commercialService.getCompanyDetails(id).subscribe({
      next: (details) => {
        this.editingCompanyId = id;
        this.editingCompanyData = {
          nom: details.name,
          secteur: details.sectorLabel || '',
          localisation: details.location || '',
          statut: details.statusLabel || details.status || 'En attente',
          contact: details.contactName || '',
          email: details.contactEmail || '',
          telephone: details.contactPhone || '',
          note: details.note || ''
        };
        this.editingCompanyLogoUrl = details.logo ? `${environment.imageServerUrl}/files/${details.logo}` : null;
        this.isEditModalOpen = true;
        this.loadingCompanyList = false;
      },
      error: () => this.loadingCompanyList = false
    });
  }

  onCloseEditModal(): void {
    this.isEditModalOpen = false;
    this.editingCompanyId = null;
    this.editingCompanyData = null;
  }

  onCompanyEdit(payload: CompanySubmitPayload): void {
    if (!this.editingCompanyId) return;
    this.isCompanySubmitting = true;
    const sectorId = this.sectorOptions.find(s => s.name === payload.formData.secteur)?.id;
    
    const update$ = this.commercialService.updateCompany(this.editingCompanyId, {
      name: payload.formData.nom,
      sectorId,
      location: payload.formData.localisation,
      contactName: payload.formData.contact,
      contactEmail: payload.formData.email,
      contactPhone: payload.formData.telephone,
      status: payload.formData.statut,
      note: payload.formData.note
    });

    update$.pipe(finalize(() => this.isCompanySubmitting = false))
      .subscribe({
        next: () => {
          // Gérer le logo séparément si nécessaire (si payload.logo ou deleteLogo)
          if (payload.logo) {
            this.commercialService.uploadCompanyLogo(this.editingCompanyId!, payload.logo).subscribe();
          } else if (payload.deleteLogo) {
            this.commercialService.deleteCompanyLogo(this.editingCompanyId!).subscribe();
          }

          const wasNotSigned = this.editingCompanyData?.statut !== 'Partenaire signé';
          const isNowSigned = payload.formData.statut === 'Partenaire signé';

          this.onCloseEditModal();
          this.loadData();
          this.loadStats();

          if (wasNotSigned && isNowSigned) {
            Swal.fire({
              title: 'Statut changé avec succès !',
              text: 'L\'entreprise est désormais partenaire. L\'utilisateur peut maintenant activer son compte via l\'email envoyé.',
              icon: 'success',
              confirmButtonColor: '#2B3674'
            });
          } else {
            Swal.fire('Succès', 'Entreprise modifiée avec succès', 'success');
          }
        },
        error: () => Swal.fire('Erreur', 'Impossible de modifier l\'entreprise', 'error')
      });
  }

  toggleProspectStatus(prospect: Prospect): void {
    const nextActive = prospect.statut !== 'Actif';
    this.commercialService.updateCompanyStatus(prospect.id, nextActive).subscribe({
      next: () => {
        prospect.statut = nextActive ? 'Actif' : 'Inactif';
        Swal.fire({
          toast: true,
          position: 'top-end',
          icon: 'success',
          title: `Entreprise ${nextActive ? 'activée' : 'désactivée'}`,
          showConfirmButton: false,
          timer: 2000
        });
      },
      error: () => Swal.fire('Erreur', 'Impossible de changer le statut', 'error')
    });
  }

  trackByProspect(index: number, item: Prospect): string {
    return item.id;
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadData();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadData();
    }
  }

  getCurrentPageProspects(): Prospect[] {
    return this.companyList;
  }
}