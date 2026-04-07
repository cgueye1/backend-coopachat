import { Component, OnInit, AfterViewInit, ViewChild, ElementRef, PLATFORM_ID, Inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import {
  AdminService,
  AlertItemDTO,
  LivraisonParJourDTO,
  StatutTourneesDTO,
  StockEtatGlobalDTO,
  UserStatsByRoleItemDTO
} from '../../../shared/services/admin.service';


 
/** Une carte KPI du dashboard (titre, valeur, icône). */
interface MetricCard {
  title: string;
  value: string;
  icon: string;
}
/** Interface pour les données du graphique "Paiements par statut" */
interface PaymentStatusSlice {
  status: string;
  value: number;
  color: string;
}


/** Aligné sur LivraisonParJourDTO : planifies = nbPrevues (total prévu ce jour, hors annulées). */
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
  imports: [CommonModule, MainLayoutComponent, HeaderComponent, RouterModule],
  templateUrl: './dashboard.component.html'
})
export class AdminPageComponent implements OnInit, AfterViewInit {
  @ViewChild('commandesChart') commandesChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('paiementsChart') paiementsChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('rolesChart') rolesChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('stocksEtatChart') stocksEtatChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('deliveryStatusChart') deliveryStatusChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('couponsChart') couponsChartRef!: ElementRef<HTMLCanvasElement>;

  role: 'admin' = 'admin';

  /**
   * Données des 3 cartes KPI en haut de page.
   * Rempli au chargement par l'API GET /admin/dashboard/stats (loadDashboardStats).
   * Le HTML fait *ngFor="let metric of metricsData" et affiche title, value, icon.
   */
  metricsData: MetricCard[] = [
    { title: 'Commandes en attente', value: '', icon: '/icones/commandefour.svg' },
    { title: 'Paiements échoués', value: '', icon: '/icones/temps.svg' },
    { title: 'Réclamations à traiter', value: '', icon: '/icones/users.svg' }
  ];

  /** Donut « Paiements par statut » : rempli par loadDashboardStats (vide tant que l’API n’a pas répondu). */
  paymentStatusData: PaymentStatusSlice[] = [];


  /** Données du graphique « Livraisons par jour » (GET /admin/dashboard/livraisons-par-jour), affiché en grand en haut. */
  livraisonsData: LivraisonStackPoint[] = [];

  // [API] Utilisateurs par rôle : rempli par loadUsersStatsByRole() (GET /admin/users/stats/by-role)
  /** Données du graphique "Utilisateurs par rôle". role = libellé (ex. Salariés), total = effectif. */
  rolesData: RoleStat[] = [];

  // [API] Stocks - État global : rempli par loadStockEtatGlobal() (GET /admin/dashboard/stocks-etat-global)
  /**
   * Donut "Stocks - État global" : chaque élément a category (ex. "Normal") et value (nombre).
   * Type : tableau d’objets { category: string; value: number }  |  [] = tableau vide au départ.
   */
  stockCategoryData: { category: string; value: number }[] = [];

  /** Donut « Statut des livraisons » (tournées) — GET /admin/dashboard/statut-tournees. */
  deliveryStatusData: { status: string; value: number; color: string }[] = [];

  /** Données du graphique "Tendance des coupons utilisés" (remplies par l’API GET /admin/dashboard/coupons-utilises-par-jour). */
  couponsTrendData: CouponTrendPoint[] = [];

  /** Alertes du tableau de bord (livraisons en retard, stocks critiques). Rempli par loadAlerts() — GET /admin/alerts. */
  alertsData: AlertItemDTO[] = [];

  /** Loaders par graphique (API + Chart.js). */
  loadingChartLivraisons = true;
  loadingChartPaiements = true;
  loadingChartRoles = true;
  loadingChartStocksEtat = true;
  loadingChartDeliveryStatus = true;
  loadingChartCoupons = true;

  /** Références aux instances Chart.js (pour mise à jour du graphique paiements après l’API). */
  private commandesChart?: any;
  private paiementsChart?: any;
  private rolesChart?: any;
  private stocksEtatChart?: any;
  private deliveryStatusChart?: any;
  private couponsChart?: any;

  constructor(
    private adminService: AdminService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    this.loadDashboardStats();
    this.loadLivraisonsParJour();
    this.loadUsersStatsByRole();
    this.loadStockEtatGlobal();
    this.loadStatutTournees();
    this.loadCouponsUtilisesParJour();
    this.loadAlerts();
  }

  /** Appelle GET /admin/alerts et affiche les alertes (clic = redirection vers le module STOCKS ou LIVRAISONS). */
  loadAlerts(): void {
    this.adminService.getAlerts().subscribe({
      next: (res) => {
        this.alertsData = res.alerts || [];
      },
      error: (err) => console.error('Erreur chargement alertes:', err)
    });
  }

  /** Redirection au clic sur une alerte : STOCKS → /log/stocks, LIVRAISONS → /log/commandes. */
  goToModule(module: string): void {
    if (module === 'STOCKS') {
      this.router.navigate(['/log/stocks']);
    } else if (module === 'LIVRAISONS') {
      this.router.navigate(['/log/commandes']);
    }
  }

  /**
   * GET /admin/dashboard/livraisons-par-jour → graphique « Livraisons par jour » (même sémantique que le back).
   */
  loadLivraisonsParJour(): void {
    this.adminService.getLivraisonsParJour().pipe(
      finalize(() => { this.loadingChartLivraisons = false; })
    ).subscribe({
      next: (list: LivraisonParJourDTO[]) => {
        this.livraisonsData = list.map(d => ({
          date: d.date,
          livres: d.nbLivreesALaDate,
          planifies: d.nbPrevues,
          retard: d.nbRetard
        }));
        if (this.commandesChart) {
          const d = this.livraisonsData;
          const barTotals = d.map((p) => p.livres + p.planifies + p.retard);
          const yMax = (barTotals.length ? Math.max(...barTotals, 0) : 0) + 2;
          this.commandesChart.data.labels = d.map((p) => p.date);
          this.commandesChart.data.datasets[0].data = d.map((p) => p.planifies);
          this.commandesChart.data.datasets[1].data = d.map((p) => p.livres);
          this.commandesChart.data.datasets[2].data = d.map((p) => p.retard);
          if (this.commandesChart.options?.scales?.y) {
            this.commandesChart.options.scales.y.max = yMax;
          }
          this.commandesChart.update();
        }
      },
      error: (err) => console.error('Erreur chargement livraisons par jour:', err)
    });
  }

  // [API] GET /admin/users/stats/by-role → effectifs par rôle (Salariés, Commerciaux, etc.)
  /** Remplit rolesData puis met à jour le graphique "Utilisateurs par rôle" s’il est déjà créé. */
  loadUsersStatsByRole(): void {
    this.adminService.getUsersStatsByRole().subscribe({
      next: (list: UserStatsByRoleItemDTO[]) => {
        // r = un rôle dans la liste API. On passe au format graphique : roleLabel → role, count → total
        this.rolesData = list.map(r => ({
          role: r.roleLabel,
          total: r.count
        }));
        // Si le graphique est déjà créé, on met à jour les labels (noms des rôles) et les barres (effectifs)
        if (this.rolesChart) {
          this.rolesChart.data.labels = this.rolesData.map(p => p.role);//on met à jour les labels du graphique pour chaque rôle on prend le libellé du rôle
          this.rolesChart.data.datasets[0].data = this.rolesData.map(p => p.total);//on met à jour les données du graphique pour chaque rôle on prend le nombre d’utilisateurs pour ce rôle
          this.rolesChart.update();
        }
        this.loadingChartRoles = false;
      },
      error: (err) => {
        console.error('Erreur chargement utilisateurs par rôle:', err);
        this.loadingChartRoles = false;
      }
    });
  }

  // [API] GET /admin/dashboard/stocks-etat-global → Normal, Sous seuil, Critique
  /** Remplit stockCategoryData puis met à jour le donut "Stocks - État global" s’il est déjà créé. */
  loadStockEtatGlobal(): void {
    this.adminService.getStockEtatGlobal().subscribe({
      next: (data: StockEtatGlobalDTO) => {
        // Passage du format API (normal, sousSeuil, critique) vers le format donut (category, value)
        this.stockCategoryData = [
          { category: 'Normal', value: data.normal },
          { category: 'Sous seuil', value: data.sousSeuil },
          { category: 'Rupture', value: data.critique }
        ];
        // Si le graphique est déjà créé, on met à jour les labels (catégories) et les données (effectifs)
        if (this.stocksEtatChart) {
          this.stocksEtatChart.data.labels = this.stockCategoryData.map(d => d.category);//on met à jour les labels du graphique pour chaque catégorie on prend le nom de la catégorie
          this.stocksEtatChart.data.datasets[0].data = this.stockCategoryData.map(d => d.value);//on met à jour les données du graphique pour chaque catégorie on prend le nombre de stocks pour cette catégorie
          this.stocksEtatChart.update();
        }
        this.loadingChartStocksEtat = false;
      },
      error: (err) => {
        console.error('Erreur chargement stocks état global:', err);
        this.loadingChartStocksEtat = false;
      }
    });
  }

  /** GET /admin/dashboard/statut-tournees — même logique que le dashboard RL (Assignée, En cours, Terminée, Annulée). */
  loadStatutTournees(): void {
    this.adminService.getStatutTournees().pipe(
      finalize(() => { this.loadingChartDeliveryStatus = false; })
    ).subscribe({
      next: (data: StatutTourneesDTO) => {
        const parStatut = data.parStatut || {};
        const statusConfig: Record<string, { label: string; color: string }> = {
          ASSIGNEE: { label: 'Assignée', color: '#22C55E' },
          EN_COURS: { label: 'En cours', color: '#FFE7C2' },
          TERMINEE: { label: 'Terminée', color: '#4F46E5' },
          ANNULEE: { label: 'Annulée', color: '#FFD3D3' }
        };
        this.deliveryStatusData = Object.entries(statusConfig).map(([key, { label, color }]) => ({
          status: label,
          value: Number(parStatut[key] ?? 0),
          color
        }));
        if (this.deliveryStatusChart) {
          this.deliveryStatusChart.data.labels = this.deliveryStatusData.map((d) => d.status);
          this.deliveryStatusChart.data.datasets[0].data = this.deliveryStatusData.map((d) => d.value);
          this.deliveryStatusChart.data.datasets[0].backgroundColor = this.deliveryStatusData.map((d) => d.color);
          this.deliveryStatusChart.update();
        }
      },
      error: (err) => console.error('Erreur chargement statut des livraisons:', err)
    });
  }

  /**
   * Appelle l’API GET /admin/dashboard/coupons-utilises-par-jour et met à jour le graphique "Tendance des coupons utilisés".
   */
  loadCouponsUtilisesParJour(): void {
    this.adminService.getCouponsUtilisesParJour().subscribe({
      next: (list) => {
        this.couponsTrendData = list.map(d => ({ date: d.date, value: d.nbUtilisations }));
        if (this.couponsChart) {
          this.couponsChart.data.labels = this.couponsTrendData.map(p => p.date);
          this.couponsChart.data.datasets[0].data = this.couponsTrendData.map(p => p.value);
          const yMax = this.couponsChartYMax();
          if (this.couponsChart.options?.scales?.y) {
            this.couponsChart.options.scales.y.max = yMax;
          }
          this.couponsChart.update();
        }
        this.loadingChartCoupons = false;
      },
      error: (err) => {
        console.error('Erreur chargement coupons par jour:', err);
        this.loadingChartCoupons = false;
      }
    });
  }

  /**
   * Appelle l'API GET /admin/dashboard/stats et met à jour les 3 KPIs + le graphique "Paiements par statut".
   */
  loadDashboardStats(): void {
    this.adminService.getDashboardStats().subscribe({
      next: (data) => {
        // 1) Mettre à jour les 3 cartes KPI
        this.metricsData[0].value = String(data.commandesEnAttente);
        this.metricsData[1].value = String(data.paiementsEchoues);
        this.metricsData[2].value = String(data.reclamationsOuvertes);

        // 2) Mapper la réponse "paiements par statut" vers le format du graphique (status, value, color)
        const colors: Record<string, string> = {
          'Payé': '#22C55F',//couleur du statut Payé
          'Impayé': '#FFE7C2',//couleur du statut Impayé
          'Échoué': '#FFD3D3',//couleur du statut Échoué
        };
        //on mappe la réponse "paiements par statut" vers le format du graphique (status, value, color)
        this.paymentStatusData = data.paiementsParStatut.map(item => ({
          status: item.statusLabel,//statut du paiement
          value: item.count,//nombre de paiements pour ce statut
          color: colors[item.statusLabel] ?? '#E5E7EB'//couleur du statut si le statut n'est pas trouvé on prend la couleur par défaut #E5E7EB
        }));

        // 3) Si le graphique paiements est déjà créé, le mettre à jour
        if(this.paiementsChart) {
          this.paiementsChart.data.labels = this.paymentStatusData.map(s => s.status);//on met à jour les labels du graphique pour chaque paiement on prend le statut
          this.paiementsChart.data.datasets[0].data = this.paymentStatusData.map(s => s.value);//on met à jour les données du graphique pour chaque paiement on prend le nombre de paiements pour ce statut
          this.paiementsChart.data.datasets[0].backgroundColor = this.paymentStatusData.map(s => s.color);//on met à jour les couleurs du graphique pour chaque paiement on prend la couleur pour ce statut
          this.paiementsChart.update();//on met à jour le graphique
        }
        this.loadingChartPaiements = false;
      },
      //si erreur on affiche l'erreur dans la console
      error: (err) => {
        console.error('Erreur chargement stats dashboard:', err);
        this.loadingChartPaiements = false;
      }
    });
  }

  ngAfterViewInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
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
      this.initRolesChart(Chart);
      this.initStocksEtatChart(Chart);
      this.initDeliveryStatusChart(Chart);
      this.initCouponsChart(Chart);
    } catch (error) {
      console.error('Erreur lors du chargement de Chart.js:', error);
    }
  }

  // --- Livraisons par jour (barres groupées) : livraisonsData, GET /admin/dashboard/livraisons-par-jour ---
  initCommandesChart(Chart: any): void {
    const ctx = this.commandesChartRef?.nativeElement?.getContext('2d');
    if (!ctx) {
      return;
    }

    const d = this.livraisonsData;
    const barTotals = d.map((p) => p.livres + p.planifies + p.retard);
    const yMax = (barTotals.length ? Math.max(...barTotals, 0) : 0) + 2;

    this.commandesChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: d.map((p) => p.date),
        datasets: [
          {
            label: 'Prévues ce jour',
            data: d.map((p) => p.planifies),
            backgroundColor: '#E5E7EB',
            borderColor: '#fff',
            borderWidth: 1,
            categoryPercentage: 0.7,
            barPercentage: 0.9
          },
          {
            label: 'Livrées ce jour',
            data: d.map((p) => p.livres),
            backgroundColor: '#22C55E',
            borderColor: '#fff',
            borderWidth: 1,
            categoryPercentage: 0.7,
            barPercentage: 0.9
          },
          {
            label: 'En retard',
            data: d.map((p) => p.retard),
            backgroundColor: '#F97316',
            borderColor: '#fff',
            borderWidth: 1,
            categoryPercentage: 0.7,
            barPercentage: 0.9
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
          x: {
            stacked: false,
            grid: { color: '#F3F4F6' },
            ticks: { color: '#6B7280', font: { size: 12 } },
            border: { display: false }
          },
          y: {
            stacked: false,
            beginAtZero: true,
            max: yMax,
            grid: { color: '#F3F4F6' },
            ticks: {
              color: '#6B7280',
              font: { size: 12 },
              padding: 10,
              stepSize: 1,
              callback: (value: string | number) => (Number.isInteger(Number(value)) ? value : '')
            }
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
            borderWidth: 1,
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

  // --- Graphique Utilisateurs par rôle (données = rolesData, rempli par l’API GET users/stats/by-role) ---
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
            // Pas de max fixe : si tu as 25, 50, 100 salariés, l’axe Y s’adapte tout seul en mettant taille à 10
            ticks: { color: '#6B7280', font: { size: 12 }, stepSize: 10 }
          }
        }
      }
    });
  }

  // --- Donut Stocks - État global (données = stockCategoryData, rempli par l’API GET stocks-etat-global) ---
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
          borderWidth: 1
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

  // --- Donut Statut des livraisons (tournées) — même schéma que le dashboard RL ---
  initDeliveryStatusChart(Chart: any): void {
    const ctx = this.deliveryStatusChartRef?.nativeElement?.getContext('2d');
    if (!ctx) {
      return;
    }

    this.deliveryStatusChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: this.deliveryStatusData.map((d) => d.status),
        datasets: [{
          data: this.deliveryStatusData.map((d) => d.value),
          backgroundColor: this.deliveryStatusData.map((d) => d.color),
          borderWidth: 1
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

  /** Même principe que « Livraisons par jour » : max des valeurs réelles + marge 2. */
  private couponsChartYMax(): number {
    const vals = this.couponsTrendData.map((p) => p.value);
    return (vals.length ? Math.max(...vals, 0) : 0) + 2;
  }

  // --- Tendance des coupons utilisés ---
  initCouponsChart(Chart: any): void {
    const ctx = this.couponsChartRef?.nativeElement?.getContext('2d');
    if (!ctx) {
      return;
    }

    const yMax = this.couponsChartYMax();

    this.couponsChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: this.couponsTrendData.map(point => point.date),
        datasets: [{
          data: this.couponsTrendData.map(point => point.value),
          borderColor: '#F97316',
          tension: 0.4,
          cubicInterpolationMode: 'monotone', // ← CLÉ pour des courbes naturelles
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
            beginAtZero: true,
            max: yMax,
            grid: { color: '#F3F4F6' },
            ticks: {
              color: '#6B7280',
              font: { size: 12 },
              stepSize: 1,
              precision: 0,
              callback: (raw: number | string) => String(Math.round(Number(raw)))
            }
          }
        }
      }
    });
  }
}