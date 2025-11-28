import { Component, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';

interface MetricCard {
  title: string;
  value: string;
  delta: string;
  trend: 'up' | 'down';
  icon: string;
  iconBg: string;
}

interface CommandRevenuePoint {
  date: string;
  commandes: number;
  livraisons: number;
  montantEncaisse: number;
}

interface PaymentStatusSlice {
  status: string;
  value: number;
  color: string;
}

interface LivraisonStackPoint {
  date: string;
  livres: number;
  planifies: number;
  retard: number;
}

interface RoleStat {
  role: string;
  total: number;
}

interface CouponTrendPoint {
  date: string;
  value: number;
}

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [CommonModule, MainLayoutComponent, HeaderComponent],
  templateUrl: './dashboard.component.html'
})
export class AdminPageComponent implements OnInit, AfterViewInit {
  @ViewChild('commandesChart') commandesChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('paiementsChart') paiementsChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('livraisonsChart') livraisonsChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('rolesChart') rolesChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('stocksEtatChart') stocksEtatChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('couponsChart') couponsChartRef!: ElementRef<HTMLCanvasElement>;

  role: 'admin' = 'admin';
  
  // --- Cartes KPI supérieures ---
  metricsData: MetricCard[] = [
    {
      title: 'Commandes',
      value: '1 961',
      delta: '↗ 4 %',
      trend: 'up',
      icon: '/icones/commandefour.svg',
      iconBg: 'bg-[#EEF2FF]'
    },
    {
      title: 'Utilisateurs',
      value: '1 041',
      delta: '↘ 3 %',
      trend: 'down',
      icon: '/icones/utilisateurs.svg',
      iconBg: 'bg-[#FFF7ED]'
    },
    {
      title: 'Montant encaissé',
      value: '1,25 Md FCFA',
      delta: '↗ 12 %',
      trend: 'up',
      icon: '/icones/vente.svg',
      iconBg: 'bg-[#ECFDF5]'
    }
  ];

  // --- Jeu de données principal pour commandes vs chiffre d'affaires ---
  commandesChiffreData: CommandRevenuePoint[] = [
    { date: '06/09', commandes: 7, livraisons: 7, montantEncaisse: 8800000 },
    { date: '07/09', commandes: 7, livraisons: 7, montantEncaisse: 9600000 },
    { date: '08/09', commandes: 6, livraisons: 5, montantEncaisse: 6200000 },
    { date: '09/09', commandes: 6, livraisons: 6, montantEncaisse: 7200000 },
    { date: '10/09', commandes: 7, livraisons: 6, montantEncaisse: 8400000 },
    { date: '11/09', commandes: 7, livraisons: 6, montantEncaisse: 9600000 },
    { date: '12/09', commandes: 7, livraisons: 6, montantEncaisse: 8000000 }
  ];

  // --- Répartition des statuts de paiement ---
  paymentStatusData: PaymentStatusSlice[] = [
    { status: 'Payé', value: 72, color: '#22C55F' },
    { status: 'En attente', value: 18, color: '#FFE7C2' },
    { status: 'Échoué', value: 10, color: '#FFD3D3' }
  ];

  // --- Histogramme empilé livraisons ---
  livraisonsData: LivraisonStackPoint[] = [
    { date: '06/09', livres: 82, planifies: 15, retard: 3 },
    { date: '07/09', livres: 78, planifies: 18, retard: 4 },
    { date: '08/09', livres: 74, planifies: 20, retard: 6 },
    { date: '09/09', livres: 80, planifies: 16, retard: 4 },
    { date: '10/09', livres: 85, planifies: 12, retard: 3 }
  ];

  // --- Répartition des utilisateurs par rôle ---
  rolesData: RoleStat[] = [
    { role: 'Salariés', total: 40 },
    { role: 'Commerciaux', total: 16 },
    { role: 'Livreurs', total: 7 },
    { role: 'Responsable logistique', total: 13 }
  ];

  // --- Donut stocks global (identique au dashboard logistique) ---
  stockCategoryData = [
    { category: 'Ok', value: 75 },
    { category: 'Sous seuil', value: 15 },
    { category: 'Critique', value: 10 }
  ];

  // --- Série pour la tendance des coupons utilisés ---
  couponsTrendData: CouponTrendPoint[] = [
    { date: '06/09', value: 14 },
    { date: '07/09', value: 8 },
    { date: '08/09', value: 14 },
    { date: '09/09', value: 6 },
    { date: '10/09', value: 15 }
  ];

  periodOptions: string[] = ['Cette année', 'Ce trimestre', 'Ce mois'];
  sectorOptions: string[] = ['Tous', 'Distribution', 'Corporate'];

  // --- Références Chart.js (nettoyage facilité si besoin) ---
  private commandesChart?: any;
  private paiementsChart?: any;
  private livraisonsChart?: any;
  private rolesChart?: any;
  private stocksEtatChart?: any;
  private couponsChart?: any;

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    // Le léger délai garantit que les templates sont rendus avant d’initialiser Chart.js
    setTimeout(() => {
      this.loadChartJs();
    }, 120);
  }

  // Chargement lazy de Chart.js pour éviter de pénaliser le bundle initial
  async loadChartJs(): Promise<void> {
    try {
      const Chart = (await import('chart.js/auto')).default;
      this.initCommandesChart(Chart);
      this.initPaiementsChart(Chart);
      this.initLivraisonsChart(Chart);
      this.initRolesChart(Chart);
      this.initStocksEtatChart(Chart);
      this.initCouponsChart(Chart);
    } catch (error) {
      console.error('Erreur lors du chargement de Chart.js:', error);
    }
  }

  // --- Commandes & chiffre d'affaires ---
  initCommandesChart(Chart: any): void {
    const ctx = this.commandesChartRef?.nativeElement?.getContext('2d');
    if (!ctx) {
      return;
    }

    this.commandesChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: this.commandesChiffreData.map(point => point.date),
        datasets: [
          {
            label: 'Commandes fournisseurs',
            data: this.commandesChiffreData.map(point => point.commandes),
            backgroundColor: '#E0E7FF',
            hoverBackgroundColor: '#C7D2FE',
            categoryPercentage: 0.7,
            order: 2,
            yAxisID: 'y'
          },
          {
            label: 'Livraisons',
            data: this.commandesChiffreData.map(point => point.livraisons),
            backgroundColor: '#C1F3DC',
            hoverBackgroundColor: '#A5E9CD',
            categoryPercentage: 0.7,
            order: 3
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
            backgroundColor: '#03031999',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8,
            displayColors: true,
            boxPadding: 4
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            max: 8,
            grid: { color: '#F3F4F6' },
            ticks: { color: '#6B7280', font: { size: 12 }, padding: 10 }
          },
          x: {
            grid: { color: '#F3F4F6' },
            ticks: { color: '#6B7280', font: { size: 12 } },
            border: { display: false }
          }
        }
      }
    });
  }

  // --- Répartition des paiements (doughnut) ---
  initPaiementsChart(Chart: any): void {
    const ctx = this.paiementsChartRef?.nativeElement?.getContext('2d');
    if (!ctx) {
      return;
    }

    this.paiementsChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: this.paymentStatusData.map(slice => slice.status),
        datasets: [
          {
            data: this.paymentStatusData.map(slice => slice.value),
            backgroundColor: this.paymentStatusData.map(slice => slice.color),
            borderWidth: 0,
            hoverOffset: 4
          }
        ]
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
              padding: 20,
              boxWidth: 6,
              boxHeight: 6,
              color: '#2B3674',
              font: {
                size: 12,
                weight: '500'
              }
            }
          },
          tooltip: {
            backgroundColor: '#03031999',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8
          }
        }
      }
    });
  }

  // --- Histogramme empilé des livraisons ---
  initLivraisonsChart(Chart: any): void {
    const ctx = this.livraisonsChartRef?.nativeElement?.getContext('2d');
    if (!ctx) {
      return;
    }

    this.livraisonsChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: this.livraisonsData.map(point => point.date),
        datasets: [
          {
            label: 'Livrés',
            data: this.livraisonsData.map(point => point.livres),
            backgroundColor: '#22C55E',
            barThickness: 24
          },
          {
            label: 'Planifiés',
            data: this.livraisonsData.map(point => point.planifies),
            backgroundColor: '#E5E7EB',
            barThickness: 24
          },
          {
            label: 'Retard',
            data: this.livraisonsData.map(point => point.retard),
            backgroundColor: '#F97316',
            barThickness: 24
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
            position: 'bottom',
            labels: {
              usePointStyle: true,
              pointStyle: 'circle',
              padding: 20,
              boxWidth: 6,
              boxHeight: 6,
              color: '#2B3674',
              font: { size: 12 }
            }
          },
          tooltip: {
            backgroundColor: '#03031999',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8
          }
        },
        scales: {
          x: {
            stacked: true,
            grid: { display: false },
            ticks: { color: '#6B7280', font: { size: 12 } }
          },
          y: {
            stacked: true,
            beginAtZero: true,
            max: 100,
            grid: { color: '#F3F4F6' },
            ticks: {
              callback: (value: number) => `${value}%`,
              stepSize: 20,
              color: '#6B7280',
              font: { size: 12 }
              
            }
          }
        }
      }
    });
  }

  // --- Histogramme utilisateurs par rôle ---
  initRolesChart(Chart: any): void {
    const ctx = this.rolesChartRef?.nativeElement?.getContext('2d');
    if (!ctx) {
      return;
    }

    this.rolesChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: this.rolesData.map(role => role.role),
        datasets: [
          {
            data: this.rolesData.map(role => role.total),
            backgroundColor: '#4F46E5',
            barThickness: 60
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#03031999',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8,
            displayColors: false
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: '#6B7280', font: { size: 12 } }
          },
          y: {
            beginAtZero: true,
            grid: { display: false },
            ticks: { color: '#6B7280', font: { size: 12 }, stepSize: 10 }
          }
        }
      }
    });
  }

  // --- Donut stocks global (copie du composant logistique) ---
  initStocksEtatChart(Chart: any): void {
    const ctx = this.stocksEtatChartRef?.nativeElement?.getContext('2d');
    if (!ctx) {
      return;
    }

    this.stocksEtatChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: this.stockCategoryData.map(d => d.category),
        datasets: [{
          data: this.stockCategoryData.map(d => d.value),
          backgroundColor: ['#22C55F', '#FFE7C2', '#FFD3D3'],
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '65%',
        plugins: {
          legend: {
            display: true,
            position: 'bottom',
            labels: {
              usePointStyle: true,
              pointStyle: 'circle',
              padding: 20,
              boxWidth: 6,
              boxHeight: 6,
              color: '#2B3674',
              font: { size: 12 }
            }
          },
          tooltip: {
            backgroundColor: '#03031999',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8
          }
        }
      }
    });
  }

  // --- Tendance des coupons utilisés ---
  initCouponsChart(Chart: any): void {
    const ctx = this.couponsChartRef?.nativeElement?.getContext('2d');
    if (!ctx) {
      return;
    }

    this.couponsChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: this.couponsTrendData.map(point => point.date),
        datasets: [{
          data: this.couponsTrendData.map(point => point.value),
          borderColor: '#F97316',
          tension: 0.5,
          borderWidth: 2,
          pointRadius: 0,
          fill: false
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#03031999',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8,
            displayColors: false
          }
        },
        scales: {
          x: {
            grid: { color: '#F3F4F6' },
            ticks: { color: '#6B7280', font: { size: 12 } }
          },
          y: {
            beginAtZero: false,
            grid: { color: '#F3F4F6' },
            ticks: { color: '#6B7280', font: { size: 12 }, stepSize: 2 }
          }
        }
      }
    });
  }
  getTrendClass(trend: 'up' | 'down'): string {
    return trend === 'up' ? 'text-green-500' : 'text-red-500';
  }
}