import { Component, HostListener, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { ProductService, Product } from '../../../shared/services/product.service';

Chart.register(...registerables);

interface MetricCard {
  title: string;
  value: string;
  icon: string;
  subtitle?: string;
  description?: string;
}

interface Commande {
  numero: string;
  salarie: string;
  dateValidation: string;
  produits: string;
  frequence: string;
  statut: 'En cours' | 'Livrée' | 'En attente' | 'Annulée';
  reference: string;
  note?: string;
  produitsDetails?: { productId: string; quantity: number }[];
}

@Component({
  selector: 'app-gestion-commandes',
  standalone: true,
  imports: [MainLayoutComponent, CommonModule, FormsModule],
  templateUrl: './gestion-commandes.component.html',
  styleUrl: './gestion-commandes.component.css'
})
export class GestionCommandesComponent implements AfterViewInit {
  @ViewChild('frequentProductsChart') frequentProductsChart!: ElementRef<HTMLCanvasElement>;
  @ViewChild('dailyOrdersChart') dailyOrdersChart!: ElementRef<HTMLCanvasElement>;

  private frequentProductsChartInstance?: Chart;
  private dailyOrdersChartInstance?: Chart;

  showDetailModal = false;
  selectedCommande: Commande | null = null;
  allProducts: Product[] = [];

  metricsData: MetricCard[] = [
    {
      title: 'Total commandes',
      value: '43',
      icon: '/icones/commandefour.svg',
      subtitle: '24 commandes cette semaine'
    },
    {
      title: 'Temps moyen de préparation',
      value: '18 min',
      icon: '/icones/minuteur.svg',
      description: '3 % vs le mois dernier'
    },
    {
      title: 'Taux de satisfaction',
      value: '94%',
      icon: '/icones/zigzag.svg',
      description: '3 % vs le mois dernier'
    }
  ];

  searchTerm = '';
  selectedStatutFilter = 'Tous les statuts';
  showStatutDropdown = false;
  currentPage = 1;
  itemsPerPage = 10;

  commandes: Commande[] = [
    {
      numero: 'CMD-0012',
      salarie: 'Moussa Fall',
      dateValidation: '03/10/2025',
      produits: 'Riz parfumé 25kg    Lait 1L',
      frequence: 'Quotidienne',
      statut: 'En cours',
      reference: 'CMD-0012',
      note: 'Lorem Ipsum is simply dummy text of the printing and typesetting industry.',
      produitsDetails: [
        { productId: '1', quantity: 25 },
        { productId: '4', quantity: 30 }
      ]
    },
    {
      numero: 'CMD-0011',
      salarie: 'Aicha Diaw',
      dateValidation: '03/10/2025',
      produits: 'Sucre    Huile    +3',
      frequence: 'Mensuelle',
      statut: 'Livrée',
      reference: 'CMD-0011',
      note: 'Lorem Ipsum is simply dummy text of the printing and typesetting industry.',
      produitsDetails: [
        { productId: '2', quantity: 15 },
        { productId: '3', quantity: 20 }
      ]
    },
    {
      numero: 'CMD-0010',
      salarie: 'Fama Yade',
      dateValidation: '03/10/2025',
      produits: 'Lait    Riz',
      frequence: 'Hebdomadaire',
      statut: 'En attente',
      reference: 'CMD-0010',
      note: 'Lorem Ipsum is simply dummy text of the printing and typesetting industry.',
      produitsDetails: [
        { productId: '4', quantity: 10 },
        { productId: '1', quantity: 12 }
      ]
    },
    {
      numero: 'CMD-0009',
      salarie: 'Mame Ndiaye',
      dateValidation: '03/10/2025',
      produits: 'Sucre    Huile    +4',
      frequence: 'Quotidienne',
      statut: 'Annulée',
      reference: 'CMD-0009',
      note: 'Lorem Ipsum is simply dummy text of the printing and typesetting industry.',
      produitsDetails: [
        { productId: '2', quantity: 8 },
        { productId: '3', quantity: 15 }
      ]
    }
  ];

  constructor(private router: Router, private productService: ProductService) {
    this.allProducts = this.productService.getProducts();
  }

  ngAfterViewInit(): void {
    this.initFrequentProductsChart();
    this.initDailyOrdersChart();
  }

  private initFrequentProductsChart(): void {
    const ctx = this.frequentProductsChart.nativeElement.getContext('2d');
    if (!ctx) return;

    const gradient = ctx.createLinearGradient(0, 0, 500, 0);
    gradient.addColorStop(0, '#FF6B00');
    gradient.addColorStop(1, '#FF914D');

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: ['Riz parfumé 25kg', 'Lait 1L', 'Huile', 'Savon'],
        datasets: [{
          label: 'Utilisation(%)',
          data: [50, 40, 32, 25],
          backgroundColor: gradient,
          borderRadius: 4,
          barThickness: 24,
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: true,
            position: 'top',
            align: 'start',
            labels: {
              usePointStyle: true,
              pointStyle: 'rect',
              padding: 15,
              font: {
                size: 11,
                family: 'Inter, sans-serif'
              },
              color: '#6B7280',
              boxWidth: 20,
              boxHeight: 12
            }
          },
          tooltip: {
            enabled: true,
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            padding: 10,
            cornerRadius: 8,
            titleFont: {
              size: 12
            },
            bodyFont: {
              size: 12
            }
          }
        },
        scales: {
          x: {
            beginAtZero: true,
            max: 50,
            ticks: {
              stepSize: 5,
              font: {
                size: 11
              },
              color: '#6B7280'
            },
            grid: {
              display: true,
              color: '#F3F4F6',
              drawBorder: false
            }
          },
          y: {
            ticks: {
              font: {
                size: 11
              },
              color: '#4B4848'
            },
            grid: {
              display: false
            }
          }
        }
      }
    };

    this.frequentProductsChartInstance = new Chart(ctx, config);
  }

  private initDailyOrdersChart(): void {
    const ctx = this.dailyOrdersChart.nativeElement.getContext('2d');
    if (!ctx) return;

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: ['Lun.', 'Mar.', 'Mer.', 'Jeu.', 'Ven.', 'Sam.', 'Dim.'],
        datasets: [{
          label: 'Commandes',
          data: [13, 21, 17, 19, 25, 12, 7],
          backgroundColor: '#318F3F',
          borderRadius: 4,
          barThickness: 32,
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
            enabled: true,
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            padding: 10,
            cornerRadius: 8,
            titleFont: {
              size: 12
            },
            bodyFont: {
              size: 12
            }
          }
        },
        scales: {
          x: {
            ticks: {
              font: {
                size: 11
              },
              color: '#6B7280'
            },
            grid: {
              display: false
            }
          },
          y: {
            beginAtZero: true,
            max: 25,
            ticks: {
              stepSize: 5,
              font: {
                size: 11
              },
              color: '#6B7280'
            },
            grid: {
              display: true,
              color: '#F3F4F6',
              drawBorder: false
            }
          }
        }
      }
    };

    this.dailyOrdersChartInstance = new Chart(ctx, config);
  }

  get uniqueStatuts(): string[] {
    const statuts = new Set(this.commandes.map(c => c.statut));
    return ['Tous les statuts', ...Array.from(statuts)];
  }

  get filteredCommandes(): Commande[] {
    return this.commandes.filter(commande => {
      const matchesSearch =
        commande.numero.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        commande.salarie.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        commande.produits.toLowerCase().includes(this.searchTerm.toLowerCase());

      const matchesStatut = this.selectedStatutFilter === 'Tous les statuts' ||
        commande.statut === this.selectedStatutFilter;

      return matchesSearch && matchesStatut;
    });
  }

  get paginatedCommandes(): Commande[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredCommandes.slice(startIndex, endIndex);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredCommandes.length / this.itemsPerPage);
  }

  toggleStatutDropdown(): void {
    this.showStatutDropdown = !this.showStatutDropdown;
  }

  selectStatut(statut: string): void {
    this.selectedStatutFilter = statut;
    this.showStatutDropdown = false;
    this.currentPage = 1;
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }

  viewCommande(numero: string): void {
    const commande = this.commandes.find(c => c.numero === numero);
    if (commande) {
      this.selectedCommande = commande;
      this.showDetailModal = true;
    }
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedCommande = null;
  }

  getCommandeProducts(): Product[] {
    if (!this.selectedCommande?.produitsDetails) return [];
    return this.allProducts.filter(p =>
      this.selectedCommande!.produitsDetails!.some(pd => pd.productId === p.id)
    );
  }

  getProductQuantity(productId: string): number {
    if (!this.selectedCommande?.produitsDetails) return 0;
    const detail = this.selectedCommande.produitsDetails.find(pd => pd.productId === productId);
    return detail?.quantity || 0;
  }

  exportData(): void {
    console.log('Export des données');
  }

  getStatusClass(statut: string): string {
    switch (statut) {
      case 'En cours':
        return 'bg-[#EAB3080F] text-[#EAB308]';
      case 'Livrée':
        return 'bg-[#0A97480F] text-[#0A9748]';
      case 'En attente':
        return 'bg-[#F2F2F2] text-[#2C3E50]';
      case 'Annulée':
        return 'bg-[#FF09090F] text-[#FF0909]';
      default:
        return 'bg-gray-100 text-gray-600';
    }
  }

  getStatusDotClass(statut: string): string {
    switch (statut) {
      case 'En cours':
        return 'bg-[#EAB308]';
      case 'Livrée':
        return 'bg-[#0A9748]';
      case 'En attente':
        return 'bg-[#2C3E50]';
      case 'Annulée':
        return 'bg-[#FF0909]';
      default:
        return 'bg-gray-400';
    }
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.showStatutDropdown) {
      this.showStatutDropdown = false;
    }
  }
}
