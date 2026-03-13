import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';

interface StatCard {
  title: string;
  value: string;
  icon?: string;
}

interface GeneralInfo {
  label: string;
  value: string;
  trend?: string;
  trendType?: 'positive' | 'negative' | 'neutral';
}

@Component({
  selector: 'app-sales-statistics',
  standalone: true,
  imports: [CommonModule, MainLayoutComponent, HeaderComponent],
  templateUrl: './statistiques.component.html',
  styles: [`
    :host {
      display: block;
    }
  `]
})

export class SalesStatisticsComponent implements OnInit {
  statsCards: StatCard[] = [
    {
      title: 'Salaires inscrits',
      value: '458',
      icon: '/icones/utilisateurs.svg'
    },
    {
      title: 'Commandes passées',
      value: '756',
      icon: '/icones/cart.svg'
    },
    {
      title: 'Montant total',
      value: '451 090 F',
      icon: '/icones/money-filled.svg'
    },
    {
      title: 'Moyenne par salarié',
      value: '1.65',
      icon: '/icones/activity.svg'
    }
  ];

  generalInfo: GeneralInfo[] = [
    {
      label: 'Dernière commande',
      value: '28/07/2023 - 1250 €'
    },
    {
      label: 'Évolution mensuelle',
      value: '+12%',
      trend: '+12%',
      trendType: 'positive'
    },
    {
      label: 'Taux d\'adoption',
      value: '78%'
    }
  ];

  ngOnInit() {}

  analyzePotential() {
    // Logique pour analyser le potentiel
    console.log('Analyse du potentiel en cours...');
  }
}

// Service pour les données (optionnel)
export interface SalesStatisticsService {
  getStatsCards(): StatCard[];
}

// Exemple de route (à ajouter dans votre routing)
export const SALES_STATISTICS_ROUTE = {
  path: 'statistiques',
  component: SalesStatisticsComponent,
  title: 'Statistiques des Ventes'
};
