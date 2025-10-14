import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { Chart, ChartConfiguration, ChartType, registerables } from 'chart.js';
import { ViewChild, ElementRef, OnDestroy } from '@angular/core';

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
  imports: [CommonModule, MainLayoutComponent],
  template: `
    <app-main-layout role="com">
      <div class="space-y-6">
        <!-- Header avec filtres -->
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between">
          <h1 class="text-2xl font-semibold text-gray-900 mb-4 sm:mb-0">
            Statistiques des Ventes
          </h1>
          <div class="flex flex-col sm:flex-row gap-3">
            <!-- Filtre entreprise -->
            

            <!-- Filtre période -->
            

            <!-- Bouton Export -->
            <button class="inline-flex items-center px-4 py-2 bg-white border border-gray-300 focus:border-transparent rounded-lg px-4 py-2 pr-8 bg-gray-100 text-gray-700 text-sm font-medium rounded-lg hover:bg-gray-200 transition-colors">
              <svg width="17" height="16" viewBox="0 0 17 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M8.94385 10V2" stroke="#374151" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M14.9438 10V12.6667C14.9438 13.0203 14.8034 13.3594 14.5533 13.6095C14.3033 13.8595 13.9641 14 13.6105 14H4.27718C3.92356 14 3.58442 13.8595 3.33437 13.6095C3.08432 13.3594 2.94385 13.0203 2.94385 12.6667V10" stroke="#374151" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M5.61035 6.6665L8.94368 9.99984L12.277 6.6665" stroke="#374151" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              Exporter
            </button>
          </div>
        </div>
        <div class="w-full bg-white rounded-lg shadow p-6 grid grid-cols-1 sm:grid-cols-2 flex justify-between gap-80">
  <!-- Sélection entreprise -->
  <div >
    <label class="block text-sm font-medium text-gray-600 mb-2 ">Sélectionner une entreprise</label>
    <select
      class="w-80 text-gray-700 focus:outline-none focus:ring-0 border-none px-3 py-2"
    >
      <option>Toutes les entreprises</option>
      <option>Entreprise 1</option>
      <option>Entreprise 2</option>
    </select>
  </div>

  <!-- Sélection période -->
  <div class="flex justify-end w-full">
  <div>
    <label class="block text-sm font-medium text-gray-600 mb-1 px-3 py-2">Période</label>
    <select
      class="w-80 text-gray-700 focus:outline-none focus:ring-0 border-none px-3 py-2"
    >
      <option>Mois en cours</option>
      <option>Mois précédent</option>
      <option>Cette année</option>
    </select>
  </div>
</div>
    </div>
        <!-- Cartes de statistiques -->
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div *ngFor="let stat of statsCards" class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
            <div class="space-y-2">
              <h3 class="text-sm font-medium text-gray-600">{{ stat.title }}</h3>
              <p class="text-2xl font-bold text-gray-900">{{ stat.value }}</p>
            </div>
          </div>
        </div>
        <!-- Graphiques -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 pb-4">
          <!-- Évolution des ventes -->
          <div class="bg-white rounded-xl  shadow-sm border border-gray-100 py-4 sm:py-6">
            <h3 class="text-lg font-semibold text-gray-900 mb-4 border-b border-gray-200 pb-4 sm:px-6">Évolution des ventes</h3>
            <div class="h-80">
              <canvas #salesChart></canvas>
            </div>
          </div>
          <!-- Nombre de commandes -->
          <div class="bg-white rounded-xl py-4 shadow-sm border border-gray-100 sm:py-6">
            <h3 class="text-lg font-semibold text-gray-900 mb-4 border-b border-gray-200 pb-4 sm:px-6">Nombre de commandes</h3>
            <div class="h-80">
              <canvas #ordersChart></canvas>
            </div>
          </div>
        </div>
        <!-- Section Détails et Analyse -->
        <div class="bg-white rounded-xl shadow-md border border-gray-100">
           <div class="border-b border-gray-200 pb-2 sm:py-4">
               <h2 class="text-lg sm:text-xl font-semibold text-gray-900 pb-2 sm:px-6">Détails et Analyse</h2>
            </div>
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-8 mt-4">
            <!-- Informations générales -->
            <div>
              <h3 class="text-lg font-medium text-gray-900 mb-4 sm:px-6">Informations générales</h3>
              <div class="space-y-4 sm:px-6">
                <div *ngFor="let info of generalInfo" class="flex items-center justify-between py-2">
                  <span class="text-sm text-gray-600">{{ info.label }}</span>
                  <div class="flex items-center gap-2">
             <!-- Icône positive -->
                <span *ngIf="info.trend && info.trendType === 'positive'" 
                      class="inline-flex items-center text-xs font-medium text-green-600">
                  <svg width="17" height="16" viewBox="0 0 17 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <g clip-path="url(#clip0_24_798)">
                      <path d="M11.165 4.6665H15.165V8.6665" stroke="#22C55E" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M15.1654 4.6665L9.4987 10.3332L6.16536 6.99984L1.83203 11.3332" stroke="#22C55E" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </g>
                    <defs>
                      <clipPath id="clip0_24_798">
                        <rect width="16" height="16" fill="white" transform="translate(0.498535)"/>
                      </clipPath>
                    </defs>
                  </svg>
                </span>
<!-- Icône négative -->
            <span *ngIf="info.trend && info.trendType === 'negative'" 
                  class="inline-flex items-center text-xs font-medium text-red-600">
              <svg class="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
                <path fill-rule="evenodd" d="M14.707 10.293a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 111.414-1.414L9 12.586V5a1 1 0 012 0v7.586l2.293-2.293a1 1 0 011.414 0z" clip-rule="evenodd"></path>
              </svg>
              {{ info.trend }}
            </span>

  <!-- Valeur -->
  <span class="text-sm font-medium text-gray-900">{{ info.value }}</span>
</div>

                </div>
              </div>
            </div>

            <!-- Analyse et recommandations -->
            <div>
              <h3 class="text-lg font-medium text-gray-900 mb-4">Analyse et recommandations</h3>
              <div class="space-y-6 bg-gray-50 p-4 rounded-lg">
                  <p class="text-sm text-gray-600">
                    Sélectionnez une entreprise spécifique pour voir des recommandations personnalisées.
                  </p>
                  <span></span>
                  <button (click)="analyzePotential()" 
                          class="w-full inline-flex items-center justify-center px-4 py-3 bg-[#6366F1] text-white text-sm font-medium rounded-lg hover:bg-[#5856EB] transition-colors">
                    <svg width="17" height="16" viewBox="0 0 17 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M8.26416 13.3332V6.6665" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M12.2642 13.3332V2.6665" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M4.26416 13.3332V10.6665" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    Analyser le potentiel
                  </button>
            </div>
            </div>
          </div>
        </div>
      </div>
    </app-main-layout>
  `,
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
      value: '458'
    },
    {
      title: 'Commandes passées',
      value: '756'
    },
    {
      title: 'Montant total',
      value: '98 450 €'
    },
    {
      title: 'Moyenne par salarié',
      value: '1.65'
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
          borderColor: '#6366F1',
          backgroundColor: 'transparent',
          borderWidth: 1,
          pointBackgroundColor: '#ffffffff',
          pointBorderColor: '#002cf1ff',
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
              color: '#6366F1',
              usePointStyle: true,
              padding: 20,
              pointStyle: this.createSVGIcon(), // Utiliser l'icône SVG personnalisée
              boxWidth: 17,
              boxHeight: 16
            }
          }
        },
        scales: {
          x: {
            grid: {
              display: true,
              color: '#f3f4f6'
            },
            ticks: {
              color: '#6b7280'
            }
          },
          y: {
            grid: {
              display: true,
              color: '#f3f4f6'
            },
            ticks: {
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
      backgroundColor: '#6366F1',
      // ON ENLEVE la bordure sur les barres :
      // borderColor: '#000000',
      // borderWidth: 2,
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
    color: '#6366F1',
    padding: 20,
    font: { size: 13, weight: '500' },
    boxWidth: 20,
    boxHeight: 12,

    // On génère manuellement les vignettes carrées avec bordure
    generateLabels: function(chart) {
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
          label: function(context) {
            return 'Commandes: ' + context.parsed.y.toLocaleString();
          }
        }
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