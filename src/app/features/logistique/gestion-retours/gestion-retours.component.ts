import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import {
  LogisticsService,
  ClaimStats,
  ClaimListItem,
  ClaimDetail,
  ClaimListResponse
} from '../../../shared/services/logistics.service';
import { environment } from '../../../../environments/environment';
import Swal from 'sweetalert2';

interface Metric {
  title: string;
  value: string;
  icon: string;
}

@Component({
  selector: 'app-gestion-retours',
  standalone: true,
  imports: [CommonModule, FormsModule, MainLayoutComponent],
  templateUrl: './gestion-retours.component.html',
  styles: []
})
export class GestionRetoursComponent {
  searchText = '';
  selectedStatus = 'Tous les status';
  showStatusDropdown = false;
  currentPage = 1;
  itemsPerPage = 10;
  totalPages = 1;
  totalElements = 0;

  listResponse: ClaimListResponse | null = null;
  loading = false;
  loadingStats = false;
  loadingDetail = false;

  showDetailModal = false;
  selectedDetail: ClaimDetail | null = null;
  showValidationModal = false;
  selectedClaim: ClaimListItem | null = null;
  validationOption: 'reintegrer' | 'rembourser' = 'reintegrer';
  validationOptions = [
    { value: 'reintegrer', label: 'Réintégrer au stock' },
    { value: 'rembourser', label: 'Remboursement (montant)' }
  ];
  showRefundModal = false;
  refundAmount: number | null = null;
  showRejectModal = false;
  rejectReason = '';
  selectedClaimToReject: ClaimListItem | null = null;

  metricsData: Metric[] = [
    { title: 'Total', value: '0', icon: 'box-blue' },
    { title: 'Validés', value: '0', icon: 'check-green' },
    { title: 'Rejetés', value: '0', icon: 'box-red' },
    { title: 'Réintégrés', value: '0', icon: 'box-indigo' },
    { title: 'Montant remboursé', value: '0 F', icon: 'money-green' }
  ];

  statusOptions = [
    { label: 'Tous les status', value: '' },
    { label: 'En Attente', value: 'EN_ATTENTE' },
    { label: 'Validé', value: 'VALIDE' },
    { label: 'Rejeté', value: 'REJETE' }
  ];

  constructor(private logisticsService: LogisticsService) {
    this.loadStats();
    this.loadClaims();
  }

  get content(): ClaimListItem[] {
    return this.listResponse?.content ?? [];
  }

  loadStats(): void {
    this.loadingStats = true;
    this.logisticsService.getClaimStats().subscribe({
      next: (stats: ClaimStats) => {
        this.metricsData[0].value = String(stats.total);
        this.metricsData[1].value = String(stats.validatedCount);
        this.metricsData[2].value = String(stats.rejectedCount);
        this.metricsData[3].value = String(stats.reintegratedCount);
        const amount = stats.totalRefundAmount ?? 0;
        this.metricsData[4].value = amount > 0 ? `${Number(amount).toLocaleString('fr-FR')} F` : '0 F';
        this.loadingStats = false;
      },
      error: () => {
        this.loadingStats = false;
      }
    });
  }

  loadClaims(): void {
    this.loading = true;
    const page = this.currentPage - 1;
    const statusParam = this.statusOptions.find(s => s.label === this.selectedStatus)?.value ?? '';
    this.logisticsService.getClaims(page, this.itemsPerPage, this.searchText.trim() || undefined, statusParam || undefined).subscribe({
      next: (res) => {
        this.listResponse = res;
        this.totalPages = Math.max(1, res.totalPages);
        this.totalElements = res.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  onSearch(): void {
    this.currentPage = 1;
    this.loadClaims();
  }

  toggleStatusDropdown(): void {
    this.showStatusDropdown = !this.showStatusDropdown;
  }

  selectStatus(label: string): void {
    this.selectedStatus = label;
    this.showStatusDropdown = false;
    this.currentPage = 1;
    this.loadClaims();
  }

  goPrev(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadClaims();
    }
  }

  goNext(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadClaims();
    }
  }

  formatDate(d: string | null): string {
    if (!d) return '-';
    const date = new Date(d);
    return date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  openDetailModal(item: ClaimListItem): void {
    this.loadingDetail = true;
    this.showDetailModal = true;
    this.selectedDetail = null;
    this.logisticsService.getClaimById(item.claimId).subscribe({
      next: (detail) => {
        this.selectedDetail = detail;
        this.loadingDetail = false;
      },
      error: () => {
        this.loadingDetail = false;
      }
    });
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedDetail = null;
  }

  getStatusClass(status: string): string {
    if (!status) return 'bg-gray-100 text-gray-600';
    if (status.includes('Attente') || status === 'En attente') return 'bg-gray-100 text-gray-600';
    if (status.includes('Validé') || status === 'Validé') return 'bg-[#0A97480F] text-[#0A9748]';
    if (status.includes('Rejeté') || status === 'Rejeté') return 'bg-[#FF09090F] text-[#FF0909]';
    return 'bg-gray-50 text-gray-600';
  }

  getIconPath(iconName: string): string {
    const iconMap: { [key: string]: string } = {
      water: '/icones/eau.svg',
      milk: '/icones/lait.svg',
      soap: '/icones/savon.svg',
      'box-blue': '/icones/commandefour.svg',
      'check-green': '/icones/green-box.svg',
      'box-red': '/icones/red-box.svg',
      'box-indigo': '/icones/purple-box.svg',
      'money-green': '/icones/money.svg'
    };
    return iconMap[iconName] ?? '/icones/stocks.svg';
  }

  getStatusDotClass(status: string): string {
    if (!status) return 'bg-gray-500';
    if (status.includes('Attente') || status === 'En attente') return 'bg-gray-500';
    if (status.includes('Validé') || status === 'Validé') return 'bg-[#0A9748]';
    if (status.includes('Rejeté') || status === 'Rejeté') return 'bg-[#FF0909]';
    return 'bg-gray-500';
  }

  /** Même logique que la page Produits/Catalogue. */
  buildImageUrl(image: string | null | undefined): string {
    if (!image) return '/icones/default-product.svg';
    if (image.startsWith('file://')) return '/icones/default-product.svg';
    if (image.startsWith('http') || image.startsWith('/')) return image;
    const base = environment.imageServerUrl;
    return `${base}/files/${image}`;
  }

  isPending(status: string): boolean {
    return status?.includes('Attente') === true || status === 'En attente';
  }

  openValidationModal(item: ClaimListItem): void {
    this.selectedClaim = item;
    this.validationOption = 'reintegrer';
    this.showValidationModal = true;
  }

  /** Ouvre le modal de validation depuis le détail (on n'a que selectedDetail). */
  openValidationFromDetail(): void {
    if (!this.selectedDetail) return;
    this.selectedClaim = { claimId: this.selectedDetail.claimId } as ClaimListItem;
    this.validationOption = 'reintegrer';
    this.showValidationModal = true;
  }

  /** Ouvre le modal de rejet depuis le détail. */
  openRejectFromDetail(): void {
    if (!this.selectedDetail) return;
    this.selectedClaimToReject = { claimId: this.selectedDetail.claimId } as ClaimListItem;
    this.rejectReason = '';
    this.showRejectModal = true;
  }

  closeValidationModal(): void {
    this.showValidationModal = false;
    this.selectedClaim = null;
  }

  validateRetour(): void {
    if (!this.selectedClaim) return;
    if (this.validationOption === 'reintegrer') {
      this.logisticsService.validateClaim(this.selectedClaim.claimId, { decisionType: 'REINTEGRATION' }).subscribe({
        next: () => {
          this.closeValidationModal();
          this.showSuccessMessage();
          this.loadStats();
          this.loadClaims();
        },
        error: (err) => this.showError(err?.error ?? 'Erreur lors de la validation')
      });
    } else {
      this.closeValidationModal();
      this.showRefundModal = true;
      this.refundAmount = null;
    }
  }

  openRefundModal(): void {
    this.showRefundModal = true;
    this.refundAmount = null;
  }

  closeRefundModal(): void {
    this.showRefundModal = false;
    this.refundAmount = null;
    this.selectedClaim = null;
  }

  saveRefund(): void {
    if (!this.selectedClaim || this.refundAmount == null || this.refundAmount <= 0) {
      return;
    }
    this.logisticsService.validateClaim(this.selectedClaim.claimId, {
      decisionType: 'REMBOURSEMENT',
      refundAmount: this.refundAmount
    }).subscribe({
      next: () => {
        this.closeRefundModal();
        this.showRefundSuccessMessage();
        this.loadStats();
        this.loadClaims();
      },
      error: (err) => this.showError(err?.error ?? 'Erreur lors du remboursement')
    });
  }

  openRejectModal(item: ClaimListItem): void {
    this.selectedClaimToReject = item;
    this.rejectReason = '';
    this.showRejectModal = true;
  }

  closeRejectModal(): void {
    this.showRejectModal = false;
    this.selectedClaimToReject = null;
    this.rejectReason = '';
  }

  confirmReject(): void {
    if (!this.rejectReason.trim() || !this.selectedClaimToReject) return;
    this.logisticsService.rejectClaim(this.selectedClaimToReject.claimId, { rejectionReason: this.rejectReason.trim() }).subscribe({
      next: () => {
        this.closeRejectModal();
        this.showRejectSuccessMessage();
        this.loadStats();
        this.loadClaims();
      },
      error: (err) => this.showError(err?.error ?? 'Erreur lors du rejet')
    });
  }

  exportRetours(): void {
    // L'API n'expose pas encore d'export claims ; on peut ouvrir la même liste en CSV côté client ou afficher un message.
    Swal.fire({
      title: 'Export',
      text: 'L\'export des retours sera disponible prochainement.',
      icon: 'info'
    });
  }

  private showError(message: string): void {
    Swal.fire({ title: 'Erreur', text: message, icon: 'error' });
  }

  showRejectSuccessMessage(): void {
    Swal.fire({
      title: 'Demande rejetée',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showConfirmButton: false,
      timer: 1500,
      buttonsStyling: false,
      customClass: { popup: 'rounded-3xl p-6', title: 'text-xl font-medium text-gray-900', icon: 'border-none' },
      backdrop: 'rgba(0,0,0,0.2)',
      width: '580px'
    });
  }

  showSuccessMessage(): void {
    Swal.fire({
      title: 'Retour enregistré - Produit réintégré au stock',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showConfirmButton: false,
      timer: 1500,
      buttonsStyling: false,
      customClass: { popup: 'rounded-3xl p-6', title: 'text-xl font-medium text-gray-900', icon: 'border-none' },
      backdrop: 'rgba(0,0,0,0.2)',
      width: '580px'
    });
  }

  showRefundSuccessMessage(): void {
    Swal.fire({
      title: 'Remboursé avec succès',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showConfirmButton: false,
      timer: 1500,
      buttonsStyling: false,
      customClass: { popup: 'rounded-3xl p-6', title: 'text-xl font-medium text-gray-900', icon: 'border-none' },
      backdrop: 'rgba(0,0,0,0.2)',
      width: '580px'
    });
  }
}
