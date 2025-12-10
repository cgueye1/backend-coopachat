import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { Chart, ChartConfiguration, ChartType, registerables } from 'chart.js';
import { ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { HeaderComponent } from '../../../core/layouts/header/header.component';

// Enregistrement des composants Chart.js
Chart.register(...registerables);

interface StatCard {
  title: string;
  value: string;
  icon?: string;
}

interface SalesData {
  month: string;
  amount: number;
}

interface OrdersData {
  month: string;
  orders: number;
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

export class SalesStatisticsComponent implements OnInit, OnDestroy {
  @ViewChild('salesChart', { static: false }) salesChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('ordersChart', { static: false }) ordersChartRef!: ElementRef<HTMLCanvasElement>;

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

  salesData: SalesData[] = [
    { month: 'Jan', amount: 4500 },
    { month: 'Fév', amount: 3000 },
    { month: 'Mar', amount: 4800 },
    { month: 'Avr', amount: 2800 },
    { month: 'Mai', amount: 1500 },
    { month: 'Jun', amount: 2200 },
    { month: 'Jul', amount: 3200 }
  ];

  ordersData: OrdersData[] = [
    { month: 'Jan', orders: 24 },
    { month: 'Fév', orders: 18 },
    { month: 'Mar', orders: 32 },
    { month: 'Avr', orders: 15 },
    { month: 'Mai', orders: 12 },
    { month: 'Jun', orders: 14 },
    { month: 'Jul', orders: 22 }
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

  private salesChart!: Chart;
  private ordersChart!: Chart;

  ngOnInit() {
    // Les graphiques seront initialisés après la vue
    setTimeout(() => {
      this.initSalesChart();
      this.initOrdersChart();
    }, 100);
  }

  ngOnDestroy() {
    if (this.salesChart) {
      this.salesChart.destroy();
    }
    if (this.ordersChart) {
      this.ordersChart.destroy();
    }
  }

  analyzePotential() {
    // Logique pour analyser le potentiel
    console.log('Analyse du potentiel en cours...');
  }

  // Méthode pour créer l'icône SVG personnalisée
  private createSVGIcon(): HTMLCanvasElement {
    const canvas = document.createElement('canvas');
    canvas.width = 15;
    canvas.height = 15;
    const ctx = canvas.getContext('2d')!;

    ctx.strokeStyle = 'black';
    ctx.lineWidth = 1; // ton SVG a une largeur fine
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    ctx.beginPath();

    // Partie gauche : ligne horizontale
    ctx.moveTo(0.654785, 7.78902);
    ctx.lineTo(5.32145, 7.78902);

    // Courbe centrale (approximation par arcs)
    // Haut de l’ellipse
    ctx.moveTo(5.32145, 7.78902);
    ctx.bezierCurveTo(
      5.32145, 6.5, // point de contrôle gauche
      6.8, 5.45569, // point de contrôle haut
      7.65479, 5.45569 // sommet haut
    );

    // Symétrie vers la droite
    ctx.bezierCurveTo(
      8.5, 5.45569, // contrôle haut droit
      9.98812, 6.5, // contrôle descente
      9.98812, 7.78902 // bas droit
    );

    // Partie basse de l’ellipse
    ctx.moveTo(5.32145, 7.78902);
    ctx.bezierCurveTo(
      5.32145, 9, // contrôle bas gauche
      6.8, 10.1224, // contrôle bas
      7.65479, 10.1224 // bas centre
    );

    ctx.bezierCurveTo(
      8.5, 10.1224, // contrôle bas droit
      9.98812, 9, // remontée
      9.98812, 7.78902 // retour au centre
    );

    // Partie droite : ligne horizontale
    ctx.moveTo(9.98812, 7.78902);
    ctx.lineTo(14.6548, 7.78902);

    ctx.stroke();

    return canvas;
  }


  private initSalesChart() {
    if (!this.salesChartRef?.nativeElement) {
      console.error('Canvas element not found for sales chart');
      return;
    }

    const ctx = this.salesChartRef.nativeElement.getContext('2d');
    if (!ctx) {
      console.error('Cannot get 2d context for sales chart');
      return;
    }

    // Détruire le graphique existant s'il y en a un
    if (this.salesChart) {
      this.salesChart.destroy();
    }

    this.salesChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: this.salesData.map(d => d.month),
        datasets: [{
          label: 'Montant (€)',
          data: this.salesData.map(d => d.amount),
          borderColor: '#4F46E5',
          backgroundColor: 'transparent',
          borderWidth: 1,
          pointBackgroundColor: '#ffffffff',
          pointBorderColor: '#4F46E5',
          pointBorderWidth: 1,
          pointRadius: 4,
          tension: 0.4,
          fill: false
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: true,
            position: 'bottom',
            labels: {
              color: '#4F46E5',
              usePointStyle: true,
              padding: 20,
              pointStyle: this.createSVGIcon(), // Utiliser l'icône SVG personnalisée
              boxWidth: 14,
              boxHeight: 14
            }
          }
        },
        scales: {
          x: {
            grid: {
              display: true,
              color: '#CCCCCC',
              borderDash: [2, 4],
            },
            ticks: {
              color: '#6b7280'
            }
          },
          y: {
            grid: {
              display: true,
              color: '#CCCCCC',
              borderDash: [2, 4],
            },
            ticks: {
              stepSize: 1500,
              color: '#6b7280',
              callback: function (value) {
                return value + '';
              }
            },
            beginAtZero: true
          }
        },
        elements: {
          point: {
            hoverRadius: 7,
          }
        }
      }
    });
  }

  private initOrdersChart() {
    if (!this.ordersChartRef?.nativeElement) {
      console.error('Canvas element not found for orders chart');
      return;
    }

    const ctx = this.ordersChartRef.nativeElement.getContext('2d');
    if (!ctx) {
      console.error('Cannot get 2d context for orders chart');
      return;
    }

    // Détruire le graphique existant s'il y en a un
    if (this.ordersChart) {
      this.ordersChart.destroy();
    }

    this.ordersChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: this.ordersData.map(d => d.month),
        datasets: [{
          label: 'Commandes',
          data: this.ordersData.map(d => d.orders),
          backgroundColor: '#4F46E5',
          borderRadius: 0,
          borderSkipped: false,
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
          intersect: false,
          mode: 'index'
        },
        plugins: {
          legend: {
            display: true,
            position: 'bottom',
            labels: {
              color: '#4F46E5',
              padding: 20,
              font: { size: 13, weight: '500' },
              boxWidth: 14,
              boxHeight: 10,

              // On génère manuellement les vignettes carrées avec bordure
              generateLabels: function (chart) {
                const datasets = chart.data.datasets;
                return datasets.map((dataset: any, i: number) => ({
                  text: dataset.label,
                  fillStyle: Array.isArray(dataset.backgroundColor) ? dataset.backgroundColor[0] : dataset.backgroundColor,
                  strokeStyle: '#000000',  // Bordure noire
                  lineWidth: 2,            // Épaisseur bordure
                  hidden: !chart.isDatasetVisible(i),
                  datasetIndex: i
                }));
              }
            }
          },

          tooltip: {
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            titleColor: '#ffffff',
            bodyColor: '#ffffff',
            borderColor: '#6b7280',
            borderWidth: 1,
            displayColors: false,
            callbacks: {
              label: function (context) {
                return 'Commandes: ' + context.parsed.y.toLocaleString();
              }
            }
          }
        },
        scales: {
          x: {
            grid: {
              display: true,
              color: '#CCCCCC',
              borderDash: [2, 4],
            },
            ticks: {
              color: '#6b7280'
            }
          },
          y: {
            grid: {
              display: true,
              color: '#CCCCCC',
              borderDash: [2, 4],
            },
            ticks: {
              color: '#6b7280',
              stepSize: 8,
            },
            beginAtZero: true
          }
        }
      }
    });

  }
}

// Service pour les données (optionnel)
export interface SalesStatisticsService {
  getStatsCards(): StatCard[];
  getSalesData(): SalesData[];
  getOrdersData(): OrdersData[];
}

// Exemple de route (à ajouter dans votre routing)
export const SALES_STATISTICS_ROUTE = {
  path: 'statistiques',
  component: SalesStatisticsComponent,
  title: 'Statistiques des Ventes'
};