import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import {
  CommercialService,
  PromotionListResponse,
  PromotionListItem,
  PromotionDetails,
  PromotionStats
} from '../../../shared/services/commercial.service';
import { PromotionModalComponent } from '../../../shared/components/promotion-modal/promotion-modal.component';
import { forkJoin } from 'rxjs';
import Swal from 'sweetalert2';
import { environment } from '../../../../environments/environment';

interface StatCard {
  title: string;
  value: string | number;
  icon: string;
  subtitle?: string;
}

@Component({
  selector: 'app-promotions-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, MainLayoutComponent, HeaderComponent, PromotionModalComponent],
  templateUrl: './promotions-list.component.html',
})
export class PromotionsListComponent {
  searchTerm = '';
  selectedStatusFilter = '';
  currentPage = 1;
  itemsPerPage = 6;

  listResponse: PromotionListResponse | null = null;
  loading = false;
  loadingDetail = false;
  showDetailModal = false;
  selectedPromotionDetail: PromotionDetails | null = null;
  showCreateModal = false;

  statsCards: StatCard[] = [
    { title: 'Total promotions', value: 0, icon: '/icones/label.svg' },
    { title: 'Planifiées', value: 0, icon: '/icones/label.svg', subtitle: 'Créées, pas encore en cours (date début à venir)' },
    { title: 'Produits concernés', value: 0, icon: '/icones/cart.svg', subtitle: 'Tous produits dans les promotions' },
  ];

  readonly availableStatuses = ['Actif', 'Inactif', 'Expiré', 'Planifié'];

  constructor(private commercialService: CommercialService) {
    this.loadPromotions();
  }

  openCreateModal(): void {
    this.showCreateModal = true;
  }

  onPromotionCreated(): void {
    this.showCreateModal = false;
    this.loadPromotions();
  }

  get content(): PromotionListItem[] {
    return this.listResponse?.content ?? [];
  }

  get totalPages(): number {
    return Math.max(1, this.listResponse?.totalPages ?? 0);
  }

  get totalElements(): number {
    return this.listResponse?.totalElements ?? 0;
  }

  loadPromotions(): void {
    this.loading = true;
    const page = this.currentPage - 1;
    const status = this.selectedStatusFilter === 'Actif' ? 'ACTIVE' :
      this.selectedStatusFilter === 'Inactif' ? 'DISABLED' :
        this.selectedStatusFilter === 'Expiré' ? 'EXPIRED' :
          this.selectedStatusFilter === 'Planifié' ? 'PLANNED' : undefined;

    forkJoin({
      list: this.commercialService.getPromotions(page, this.itemsPerPage, this.searchTerm || undefined, status),
      stats: this.commercialService.getPromotionStats(),
    }).subscribe({
      next: ({ list, stats }) => {
        this.listResponse = list;
        this.statsCards[0].value = stats.totalPromotions;
        this.statsCards[1].value = stats.promotionsPlanifiees;
        this.statsCards[2].value = stats.totalProduitsConcernes;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  formatStatut(status: string): string {
    const map: Record<string, string> = {
      PLANNED: 'Planifié',
      ACTIVE: 'Actif',
      EXPIRED: 'Expiré',
      DISABLED: 'Inactif',
    };
    return map[status] || status;
  }

  fmtDate(d: string): string {
    if (!d) return '';
    const sep = d.includes('-') ? '-' : d.includes('/') ? '/' : null;
    if (!sep) return d;
    const parts = d.split(sep);
    if (parts.length < 3) return d;
    // Formats possibles:
    // - yyyy-mm-dd ou yyyy/mm/dd -> on renvoie dd/MM/yyyy
    // - dd/mm/yyyy -> on laisse tel quel
    if (parts[0].length === 4) {
      return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    if (parts[2].length === 4) {
      return `${parts[0]}/${parts[1]}/${parts[2]}`;
    }
    return d;
  }

  formatValidite(item: PromotionListItem): string {
    const from = item.startDate ? this.fmtDate(item.startDate) : '';
    const to = item.endDate ? this.fmtDate(item.endDate) : '';
    return from && to ? `${from} → ${to}` : from || to || '-';
  }

  /**
   * Convertit une date string (ISO ou dd/MM/yyyy ou dd-MM-yyyy) en Date.
   * Comparaison en heure locale (UI).
   */
  private toDate(value: string | null | undefined): Date | null {
    if (!value) return null;
    const isoLike = value.includes('T') || value.endsWith('Z');
    if (isoLike) {
      const d = new Date(value);
      return Number.isNaN(d.getTime()) ? null : d;
    }
    if (/^\d{4}[-/]\d{2}[-/]\d{2}$/.test(value)) {
      const [y, m, day] = value.split(value.includes('-') ? '-' : '/').map(Number);
      const d = new Date(y, m - 1, day, 0, 0, 0, 0);
      return Number.isNaN(d.getTime()) ? null : d;
    }
    if (/^\d{2}[-/]\d{2}[-/]\d{4}$/.test(value)) {
      const [day, m, y] = value.split(value.includes('-') ? '-' : '/').map(Number);
      const d = new Date(y, m - 1, day, 0, 0, 0, 0);
      return Number.isNaN(d.getTime()) ? null : d;
    }
    const fallback = new Date(value);
    return Number.isNaN(fallback.getTime()) ? null : fallback;
  }

  /**
   * Option A (UX) : si la date de début n'est pas atteinte, on bloque l'action "Activer".
   * On autorise l'activation uniquement si now >= startDate.
   */
  canActivatePromotion(item: Pick<PromotionListItem, 'startDate' | 'status'>): boolean {
    if (!(item.status === 'PLANNED' || item.status === 'DISABLED')) return false;
    const start = this.toDate(item.startDate);
    if (!start) return true;
    return new Date().getTime() >= start.getTime();
  }

  //si la condition de canActivatePromotion est false, on affiche le message d'activation impossible
  activationBlockedReasonPromotion(item: Pick<PromotionListItem, 'startDate'>): string {
    return item.startDate ? `Disponible à partir du ${this.fmtDate(item.startDate)}` : `Disponible plus tard`;
  }

  getSelectedStatusLabel(): string {
    return this.selectedStatusFilter || 'Tous les statuts';
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'bg-[#0A97480F] text-[#0A9748]';
      case 'EXPIRED': return 'bg-[#F2F2F2] text-[#2C3E50]';
      case 'PLANNED': return 'bg-[#1E40AF0F] text-[#1E40AF]';
      case 'DISABLED': return 'bg-[#FEE2E2] text-[#DC2626]';
      default: return 'bg-gray-50 text-gray-600';
    }
  }

  getStatusDotClass(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'bg-[#0A9748]';
      case 'EXPIRED': return 'bg-[#2C3E50]';
      case 'PLANNED': return 'bg-[#1E40AF]';
      case 'DISABLED': return 'bg-[#DC2626]';
      default: return 'bg-gray-400';
    }
  }

  showStatusDropdown = false;
  toggleStatusDropdown(): void {
    this.showStatusDropdown = !this.showStatusDropdown;
  }

  onSearchInput(): void {
    this.currentPage = 1;
    this.loadPromotions();
  }

  selectStatus(status: string): void {
    this.selectedStatusFilter = status;
    this.showStatusDropdown = false;
    this.currentPage = 1;
    this.loadPromotions();
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadPromotions();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadPromotions();
    }
  }

  viewDetails(id: number): void {
    this.loadingDetail = true;
    this.showDetailModal = true;
    this.selectedPromotionDetail = null;
    this.commercialService.getPromotionById(id).subscribe({
      next: (detail) => {
        this.selectedPromotionDetail = detail;
        this.loadingDetail = false;
      },
      error: () => {
        this.loadingDetail = false;
        this.showDetailModal = false;
      },
    });
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedPromotionDetail = null;
  }

  /** Activer/Désactiver depuis la liste (avec confirmation, comme pour les coupons). */
  togglePromotionStatus(promoId: number, isActive: boolean): void {
    const item = this.content.find(p => p.id === promoId);
    if (!item) return;
    if (isActive && !this.canActivatePromotion(item)) {
      Swal.fire({
        title: 'Activation impossible',
        text: this.activationBlockedReasonPromotion(item),
        icon: 'info',
        confirmButtonText: 'OK'
      });
      return;
    }
    Swal.fire({
      title: isActive ? 'Activer cette promotion' : 'Désactiver cette promotion',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: isActive ? 'Activer' : 'Désactiver',
      cancelButtonText: 'Annuler'
    }).then(result => {
      if (!result.isConfirmed) return;
      this.commercialService.updatePromotionStatus(promoId, isActive).subscribe({
        next: () => {
          this.loadPromotions();
          Swal.fire({
            title: isActive ? 'Promotion activée' : 'Promotion désactivée',
            icon: 'success',
            timer: 1500,
            showConfirmButton: false
          });
        },
        error: (err) => {
          Swal.fire({
            title: 'Erreur',
            text: err?.error || 'Action impossible',
            icon: 'error'
          });
        },
      });
    });
  }

  /** Activer/Désactiver depuis le modal détails. */
  togglePromotionStatusInDetail(isActive: boolean): void {
    const d = this.selectedPromotionDetail;
    if (!d?.id) return;
    if (isActive && !this.canActivatePromotion(d)) {
      Swal.fire({
        title: 'Activation impossible',
        text: this.activationBlockedReasonPromotion(d),
        icon: 'info',
        confirmButtonText: 'OK'
      });
      return;
    }
    Swal.fire({
      title: isActive ? 'Activer cette promotion' : 'Désactiver cette promotion',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: isActive ? 'Activer' : 'Désactiver',
      cancelButtonText: 'Annuler'
    }).then(result => {
      if (!result.isConfirmed) return;
      this.commercialService.updatePromotionStatus(d.id, isActive).subscribe({
        next: () => {
          this.loadPromotions();
          this.commercialService.getPromotionById(d.id).subscribe({
            next: (updated) => { this.selectedPromotionDetail = updated; },
          });
          Swal.fire({
            title: isActive ? 'Promotion activée' : 'Promotion désactivée',
            icon: 'success',
            timer: 1500,
            showConfirmButton: false
          });
        },
        error: (err) => {
          Swal.fire({
            title: 'Erreur',
            text: err?.error || 'Action impossible',
            icon: 'error'
          });
        },
      });
    });
  }

  getDetailPeriod(d: PromotionDetails): string {
    return `${this.fmtDate(d.startDate)} → ${this.fmtDate(d.endDate)}`;
  }

  getDetailStatusDotClass(d: PromotionDetails): string {
    return this.getStatusDotClass(d.status);
  }

  getDetailStatutLabel(d: PromotionDetails): string {
    return this.formatStatut(d.status);
  }

  /** Construit l'URL image produit depuis Minio (comme pour les autres écrans). */
  productImageUrl(img?: string | null): string {
    if (!img) return '/icones/default-product.svg';
    if (img.startsWith('http')) return img;
    const base = environment.imageServerUrl?.replace(/\/$/, '') ?? '';
    return base ? `${base}/files/${img}` : `/files/${img}`;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(): void {
    if (this.showStatusDropdown) this.showStatusDropdown = false;
  }
}
