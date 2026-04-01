import { Component, OnInit, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { CommercialService } from '../../../shared/services/commercial.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

interface MetricCard {
  title: string;
  value: string;
  changePrimary: string; // Partie 1: chiffre, actives, etc.
  changeSecondary: string; // Partie 2: description
  changeType: 'positive' | 'negative';
  icon: string;
}

interface Prospect {
  id?: number | string;
  company: string;
  contact: string;
  status: string;
  statusColor: string;
  date: string;
}

@Component({
  selector: 'app-commercial-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, MainLayoutComponent, HeaderComponent],
  templateUrl: 'dashboard.component.html',
  styleUrls: ['dashboard.component.css']
})

export class DashboardComponent implements OnInit, AfterViewInit {
  @ViewChild('salesChart', { static: false }) salesChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('commandesChart', { static: false }) commandesChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('couponsChart', { static: false }) couponsChartRef!: ElementRef<HTMLCanvasElement>;

  role: 'com' = 'com';
  private salesChart?: Chart;
  private commandesChart?: Chart;
  private couponsChart?: Chart;
  evolutionVentes: { mois: string; montant: number }[] = [];
  evolutionCommandes: { mois: string; nbCommandes: number }[] = [];
  couponsTrendData: { date: string; value: number }[] = [];

  /** Graphiques ventes + commandes (API KPIs). */
  loadingChartKpis = true;
  /** Graphique tendance coupons. */
  loadingChartCoupons = true;

  metricsData: MetricCard[] = [
    { title: 'Salariés', value: '—', changePrimary: '—', changeSecondary: 'Actifs', changeType: 'positive', icon: 'icones/salaries2.svg' },
    { title: 'Nouveaux salariés', value: '—', changePrimary: '—', changeSecondary: 'Ce mois', changeType: 'positive', icon: 'icones/salaries2.svg' },
    { title: 'Commandes', value: '—', changePrimary: '—', changeSecondary: 'Ce mois (tous statuts)', changeType: 'positive', icon: 'icones/commandefour.svg' },
    { title: 'Ventes', value: '—', changePrimary: '—', changeSecondary: 'Ce mois', changeType: 'positive', icon: 'icones/vente.svg' },
    { title: 'Coupon', value: '—', changePrimary: '—', changeSecondary: 'Actives', changeType: 'positive', icon: 'icones/promo.svg' }
  ];

  recentProspects: Prospect[] = [];

  constructor(
    private commercialService: CommercialService,
    private router: Router
  ) {}

  /** Charge les derniers prospects (API) pour le bloc « Prospects récents » du tableau de bord. */
  loadLastProspects(): void {
    this.commercialService.getLastProspects(3).subscribe({
      next: (list: any[]) => {
        this.recentProspects = (list ?? []).map((c: any) => this.mapCompanyToProspect(c));
      },
      error: () => {
        this.recentProspects = [];
      }
    });
  }

  private mapCompanyToProspect(company: any): Prospect {
    const name = company?.name ?? '';
    const status = (company?.status ?? '').trim() || 'En attente';
    const statusColor = this.getProspectionStatusColor(status);
    const contactName = company?.contactName ?? '';
    const createdAt = company?.createdAt ?? '';
    const date = createdAt.includes(' ') ? createdAt.split(' ')[0] : createdAt;
    return {
      id: company?.id,
      company: name,
      contact: contactName,
      status,
      statusColor,
      date
    };
  }

  /** Navigation vers la page prospections avec ouverture du détail du prospect (query param id). */
  goToProspectDetails(prospect: Prospect): void {
    if (prospect?.id != null) {
      this.router.navigate(['/com/prospections'], { queryParams: { id: prospect.id } });
    } else {
      this.router.navigate(['/com/prospections']);
    }
  }

  private getProspectionStatusColor(status: string): string {
    const s = status.toLowerCase();
    if (s.includes('refusée') || s.includes('refusee')) return 'red';
    if (s.includes('partenaire') || s.includes('signé')) return 'green';
    if (s.includes('intéressée') || s.includes('interessee')) return 'green';
    if (s.includes('relanc')) return 'yellow';
    if (s.includes('rendez-vous')) return 'blue';
    return 'gray';
  }

  ngOnInit(): void {
    this.loadDashboardKpis();
    this.loadCouponsUtilisesParJour();
    this.loadLastProspects();
  }

  /** Charge les données du graphique « Tendance des coupons utilisés » (GET /api/commercial/dashboard/coupons-utilises-par-jour). */
  loadCouponsUtilisesParJour(): void {
    this.commercialService.getCouponsUtilisesParJour().subscribe({
      next: (list) => {
        this.couponsTrendData = list.map(d => ({ date: d.date, value: d.nbUtilisations }));
        if (this.couponsChart) {
          this.couponsChart.data.labels = this.couponsTrendData.map(p => p.date);
          this.couponsChart.data.datasets[0].data = this.couponsTrendData.map(p => p.value);
          const vals = this.couponsTrendData.map(p => p.value);
          const dataMax = vals.length ? Math.max(...vals, 1) : 1;
          const yMax = dataMax + 2;
          const yScale = this.couponsChart.options?.scales?.['y'];
          if (yScale && typeof yScale === 'object') {
            const scale = yScale as { max?: number; grace?: number };
            scale.max = yMax;
            scale.grace = 0;
          }
          this.couponsChart.update('active');
        }
        this.loadingChartCoupons = false;
      },
      error: (err) => {
        console.error('Erreur chargement coupons par jour:', err);
        this.loadingChartCoupons = false;
      }
    });
  }

  /** Charge les KPIs du tableau de bord (GET /api/commercial/dashboard/kpis) et met à jour les cartes + graphiques. */
  loadDashboardKpis(): void {
    this.loadingChartKpis = true;
    this.commercialService.getDashboardKpis().subscribe({
      next: (kpis) => {
        const fmtPct = (v: number | null) => v == null ? '—' : (v >= 0 ? '+' : '') + Math.round(v) + '%';
        const fmtFcfa = (v: number) => (v ?? 0).toLocaleString('fr-FR', { style: 'decimal', maximumFractionDigits: 0 }) + ' FCFA';
        this.metricsData = [
          { title: 'Salariés', value: String(kpis.totalSalaries), changePrimary: 'Actifs', changeSecondary: '', changeType: 'positive', icon: 'icones/salaries2.svg' },
          { title: 'Nouveaux salariés', value: String(kpis.nouveauxSalariesCeMois), changePrimary: 'Ce mois', changeSecondary: '', changeType: 'positive', icon: 'icones/salaries2.svg' },
          { title: 'Commandes', value: String(kpis.commandesCeMois), changePrimary: fmtPct(kpis.evolutionCommandesPct), changeSecondary: 'Ce mois (tous statuts) · Évolution vs mois dernier', changeType: (kpis.evolutionCommandesPct ?? 0) >= 0 ? 'positive' : 'negative', icon: 'icones/commandefour.svg' },
          { title: 'Ventes', value: fmtFcfa(kpis.ventesCeMois), changePrimary: fmtPct(kpis.evolutionVentesPct), changeSecondary: 'Ce mois · Évolution vs mois dernier', changeType: (kpis.evolutionVentesPct ?? 0) >= 0 ? 'positive' : 'negative', icon: 'icones/vente.svg' },
          { title: 'Coupon', value: String(kpis.promotionsActives), changePrimary: 'Actives', changeSecondary: '', changeType: 'positive', icon: 'icones/promo.svg' }
        ];
        this.evolutionVentes = (kpis.evolutionVentes ?? []).map(v => ({
          mois: v.mois,
          montant: typeof v.montant === 'number' ? v.montant : Number(v.montant) || 0
        }));
        this.evolutionCommandes = (kpis.evolutionCommandes ?? []).map(c => ({
          mois: c.mois,
          nbCommandes: typeof c.nbCommandes === 'number' ? c.nbCommandes : Number(c.nbCommandes) || 0
        }));
        this.createOrUpdateSalesAndCommandesCharts();
        this.loadingChartKpis = false;
      },
      error: (err) => {
        console.error('Erreur chargement KPIs dashboard commercial:', err);
        this.loadingChartKpis = false;
      }
    });
  }

  /** Crée ou met à jour les graphiques « Évolution des ventes » et « Nombre de commandes » avec les données API (evolutionVentes, evolutionCommandes). */
  private createOrUpdateSalesAndCommandesCharts(): void {
    if (this.salesChart) {
      this.salesChart.destroy();
      this.salesChart = undefined;
    }
    if (this.commandesChart) {
      this.commandesChart.destroy();
      this.commandesChart = undefined;
    }
    this.createSalesChart();
    this.createCommandesChart();
  }

  ngAfterViewInit(): void {
    this.createCouponsChart();
    this.createSalesChart();
    this.createCommandesChart();
  }

  private createCouponsChart(): void {
    if (!this.couponsChartRef?.nativeElement) return;
    const ctx = this.couponsChartRef.nativeElement.getContext('2d');
    if (!ctx) return;
    const labels = this.couponsTrendData.length ? this.couponsTrendData.map(p => p.date) : [];
    const data = this.couponsTrendData.length ? this.couponsTrendData.map(p => p.value) : [];
    const dataMax = data.length ? Math.max(...data, 1) : 1;
    const maxVal = dataMax + 2;
    this.couponsChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          data,
          borderColor: '#F97316',
          tension: 0.4,
          cubicInterpolationMode: 'monotone',
          borderWidth: 2,
          pointRadius: 0,
          fill: false
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        layout: {
          padding: { top: 4, right: 4, bottom: 4, left: 4 }
        },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8,
            displayColors: false,
            callbacks: {
              label: (ctx) => ctx.parsed.y + ' utilisation(s)'
            }
          }
        },
        scales: {
          x: {
            display: true,
            grid: { display: false },
            ticks: { color: '#6B7280', font: { size: 11 } }
          },
          y: {
            beginAtZero: true,
            display: true,
            min: 0,
            max: maxVal,
            grace: 0,
            grid: { display: false },
            ticks: {
              color: '#6B7280',
              font: { size: 11 },
              stepSize: 1,
              callback: (value: string | number) => Number.isInteger(Number(value)) ? value : ''
            }
          }
        }
      }
    });
  }

  private createSalesChart(): void {
    const ctx = this.salesChartRef?.nativeElement?.getContext('2d');
    if (!ctx) return;
    const labels = this.evolutionVentes.length ? this.evolutionVentes.map(v => v.mois) : ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Juil'];
    const data = this.evolutionVentes.length ? this.evolutionVentes.map(v => v.montant) : [4000, 3000, 5000, 2900, 1700, 2600, 3500];
    const dataMax = data.length ? Math.max(...data, 0) : 1;
    const yMax = Math.ceil((dataMax || 1) * 1.25);

    this.salesChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Montant (FCFA)',
          data: data,
          borderColor: '#4B5FD4',
          backgroundColor: 'transparent',
          borderWidth: 2,
          pointBackgroundColor: '#4B5FD4',
          pointBorderColor: '#fff',
          pointBorderWidth: 1,
          pointRadius: 3,
          pointHoverRadius: 5,
          tension: 0.4,
          fill: false
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
              color: '#374151',
              font: { size: 12 },
              usePointStyle: true,
              padding: 16
            }
          },
          tooltip: {
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            titleColor: '#ffffff',
            bodyColor: '#ffffff',
            borderColor: '#6b7280',
            borderWidth: 1,
            displayColors: true,
            callbacks: {
              label: function (context: any) {
                return context.parsed.y.toLocaleString('fr-FR') + ' FCFA';
              }
            }
          }
        },
        scales: {
          x: {
            grid: {
              display: true,
              color: 'rgba(0,0,0,0.06)',
              drawBorder: false,
              borderDash: [4, 4]
            },
            ticks: {
              color: '#6b7280',
              font: {
                size: 11
              },
              padding: 8
            },
          },
          y: {
            beginAtZero: true,
            min: 0,
            max: yMax,
            grace: 0,
            grid: {
              display: true,
              color: 'rgba(0,0,0,0.06)',
              drawBorder: false,
              borderDash: [4, 4]
            },
            ticks: {
              color: '#6b7280',
              font: { size: 11 },
              padding: 8,
              callback: function (value) {
                return typeof value === 'number' ? value.toLocaleString('fr-FR') : value;
              }
            }
          }
        },
        elements: {
          line: {
            borderJoinStyle: 'round',
            borderCapStyle: 'round'
          }
        },
        layout: {
          padding: {
            top: 10,
            right: 10,
            bottom: 10,
            left: 10
          }
        }
      }
    });
  }

  private createCommandesChart(): void {
    const el = this.commandesChartRef?.nativeElement;
    if (!el) return;
    const ctx = el.getContext('2d');
    if (!ctx) return;
    const labels = this.evolutionCommandes.length ? this.evolutionCommandes.map(c => c.mois) : ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Juil'];
    const data = this.evolutionCommandes.length ? this.evolutionCommandes.map(c => c.nbCommandes) : [20, 17, 30, 14, 10, 14, 20];
    const dataMax = data.length ? Math.max(...data, 1) : 1;
    const yMax = dataMax + 2;
    this.commandesChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: 'Commandes',
          data,
          backgroundColor: '#4B5FD4',
          borderColor: '#4B5FD4',
          borderWidth: 0
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
              color: '#374151',
              font: { size: 12 },
              usePointStyle: true,
              pointStyle: 'rect',
              padding: 16
            }
          },
          tooltip: {
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            titleColor: '#ffffff',
            bodyColor: '#ffffff',
            displayColors: true,
            callbacks: {
              label: (ctx) => ctx.parsed.y + ' commande(s)'
            }
          }
        },
        scales: {
          x: {
            grid: {
              display: true,
              color: 'rgba(0,0,0,0.06)',
              drawBorder: false,
              borderDash: [4, 4]
            },
            ticks: { color: '#6b7280', font: { size: 11 }, padding: 8 }
          },
          y: {
            beginAtZero: true,
            min: 0,
            max: yMax,
            grace: 0,
            grid: {
              display: true,
              color: 'rgba(0,0,0,0.06)',
              drawBorder: false,
              borderDash: [4, 4]
            },
            ticks: { color: '#6b7280', font: { size: 11 }, padding: 8, stepSize: 1 }
          }
        },
        layout: { padding: { top: 10, right: 10, bottom: 10, left: 10 } }
      }
    });
  }

  getStatusClass(color: string): string {
    const classes: Record<string, string> = {
      yellow: 'bg-[#FEF9C3] text-[#854D0E]',
      blue: 'bg-[#DBEAFE] text-[#1E40AF]',
      green: 'bg-[#DCFCE7] text-[#166534]',
      red: 'bg-[#FEE2E2] text-[#991B1B]',
      gray: 'bg-gray-100 text-gray-800'
    };
    return classes[color] ?? 'bg-gray-100 text-gray-800';
  }

  ngOnDestroy(): void {
    this.salesChart?.destroy();
    this.commandesChart?.destroy();
    this.couponsChart?.destroy();
  }
}