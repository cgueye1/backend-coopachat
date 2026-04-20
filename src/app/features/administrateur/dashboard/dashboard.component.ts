import { Component, OnInit, AfterViewInit, ViewChild, ElementRef, PLATFORM_ID, Inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import {
  AdminService,
  AlertItemDTO,
  LivraisonParJourDTO,
  StatutTourneesDTO,
  StockEtatGlobalDTO
} from '../../../shared/services/admin.service';
import { ProductService } from '../../../shared/services/product.service';


 
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

interface CouponTrendPoint {
  date: string;
  value: number;
}

/** Ordre fixe des rôles pour le graphique "Utilisateurs par rôle". */
const USERS_ROLE_ORDER: Array<{ key: string; label: string }> = [
  { key: 'EMPLOYEE', label: 'Salarié' },
  { key: 'COMMERCIAL', label: 'Commercial' },
  { key: 'LOGISTICS_MANAGER', label: 'Responsable Logistique' },
  { key: 'DELIVERY_DRIVER', label: 'Livreur' },
  { key: 'SUPPLIER', label: 'Fournisseur' },
  { key: 'ADMINISTRATOR', label: 'Administrateur' }
];

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [CommonModule, MainLayoutComponent, HeaderComponent, RouterModule],
  templateUrl: './dashboard.component.html'
})
export class AdminPageComponent implements OnInit, AfterViewInit {
  @ViewChild('commandesChart') commandesChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('paiementsChart') paiementsChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('usersRolePctChart') usersRolePctChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('usersStatusDonutChart') usersStatusDonutChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('stocksEtatChart') stocksEtatChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('deliveryStatusChart') deliveryStatusChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('couponsChart') couponsChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('top5ProductsChart') top5ProductsChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('productsRepartitionChart') productsRepartitionChartRef!: ElementRef<HTMLCanvasElement>;

  role: 'admin' = 'admin';

  /** Afficher les graphiques produits uniquement en navigateur (Chart.js). */
  isBrowser = false;

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

  /** Graphiques utilisateurs (%, ex-page liste utilisateurs) : GET by-role + by-status. */
  usersRolePctLabels: string[] = [];
  usersRolePctData: number[] = [];
  usersStatusPct: [number, number] = [0, 0];

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

  /**
   * Loaders par graphique (API + Chart.js).
   * `public` + type explicite : requis pour strictTemplates / ngtsc (bindings dans le HTML).
   */
  public loadingChartLivraisons: boolean = true;
  public loadingChartPaiements: boolean = true;
  public loadingChartUsersRolePct: boolean = true;
  public loadingChartUsersStatus: boolean = true;
  public loadingChartStocksEtat: boolean = true;
  public loadingChartDeliveryStatus: boolean = true;
  public loadingChartCoupons: boolean = true;
  public loadingChartTop5Products: boolean = true;
  public loadingChartProductsRepartition: boolean = true;

  /** Données graphiques « Top 5 produits commandés » et « Répartition des produits » (API Product). */
  top5ProductLabels: string[] = [];
  top5ProductData: number[] = [];
  productsRepartitionPct: [number, number] = [0, 0];

  /** Références aux instances Chart.js (pour mise à jour du graphique paiements après l’API). */
  private commandesChart?: any;
  private paiementsChart?: any;
  private usersRolePctChart?: any;
  private usersStatusDonutChart?: any;
  private stocksEtatChart?: any;
  private deliveryStatusChart?: any;
  private couponsChart?: any;
  private top5ProductsChart?: any;
  private productsRepartitionChart?: any;

  constructor(
    private adminService: AdminService,
    private productService: ProductService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit(): void {
    this.loadDashboardStats();
    this.loadLivraisonsParJour();
    this.loadUsersAdminCharts();
    this.loadStockEtatGlobal();
    this.loadStatutTournees();
    this.loadCouponsUtilisesParJour();
    this.loadAlerts();
    this.loadTop5ProductsUsage();
    this.loadProductCatalogStats();
  }

  /** GET top 5 produits les plus commandés (%), même source que l’ancien écran Catalogues. */
  loadTop5ProductsUsage(): void {
    this.loadingChartTop5Products = true;
    this.productService.getTop5ProductsUsage().pipe(
      finalize(() => { this.loadingChartTop5Products = false; })
    ).subscribe({
      next: (list) => {
        this.top5ProductLabels = list?.length
          ? list.map((x: { productName: string }) => x.productName)
          : [];
        this.top5ProductData = list?.length
          ? list.map((x: { usagePercent: number }) => x.usagePercent)
          : [];
        if (this.top5ProductsChart) {
          this.top5ProductsChart.data.labels = this.top5ProductLabels;
          this.top5ProductsChart.data.datasets[0].data = this.top5ProductData;
          this.top5ProductsChart.update();
        }
      },
      error: (err) => {
        console.error('Erreur chargement top 5 produits:', err);
        this.top5ProductLabels = [];
        this.top5ProductData = [];
        if (this.top5ProductsChart) {
          this.top5ProductsChart.data.labels = [];
          this.top5ProductsChart.data.datasets[0].data = [];
          this.top5ProductsChart.update();
        }
      }
    });
  }

  /** Stats catalogue : répartition Actifs / Inactifs en % (donut). */
  loadProductCatalogStats(): void {
    this.productService.getProductStats().subscribe({
      next: (stats) => {
        const total = stats?.totalProducts ?? 0;
        const actifs = stats?.activeProducts ?? 0;
        const inactifs = stats?.inactiveProducts ?? 0;
        const pctActifs = total > 0 ? Math.round((actifs / total) * 1000) / 10 : 0;
        const pctInactifs = total > 0 ? Math.round((inactifs / total) * 1000) / 10 : 0;
        this.productsRepartitionPct = [pctActifs, pctInactifs];
        if (this.productsRepartitionChart) {
          this.productsRepartitionChart.data.datasets[0].data = [pctActifs, pctInactifs];
          this.productsRepartitionChart.update();
        }
        this.loadingChartProductsRepartition = false;
      },
      error: (err) => {
        console.error('Erreur chargement stats produits:', err);
        this.loadingChartProductsRepartition = false;
      }
    });
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

  /** GET /admin/users/stats/by-role (%) + /by-status → barres horizontales + donut (ex-liste utilisateurs). */
  loadUsersAdminCharts(): void {
    this.loadingChartUsersRolePct = true;
    this.loadingChartUsersStatus = true;
    forkJoin({
      byRole: this.adminService.getUsersStatsByRole(),
      byStatus: this.adminService.getUsersStatsByStatus()
    }).pipe(
      finalize(() => {
        this.loadingChartUsersRolePct = false;
        this.loadingChartUsersStatus = false;
      })
    ).subscribe({
      next: ({ byRole, byStatus }) => {
        const byRoleList = Array.isArray(byRole) ? byRole : [];
        const pctByRole = new Map<string, number>();
        for (const item of byRoleList) {
          const roleKey = String((item as { role?: string }).role ?? '').trim().toUpperCase();
          const roleLabel = String((item as { roleLabel?: string }).roleLabel ?? '').trim().toLowerCase();
          const pct = Number((item as { percentage?: number }).percentage ?? 0) || 0;
          if (roleKey) pctByRole.set(roleKey, pct);
          // Fallback si l'API renvoie uniquement un libellé.
          if (!roleKey && roleLabel.includes('fournisseur')) pctByRole.set('SUPPLIER', pct);
        }

        this.usersRolePctLabels = USERS_ROLE_ORDER.map(r => r.label);
        this.usersRolePctData = USERS_ROLE_ORDER.map(r => pctByRole.get(r.key) ?? 0);
        if (this.usersRolePctChart) {
          this.usersRolePctChart.data.labels = this.usersRolePctLabels;
          this.usersRolePctChart.data.datasets[0].data = this.usersRolePctData;
          this.usersRolePctChart.update();
        }
        const actifs = (byStatus || []).find(x => /actif/i.test(x.label ?? ''));
        const inactifs = (byStatus || []).find(x => /inactif/i.test(x.label ?? ''));
        const a = actifs != null ? Number(actifs.percentage) : 0;
        const i = inactifs != null ? Number(inactifs.percentage) : 0;
        this.usersStatusPct = [a, i];
        if (this.usersStatusDonutChart) {
          this.usersStatusDonutChart.data.datasets[0].data = [a, i];
          this.usersStatusDonutChart.update();
        }
      },
      error: (err) => console.error('Erreur chargement graphiques utilisateurs:', err)
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
      this.initStocksEtatChart(Chart);
      this.initDeliveryStatusChart(Chart);
      this.initCouponsChart(Chart);
      this.initTop5ProductsChart(Chart);
      this.initProductsRepartitionChart(Chart);
      this.initUsersRolePctChart(Chart);
      this.initUsersStatusDonutChart(Chart);
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

  // --- Utilisateurs par rôle (barres horizontales, % — indigo) ---
  initUsersRolePctChart(Chart: any): void {
    const ctx = this.usersRolePctChartRef?.nativeElement?.getContext('2d');
    if (!ctx) {
      return;
    }

    this.usersRolePctChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: this.usersRolePctLabels,
        datasets: [
          {
            data: this.usersRolePctData,
            backgroundColor: '#4F46E5',
            hoverBackgroundColor: '#4338CA',
            barThickness: 30
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y',
        plugins: {
          legend: {
            display: true,
            position: 'top',
            align: 'start',
            labels: {
              boxWidth: 40,
              boxHeight: 12,
              padding: 15,
              font: { size: 12 },
              generateLabels: () => [
                { text: 'Utilisation(%)', fillStyle: '#4F46E5', strokeStyle: '#4F46E5', lineWidth: 0 }
              ]
            }
          },
          tooltip: {
            backgroundColor: '#03031999',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8,
            callbacks: {
              label: (ctx: { parsed: { x: number } }) => `${Number(ctx.parsed.x).toFixed(1)}%`
            }
          }
        },
        scales: {
          x: {
            beginAtZero: true,
            max: 100,
            ticks: {
              stepSize: 10,
              font: { size: 10 },
              color: '#6B7280',
              callback: (value: string | number) => value + ' %'
            },
            grid: { display: true, color: '#F2F5F9' }
          },
          y: {
            ticks: { font: { size: 12 }, color: '#6B7280' },
            grid: { display: true, color: '#F2F5F9' }
          }
        }
      }
    });
  }

  // --- Répartition des statuts utilisateurs Actifs / Inactifs (%) ---
  initUsersStatusDonutChart(Chart: any): void {
    const ctx = this.usersStatusDonutChartRef?.nativeElement?.getContext('2d');
    if (!ctx) {
      return;
    }

    const [a, i] = this.usersStatusPct;
    this.usersStatusDonutChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Actifs', 'Inactifs'],
        datasets: [
          {
            data: [a, i],
            backgroundColor: ['#22C55F', '#FFD3D3'],
            hoverBackgroundColor: ['#22C55E', '#eeb8b8'],
            borderWidth: 2,
            hoverBorderColor: '#FFFFFF'
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
            position: 'right',
            labels: {
              usePointStyle: true,
              pointStyle: 'circle',
              boxWidth: 6,
              boxHeight: 6,
              padding: 20,
              font: { size: 12 },
              color: '#2B3674'
            }
          },
          tooltip: {
            backgroundColor: '#03031999',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8,
            callbacks: {
              label: (ctx: { label?: string; parsed: number }) =>
                `${ctx.label}: ${Number(ctx.parsed).toFixed(1)}%`
            }
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

  // --- Top 5 produits commandés (barres horizontales, % utilisation) ---
  initTop5ProductsChart(Chart: any): void {
    const ctx = this.top5ProductsChartRef?.nativeElement?.getContext('2d');
    if (!ctx) {
      return;
    }

    this.top5ProductsChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: this.top5ProductLabels,
        datasets: [
          {
            data: this.top5ProductData,
            backgroundColor: '#FF914D',
            hoverBackgroundColor: '#FF6B00',
            barThickness: 30
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y',
        plugins: {
          legend: {
            display: true,
            position: 'top',
            align: 'start',
            labels: {
              boxWidth: 40,
              boxHeight: 12,
              padding: 15,
              font: { size: 12 },
              generateLabels: () => [
                { text: 'Utilisation (%)', fillStyle: '#FF6B00', strokeStyle: '#FF6B00', lineWidth: 0 }
              ]
            }
          },
          tooltip: {
            backgroundColor: '#03031999',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8,
            callbacks: {
              label: (context: { parsed: { x: number } }) =>
                `Utilisation : ${Number(context.parsed.x).toFixed(1)} %`
            }
          }
        },
        scales: {
          x: {
            beginAtZero: true,
            max: 100,
            ticks: {
              stepSize: 10,
              callback: (value: string | number) => value + ' %',
              font: { size: 12 },
              color: '#6B7280'
            },
            grid: { display: true, color: '#F2F5F9' }
          },
          y: {
            ticks: { font: { size: 12 }, color: '#6B7280' },
            grid: { display: true, color: '#F2F5F9' }
          }
        }
      }
    });
  }

  // --- Répartition des produits Actifs / Inactifs (%) ---
  initProductsRepartitionChart(Chart: any): void {
    const ctx = this.productsRepartitionChartRef?.nativeElement?.getContext('2d');
    if (!ctx) {
      return;
    }

    const [a, i] = this.productsRepartitionPct;
    this.productsRepartitionChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Actifs', 'Inactifs'],
        datasets: [
          {
            data: [a, i],
            backgroundColor: ['#22C55F', '#FFD3D3'],
            hoverBackgroundColor: ['#22C55E', '#eeb8b8'],
            borderWidth: 2,
            hoverBorderColor: '#FFFFFF'
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
            position: 'right',
            labels: {
              usePointStyle: true,
              pointStyle: 'circle',
              boxWidth: 6,
              boxHeight: 6,
              padding: 20,
              font: { size: 12 },
              color: '#2B3674'
            }
          },
          tooltip: {
            backgroundColor: '#03031999',
            titleColor: '#fff',
            bodyColor: '#fff',
            padding: 12,
            cornerRadius: 8,
            callbacks: {
              label: (context: { label?: string; parsed: number }) =>
                `${context.label}: ${Number(context.parsed).toFixed(1)}%`
            }
          }
        }
      }
    });
  }
}