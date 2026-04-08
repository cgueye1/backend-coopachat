import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { Product } from '../../../shared/services/product.service';
import { environment } from '../../../../environments/environment';
import { finalize } from 'rxjs';
import { LogisticsService } from '../../../shared/services/logistics.service';
import { PAGE_SIZE_OPTIONS } from '../../../shared/constants/pagination';
import Swal from 'sweetalert2';

interface MetricCard {
  title: string;
  value: string;
  icon: string;
}

interface Commande {
  id: string;
  reference: string;
  fournisseur: string;
  date: string;
  produits: string;
  statut: 'En cours' | 'Livrée' | 'En attente' | 'Annulée';
  datePrevue?: string;
  note?: string;
  produitsDetails?: { productId: string; quantity: number }[];
}

@Component({
  selector: 'app-fournisseur',
  standalone: true,
  imports: [MainLayoutComponent, CommonModule, FormsModule, HeaderComponent],
  templateUrl: './fournisseur.component.html',
  styles: []
})
export class FournisseurComponent {
  constructor(private logisticsService: LogisticsService) {
    this.loadAllProducts();
    this.loadSuppliers();
    this.loadOrders();
    this.loadOrderStats();
  }

  metricsData: MetricCard[] = [];

  searchText = '';
  selectedFournisseur = 'Tous les fournisseurs';
  selectedSupplierId: number | null = null;
  selectedStatut = 'Tous les statuts';
  showFournisseurDropdown = false;
  showStatutDropdown = false;
  showModal = false;
  showDetailModal = false;
  isEditMode = false;
  editingOrderId: string | null = null;
  /** Statut de la commande à l'ouverture du modal (pour savoir si on peut appeler le PUT ou seulement le PATCH). */
  initialEditStatus: string | null = null;
  pendingSupplierName: string | null = null;
  selectedCommande: Commande | null = null;
  allProducts: Product[] = [];
  suppliers: { id: number; name: string }[] = [];
  selectedCommandeItems: { id: string; name: string; category: string; icon: string; quantity: number }[] = [];
  newCommande: any = {
    fournisseur: null,
    produit: '',
    quantite: '',
    eta: '',
    note: '',
    statut: 'En attente'
  };

  errors: any = {
    fournisseur: '',
    quantite: '',
    eta: ''
  };

  get uniqueFournisseurs(): { id: number | null; name: string }[] {
    return [{ id: null, name: 'Tous les fournisseurs' }, ...this.suppliers];
  }

  get uniqueStatuts(): string[] {
    return ['Tous les statuts', 'En attente', 'En cours', 'Livrée', 'Annulée'];
  }

  get minDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  get filteredCommandes(): Commande[] {
    // La liste est déjà filtrée par l'API
    return this.commandes;
  }

  toggleFournisseurDropdown() {
    this.showFournisseurDropdown = !this.showFournisseurDropdown;
    this.showStatutDropdown = false;
  }

  toggleStatutDropdown() {
    this.showStatutDropdown = !this.showStatutDropdown;
    this.showFournisseurDropdown = false;
  }

  selectFournisseur(fournisseur: { id: number | null; name: string }) {
    this.selectedFournisseur = fournisseur.name;
    this.selectedSupplierId = fournisseur.id;
    this.showFournisseurDropdown = false;
    this.currentPage = 1;
    this.loadOrders();
  }

  selectStatut(statut: string) {
    this.selectedStatut = statut;
    this.showStatutDropdown = false;
    this.currentPage = 1;
    this.loadOrders();
  }

  getMetricSubtitleClass(subtitle: string): string {
    if (subtitle.includes('↗')) {
      return 'text-green-600 font-medium';
    } else if (subtitle.includes('↘')) {
      return 'text-red-600 font-medium';
    }
    return 'text-gray-600';
  }

  commandes: Commande[] = [];
  loadingList = false;
  loadingExport = false;

  currentPage = 1;
  totalPages = 1;
  itemsPerPage = 6;
  pageSizeOptions = PAGE_SIZE_OPTIONS;

  getStatusClass(status: string): string {
    switch (status) {
      case 'En attente': return 'bg-[#F2F2F2] text-[#2C3E50]';
      case 'Livrée': return 'bg-[#0A97480F] text-[#0A9748]';
      case 'En cours': return 'bg-[#EAB3080F] text-[#EAB308]';
      case 'Annulée': return 'bg-[#FF09090F] text-[#FF0909]';
      default: return 'bg-gray-50 text-gray-600';
    }
  }

  getStatusDotClass(status: string): string {
    switch (status) {
      case 'En attente': return 'bg-[#2C3E50]';
      case 'Livrée': return 'bg-[#0A9748]';
      case 'En cours': return 'bg-[#EAB308]';
      case 'Annulée': return 'bg-[#FF0909]';
      default: return 'bg-gray-500';
    }
  }

  viewCommande(orderId: string): void {
    this.logisticsService.getSupplierOrderDetails(orderId).subscribe({
      next: (details) => {
        this.selectedCommande = this.mapOrderDetailsToCommande(details);
        this.selectedCommandeItems = (details?.items ?? []).map((item: any) => ({
          id: item?.productId?.toString() ?? '',
          name: item?.productName ?? '',
          category: item?.productCategory ?? '',
          icon: this.buildImageUrl(item?.productImage),
          quantity: item?.quantite ?? 0
        }));
        this.showDetailModal = true;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des détails:', error);
      }
    });
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedCommande = null;
  }

  getCommandeProducts(): { id: string; name: string; category: string; icon: string; quantity: number }[] {
    return this.selectedCommandeItems;
  }

  /** Ouvre le module de modification directement depuis la page de détail (charge les données et affiche le formulaire d'édition). */
  modifierCommande(): void {
    if (!this.selectedCommande?.id) return;
    const orderId = this.selectedCommande.id;
    this.closeDetailModal();
    this.editCommande(orderId);
  }

  annulerCommande(): void {
    console.log('Annuler commande:', this.selectedCommande);
    this.closeDetailModal();
  }

  editCommande(orderId: string): void {
    this.isEditMode = true;
    this.editingOrderId = orderId;
    this.initialEditStatus = null;
    this.errors = { fournisseur: '', quantite: '', eta: '' };
    this.logisticsService.getSupplierOrderDetails(orderId).subscribe({
      next: (details) => {
        const firstItem = (details?.items ?? [])[0];
        const statusLabel = this.normalizeOrderStatus(details?.status);
        this.initialEditStatus = statusLabel;
        let fournisseurId: number | null = null;
        if (details?.supplierId != null) {
          fournisseurId = Number(details.supplierId);
        } else {
          this.pendingSupplierName = details?.supplierName ?? null;
          this.setSupplierFromName();
          fournisseurId = this.newCommande.fournisseur;
        }
        this.newCommande = {
          fournisseur: fournisseurId,
          produit: firstItem?.productId != null ? String(firstItem.productId) : '',
          quantite: firstItem?.quantite != null ? String(firstItem.quantite) : '',
          eta: this.formatDateForInput(details?.expectedDate),
          note: details?.notes ?? '',
          statut: statusLabel
        };
        if (fournisseurId == null && details?.supplierName) {
          const match = this.suppliers.find(s => s.name === details.supplierName);
          if (match) this.newCommande.fournisseur = match.id;
        }
        this.showModal = true;
      },
      error: (error) => {
        console.error('Erreur lors du chargement pour modification:', error);
      }
    });
  }

  nouvelleCommande(): void {
    this.isEditMode = false;
    this.editingOrderId = null;
    this.errors = { fournisseur: '', quantite: '', eta: '' };
    this.newCommande = {
      fournisseur: null,
      produit: '',
      quantite: '',
      eta: '',
      note: '',
      statut: 'En attente'
    };
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.isEditMode = false;
    this.editingOrderId = null;
    this.initialEditStatus = null;
    this.newCommande = {
      fournisseur: null,
      produit: '',
      quantite: '',
      eta: '',
      note: '',
      statut: 'En attente'
    };
  }

  saveCommande(): void {
    // Reset errors
    this.errors = {
      fournisseur: '',
      quantite: '',
      eta: ''
    };

    let isValid = true;

    // Validation Fournisseur
    if (!this.newCommande.fournisseur) {
      this.errors.fournisseur = 'Le fournisseur est obligatoire';
      isValid = false;
    }

    // Validation Quantité (0 autorisé)
    const q = this.newCommande.quantite;
    if (q === '' || q === null || q === undefined) {
      this.errors.quantite = 'La quantité est obligatoire';
      isValid = false;
    } else if (isNaN(Number(q)) || Number(q) < 0) {
      this.errors.quantite = 'La quantité doit être un nombre positif';
      isValid = false;
    }

    // Validation Date (ETA) : en édition, la date prévue peut être dans le passé
    if (this.newCommande.eta && !this.isEditMode) {
      const selectedDate = new Date(this.newCommande.eta);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (selectedDate < today) {
        this.errors.eta = 'La date ne peut pas être dans le passé';
        isValid = false;
      }
    }

    if (!isValid) {
      return;
    }

    const expectedDate = this.formatDateForApi(this.newCommande.eta);
    const items = [
      {
        productId: Number(this.newCommande.produit),
        quantite: Number(this.newCommande.quantite)
      }
    ];

    if (this.isEditMode && this.editingOrderId) {
      const orderId = this.editingOrderId;
      const desiredStatus = this.newCommande.statut;
      const apiStatus = this.mapStatusToApi(desiredStatus);
      const isPendingOrder = this.initialEditStatus === 'En attente';
      const isDeliveredOrder = this.initialEditStatus === 'Livrée';

      // Règle UX : une commande livrée est finale → on bloque tout changement de statut depuis l'UI.
      if (isDeliveredOrder && desiredStatus !== 'Livrée') {
        Swal.fire({
          title: 'Modification impossible',
          text: 'Cette commande est déjà livrée. Son statut ne peut plus être modifié.',
          icon: 'warning',
          confirmButtonText: 'OK'
        });
        return;
      }

      // Si la commande n'est pas "En attente", on ne peut pas modifier les données (PUT) ; on peut seulement changer le statut (PATCH).
      if (!isPendingOrder) {
        if (desiredStatus === this.initialEditStatus) {
          Swal.fire({
            title: 'Modification limitée',
            text: 'Seules les commandes en attente permettent de modifier fournisseur, produit, quantité ou date. Vous pouvez uniquement changer le statut ici.',
            icon: 'info',
            confirmButtonText: 'OK'
          });
          return;
        }
        if (!apiStatus) {
          Swal.fire({
            title: 'Erreur',
            text: 'Statut invalide, impossible de mettre à jour la commande.',
            icon: 'error',
            confirmButtonText: 'OK'
          });
          return;
        }
        this.logisticsService.updateSupplierOrderStatus(orderId, apiStatus).subscribe({
          next: () => {
            this.closeModal();
            this.loadOrders();
            this.loadOrderStats();
            this.showSuccessPopup('Statut mis à jour avec succès');
          },
          error: (err) => {
            const msg = this.getApiErrorMessage(err);
            Swal.fire({ title: 'Erreur', text: msg, icon: 'error', confirmButtonText: 'OK' });
          }
        });
        return;
      }

      const updatePayload = {
        expectedDate: expectedDate || undefined,
        notes: this.newCommande.note || undefined,
        items
      };
      this.logisticsService.updateSupplierOrder(orderId, updatePayload).subscribe({
        next: () => {
          if (apiStatus) {
            this.logisticsService.updateSupplierOrderStatus(orderId, apiStatus).subscribe({
              next: () => {
                this.closeModal();
                this.loadOrders();
                this.loadOrderStats();
                this.showSuccessPopup('Commande modifiée avec succès');
              },
              error: (err) => {
                const msg = this.getApiErrorMessage(err);
                Swal.fire({ title: 'Erreur', text: msg, icon: 'error', confirmButtonText: 'OK' });
              }
            });
          } else {
            this.closeModal();
            this.loadOrders();
            this.loadOrderStats();
            this.showSuccessPopup('Commande modifiée avec succès');
          }
        },
        error: (err) => {
          const msg = this.getApiErrorMessage(err);
          Swal.fire({ title: 'Erreur', text: msg, icon: 'error', confirmButtonText: 'OK' });
        }
      });
    } else {
      const createPayload = {
        supplierId: Number(this.newCommande.fournisseur),
        items,
        expectedDate: expectedDate || undefined,
        notes: this.newCommande.note || undefined
      };
      this.logisticsService.createSupplierOrder(createPayload).subscribe({
        next: () => {
          this.closeModal();
          this.loadOrders();
          this.loadOrderStats();
          this.showSuccessPopup('Commande ajoutée avec succès');
        },
        error: (err) => {
          const msg = this.getApiErrorMessage(err);
          Swal.fire({ title: 'Erreur', text: msg, icon: 'error', confirmButtonText: 'OK' });
        }
      });
    }
  }

  /** Extrait le message d'erreur de la réponse API (body texte ou objet avec message). */
  private getApiErrorMessage(err: unknown): string {
    const e = err as { error?: string | { message?: string }; message?: string };
    if (typeof e?.error === 'string') return e.error;
    if (e?.error?.message) return e.error.message;
    if (e?.message) return e.message;
    return 'Une erreur est survenue. Réessayez ou contactez le support.';
  }

  onPageSizeChange(size: number): void {
    this.itemsPerPage = size;
    this.currentPage = 1;
    this.loadOrders();
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadOrders();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadOrders();
    }
  }

  onSearch(): void {
    this.currentPage = 1;
    this.loadOrders();
  }

  exportData(): void {
    if (this.loadingExport) return;
    this.loadingExport = true;
    const statusFilter = this.getStatusFilter();
    this.logisticsService
      .exportSupplierOrders(this.searchText, this.selectedSupplierId ?? undefined, statusFilter)
      .pipe(finalize(() => { this.loadingExport = false; }))
      .subscribe({
        next: async (blob) => {
          const t = blob.type || '';
          if (t.includes('json') || t.includes('text/plain') || t.includes('text/html')) {
            try {
              const text = await blob.text();
              const j = JSON.parse(text) as { message?: string; error?: string };
              const msg = j.message ?? j.error ?? text.slice(0, 400);
              Swal.fire({ title: 'Export impossible', text: msg || 'Réponse serveur invalide.', icon: 'error' });
            } catch {
              Swal.fire({
                title: 'Export impossible',
                text: 'Le serveur n’a pas renvoyé un fichier Excel valide.',
                icon: 'error'
              });
            }
            return;
          }
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `commandes_fournisseurs_${new Date().toISOString().slice(0, 10)}.xlsx`;
          a.click();
          window.URL.revokeObjectURL(url);
        },
        error: async (err: HttpErrorResponse) => {
          let msg = 'Une erreur est survenue lors du téléchargement.';
          if (err.error instanceof Blob) {
            try {
              const text = await err.error.text();
              try {
                const j = JSON.parse(text) as { message?: string; error?: string };
                if (typeof j.message === 'string') msg = j.message;
                else if (typeof j.error === 'string') msg = j.error;
                else if (text.trim()) msg = text.trim().slice(0, 500);
              } catch {
                if (text.trim()) msg = text.trim().slice(0, 500);
              }
            } catch {
              /* ignore */
            }
          }
          Swal.fire({ title: 'Export impossible', text: msg, icon: 'error' });
        }
      });
  }

  private loadOrders(): void {
    this.loadingList = true;
    const statusFilter = this.getStatusFilter();
    this.logisticsService
      .getSupplierOrders(
        this.currentPage - 1,
        this.itemsPerPage,
        this.searchText,
        this.selectedSupplierId ?? undefined,
        statusFilter
      )
      .subscribe({
        next: (response) => {
          const orders = response?.content ?? [];
          this.commandes = orders.map((order: any) => this.mapOrderListItemToCommande(order));
          this.totalPages = Math.max(1, response?.totalPages ?? 1);
          this.loadingList = false;
        },
        error: (error) => {
          console.error('Erreur lors du chargement des commandes:', error);
          this.loadingList = false;
        }
      });
  }

  private loadOrderStats(): void {
    this.logisticsService.getSupplierOrderStats().subscribe({
      next: (stats) => {
        this.metricsData = [
          { title: 'Total commandes', value: String(stats.total), icon: '/icones/commandes.svg' },
          { title: 'En attente', value: String(stats.pending), icon: '/icones/attente.svg' },
          { title: 'Livrées', value: String(stats.delivered), icon: '/icones/green-box.svg' },
          { title: 'Annulées', value: String(stats.cancelled), icon: '/icones/red-box.svg' }
        ];
      },
      error: (error) => {
        console.error('Erreur lors du chargement des stats:', error);
      }
    });
  }

  private loadSuppliers(): void {
    this.logisticsService.getSuppliers().subscribe({
      next: (suppliers) => {
        this.suppliers = Array.isArray(suppliers) ? suppliers : [];
        this.setSupplierFromName();
      },
      error: (error) => {
        console.error('Erreur lors du chargement des fournisseurs:', error);
      }
    });
  }

  private mapOrderListItemToCommande(order: any): Commande {
    return {
      id: order?.id?.toString() ?? '',
      reference: order?.orderNumber ?? '',
      fournisseur: order?.supplierName ?? '',
      date: this.formatDate(order?.expectedDate),
      produits: order?.productsSummary ?? '',
      statut: this.normalizeOrderStatus(order?.status),
      datePrevue: this.formatDate(order?.expectedDate)
    };
  }

  private mapOrderDetailsToCommande(details: any): Commande {
    return {
      id: details?.id?.toString() ?? '',
      reference: details?.orderNumber ?? '',
      fournisseur: details?.supplierName ?? '',
      date: this.formatDate(details?.expectedDate),
      produits: '',
      statut: this.normalizeOrderStatus(details?.status),
      datePrevue: this.formatDate(details?.expectedDate),
      note: details?.notes ?? ''
    };
  }

  private setSupplierFromName(): void {
    if (!this.pendingSupplierName || this.suppliers.length === 0) return;
    const match = this.suppliers.find(s => s.name === this.pendingSupplierName);
    if (match) {
      this.newCommande.fournisseur = match.id;
      this.pendingSupplierName = null;
    }
  }

  private normalizeOrderStatus(status: string | undefined): Commande['statut'] {
    if (!status) return 'En attente';
    if (status.toLowerCase().includes('cours')) return 'En cours';
    if (status.toLowerCase().includes('livr')) return 'Livrée';
    if (status.toLowerCase().includes('annul')) return 'Annulée';
    return 'En attente';
  }

  private getStatusFilter(): string | undefined {
    if (this.selectedStatut === 'En attente') return 'EN_ATTENTE';
    if (this.selectedStatut === 'En cours') return 'EN_COURS';
    if (this.selectedStatut === 'Livrée') return 'LIVREE';
    if (this.selectedStatut === 'Annulée') return 'ANNULEE';
    return undefined;
  }

  private mapStatusToApi(status: string): string | undefined {
    if (status === 'En attente') return 'EN_ATTENTE';
    if (status === 'En cours') return 'EN_COURS';
    if (status === 'Livrée') return 'LIVREE';
    if (status === 'Annulée') return 'ANNULEE';
    return undefined;
  }

  private showSuccessPopup(message: string): void {
    Swal.fire({
      iconHtml: '<img src="/icones/message success.svg" style="width: 95px; height: 95px; margin: 0 auto;" />',
      title: message,
      showConfirmButton: false,
      timer: 1500,
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-3xl p-6',
        title: 'text-xl font-medium text-gray-900',
        icon: 'border-none'
      },
      backdrop: `rgba(0,0,0,0.2)`,
      width: '580px'
    });
  }


  private formatDateForApi(dateValue: string | undefined): string | undefined {
    if (!dateValue) return undefined;
    const [year, month, day] = dateValue.split('-');
    if (!day || !month || !year) return undefined;
    return `${day}-${month}-${year} 00:00:00`;
  }

  private formatDateForInput(dateValue: string | undefined): string {
    if (!dateValue) return '';
    const datePart = (dateValue.split('T')[0] || dateValue).split(' ')[0];
    const parts = datePart.split('-');
    if (parts.length === 3) {
      const [a, b, c] = parts;
      if (a.length === 4) return `${a}-${b}-${c}`;
      return `${c}-${b}-${a}`;
    }
    return '';
  }

  private loadAllProducts(): void {
    // Utilise l'API logistique pour récupérer les produits disponibles
    this.logisticsService.getStockList(0, 1000).subscribe({
      next: (response) => {
        const products = response?.content ?? [];
        this.allProducts = products.map((item: any) => this.mapApiProductToFrontend(item));
      },
      error: (error) => {
        console.error('Erreur lors du chargement des produits:', error);
      }
    });
  }

  private mapApiProductToFrontend(item: any): Product {
    return {
      id: item.id?.toString() ?? '',
      name: item.name ?? '',
      reference: item.productCode ?? '',
      category: item.categoryName ?? '',
      price: this.formatPrice(item.price),
      stock: item.currentStock ?? 0,
      updatedAt: this.formatDate(item.updatedAt),
      status: this.normalizeStatus(item.status),
      icon: this.buildImageUrl(item.image),
      description: item.description
    };
  }

  private normalizeStatus(status: string | boolean | undefined): 'Actif' | 'Inactif' {
    if (status === true || status === 'ACTIF' || status === 'ACTIVE' || status === 'Actif') return 'Actif';
    if (status === false || status === 'INACTIF' || status === 'INACTIVE' || status === 'Inactif') return 'Inactif';
    return 'Inactif';
  }

  private formatPrice(price: any): string {
    if (price === null || price === undefined || price === '') return '';
    const value = typeof price === 'number' ? price : Number(price);
    if (Number.isNaN(value)) return `${price}`;
    return `${value.toLocaleString('fr-FR')} F`;
  }

  private formatDate(updatedAt: string | undefined): string {
    if (!updatedAt) return '';
    const datePart = updatedAt.split(' ')[0];
    return datePart ? datePart.replace(/-/g, '/') : updatedAt;
  }

  private buildImageUrl(image: string | undefined): string {
    if (!image) return '/icones/default-product.svg';
    if (image.startsWith('file://')) return '/icones/default-product.svg';
    if (image.startsWith('http') || image.startsWith('/')) return image;
    const base = environment.imageServerUrl;
    return `${base}/files/${image}`;
  }
}