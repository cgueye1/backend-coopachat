import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import {
  LogisticsService,
  ClaimStats,
  ClaimListItem,
  ClaimDetail,
  ClaimListResponse,
} from '../../../shared/services/logistics.service';
import { environment } from '../../../../environments/environment';
import { finalize } from 'rxjs';
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
  loadingExport = false;

  showDetailModal = false;
  selectedDetail: ClaimDetail | null = null;
  showValidationModal = false;
  selectedClaim: ClaimListItem | null = null;
  validationOption: 'reintegrer' | 'rembourser' = 'reintegrer';
  validationOptions = [
    { value: 'reintegrer', label: 'Réintégrer au stock et rembourser' },
    { value: 'rembourser', label: 'Remboursement uniquement' },
  ];
  /** Détail chargé pour afficher le montant ligne (sous-total) et appeler l’API. */
  validationClaimDetail: ClaimDetail | null = null;
  loadingValidationDetail = false;
  isValidationSubmitting = false;
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
    this.validationClaimDetail = null;
    this.showValidationModal = true;
    this.loadingValidationDetail = true;
    this.logisticsService.getClaimById(item.claimId).subscribe({
      next: (d) => {
        this.validationClaimDetail = d;
        this.loadingValidationDetail = false;
      },
      error: () => {
        this.loadingValidationDetail = false;
        this.showError('Impossible de charger le détail du retour.');
        this.closeValidationModal();
      },
    });
  }

  /** Ouvre le modal de validation depuis le détail (on n'a que selectedDetail). */
  openValidationFromDetail(): void {
    if (!this.selectedDetail) return;
    this.validationClaimDetail = this.selectedDetail;
    this.selectedClaim = { claimId: this.selectedDetail.claimId } as ClaimListItem;
    this.validationOption = 'reintegrer';
    this.loadingValidationDetail = false;
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
    this.validationClaimDetail = null;
    this.loadingValidationDetail = false;
  }

  validateRetour(): void {
    if (!this.selectedClaim || this.loadingValidationDetail || this.isValidationSubmitting) {
      return;
    }
    const detail = this.validationClaimDetail;
    if (!detail || detail.subtotalProduct == null || Number(detail.subtotalProduct) <= 0) {
      this.showError('Montant de la ligne indisponible. Réessayez ou rouvrez le détail du retour.');
      return;
    }
    const amountLabel = `${Number(detail.subtotalProduct).toLocaleString('fr-FR')} Fcfa`;

    if (this.validationOption === 'reintegrer') {
      this.showRlFinalConfirmation(
        'Confirmation responsable logistique',
        `<p class="text-left text-gray-700 mb-2"><strong>Confirmez-vous</strong> cette validation du retour ?</p>
         <ul class="text-left text-gray-700 list-disc pl-5 space-y-1">
           <li>Réintégration du produit au <strong>stock</strong></li>
           <li>Remboursement enregistré : <strong>${amountLabel}</strong> (montant de la ligne)</li>
           <li>Le <strong>salarié</strong> recevra un <strong>e-mail</strong> de notification</li>
         </ul>`,
        'Oui, valider',
        'REINTEGRATION'
      );
      return;
    }

    this.showRlFinalConfirmation(
      'Confirmation responsable logistique',
      `<p class="text-left text-gray-700 mb-2"><strong>Confirmez-vous</strong> le remboursement uniquement (sans réintégration au stock) ?</p>
       <ul class="text-left text-gray-700 list-disc pl-5 space-y-1">
         <li>Montant : <strong>${amountLabel}</strong> (calculé automatiquement pour la quantité concernée)</li>
         <li>Aucune réintégration au stock</li>
         <li>Le <strong>salarié</strong> recevra un <strong>e-mail</strong> de notification</li>
       </ul>`,
      'Oui, rembourser',
      'REMBOURSEMENT'
    );
  }

  /**
   * Alerte finale réservée au RL : les deux flux (réintégration ou remboursement seul) passent par cette confirmation.
   */
  private showRlFinalConfirmation(
    title: string,
    html: string,
    confirmButtonText: string,
    decisionType: 'REINTEGRATION' | 'REMBOURSEMENT'
  ): void {
    Swal.fire({
      title,
      html,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText,
      cancelButtonText: 'Annuler',
      focusCancel: true,
      customClass: { popup: 'rounded-2xl p-2', confirmButton: 'font-medium', cancelButton: 'font-medium' },
    }).then((r) => {
      if (r.isConfirmed) {
        this.execValidate(decisionType);
      }
    });
  }

  private execValidate(decisionType: 'REINTEGRATION' | 'REMBOURSEMENT'): void {
    if (!this.selectedClaim) return;
    this.isValidationSubmitting = true;
    this.logisticsService.validateClaim(this.selectedClaim.claimId, { decisionType }).subscribe({
      next: () => {
        this.isValidationSubmitting = false;
        this.closeValidationModal();
        if (decisionType === 'REMBOURSEMENT') {
          this.showRefundSuccessMessage();
        } else {
          this.showSuccessMessage();
        }
        this.loadStats();
        this.loadClaims();
      },
      error: (err) => {
        this.isValidationSubmitting = false;
        this.showError(
          typeof err?.error === 'string' ? err.error : err?.error?.message ?? 'Erreur lors de la validation'
        );
      },
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
    if (this.loadingExport) return;
    this.loadingExport = true;
    const statusParam = this.statusOptions.find(s => s.label === this.selectedStatus)?.value ?? '';
    this.logisticsService
      .exportClaims(this.searchText.trim() || undefined, statusParam || undefined)
      .pipe(finalize(() => { this.loadingExport = false; }))
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `retours_${new Date().toISOString().slice(0, 10)}.xlsx`;
          a.click();
          window.URL.revokeObjectURL(url);
        },
        error: () => {
          Swal.fire({
            title: 'Export impossible',
            text: 'Une erreur est survenue lors du téléchargement.',
            icon: 'error'
          });
        }
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
      title: 'Retour validé — stock et remboursement',
      html: '<p class="text-gray-600 text-base px-2">Le salarié a été notifié par e-mail.</p>',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showConfirmButton: false,
      timer: 2000,
      buttonsStyling: false,
      customClass: { popup: 'rounded-3xl p-6', title: 'text-xl font-medium text-gray-900', icon: 'border-none' },
      backdrop: 'rgba(0,0,0,0.2)',
      width: '580px'
    });
  }

  showRefundSuccessMessage(): void {
    Swal.fire({
      title: 'Remboursement enregistré',
      html: '<p class="text-gray-600 text-base px-2">Le salarié a été notifié par e-mail.</p>',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showConfirmButton: false,
      timer: 2000,
      buttonsStyling: false,
      customClass: { popup: 'rounded-3xl p-6', title: 'text-xl font-medium text-gray-900', icon: 'border-none' },
      backdrop: 'rgba(0,0,0,0.2)',
      width: '580px'
    });
  }
}
