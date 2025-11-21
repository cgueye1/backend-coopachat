import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { CouponModalComponent, CouponFormData } from '../../../shared/components/coupon-modal/coupon-modal.component';

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
}

@Component({
  selector: 'app-promotions',
  standalone: true,
  imports: [CommonModule, FormsModule, MainLayoutComponent, CouponModalComponent],
  templateUrl:'promotions.component.html',
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
      montantGenere: '8 700 €'
    },
    {
      id: 2,
      nom: 'Été 2023',
      reduction: '5%',
      produits: 'Catégorie Électroménager',
      validite: '15/06/2023 - 31/07/2023',
      icon: "/icones/inactif.svg",
      statut: 'Expiré',
      utilisations: 215,
      montantGenere: '15 000 €'
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
      montantGenere: '3 200 €'
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
      montantGenere: '- €'
    }
  ];

  filteredPromotions: Promotion[] = [...this.promotions];

  constructor() {
    this.filterPromotions();
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
        montantGenere: '- €'
      };

      // Ajouter la nouvelle promotion
      this.promotions.push(newPromotion);
      this.filterPromotions();

      // Fermer le modal
      this.isSubmitting = false;
      this.closeCouponModal();

      // Optionnel : afficher un message de succès
      alert('Coupon créé avec succès !');
    }, 2000);
  }

  getActivePromotions(): number {
    return this.promotions.filter(promo => promo.statut === 'Actif').length;
  }

  getTotalUtilisations(): number {
    return this.promotions.reduce((total, promo) => total + promo.utilisations, 0);
  }

  getTotalMontant(): string {
    const total = this.promotions
      .filter(promo => promo.montantGenere !== '- €')
      .reduce((sum, promo) => {
        const amount = parseInt(promo.montantGenere.replace(/[^\d]/g, ''));
        return sum + amount;
      }, 0);
    return `${total.toLocaleString()} €`;
  }

  getPanierMoyen(): string {
    return '72 €';
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
    console.log('Afficher les détails de la promotion:', id);
    // Ici vous pouvez implémenter la navigation vers les détails
  }

  // Et ajouter cette méthode dans la classe du composant :
  togglePromotionStatus(promotionId: number, action: 'activer' | 'desactiver'): void {
    const promotion = this.promotions.find(p => p.id === promotionId);
    if (promotion) {
      if (action === 'activer') {
        promotion.statut = 'Actif';
        promotion.icon = '/icones/actif.svg';
        console.log(`Promotion "${promotion.nom}" activée`);
      } else if (action === 'desactiver') {
        promotion.statut = 'Planifié';
        promotion.icon = '/icones/attente.svg';
        console.log(`Promotion "${promotion.nom}" désactivée`);
      }

      // Rafraîchir les promotions filtrées
      this.filterPromotions();

      // Optionnel : afficher un message de confirmation
      const actionText = action === 'activer' ? 'activée' : 'désactivée';
      alert(`Promotion "${promotion.nom}" ${actionText} avec succès !`);
    }
  }
}