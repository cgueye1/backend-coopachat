import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';

interface MetricCard {
  title: string;
  value: string;
  icon: string;
}

interface Commande {
  reference: string;
  fournisseur: string;
  date: string;
  produits: string;
  statut: 'En cours' | 'Livrée' | 'En attente' | 'Annulée';
}

@Component({
  selector: 'app-fournisseur',
  standalone: true,
  imports: [MainLayoutComponent, CommonModule, FormsModule, HeaderComponent],
  templateUrl: './fournisseur.component.html',
  styles: []
})
export class FournisseurComponent {
  metricsData: MetricCard[] = [
    {
      title: 'Total commandes',
      value: '12',
      icon: '/icones/utilisateurs.svg',
    },
    {
      title: 'En attente',
      value: '3',
      icon: '/icones/GreenUser.svg',
    },
    {
      title: 'Livrées',
      value: '7',
      icon: '/icones/GreenUser.svg',
    },
    {
      title: 'Annulées',
      value: '2',
      icon: '/icones/OrangeUser.svg',
    }
  ];

  searchText = '';
  selectedFournisseur = 'Tous les fournisseurs';
  selectedStatut = 'Tous les statuts';
  showFournisseurDropdown = false;
  showStatutDropdown = false;
  showModal = false;
  newCommande: any = {
    fournisseur: '',
    produit: '',
    quantite: '',
    eta: '',
    note: ''
  };

  errors: any = {
    fournisseur: '',
    quantite: '',
    eta: ''
  };

  get uniqueFournisseurs(): string[] {
    const fournisseurs = new Set(this.commandes.map(c => c.fournisseur));
    return ['Tous les fournisseurs', ...Array.from(fournisseurs)];
  }

  get uniqueStatuts(): string[] {
    const statuts = new Set(this.commandes.map(c => c.statut));
    return ['Tous les statuts', ...Array.from(statuts)];
  }

  get minDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  get filteredCommandes(): Commande[] {
    return this.commandes.filter(commande => {
      const matchesSearch =
        commande.reference.toLowerCase().includes(this.searchText.toLowerCase()) ||
        commande.produits.toLowerCase().includes(this.searchText.toLowerCase()) ||
        commande.fournisseur.toLowerCase().includes(this.searchText.toLowerCase());

      const matchesFournisseur = this.selectedFournisseur === 'Tous les fournisseurs' ||
        commande.fournisseur === this.selectedFournisseur;

      const matchesStatut = this.selectedStatut === 'Tous les statuts' ||
        commande.statut === this.selectedStatut;

      return matchesSearch && matchesFournisseur && matchesStatut;
    });
  }

  toggleFournisseurDropdown() {
    this.showFournisseurDropdown = !this.showFournisseurDropdown;
    this.showStatutDropdown = false;
  }

  toggleStatutDropdown() {
    this.showStatutDropdown = !this.showStatutDropdown;
    this.showFournisseurDropdown = false;
  }

  selectFournisseur(fournisseur: string) {
    this.selectedFournisseur = fournisseur;
    this.showFournisseurDropdown = false;
  }

  selectStatut(statut: string) {
    this.selectedStatut = statut;
    this.showStatutDropdown = false;
  }

  getMetricSubtitleClass(subtitle: string): string {
    if (subtitle.includes('↗')) {
      return 'text-green-600 font-medium';
    } else if (subtitle.includes('↘')) {
      return 'text-red-600 font-medium';
    }
    return 'text-gray-600';
  }

  commandes: Commande[] = [
    {
      reference: 'CMD-0012',
      fournisseur: 'Fourniture Express',
      date: '03/10/2025',
      produits: 'Riz (100), Huile (50)',
      statut: 'En cours'
    },
    {
      reference: 'CMD-0011',
      fournisseur: 'Stock Pro',
      date: '03/10/2025',
      produits: 'Sucre (80), Sel (40)',
      statut: 'Livrée'
    },
    {
      reference: 'CMD-0010',
      fournisseur: 'Stock Pro',
      date: '03/10/2025',
      produits: 'Sucre (80), Sel (40)',
      statut: 'En attente'
    },
    {
      reference: 'CMD-0009',
      fournisseur: 'Stock Pro',
      date: '03/10/2025',
      produits: 'Sucre (80), Sel (40)',
      statut: 'Annulée'
    }
  ];

  currentPage = 1;
  totalPages = 12;

  getStatusClass(status: string): string {
    switch (status) {
      case 'En attente': return 'bg-gray-100 text-gray-600';
      case 'Livrée': return 'bg-[#0A97480F] text-[#0A9748]';
      case 'En cours': return 'bg-[#EAB3080F] text-[#EAB308]';
      case 'Annulée': return 'bg-[#FF09090F] text-[#FF0909]';
      default: return 'bg-gray-50 text-gray-600';
    }
  }

  getStatusDotClass(status: string): string {
    switch (status) {
      case 'En attente': return 'bg-gray-500';
      case 'Livrée': return 'bg-[#0A9748]';
      case 'En cours': return 'bg-[#EAB308]';
      case 'Annulée': return 'bg-[#FF0909]';
      default: return 'bg-gray-500';
    }
  }

  viewCommande(reference: string): void {
    console.log('Voir commande:', reference);
  }

  editCommande(reference: string): void {
    console.log('Modifier commande:', reference);
  }

  nouvelleCommande(): void {
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
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
    if (!this.newCommande.fournisseur || this.newCommande.fournisseur.trim() === '') {
      this.errors.fournisseur = 'Le fournisseur est obligatoire';
      isValid = false;
    }

    // Validation Quantité
    if (!this.newCommande.quantite) {
      this.errors.quantite = 'La quantité est obligatoire';
      isValid = false;
    } else if (isNaN(Number(this.newCommande.quantite))) {
      this.errors.quantite = 'La quantité doit être un nombre';
      isValid = false;
    }

    // Validation Date (ETA)
    if (this.newCommande.eta) {
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

    const newCmd: Commande = {
      reference: `CMD-${Math.floor(Math.random() * 10000)}`, // Génération d'une référence aléatoire
      fournisseur: this.newCommande.fournisseur,
      date: new Date().toLocaleDateString('fr-FR'),
      produits: `${this.newCommande.produit} (${this.newCommande.quantite})`,
      statut: 'En attente'
    };

    this.commandes.unshift(newCmd);
    this.closeModal();

    // Reset form
    this.newCommande = {
      fournisseur: '',
      produit: '',
      quantite: '',
      eta: '',
      note: ''
    };
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }
}