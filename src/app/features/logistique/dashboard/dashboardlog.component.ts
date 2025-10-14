import { Component, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';

interface MetricCard {
  title: string;
  value: string;
  subtitle: string;
  subtitleDetail: string;
  icon: string;
}

interface Delivery {
  reference: string;
  client: string;
  date: string;
  produits: number;
  statut: string;
  statutColor: string;
}

interface CommandeLivraison {
  date: string;
  commandes: number;
  livraisons: number;
}

@Component({
  selector: 'app-dashboardlog',
  standalone: true,
  imports: [CommonModule, MainLayoutComponent],
  template: `
    <app-main-layout [role]="role">
      <!-- Header Section -->
      <div class="flex flex-col space-y-4 sm:flex-row sm:items-center sm:justify-between sm:space-y-0 mb-6 lg:mb-8">
        <div>
          <div class="text-sm text-gray-500 mb-1">Pages / Tableau de bord</div>
          <h1 class="text-2xl sm:text-3xl font-bold text-gray-900">Tableau de bord</h1>
        </div>
        <div class="flex items-center space-x-3">
          <select class="px-4 py-2 border border-gray-300 rounded-lg text-sm bg-white">
            <option>Période: Cette année</option>
            <option>Ce mois</option>
            <option>Cette semaine</option>
          </select>
          <select class="px-4 py-2 border border-gray-300 rounded-lg text-sm bg-white">
            <option>Fournisseurs: Tous</option>
          </select>
          <button class="bg-indigo-900 text-white px-4 py-2 rounded-lg text-sm font-medium flex items-center hover:bg-indigo-800 transition-colors">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M6.66699 14.1667L10.0003 17.5M10.0003 17.5L13.3337 14.1667M10.0003 17.5V10M16.667 13.9524C17.6849 13.1117 18.3337 11.8399 18.3337 10.4167C18.3337 7.88536 16.2816 5.83333 13.7503 5.83333C13.5682 5.83333 13.3979 5.73833 13.3054 5.58145C12.2187 3.73736 10.2124 2.5 7.91699 2.5C4.46521 2.5 1.66699 5.29822 1.66699 8.75C1.66699 10.4718 2.3632 12.0309 3.48945 13.1613" stroke="white" stroke-width="1.66667" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            &nbsp;
            Exporter
          </button>
        </div>
      </div>
      <!-- Metrics Cards -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6 lg:mb-8">
        <div *ngFor="let metric of metricsData" class="bg-white rounded-lg p-6 border border-gray-200 hover:shadow-lg transition-shadow">
          <div class="flex items-start justify-between mb-4">
            <div class="flex-1">
              <div class="text-sm text-gray-600 mb-2">{{ metric.title }}</div>
              <div class="text-3xl font-bold text-gray-900 mb-2">{{ metric.value }}</div>
              <div class="text-xs" [ngClass]="getMetricSubtitleClass(metric.subtitle)">
                {{ metric.subtitle }}
              </div>
            </div>
            <div class="w-12 h-12 rounded-lg flex items-center justify-center flex-shrink-0">
              <img [src]="metric.icon" alt="icon" class="w-8 h-8">
            </div>
          </div>
        </div>
      </div>
      <!-- Charts Row -->
      <div class="grid grid-cols-1 xl:grid-cols-2 gap-6 mb-6 lg:mb-8">
        <!-- Commandes vs Livraisons Chart -->
        <div class="bg-white rounded-lg p-6 border border-gray-200 hover:shadow-lg transition-shadow">
          <div class="flex items-center justify-between mb-6">
            <h3 class="text-lg font-semibold text-gray-900">Commandes vs Livraisons</h3>
            <button class="text-sm text-gray-600 hover:text-gray-900 px-3 py-1 border border-gray-300 rounded-lg">
              Voir détails
            </button>
          </div> 
          <div class="flex items-center justify-center space-x-6 mb-4">
            <div class="flex items-center space-x-2">
              <div class="w-4 h-4 bg-indigo-200 rounded"></div>
              <span class="text-sm text-gray-700">Commandes fournisseurs</span>
            </div>
            <div class="flex items-center space-x-2">
              <div class="w-4 h-4 bg-green-400 rounded"></div>
              <span class="text-sm text-gray-700">Livraisons</span>
            </div>
          </div>
          <div class="relative h-80">
            <canvas #commandesChart></canvas>
          </div>
        </div>
        <!-- Stocks - État global -->
        <div class="bg-white rounded-lg p-6 border border-gray-200 hover:shadow-lg transition-shadow">
          <div class="flex items-center justify-between mb-6">
            <h3 class="text-lg font-semibold text-gray-900">Stocks - État global</h3>
            <button class="text-sm text-gray-600 hover:text-gray-900 px-3 py-1 border border-gray-300 rounded-lg">
              Voir détails
            </button>
          </div>

          <div class="flex items-center justify-center h-80">
            <div class="relative" style="width: 300px; height: 300px;">
              <canvas #stocksChart></canvas>
            </div>
          </div>

          <div class="flex items-center justify-center space-x-8 mt-6">
            <div class="flex items-center space-x-2">
              <div class="w-4 h-4 bg-green-500 rounded-full"></div>
              <span class="text-sm text-gray-700">Ok</span>
            </div>
            <div class="flex items-center space-x-2">
              <div class="w-4 h-4 bg-yellow-400 rounded-full"></div>
              <span class="text-sm text-gray-700">Sous seuil</span>
            </div>
            <div class="flex items-center space-x-2">
              <div class="w-4 h-4 bg-pink-300 rounded-full"></div>
              <span class="text-sm text-gray-700">Critique</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Graphiques de statistiques -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <!-- Stocks sous seuil -->
        <div class="bg-white rounded-lg p-6 border border-gray-200 hover:shadow-lg transition-shadow">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-base font-semibold text-gray-900">Stocks sous seuil — par catégorie</h3>
            <button class="text-xs text-gray-600 px-3 py-1.5 border border-gray-300 rounded hover:bg-gray-50 transition-colors">
              Voir détails
            </button>
          </div>
          <div class="relative h-64">
            <canvas #stockCategoryChart></canvas>
          </div>
        </div>

        <!-- Statut des livraisons -->
        <div class="bg-white rounded-lg p-6 border border-gray-200 hover:shadow-lg transition-shadow">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-base font-semibold text-gray-900">Statut des livraisons</h3>
            <button class="text-xs text-gray-600 px-3 py-1.5 border border-gray-300 rounded hover:bg-gray-50 transition-colors">
              Voir détails
            </button>
          </div>
          <div class="flex items-center justify-center" style="height: 200px;">
            <div class="relative" style="width: 200px; height: 200px;">
              <canvas #deliveryStatusChart></canvas>
            </div>
          </div>
          <div class="flex items-center justify-center gap-6 mt-4 text-xs">
            <div class="flex items-center gap-2">
              <span class="w-3 h-3 rounded-full bg-green-500"></span>
              <span class="text-gray-700">Planifiée</span>
            </div>
            <div class="flex items-center gap-2">
              <span class="w-3 h-3 rounded-full" style="background-color: #FEF3C7;"></span>
              <span class="text-gray-700">À confirmer</span>
            </div>
            <div class="flex items-center gap-2">
              <span class="w-3 h-3 rounded-full" style="background-color: #FBCFE8;"></span>
              <span class="text-gray-700">Retard</span>
            </div>
          </div>
        </div>

        <!-- Taux de retours -->
        <div class="bg-white rounded-lg p-6 border border-gray-200 hover:shadow-lg transition-shadow">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-base font-semibold text-gray-900">Taux de retours (%)</h3>
            <button class="text-xs text-gray-600 px-3 py-1.5 border border-gray-300 rounded hover:bg-gray-50 transition-colors">
              Voir détails
            </button>
          </div>
          <div class="relative h-64">
            <canvas #returnRateChart></canvas>
          </div>
        </div>
      </div>

    </app-main-layout>
  `,
  styles: [`
    :host {
      display: block;
    }
    
    @media (max-width: 640px) {
      .overflow-x-auto {
        -webkit-overflow-scrolling: touch;
      }
    }
  `]
})
export class DashboardLogComponent implements OnInit, AfterViewInit {
  @ViewChild('commandesChart') commandesChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('stocksChart') stocksChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('stockCategoryChart') stockCategoryChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('deliveryStatusChart') deliveryStatusChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('returnRateChart') returnRateChartRef!: ElementRef<HTMLCanvasElement>;
  
  role: 'log' = 'log';
  
  private commandesChart: any;
  private stocksChart: any;
  private stockCategoryChart: any;
  private deliveryStatusChart: any;
  private returnRateChart: any;

  metricsData: MetricCard[] = [
    {
      title: 'Commandes fournisseurs',
      value: '1961',
      subtitle: '↗ 5% vs 30 jours derniers',
      subtitleDetail: '',
      icon: '/icones/commandefour.svg',
    },
    {
      title: 'Livraisons à venir',
      value: '1041',
      subtitle: '↘ -3% vs mois dernier',
      subtitleDetail: '',
      icon: '/icones/livraisonavenir.svg',
    },
    {
      title: 'Stocks sous seuil',
      value: '03',
      subtitle: '↗ 5% vs SKU en alerte',
      subtitleDetail: '',
      icon: '/icones/stocks.png',
    },
    {
      title: 'Retours',
      value: '03',
      subtitle: '↗ 2% vs 30 derniers jours',
      subtitleDetail: '',
      icon: '/icones/retours.png',
    }
  ];

  commandesLivraisonsData: CommandeLivraison[] = [
    { date: '06/09', commandes: 7, livraisons: 6 },
    { date: '07/09', commandes: 6.5, livraisons: 5 },
    { date: '08/09', commandes: 7, livraisons: 6 },
    { date: '09/09', commandes: 7, livraisons: 6.5 },
    { date: '10/09', commandes: 7, livraisons: 5.5 },
    { date: '11/09', commandes: 7, livraisons: 6 },
    { date: '12/09', commandes: 6.5, livraisons: 6.5 }
  ];

  stockCategoryData = [
    { category: 'Épicerie', value: 40 },
    { category: 'Boissons', value: 7 },
    { category: 'Frais', value: 14 },
    { category: 'Hygiène', value: 14 }
  ];

  deliveryStatusData = [
    { status: 'Planifiée', value: 75, color: '#22C55E' },
    { status: 'À confirmer', value: 15, color: '#FEF3C7' },
    { status: 'Retard', value: 10, color: '#FBCFE8' }
  ];

  returnRateData = [
    { date: '06/09', rate: 7 },
    { date: '07/09', rate: 14 },
    { date: '08/09', rate: 6 },
    { date: '09/09', rate: 13 },
    { date: '10/09', rate: 8 },
    { date: '11/09', rate: 13 },
    { date: '12/09', rate: 7 },
    { date: '13/09', rate: 15 },
    { date: '14/09', rate: 9 },
    { date: '15/09', rate: 16 },
    { date: '16/09', rate: 15 }
  ];

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.loadChartJs();
    }, 100);
  }

  async loadChartJs(): Promise<void> {
    try {
      const Chart = (await import('chart.js/auto')).default;
      this.initCommandesChart(Chart);
      this.initStocksChart(Chart);
      this.initStockCategoryChart(Chart);
      this.initDeliveryStatusChart(Chart);
      this.initReturnRateChart(Chart);
    } catch (error) {
      console.error('Erreur lors du chargement de Chart.js:', error);
    }
  }

 initCommandesChart(Chart: any): void {
  const ctx = this.commandesChartRef?.nativeElement?.getContext('2d');
  if (!ctx) return;

  // Plugin pour dessiner les flèches UNIQUEMENT sur le trait bleu (commandes)
  const arrowPlugin = {
    id: 'arrowPlugin',
    afterDatasetsDraw: (chart: any) => {
      const ctx = chart.ctx;
      chart.data.datasets.forEach((dataset: any, i: number) => {
        // Ne dessiner les flèches que pour le dataset "Trend Commandes" (index 2)
        if (dataset.type === 'line' && i === 2) {
          const meta = chart.getDatasetMeta(i);
          const points = meta.data;
          
          ctx.save();
          ctx.strokeStyle = dataset.borderColor;
          ctx.fillStyle = dataset.borderColor;
          ctx.lineWidth = 2;
          
          // Dessiner toutes les lignes
          for (let j = 0; j < points.length - 1; j++) {
            const p1 = points[j];
            const p2 = points[j + 1];
            
            ctx.beginPath();
            ctx.moveTo(p1.x, p1.y);
            ctx.lineTo(p2.x, p2.y);
            ctx.stroke();
          }
          
          // Dessiner la flèche au début (premier segment) - FERMÉE
          if (points.length > 1) {
            const p1 = points[0];
            const p2 = points[1];
            const angle = Math.atan2(p2.y - p1.y, p2.x - p1.x);
            const headLength = 8;
            const arrowX = p1.x + (p2.x - p1.x) * 0.3;
            const arrowY = p1.y + (p2.y - p1.y) * 0.3;
            
            ctx.beginPath();
            ctx.moveTo(arrowX, arrowY);
            ctx.lineTo(
              arrowX - headLength * Math.cos(angle - Math.PI / 6),
              arrowY - headLength * Math.sin(angle - Math.PI / 6)
            );
            ctx.lineTo(
              arrowX - headLength * Math.cos(angle + Math.PI / 6),
              arrowY - headLength * Math.sin(angle + Math.PI / 6)
            );
            ctx.closePath();
            ctx.fill();
          }
          
          // Dessiner la flèche à la fin (dernier segment) - FERMÉE
          if (points.length > 1) {
            const p1 = points[points.length - 2];
            const p2 = points[points.length - 1];
            const angle = Math.atan2(p2.y - p1.y, p2.x - p1.x);
            const headLength = 8;
            const arrowX = p1.x + (p2.x - p1.x) * 0.7;
            const arrowY = p1.y + (p2.y - p1.y) * 0.7;
            
            ctx.beginPath();
            ctx.moveTo(arrowX, arrowY);
            ctx.lineTo(
              arrowX - headLength * Math.cos(angle - Math.PI / 6),
              arrowY - headLength * Math.sin(angle - Math.PI / 6)
            );
            ctx.lineTo(
              arrowX - headLength * Math.cos(angle + Math.PI / 6),
              arrowY - headLength * Math.sin(angle + Math.PI / 6)
            );
            ctx.closePath();
            ctx.fill();
          }
          
          ctx.restore();
        }
      });
    }
  };

  this.commandesChart = new Chart(ctx, {
    type: 'bar',
    data: {
      labels: this.commandesLivraisonsData.map(d => d.date),
      datasets: [
        {
          label: 'Commandes fournisseurs',
          data: this.commandesLivraisonsData.map(d => d.commandes),
          backgroundColor: 'rgba(199, 210, 254, 0.5)',
          borderColor: '#C7D2FE',
          borderWidth: 0,
          borderRadius: 4,
          order: 2
        },
        {
          label: 'Livraisons',
          data: this.commandesLivraisonsData.map(d => d.livraisons),
          backgroundColor: 'rgba(134, 239, 172, 0.5)',
          borderColor: '#86EFAC',
          borderWidth: 0,
          borderRadius: 4,
          order: 3
        },
        {
          label: 'Trend Commandes',
          data: this.commandesLivraisonsData.map(d => d.commandes),
          type: 'line',
          borderColor: '#6366F1',
          backgroundColor: 'transparent',
          borderWidth: 2,
          tension: 0,
          pointRadius: 0,
          pointHoverRadius: 0,
          order: 1
        }
        // Dataset "Trend Livraisons" supprimé
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: 'index',
        intersect: false,
      },
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          backgroundColor: '#1F2937',
          titleColor: '#fff',
          bodyColor: '#fff',
          padding: 12,
          cornerRadius: 8,
          displayColors: true,
          filter: (item: any) => item.datasetIndex < 2,
          callbacks: {
            label: (context: any) => {
              const label = context.dataset.label || '';
              return label + ': ' + context.parsed.y;
            }
          }
        }
      },
      scales: {
        y: {
          beginAtZero: true,
          max: 8,
          grid: {
            color: '#F3F4F6',
            drawBorder: false
          },
          ticks: {
            color: '#6B7280',
            font: {
              size: 11
            }
          }
        },
        x: {
          grid: {
            display: false,
            drawBorder: false
          },
          ticks: {
            color: '#6B7280',
            font: {
              size: 11
            }
          }
        }
      }
    },
    plugins: [arrowPlugin]
  });
}
  initStocksChart(Chart: any): void {
    const ctx = this.stocksChartRef?.nativeElement?.getContext('2d');
    if (!ctx) return;

    this.stocksChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Ok', 'Sous seuil', 'Critique'],
        datasets: [{
          data: [75, 15, 10],
          backgroundColor: ['#22C55E', '#FBBF24', '#FCA5C0'],
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '70%',
        plugins: {
          legend: {
            display: false
          },
          tooltip: {
            backgroundColor: '#1F2937',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8
          }
        }
      }
    });
  }

  initStockCategoryChart(Chart: any): void {
    const ctx = this.stockCategoryChartRef?.nativeElement?.getContext('2d');
    if (!ctx) return;

    this.stockCategoryChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: this.stockCategoryData.map(d => d.category),
        datasets: [{
          data: this.stockCategoryData.map(d => d.value),
          backgroundColor: '#6366F1',
          borderRadius: 4,
          barThickness: 40
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false
          },
          tooltip: {
            backgroundColor: '#1F2937',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8,
            displayColors: false
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            max: 45,
            grid: {
              color: '#F3F4F6',
              drawBorder: false
            },
            ticks: {
              color: '#6B7280',
              font: {
                size: 10
              }
            }
          },
          x: {
            grid: {
              display: false,
              drawBorder: false
            },
            ticks: {
              color: '#6B7280',
              font: {
                size: 10
              }
            }
          }
        }
      }
    });
  }

  initDeliveryStatusChart(Chart: any): void {
    const ctx = this.deliveryStatusChartRef?.nativeElement?.getContext('2d');
    if (!ctx) return;

    this.deliveryStatusChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: this.deliveryStatusData.map(d => d.status),
        datasets: [{
          data: this.deliveryStatusData.map(d => d.value),
          backgroundColor: this.deliveryStatusData.map(d => d.color),
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '70%',
        plugins: {
          legend: {
            display: false
          },
          tooltip: {
            backgroundColor: '#1F2937',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8
          }
        }
      }
    });
  }

  initReturnRateChart(Chart: any): void {
    const ctx = this.returnRateChartRef?.nativeElement?.getContext('2d');
    if (!ctx) return;

    this.returnRateChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: this.returnRateData.map(d => d.date),
        datasets: [{
          data: this.returnRateData.map(d => d.rate),
          borderColor: '#F97316',
          backgroundColor: 'transparent',
          borderWidth: 2,
          tension: 0.4,
          pointRadius: 0,
          pointHoverRadius: 6,
          pointHoverBackgroundColor: '#F97316',
          pointHoverBorderColor: '#fff',
          pointHoverBorderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
          mode: 'index',
          intersect: false,
        },
        plugins: {
          legend: {
            display: false
          },
          tooltip: {
            backgroundColor: '#1F2937',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8,
            displayColors: false
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            max: 18,
            grid: {
              color: '#F3F4F6',
              drawBorder: false
            },
            ticks: {
              color: '#6B7280',
              font: {
                size: 10
              },
              stepSize: 2
            }
          },
          x: {
            grid: {
              display: false,
              drawBorder: false
            },
            ticks: {
              color: '#6B7280',
              font: {
                size: 10
              }
            }
          }
        }
      }
    });
  }

  getMetricSubtitleClass(subtitle: string): string {
    if (subtitle.includes('↗')) {
      return 'text-green-600 font-medium';
    } else if (subtitle.includes('↘')) {
      return 'text-red-600 font-medium';
    }
    return 'text-gray-600';
  }
}