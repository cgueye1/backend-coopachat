import { Component, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { AdminService } from '../../../shared/services/admin.service';


 
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


interface CommandRevenuePoint {
  date: string;
  commandes: number;
  livraisons: number;
  montantEncaisse: number;
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

  /** Période envoyée à l'API dashboard (TODAY = aujourd'hui, THIS_MONTH = mois en cours). */
  periode: 'TODAY' | 'THIS_MONTH' = 'THIS_MONTH';

  /**
   * Données des 3 cartes KPI en haut de page.
   * Rempli au chargement par l'API GET /admin/dashboard/stats (loadDashboardStats).
   * Le HTML fait *ngFor="let metric of metricsData" et affiche title, value, icon.
   */
  metricsData: MetricCard[] = [
    {
      title: 'Commandes en attente',
      value: '—',//valeur par défaut
      icon: '/icones/commandefour.svg'
    },
    {
      title: 'Paiements échoués',
      value: '—',//valeur par défaut
      icon: '/icones/temps.svg'
    },
    {
      title: 'Réclamations ouvertes',
      value: '—',//valeur par défaut
      icon: '/icones/users.svg'
    }
  ];

   /**
   * Données du donut "Paiements par statut". Rempli par l’API (loadDashboardStats).
   * initPaiementsChart() et loadDashboardStats() mettent à jour le graphique à partir de ce tableau.
   */
  paymentStatusData: PaymentStatusSlice[] = [
    { status: 'Payé', value: 0, color: '#22C55F' },
    { status: 'En attente de confirmation', value: 0, color: '#EAB308' },
    { status: 'Échoué', value: 0, color: '#FFD3D3' }
  ];


  /** Données du graphique "Commandes vs Livraisons" (données statiques pour l’instant). */
  commandesChiffreData: CommandRevenuePoint[] = [
    { date: '06/09', commandes: 7, livraisons: 7, montantEncaisse: 8800000 },
    { date: '07/09', commandes: 7, livraisons: 7, montantEncaisse: 9600000 },
    { date: '08/09', commandes: 6, livraisons: 5, montantEncaisse: 6200000 },
    { date: '09/09', commandes: 6, livraisons: 6, montantEncaisse: 7200000 },
    { date: '10/09', commandes: 7, livraisons: 6, montantEncaisse: 8400000 },
    { date: '11/09', commandes: 7, livraisons: 6, montantEncaisse: 9600000 },
    { date: '12/09', commandes: 7, livraisons: 6, montantEncaisse: 8000000 }
  ];

 
  /** Données du graphique empilé "Livraisons" (statiques). */
  livraisonsData: LivraisonStackPoint[] = [
    { date: '06/09', livres: 82, planifies: 15, retard: 3 },
    { date: '07/09', livres: 78, planifies: 18, retard: 4 },
    { date: '08/09', livres: 74, planifies: 20, retard: 6 },
    { date: '09/09', livres: 80, planifies: 16, retard: 4 },
    { date: '10/09', livres: 85, planifies: 12, retard: 3 }
  ];

  /** Données du graphique "Utilisateurs par rôle" (statiques). */
  rolesData: RoleStat[] = [
    { role: 'Salariés', total: 40 },
    { role: 'Commerciaux', total: 16 },
    { role: 'Livreurs', total: 7 },
    { role: 'Responsable logistique', total: 13 }
  ];

  /** Données du donut "Stocks - État global" (statiques). */
  stockCategoryData = [
    { category: 'Ok', value: 75 },
    { category: 'Sous seuil', value: 15 },
    { category: 'Critique', value: 10 }
  ];

  /** Données du graphique "Tendance des coupons utilisés" (statiques). */
  couponsTrendData: CouponTrendPoint[] = [
    { date: '06/09', value: 14 },
    { date: '07/09', value: 8 },
    { date: '08/09', value: 14 },
    { date: '09/09', value: 6 },
    { date: '10/09', value: 15 }
  ];

  /** Références aux instances Chart.js (pour mise à jour du graphique paiements après l’API). */
  private commandesChart?: any;
  private paiementsChart?: any;
  private livraisonsChart?: any;
  private rolesChart?: any;
  private stocksEtatChart?: any;
  private couponsChart?: any;

  //Constructeur
  constructor(private adminService: AdminService) {}

  //Au démarrage du composant 
  ngOnInit(): void {
    this.loadDashboardStats();//On charge les données du dashboard
  }

  /**
   * Appelle l'API GET /admin/dashboard/stats et met à jour les 3 KPIs + le graphique "Paiements par statut".
   */
  loadDashboardStats(): void {
    //on appelle le service et on le passe la période 
    this.adminService.getDashboardStats(this.periode).subscribe({
      next: (data) => {
        // 1) Mettre à jour les 3 cartes KPI
        this.metricsData[0].value = String(data.commandesEnAttente);
        this.metricsData[1].value = String(data.paiementsEchoues);
        this.metricsData[2].value = String(data.reclamationsOuvertes);

        // 2) Mapper la réponse "paiements par statut" vers le format du graphique (status, value, color)
        const colors: Record<string, string> = {
          'Payé': '#22C55F',//couleur du statut Payé
          'En attente de confirmation': '#FFE7C2',//couleur du statut En attente de confirmation
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
      },
      //si erreur on affiche l'erreur dans la console
      error: (err) => {
        console.error('Erreur chargement stats dashboard:', err);
      }
    });
  }

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
            barThickness: 24,
            borderColor: 'white',
            borderWidth: 1, 

          },
          {
            label: 'Planifiés',
            data: this.livraisonsData.map(point => point.planifies),
            backgroundColor: '#E5E7EB',
            barThickness: 24,
            borderColor: 'white',
            borderWidth: 1,  

          },
          {
            label: 'Retard',
            data: this.livraisonsData.map(point => point.retard),
            backgroundColor: '#F97316',
            barThickness: 24,
            borderColor: 'white',
            borderWidth: 1, 

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
            beginAtZero: false,
            grid: { color: '#F3F4F6' },
            ticks: { color: '#6B7280', font: { size: 12 }, stepSize: 2 }
          }
        }
      }
    });
  }
}