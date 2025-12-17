import { Component, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { RouterModule } from '@angular/router';
import { isBoxedPrimitive } from 'util/types';

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
  imports: [CommonModule, MainLayoutComponent, HeaderComponent, RouterModule],
  templateUrl: './dashboardlog.component.html',
  styleUrls: ['./dashboardlog.component.css']
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
    { status: 'À confirmer', value: 15, color: '#FFE7C2' },
    { status: 'Retard', value: 10, color: '#FFD3D3' }
  ];

  returnRateData = [
    { date: '06/09', rate: 7 },
    { date: '07/09', rate: 14 },
    { date: '08/09', rate: 6 },
    { date: '09/09', rate: 13 },
    { date: '10/09', rate: 8 },
  ];

  ngOnInit(): void { }

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

    this.commandesChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: this.commandesLivraisonsData.map(d => d.date),
        datasets: [
          {
            label: 'Commandes fournisseurs',
            data: this.commandesLivraisonsData.map(d => d.commandes),
            backgroundColor: '#E0E7FF',
            hoverBackgroundColor: '#C7D2FE',
            categoryPercentage: 0.7,
            order: 2,
            yAxisID: 'y'
          },
          {
            label: 'Livraisons',
            data: this.commandesLivraisonsData.map(d => d.livraisons),
            backgroundColor: '#C1F3DC',
            hoverBackgroundColor: '#C1F3DC',
            categoryPercentage: 0.7,
            order: 3,
            yAxisID: 'y'
          },
          {
            label: 'Montant',
            data: this.commandesLivraisonsData.map(d => d.commandes * 4000000),
            type: 'line',
            borderColor: 'transparent',
            pointRadius: 0,
            yAxisID: 'y1',
            order: 4
          }
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
            display: true,
            position: 'top',
            align: 'center',
            labels: {
              usePointStyle: true,
              pointStyle: 'circle',
              boxWidth: 6,
              boxHeight: 6,
              padding: 20,
              font: { size: 12 },
              filter: (item: any) => item.text !== 'Montant'
            }
          },
          tooltip: {
            backgroundColor: '#1F2937',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8,
            displayColors: true,
            boxPadding: 4,
            filter: (item: any) => item.datasetIndex < 2
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            max: 8,
            grid: {  color: '#F3F4F6', },
            ticks: { color: '#6B7280', font: { size: 11 }, padding: 10 },
            border: { display: true }
          },
          y1: {
            beginAtZero: true,
            position: 'right',
            max: 32000000,
            grid: { drawOnChartArea: false, drawBorder: false },
            ticks: {
              color: '#6B7280',
              font: { size: 11 },
              padding: 10,
              callback: (value: any) => value.toLocaleString('fr-FR') + ' F',
              stepSize: 4000000
            },
            border: { display: false }
          },
          x: {
            grid: { color: '#F3F4F6'},
            ticks: { color: '#6B7280', font: { size: 11 } },
            border: { display: false }
          }
        }
      },
      // plugins: [] // Suppression du plugin customLinePlugin
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
          backgroundColor: ['#22C55F', '#FFE7C2', '#FFD3D3'],
          borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '60%',
        plugins: {
          legend: {
            display: true,
            position: 'bottom',
            labels: {
              usePointStyle: true,
              pointStyle: 'circle',
              boxWidth: 10,
              boxHeight: 10,
              padding: 20,
              font: {
                size: 12
              }
            }
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
          backgroundColor: '#4F46E5',
          barThickness: 50
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
            max: 40,
            grid: {
              display: false,
              borderWidth: 1,
            },
            ticks: {
              color: '#6B7280',
              font: {
                size: 10
              },
              stepSize: 10
            }
          },
          x: {
            grid: {
              display: false,
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
          borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '60%',
        plugins: {
          legend: {
            display: true,
            position: 'bottom',
            labels: {
              usePointStyle: true,
              pointStyle: 'circle',
              boxWidth: 10,
              boxHeight: 10,
              padding: 20,
              font: {
                size: 12
              }
            }
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
          tension: 0.5,
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
            beginAtZero: false,
            max: 16,
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
              color: '#F3F4F6',
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