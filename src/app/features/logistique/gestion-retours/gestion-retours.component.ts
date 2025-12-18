import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import Swal from 'sweetalert2';

interface Metric {
  title: string;
  value: string;
  icon: string;

}

interface Retour {
  id: string;
  product: {
    name: string;
    reference: string;
    icon: string;
    category: string;
  };
  client: string;
  quantity: number;
  motif: string;
  date: string;
  status: 'En attente' | 'Validé' | 'En cours' | 'Rejeté';
  decision?: string;
}

@Component({
  selector: 'app-gestion-retours',
  standalone: true,
  imports: [CommonModule, FormsModule, MainLayoutComponent],
  templateUrl: './gestion-retours.component.html',
  styles: []
})
export class GestionRetoursComponent {
  searchText: string = '';
  selectedStatus: string = 'Tous les status';
  showStatusDropdown: boolean = false;
  currentPage: number = 1;
  totalPages: number = 6;

  // Modal Logic
  showDetailModal: boolean = false;
  selectedDetailRetour: Retour | null = null;
  showValidationModal: boolean = false;
  selectedRetour: Retour | null = null;
  validationOption: string = 'reintegrer';
  validationOptions = [
    { value: 'reintegrer', label: 'Réintégrer au stock' },
    { value: 'rembourser', label: 'Rembourser le stock' }
  ];
  showRefundModal: boolean = false;
  refundAmount: number | null = null;
  showRejectModal: boolean = false;
  rejectReason: string = '';
  selectedRetourToReject: Retour | null = null;

  metricsData: Metric[] = [
    { title: 'Total', value: '03', icon: 'box-blue', },
    { title: 'Validés', value: '01', icon: 'check-green', },
    { title: 'Rejetés', value: '01', icon: 'box-red', },
    { title: 'Réintégrés', value: '01', icon: 'box-indigo', },
    { title: 'Montant remboursé', value: '25 000 F', icon: 'money-green', }
  ];

  retours: Retour[] = [
    {
      id: '1',
      product: { name: 'Eau 1.5L (x6)', reference: 'CP-2025-03', icon: 'water', category: 'Épicerie' },
      client: 'Amadou Ndiaye',
      quantity: 6,
      motif: 'Bouteill fuyarde',
      date: '05/10/2025',
      status: 'En attente'
    },
    {
      id: '2',
      product: { name: 'Lait 1L', reference: 'CP-2025-02', icon: 'milk', category: 'Épicerie' },
      client: 'Elimane Ndiaye',
      quantity: 10,
      motif: 'Date courte',
      date: '05/10/2025',
      status: 'Validé'
    },
    {
      id: '3',
      product: { name: 'Eau 1.5L (x6)', reference: 'CP-2025-03', icon: 'water', category: 'Épicerie' },
      client: 'Moussa Faye',
      quantity: 4,
      motif: 'Bouteill fuyarde',
      date: '05/10/2025',
      status: 'En cours'
    },
    {
      id: '4',
      product: { name: 'Savon 250g', reference: 'CP-2025-01', icon: 'soap', category: 'Épicerie' },
      client: 'Khadija Diallo',
      quantity: 2,
      motif: 'Bouteill fuyarde',
      date: '05/10/2025',
      status: 'Rejeté',
      decision: 'Réintégration'
    }
  ];

  get uniqueStatuses(): string[] {
    const statuses = new Set(this.retours.map(r => r.status));
    return ['Tous les status', ...Array.from(statuses)];
  }

  get filteredRetours(): Retour[] {
    return this.retours.filter(retour => {
      const matchesSearch =
        retour.product.name.toLowerCase().includes(this.searchText.toLowerCase()) ||
        retour.product.reference.toLowerCase().includes(this.searchText.toLowerCase()) ||
        retour.client.toLowerCase().includes(this.searchText.toLowerCase());

      const matchesStatus = this.selectedStatus === 'Tous les status' || retour.status === this.selectedStatus;

      return matchesSearch && matchesStatus;
    });
  }

  toggleStatusDropdown() {
    this.showStatusDropdown = !this.showStatusDropdown;
  }

  selectStatus(status: string) {
    this.selectedStatus = status;
    this.showStatusDropdown = false;
  }

  openDetailModal(retour: Retour) {
    this.selectedDetailRetour = retour;
    this.showDetailModal = true;
  }

  closeDetailModal() {
    this.showDetailModal = false;
    this.selectedDetailRetour = null;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'En attente': return 'bg-gray-100 text-gray-600';
      case 'Validé': return 'bg-[#0A97480F] text-[#0A9748]';
      case 'En cours': return 'bg-[#EAB3080F] text-[#EAB308]';
      case 'Rejeté': return 'bg-[#FF09090F] text-[#FF0909]';
      default: return 'bg-gray-50 text-gray-600';
    }
  }

  getIconPath(iconName: string): string {
    const iconMap: { [key: string]: string } = {
      'water': '/icones/eau.svg',
      'milk': '/icones/lait.svg',
      'soap': '/icones/savon.svg',
      'box-blue': '/icones/commandefour.svg',
      'check-green': '/icones/green-box.svg',
      'box-red': '/icones/red-box.svg',
      'box-indigo': '/icones/purple-box.svg',
      'money-green': '/icones/money.svg'
    };
    return iconMap[iconName] || '/icones/stocks.svg';
  }

  getStatusDotClass(status: string): string {
    switch (status) {
      case 'En attente': return 'bg-gray-500';
      case 'Validé': return 'bg-[#0A9748]';
      case 'En cours': return 'bg-[#EAB308]';
      case 'Rejeté': return 'bg-[#FF0909]';
      default: return 'bg-gray-500';
    }
  }

  openValidationModal(retour: Retour) {
    this.selectedRetour = retour;
    this.validationOption = 'reintegrer';
    this.showValidationModal = true;
  }

  closeValidationModal() {
    this.showValidationModal = false;
    this.validationOption = 'reintegrer';
  }

  validateRetour() {
    if (this.selectedRetour && this.validationOption === 'reintegrer') {
      this.selectedRetour.status = 'Validé';
      this.closeValidationModal();
      this.showSuccessMessage();
      this.selectedRetour = null;
    } else if (this.selectedRetour && this.validationOption === 'rembourser') {
      this.closeValidationModal();
      this.openRefundModal();
    }
  }

  openRefundModal() {
    this.showRefundModal = true;
    this.refundAmount = null;
  }

  closeRefundModal() {
    this.showRefundModal = false;
    this.refundAmount = null;
    this.selectedRetour = null;
  }

  saveRefund() {
    if (this.selectedRetour) {
      this.selectedRetour.status = 'Validé';
      this.closeRefundModal();
      this.showRefundSuccessMessage();
    }
  }

  openRejectModal(retour: Retour) {
    this.selectedRetourToReject = retour;
    this.rejectReason = '';
    this.showRejectModal = true;
  }

  closeRejectModal() {
    this.showRejectModal = false;
    this.selectedRetourToReject = null;
    this.rejectReason = '';
  }

  confirmReject() {
    if (!this.rejectReason.trim()) {
      return;
    }

    if (this.selectedRetourToReject) {
      this.selectedRetourToReject.status = 'Rejeté';
      this.closeRejectModal();
      this.showRejectSuccessMessage();
    }
  }

  showRejectSuccessMessage() {
    Swal.fire({
      title: 'Demande rejetée',
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

  showSuccessMessage() {
    Swal.fire({
      title: 'Retour enregistré - Produit réintégré au stock',
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

  showRefundSuccessMessage() {
    Swal.fire({
      title: 'Retour enregistré - Remboursement validé',
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
