import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';

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
  imports: [MainLayoutComponent, CommonModule, FormsModule],
  styles: [`
    .statut-en-cours {
      background-color: #FEF3C7;
      color: #D97706;
    }
    
    .statut-livree {
      background-color: #D1FAE5;
      color: #059669;
    }
    
    .statut-en-attente {
      background-color: #E5E7EB;
      color: #6B7280;
    }
    
    .statut-annulee {
      background-color: #FEE2E2;
      color: #DC2626;
    }
  `],
  templateUrl: './fournisseur.component.html',
})
export class FournisseurComponent {
  metricsData: MetricCard[] = [
    {
      title: 'Total commandes',
      value: '12',
      icon: '/icones/promo.svg',
    },
    {
      title: 'En attente',
      value: '3',
      icon: '/icones/livraisonavenir.svg',
    },
    {
      title: 'Livrées',
      value: '7',
      icon: '/icones/stocks.png',
    },
    {
      title: 'Annulées',
      value: '2',
      icon: '/icones/retours.png',
    }
  ];

  searchText = '';
  selectedFournisseur = 'Tous les fournisseurs';
  selectedStatut = 'Tous les statuts';

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

  getStatutClass(statut: string): string {
    const classes: { [key: string]: string } = {
      'En cours': 'statut-en-cours',
      'Livrée': 'statut-livree',
      'En attente': 'statut-en-attente',
      'Annulée': 'statut-annulee'
    };
    return classes[statut] || '';
  }

  viewCommande(reference: string): void {
    console.log('Voir commande:', reference);
  }

  editCommande(reference: string): void {
    console.log('Modifier commande:', reference);
  }

  nouvelleCommande(): void {
    console.log('Nouvelle commande');
  }

  exportCommandes(): void {
    console.log('Exporter les commandes');
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