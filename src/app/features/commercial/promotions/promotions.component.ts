import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { CouponModalComponent, CouponFormData } from '../../../shared/components/coupon-modal/coupon-modal.component';
import { CommercialService, CouponListResponse, CouponListItem, CouponDetails } from '../../../shared/services/commercial.service';
import { forkJoin } from 'rxjs';
import Swal from 'sweetalert2';

interface StatCard {
  title: string;
  value: string | number;
  icon: string;
  /** Texte affiché sous le chiffre (ex. précision ou formule). */
  subtitle?: string;
}

@Component({
  selector: 'app-promotions',
  standalone: true,
  imports: [CommonModule, FormsModule, MainLayoutComponent, HeaderComponent, CouponModalComponent],
  templateUrl: 'promotions.component.html',
})
export class PromotionsManagementComponent {

  searchTerm: string = '';
  selectedStatusFilter: string = '';
  currentPage: number = 1;
  itemsPerPage: number = 6;
  Math = Math;

  listResponse: CouponListResponse | null = null;
  loading = false;
  loadingDetail = false;

  isModalOpen = false;
  isSubmitting = false;
  showStatusDropdown = false;
  showDetailModal = false;
  selectedCouponDetail: CouponDetails | null = null;

  statsCards: StatCard[] = [
    { title: 'Coupons actives', value: 0, icon: '/icones/label.svg' },
    { title: 'Utilisations totales', value: 0, icon: '/icones/cart.svg' },
    { title: 'Montant généré', value: '', icon: '/icones/money-filled.svg', subtitle: 'Montant total des réductions appliquées' },
    { title: 'Panier moyen', value: '', icon: '/icones/money-filled-orange.svg', subtitle: 'totalGenerated / totalUsages' }
  ];

  constructor(private commercialService: CommercialService) {
    this.loadCoupons();
  }

  get content(): CouponListItem[] {
    return this.listResponse?.content ?? [];
  }

  get totalPages(): number {
    return Math.max(1, this.listResponse?.totalPages ?? 0);
  }

  get totalElements(): number {
    return this.listResponse?.totalElements ?? 0;
  }

  loadCoupons(): void {
    this.loading = true;
    const page = this.currentPage - 1;
    const status = this.selectedStatusFilter === 'Actif' ? 'ACTIVE' : this.selectedStatusFilter === 'Inactif' ? 'DISABLED' : this.selectedStatusFilter === 'Expiré' ? 'EXPIRED' : this.selectedStatusFilter === 'Planifié' ? 'PLANNED' : undefined;
    forkJoin({
      list: this.commercialService.getCoupons(page, this.itemsPerPage, this.searchTerm || undefined, status, undefined),
      stats: this.commercialService.getCartTotalCouponStats()
    }).subscribe({
      next: ({ list, stats }) => {
        this.listResponse = list;
        this.statsCards[0].value = stats.activeCouponsCount;
        this.statsCards[1].value = stats.totalUsages;
        const totalGen = Number(stats.totalGenerated) || 0;
        this.statsCards[2].value = totalGen > 0 ? `${totalGen.toLocaleString('fr-FR')} F` : '-';
        this.statsCards[3].value = stats.totalUsages > 0 ? `${Math.round(totalGen / stats.totalUsages).toLocaleString('fr-FR')} F` : '-';
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  openCouponModal(): void {
    this.isModalOpen = true;
  }

  closeCouponModal(): void {
    this.isModalOpen = false;
  }

  onSubmitCoupon(couponData: CouponFormData): void {
    this.isSubmitting = true;
    const payload = {
      code: couponData.code.trim().toUpperCase(),
      name: couponData.name,
      value: couponData.value,
      status: 'PLANNED' as const,
      startDate: couponData.startDate,
      endDate: couponData.endDate
    };
    this.commercialService.createCoupon(payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.closeCouponModal();
        this.loadCoupons();
        this.showCreateSuccessMessage();
      },
      error: (err) => {
        this.isSubmitting = false;
        const msg = err?.error?.message || err?.message || 'Erreur lors de la création du coupon.';
        Swal.fire({ title: 'Erreur', text: msg, icon: 'error' });
      }
    });
  }

  formatReduction(item: CouponListItem): string {
    return `${item.value}%`;
  }

  formatStatut(status: string): string {
    const map: Record<string, string> = {
      PLANNED: 'Planifié',
      ACTIVE: 'Actif',
      EXPIRED: 'Expiré',
      DISABLED: 'Inactif'
    };
    return map[status] || status;
  }

  formatValidite(item: CouponListItem): string {
    const from = item.validFrom ? this.fmtDate(item.validFrom) : '';
    const to = item.validTo ? this.fmtDate(item.validTo) : '';
    return from && to ? `${from} → ${to}` : (from || to || '-');
  }

  fmtDate(d: string): string {
    if (!d) return '';
    const sep = d.includes('-') ? '-' : d.includes('/') ? '/' : null;
    if (!sep) return d;
    const parts = d.split(sep);
    if (parts.length < 3) return d;
    if (parts[0].length === 4) {
      // yyyy-mm-dd ou yyyy/mm/dd -> dd/MM/yyyy
      return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    if (parts[2].length === 4) {
      // déjà dd/mm/yyyy
      return `${parts[0]}/${parts[1]}/${parts[2]}`;
    }
    return d;
  }

  formatMontant(item: CouponListItem): string {
    const v = item.totalGenerated;
    if (v == null || Number(v) === 0) return '–';
    return `${Number(v).toLocaleString('fr-FR')} F`;
  }

  /**
   * Convertit une date string (ISO ou dd/MM/yyyy ou dd-MM-yyyy) en Date.
   * Important : on compare en heure locale du navigateur (UI).
   */
  private toDate(value: string | null | undefined): Date | null {
    if (!value) return null;
    // ISO (ex. 2026-03-02T00:00:00.000Z) -> Date native OK
    const isoLike = value.includes('T') || value.endsWith('Z');
    if (isoLike) {
      const d = new Date(value);
      return Number.isNaN(d.getTime()) ? null : d;
    }
    // Date simple (yyyy-mm-dd / yyyy/mm/dd)
    if (/^\d{4}[-/]\d{2}[-/]\d{2}$/.test(value)) {
      const [y, m, day] = value.split(value.includes('-') ? '-' : '/').map(Number);
      const d = new Date(y, m - 1, day, 0, 0, 0, 0);
      return Number.isNaN(d.getTime()) ? null : d;
    }
    // Date simple (dd/mm/yyyy ou dd-mm-yyyy)
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
   * On autorise l'activation uniquement si now >= validFrom.
   */
  canActivateCoupon(item: Pick<CouponListItem, 'validFrom' | 'status'>): boolean {
    if (!(item.status === 'PLANNED' || item.status === 'DISABLED')) return false;
    const start = this.toDate(item.validFrom);
    if (!start) return true; // si format inconnu, ne pas bloquer l'utilisateur (fallback)
    return new Date().getTime() >= start.getTime();
  }

  activationBlockedReason(item: Pick<CouponListItem, 'validFrom'>): string {
    return item.validFrom ? `Disponible à partir du ${this.fmtDate(item.validFrom)}` : `Disponible plus tard`;
  }

  toggleStatusDropdown(): void {
    this.showStatusDropdown = !this.showStatusDropdown;
  }

  onSearchInput(): void {
    this.currentPage = 1;
    this.loadCoupons();
  }

  selectStatus(status: string): void {
    this.selectedStatusFilter = status;
    this.showStatusDropdown = false;
    this.currentPage = 1;
    this.loadCoupons();
  }

  getSelectedStatusLabel(): string {
    return this.selectedStatusFilter || 'Tous les statuts';
  }

  readonly availableStatuses = ['Actif', 'Inactif', 'Expiré', 'Planifié'];

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadCoupons();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadCoupons();
    }
  }

  viewDetails(id: number): void {
    this.loadingDetail = true;
    this.showDetailModal = true;
    this.selectedCouponDetail = null;
    this.commercialService.getCouponById(id).subscribe({
      next: (detail) => {
        this.selectedCouponDetail = detail;
        this.loadingDetail = false;
      },
      error: () => {
        this.loadingDetail = false;
        this.showDetailModal = false;
      }
    });
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedCouponDetail = null;
  }

  modifierCoupon(): void {
    // TODO: édition coupon si backend exposé
    this.closeDetailModal();
  }

  toggleCouponStatus(action: 'activer' | 'desactiver'): void {
    const c = this.selectedCouponDetail;
    if (!c) return;
    const isActive = action === 'activer';
    Swal.fire({
      title: isActive ? 'Activer ce coupon' : 'Désactiver ce coupon',
      text: isActive ? 'Le coupon sera utilisable par les salariés.' : 'Le coupon ne sera plus utilisable.',
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: isActive ? 'Activer' : 'Désactiver',
      cancelButtonText: 'Annuler'
    }).then((result) => {
      if (!result.isConfirmed) return;
      this.commercialService.updateCouponStatus(c.id, isActive).subscribe({
        next: () => {
          this.loadCoupons();
          this.commercialService.getCouponById(c.id).subscribe(d => this.selectedCouponDetail = d);
          isActive ? this.showActivateSuccessMessage() : this.showDeactivateSuccessMessage();
        },
        error: (err) => Swal.fire({ title: 'Erreur', text: err?.error || 'Action impossible', icon: 'error' })
      });
    });
  }

  toggleCouponStatusFromList(couponId: number, action: 'activer' | 'desactiver'): void {
    const item = this.content.find(c => c.id === couponId);
    if (!item) return;
    const isActive = action === 'activer';
    Swal.fire({
      title: isActive ? 'Activer ce coupon' : 'Désactiver ce coupon',
      showCancelButton: true,
      confirmButtonText: isActive ? 'Activer' : 'Désactiver',
      cancelButtonText: 'Annuler'
    }).then((result) => {
      if (!result.isConfirmed) return;
      this.commercialService.updateCouponStatus(item.id, isActive).subscribe({
        next: () => {
          this.loadCoupons();
          isActive ? this.showActivateSuccessMessage() : this.showDeactivateSuccessMessage();
        },
        error: (err) => Swal.fire({ title: 'Erreur', text: err?.error || 'Action impossible', icon: 'error' })
      });
    });
  }

  deleteCoupon(): void {
    const c = this.selectedCouponDetail;
    if (!c) return;
    Swal.fire({
      title: 'Supprimer ce coupon ?',
      text: 'Cette action est irréversible.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Supprimer',
      cancelButtonText: 'Annuler',
      confirmButtonColor: '#dc2626'
    }).then((result) => {
      if (!result.isConfirmed) return;
      this.commercialService.deleteCoupon(c.id).subscribe({
        next: () => {
          this.closeDetailModal();
          this.loadCoupons();
          Swal.fire({ title: 'Coupon supprimé', icon: 'success', timer: 1500, showConfirmButton: false });
        },
        error: (err) => Swal.fire({ title: 'Erreur', text: err?.error || 'Suppression impossible', icon: 'error' })
      });
    });
  }

  getDetailReduction(d: CouponDetails): string {
    return `${d.value}%`;
  }

  getDetailStatutLabel(d: CouponDetails): string {
    return this.formatStatut(d.status);
  }

  getDetailPeriod(d: CouponDetails): string {
    return `${this.fmtDate(d.validFrom)} → ${this.fmtDate(d.validTo)}`;
  }

  getDetailStatusDotClass(d: CouponDetails): string {
    switch (d.status) {
      case 'ACTIVE': return 'bg-[#0A9748]';
      case 'EXPIRED': return 'bg-[#2C3E50]';
      case 'PLANNED': return 'bg-[#1E40AF]';
      case 'DISABLED': return 'bg-[#DC2626]';
      default: return 'bg-gray-400';
    }
  }

  String = String;

  showDeactivateSuccessMessage(): void {
    Swal.fire({ title: 'Coupon désactivé', icon: 'success', timer: 1500, showConfirmButton: false });
  }

  showActivateSuccessMessage(): void {
    Swal.fire({ title: 'Coupon activé', icon: 'success', timer: 1500, showConfirmButton: false });
  }

  showCreateSuccessMessage(): void {
    Swal.fire({ title: 'Coupon créé avec succès', icon: 'success', timer: 1500, showConfirmButton: false });
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

  @HostListener('document:click', ['$event'])
  onDocumentClick(): void {
    if (this.showStatusDropdown) this.showStatusDropdown = false;
  }
}