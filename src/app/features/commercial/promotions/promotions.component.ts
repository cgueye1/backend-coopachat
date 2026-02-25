import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { CouponModalComponent, CouponFormData } from '../../../shared/components/coupon-modal/coupon-modal.component';
import { ProductService, Product } from '../../../shared/services/product.service';
import { environment } from '../../../../environments/environment';
import Swal from 'sweetalert2';

interface StatCard {
  title: string;
  value: string | number;
  icon: string;
  method?: string;
}

interface Promotion {
  id: number;
  nom: string;
  reduction: string;
  produits: string;
  validite: string;
  icon: string;
  statut: 'Actif' | 'Expiré' | 'Planifié';
  utilisations: number;
  montantGenere: string;
  produitsIds?: string[];
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
  itemsPerPage: number = 4;
  Math = Math;

  // Propriétés pour le modal
  isModalOpen: boolean = false;
  isSubmitting: boolean = false;

  // Propriétés pour le dropdown de statut
  showStatusDropdown: boolean = false;

  // Propriétés pour le modal de détails
  showDetailModal: boolean = false;
  selectedPromotion: Promotion | null = null;
  allProducts: Product[] = [];

  statsCards: StatCard[] = [
    {
      title: 'Promotions actives',
      value: 0,
      icon: '/icones/label.svg',
      method: 'getActivePromotions'
    },
    {
      title: 'Utilisations totales',
      value: 0,
      icon: '/icones/cart.svg',
      method: 'getTotalUtilisations'
    },
    {
      title: 'Montant généré',
      value: '',
      icon: '/icones/money-filled.svg',
      method: 'getTotalMontant'
    },
    {
      title: 'Panier moyen',
      value: '',
      icon: '/icones/money-filled-orange.svg',
      method: 'getPanierMoyen'
    }
  ];

  promotions: Promotion[] = [
    {
      id: 1,
      nom: 'Rentrée 2023',
      reduction: '5%',
      produits: 'Tous les produits',
      validite: '01/09/2023 - 15/09/2023',
      icon: "/icones/actif.svg",
      statut: 'Actif',
      utilisations: 124,
      montantGenere: '8 700 Fcfa',
      produitsIds: ['1', '2', '3', '4']
    },
    {
      id: 2,
      nom: 'Été 2023',
      reduction: '5%',
      produits: 'Électroménager',
      validite: '15/06/2023 - 31/07/2023',
      icon: "/icones/inactif.svg",
      statut: 'Expiré',
      utilisations: 215,
      montantGenere: '15 000 Fcfa',
      produitsIds: ['2', '3']
    },
    {
      id: 3,
      nom: 'Bienvenue Entreprise ABC',
      reduction: '5%',
      produits: 'Tous les produits',
      validite: '01/07/2023 - 31/07/2023',
      icon: "/icones/inactif.svg",
      statut: 'Expiré',
      utilisations: 45,
      montantGenere: '3 200 Fcfa',
      produitsIds: ['1', '4']
    },
    {
      id: 4,
      nom: 'Black Friday 2023',
      reduction: '5%',
      produits: 'Tous les produits',
      validite: '24/11/2023 - 27/11/2023',
      icon: "/icones/attente.svg",
      statut: 'Planifié',
      utilisations: 0,
      montantGenere: '- Fcfa',
      produitsIds: ['1', '2', '3', '4', '5']
    }
  ];

  filteredPromotions: Promotion[] = [...this.promotions];

  constructor(private productService: ProductService) {
    this.loadAllProducts();
    this.filterPromotions();
    this.updateStatsCards();
  }

  updateStatsCards(): void {
    this.statsCards[0].value = this.getActivePromotions();
    this.statsCards[1].value = this.getTotalUtilisations();
    this.statsCards[2].value = this.getTotalMontant();
    this.statsCards[3].value = this.getPanierMoyen();
  }

  // Méthodes pour le modal
  openCouponModal(): void {
    this.isModalOpen = true;
  }

  closeCouponModal(): void {
    this.isModalOpen = false;
  }

  onSubmitCoupon(couponData: CouponFormData): void {
    this.isSubmitting = true;

    // Simuler un appel API
    setTimeout(() => {
      console.log('Nouveau coupon créé:', couponData);

      // Créer une nouvelle promotion à partir des données du formulaire
      const newPromotion: Promotion = {
        id: this.promotions.length + 1,
        nom: couponData.nom,
        reduction: couponData.taux,
        produits: couponData.produits.length > 0 ? `${couponData.produits.length} produit(s) sélectionné(s)` : 'Tous les produits',
        validite: `${couponData.dateDebut} - ${couponData.dateFin}`,
        icon: '/icones/attente.svg',
        statut: 'Planifié',
        utilisations: 0,
        montantGenere: '- Fcfa'
      };

      // Ajouter la nouvelle promotion
      this.promotions.push(newPromotion);
      this.filterPromotions();

      // Fermer le modal
      this.isSubmitting = false;
      this.closeCouponModal();

      // Afficher un message de succès
      this.showCreateSuccessMessage();
    }, 2000);
  }

  getActivePromotions(): number {
    return this.promotions.filter(promo => promo.statut === 'Actif').length;
  }

  getTotalUtilisations(): number {
    return this.promotions.reduce((total, promo) => total + promo.utilisations, 0);
  }

  getTotalMontant(): string {
    return '451 090 F';
  }

  getPanierMoyen(): string {
    return '23 000 F';
  }

  toggleStatusDropdown(): void {
    this.showStatusDropdown = !this.showStatusDropdown;
  }

  selectStatus(status: string): void {
    this.selectedStatusFilter = status;
    this.showStatusDropdown = false;
    this.filterPromotions();
    this.currentPage = 1;
  }

  getSelectedStatusLabel(): string {
    return this.selectedStatusFilter || 'Tous les statuts';
  }

  get availableStatuses(): string[] {
    const statuses = new Set(this.promotions.map(p => p.statut));
    return Array.from(statuses);
  }

  filterPromotions(): void {
    this.filteredPromotions = this.promotions.filter(promotion => {
      const matchesSearch = promotion.nom.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        promotion.produits.toLowerCase().includes(this.searchTerm.toLowerCase());

      const matchesStatus = !this.selectedStatusFilter || promotion.statut === this.selectedStatusFilter;

      return matchesSearch && matchesStatus;
    });

    this.currentPage = 1;
  }

  getTotalPages(): number {
    return Math.ceil(this.filteredPromotions.length / this.itemsPerPage);
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.getTotalPages()) {
      this.currentPage++;
    }
  }

  viewDetails(id: number): void {
    const promotion = this.promotions.find(p => p.id === id);
    if (promotion) {
      this.selectedPromotion = promotion;
      this.showDetailModal = true;
    }
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedPromotion = null;
  }

  toggleProductSelection(productId: string): void {
    if (!this.selectedPromotion) return;

    if (!this.selectedPromotion.produitsIds) {
      this.selectedPromotion.produitsIds = [];
    }

    const index = this.selectedPromotion.produitsIds.indexOf(productId);
    if (index > -1) {
      this.selectedPromotion.produitsIds.splice(index, 1);
    } else {
      this.selectedPromotion.produitsIds.push(productId);
    }
  }

  isProductSelected(productId: string): boolean {
    return this.selectedPromotion?.produitsIds?.includes(productId) || false;
  }

  getPromotionProducts(): Product[] {
    if (!this.selectedPromotion?.produitsIds) return [];
    return this.allProducts.filter(p => this.selectedPromotion!.produitsIds!.includes(p.id));
  }

  modifierCoupon(): void {
    console.log('Modifier coupon:', this.selectedPromotion);
    this.closeDetailModal();
  }

  toggleCouponStatus(action: 'activer' | 'desactiver'): void {
    if (!this.selectedPromotion) return;

    if (action === 'activer') {
      this.activateCoupon(this.selectedPromotion);
    } else {
      this.deactivateCoupon(this.selectedPromotion);
    }
  }

  // Expose String constructor for template
  String = String;

  // Et ajouter cette méthode dans la classe du composant :
  togglePromotionStatus(promotionId: number, action: 'activer' | 'desactiver'): void {
    const promotion = this.promotions.find(p => p.id === promotionId);
    if (promotion) {
      if (action === 'activer') {
        this.activateCoupon(promotion);
      } else if (action === 'desactiver') {
        this.deactivateCoupon(promotion);
      }
    }
  }

  activateCoupon(promotion: Promotion): void {
    Swal.fire({
      title: 'Activer ce coupon',
      text: 'Lorem ipsum has been the industry\'s standard dummy text ever since the 1500',
      iconHtml: `<img src="/icones/alerte.svg" alt="alert" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showCancelButton: true,
      confirmButtonText: 'Activer',
      cancelButtonText: 'Annuler',
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-2xl p-3 sm:p-6 w-[85vw] max-w-[420px] sm:max-w-[580px]',
        title: 'text-lg sm:text-2xl font-medium text-gray-900',
        htmlContainer: 'text-sm sm:text-lg text-gray-600',
        confirmButton: 'bg-[#22C55E] hover:bg-[#16A34A] text-white px-4 sm:px-8 py-2.5 sm:py-3 rounded-lg font-medium text-sm sm:text-base shadow-none border-none',
        cancelButton: 'bg-[#F3F4F6] hover:bg-gray-200 text-gray-700 px-4 sm:px-8 py-2.5 sm:py-3 rounded-lg font-medium text-sm sm:text-base shadow-none border-none',
        actions: 'flex justify-center w-full gap-2',
        icon: 'border-none'
      },
      backdrop: `rgba(0,0,0,0.2)`,
      width: '85vw',
      showClass: {
        popup: 'animate__animated animate__fadeIn animate__faster'
      }
    }).then((result) => {
      if (result.isConfirmed) {
        promotion.statut = 'Actif';
        promotion.icon = '/icones/actif.svg';
        this.filterPromotions();
        this.updateStatsCards();
        this.showActivateSuccessMessage();
      }
    });
  }

  deactivateCoupon(promotion: Promotion): void {
    Swal.fire({
      title: 'Désactiver ce coupon',
      text: 'Lorem ipsum has been the industry\'s standard dummy text ever since the 1500',
      iconHtml: `<img src="/icones/alerte.svg" alt="alert" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showCancelButton: true,
      confirmButtonText: 'Désactiver',
      cancelButtonText: 'Annuler',
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-2xl p-3 sm:p-6 w-[85vw] max-w-[420px] sm:max-w-[580px]',
        title: 'text-lg sm:text-2xl font-medium text-gray-900',
        htmlContainer: 'text-sm sm:text-lg text-gray-600',
        confirmButton: 'bg-[#EF4444] hover:bg-[#DC2626] text-white px-4 sm:px-8 py-2.5 sm:py-3 rounded-lg font-medium text-sm sm:text-base shadow-none border-none',
        cancelButton: 'bg-[#F3F4F6] hover:bg-gray-200 text-gray-700 px-4 sm:px-8 py-2.5 sm:py-3 rounded-lg font-medium text-sm sm:text-base shadow-none border-none',
        actions: 'flex justify-center w-full gap-2',
        icon: 'border-none'
      },
      backdrop: `rgba(0,0,0,0.2)`,
      width: '85vw',
      showClass: {
        popup: 'animate__animated animate__fadeIn animate__faster'
      }
    }).then((result) => {
      if (result.isConfirmed) {
        promotion.statut = 'Planifié';
        promotion.icon = '/icones/attente.svg';
        this.filterPromotions();
        this.updateStatsCards();
        this.showDeactivateSuccessMessage();
      }
    });
  }

  showDeactivateSuccessMessage(): void {
    Swal.fire({
      title: 'Coupon désactivé',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showConfirmButton: false,
      timer: 1500,
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-2xl p-3 sm:p-6 w-[85vw] max-w-[420px] sm:max-w-[580px]',
        title: 'text-base sm:text-xl font-medium text-gray-900',
        icon: 'border-none'
      },
      backdrop: `rgba(0,0,0,0.2)`,
      width: '85vw',
      showClass: {
        popup: 'animate__animated animate__fadeIn animate__faster'
      },
      hideClass: {
        popup: 'animate__animated animate__fadeOut animate__faster'
      }
    });
  }

  showActivateSuccessMessage(): void {
    Swal.fire({
      title: 'Coupon activé',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showConfirmButton: false,
      timer: 1500,
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-2xl p-3 sm:p-6 w-[85vw] max-w-[420px] sm:max-w-[580px]',
        title: 'text-base sm:text-xl font-medium text-gray-900',
        icon: 'border-none'
      },
      backdrop: `rgba(0,0,0,0.2)`,
      width: '85vw',
      showClass: {
        popup: 'animate__animated animate__fadeIn animate__faster'
      },
      hideClass: {
        popup: 'animate__animated animate__fadeOut animate__faster'
      }
    });
  }

  showCreateSuccessMessage(): void {
    Swal.fire({
      title: 'Coupon créé avec succès',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showConfirmButton: false,
      timer: 1500,
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-2xl p-3 sm:p-6 w-[85vw] max-w-[420px] sm:max-w-[580px]',
        title: 'text-base sm:text-xl font-medium text-gray-900',
        icon: 'border-none'
      },
      backdrop: `rgba(0,0,0,0.2)`,
      width: '85vw',
      showClass: {
        popup: 'animate__animated animate__fadeIn animate__faster'
      },
      hideClass: {
        popup: 'animate__animated animate__fadeOut animate__faster'
      }
    });
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'Actif':
        return 'bg-[#0A97480F] text-[#0A9748]';
      case 'Expiré':
        return 'bg-[#F2F2F2] text-[#2C3E50]';
      case 'Planifié':
        return 'bg-[#1E40AF0F] text-[#1E40AF]';
      default:
        return 'bg-gray-50 text-gray-600';
    }
  }

  getStatusDotClass(status: string): string {
    switch (status) {
      case 'Actif':
        return 'bg-[#0A9748]';
      case 'Expiré':
        return 'bg-[#2C3E50]';
      case 'Planifié':
        return 'bg-[#1E40AF]';
      default:
        return 'bg-gray-400';
    }
  }

  getCurrentPagePromotions(): Promotion[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredPromotions.slice(startIndex, endIndex);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredPromotions.length / this.itemsPerPage));
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (this.showStatusDropdown) {
      this.showStatusDropdown = false;
    }
  }

  private loadAllProducts(): void {
    this.productService.getProducts(0, 1000).subscribe({
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
    if (image.startsWith('http') || image.startsWith('/')) return image;
    return `${environment.apiUrl}/files/${image}`;
  }
}