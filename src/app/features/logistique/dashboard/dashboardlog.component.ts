import { Component, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  LogisticsService,
  RLDashboardKpis,
  StatutTournees,
  LivraisonParJourItem,
  TauxRetoursParJourItem
} from '../../../shared/services/logistics.service';

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

/** Point du graphique « Livraisons 7 jours » : aligné sur l’API (nbPrevues, nbLivreesALaDate, nbRetard). */
interface LivraisonsParJourPoint {
  date: string;
  livrees: number;
  prevues: number;
  retard: number;
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
  @ViewChild('deliveryStatusChart') deliveryStatusChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('returnRateChart') returnRateChartRef!: ElementRef<HTMLCanvasElement>;

  role: 'log' = 'log';

  dashboardLoading = true;
  dashboardError: string | null = null;

  private commandesChart: any;
  private stocksChart: any;
  private deliveryStatusChart: any;
  private returnRateChart: any;

  metricsData: MetricCard[] = [
    { title: 'Commandes en attente', value: '0', subtitle: '', subtitleDetail: '', icon: '/icones/commandefour.svg' },
    { title: 'Commandes en retard', value: '0', subtitle: '', subtitleDetail: '', icon: '/icones/livraisonavenir.svg' },
    { title: 'Tournées actives', value: '0', subtitle: '', subtitleDetail: '', icon: '/icones/stocks.png' },
    { title: 'Livrées ce mois', value: '0', subtitle: '', subtitleDetail: '', icon: '/icones/retours.png' }
  ];

  livraisonsParJourData: LivraisonsParJourPoint[] = [];

  /** Donut Stocks - État global : rempli par GET /logistics/dashboard/stock-etat-global */
  stockEtatGlobalData: { normal: number; sousSeuil: number; critique: number } = { normal: 0, sousSeuil: 0, critique: 0 };

  deliveryStatusData: { status: string; value: number; color: string }[] = [];

  /** Données pour le graphique « Taux de retours (%) » (API dashboard/taux-retours-par-jour). */
  returnRateData: { date: string; rate: number }[] = [];

  constructor(private logistics: LogisticsService) {}

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    this.dashboardLoading = true;
    this.dashboardError = null;
    forkJoin({
      kpis: this.logistics.getDashboardKpis(),
      statutTournees: this.logistics.getStatutTournees(),
      livraisonsParJour7j: this.logistics.getCommandesVsLivraisons(),
      stockEtatGlobal: this.logistics.getStockEtatGlobal(),
      tauxRetoursParJour: this.logistics.getTauxRetoursParJour()
    }).subscribe({
      next: ({ kpis, statutTournees, livraisonsParJour7j, stockEtatGlobal, tauxRetoursParJour }) => {
        this.metricsData = [
          { title: 'Commandes en attente', value: String(kpis.commandesEnAttente), subtitle: '', subtitleDetail: '', icon: '/icones/commandefour.svg' },
          { title: 'Commandes en retard', value: String(kpis.commandesEnRetard), subtitle: '', subtitleDetail: '', icon: '/icones/livraisonavenir.svg' },
          { title: 'Tournées actives', value: String(kpis.tourneesActives), subtitle: '', subtitleDetail: '', icon: '/icones/stocks.png' },
          { title: 'Livrées ce mois', value: String(kpis.livreesCeMois), subtitle: '', subtitleDetail: '', icon: '/icones/retours.png' }
        ];
        const parStatut = statutTournees.parStatut || {};
        const statusConfig: Record<string, { label: string; color: string }> = {
          ASSIGNEE: { label: 'Assignée', color: '#22C55E' },
          EN_COURS: { label: 'En cours', color: '#FFE7C2' },
          TERMINEE: { label: 'Terminée', color: '#4F46E5' },
          ANNULEE: { label: 'Annulée', color: '#FFD3D3' }
        };
        this.deliveryStatusData = Object.entries(statusConfig).map(([key, { label, color }]) => ({
          status: label,
          value: parStatut[key] ?? 0,
          color
        }));
        this.livraisonsParJourData = livraisonsParJour7j.map((d: LivraisonParJourItem) => ({
          date: d.date,
          livrees: d.nbLivreesALaDate,
          prevues: d.nbPrevues,
          retard: d.nbRetard
        }));
        this.stockEtatGlobalData = {
          normal: stockEtatGlobal?.normal ?? 0,
          sousSeuil: stockEtatGlobal?.sousSeuil ?? 0,
          critique: stockEtatGlobal?.critique ?? 0
        };
        this.returnRateData = (tauxRetoursParJour ?? []).map(d => ({ date: d.date, rate: d.tauxPercent }));
        setTimeout(async () => {
          await this.loadChartJs();
          this.dashboardLoading = false;
        }, 100);
      },
      error: (err) => {
        this.dashboardError = err?.message || 'Erreur lors du chargement du tableau de bord.';
        this.dashboardLoading = false;
      }
    });
  }

  async loadChartJs(): Promise<void> {
    try {
      const Chart = (await import('chart.js/auto')).default;
      this.initCommandesChart(Chart);
      this.initStocksChart(Chart);
      this.initDeliveryStatusChart(Chart);
      this.initReturnRateChart(Chart);
    } catch (error) {
      console.error('Erreur lors du chargement de Chart.js:', error);
    }
  }

  initCommandesChart(Chart: any): void {
    const ctx = this.commandesChartRef?.nativeElement?.getContext('2d');
    if (!ctx) return;

    const data = this.livraisonsParJourData;
    const barTotals = data.map((d) => d.livrees + d.prevues + d.retard);
    const yMax = (barTotals.length ? Math.max(...barTotals, 0) : 0) + 2;

    this.commandesChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: data.map((d) => d.date),
        datasets: [
          {
            label: 'Prévues ce jour',
            data: data.map((d) => d.prevues),
            backgroundColor: '#E5E7EB',
            borderColor: '#fff',
            borderWidth: 1,
            categoryPercentage: 0.7,
            barPercentage: 0.9,
            yAxisID: 'y'
          },
          {
            label: 'Livrées ce jour',
            data: data.map((d) => d.livrees),
            backgroundColor: '#22C55E',
            borderColor: '#fff',
            borderWidth: 1,
            categoryPercentage: 0.7,
            barPercentage: 0.9,
            yAxisID: 'y'
          },
          {
            label: 'En retard',
            data: data.map((d) => d.retard),
            backgroundColor: '#F97316',
            borderColor: '#fff',
            borderWidth: 1,
            categoryPercentage: 0.7,
            barPercentage: 0.9,
            yAxisID: 'y'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
          mode: 'index',
          intersect: false
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
              font: { size: 12 }
            }
          },
          tooltip: {
            backgroundColor: '#1F2937',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8,
            displayColors: true,
            boxPadding: 4
          }
        },
        scales: {
          x: {
            stacked: false,
            grid: { color: '#F3F4F6' },
            ticks: { color: '#6B7280', font: { size: 11 } },
            border: { display: false }
          },
          y: {
            stacked: false,
            beginAtZero: true,
            min: 0,
            max: yMax,
            grid: { color: '#F3F4F6' },
            ticks: {
              color: '#6B7280',
              font: { size: 11 },
              padding: 10,
              stepSize: 1,
              callback: (value: string | number) => (Number.isInteger(Number(value)) ? value : '')
            },
            border: { display: true }
          }
        }
      }
    });
  }
  initStocksChart(Chart: any): void {
    const ctx = this.stocksChartRef?.nativeElement?.getContext('2d');
    if (!ctx) return;

    const d = this.stockEtatGlobalData;
    this.stocksChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Normal', 'Sous seuil', 'Rupture'],
        datasets: [{
          data: [d.normal, d.sousSeuil, d.critique],
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
            displayColors: false,
            callbacks: {
              label: (ctx: any) => `Part des commandes livrées avec retour : ${ctx.parsed?.y ?? ctx.parsed ?? 0} %`
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            max: this.returnRateData.length ? Math.max(16, ...this.returnRateData.map(d => d.rate)) + 2 : 16,
            grid: {
              color: '#F3F4F6',
              drawBorder: false
            },
            ticks: {
              color: '#6B7280',
              font: { size: 10 },
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
              font: { size: 10 }
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