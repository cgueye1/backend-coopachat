import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import Swal from 'sweetalert2';
import { ProductService } from '../../../shared/services/product.service';

interface MetricCard {
  title: string;
  value: string;
  icon: string;
}

interface Product {
  id: string;
  name: string;
  reference: string;
  category: string;
  price: string;
  stock: number;
  updatedAt: string;
  status: 'Actif' | 'Inactif';
  icon: string;
  description?: string;
}

@Component({
  selector: 'app-catalogue',
  standalone: true,
  imports: [MainLayoutComponent, HeaderComponent, CommonModule, FormsModule, NgChartsModule],
  templateUrl: './catalogue.component.html',
  styles: ``
})
export class CatalogueComponent implements OnInit {
  searchText = '';
  selectedCategory = 'Toutes les catégories';
  selectedStatus = 'Tous les status';
  showCategoryDropdown = false;
  showStatusDropdown = false;
  currentPage = 1;
  totalPages = 6;
  showProductModal = false;
  selectedProduct: Product | null = null;

  constructor(private router: Router, private productService: ProductService) { }

  // Bar Chart Configuration - Utilisateurs par rôle
  public barChartData: ChartConfiguration<'bar'>['data'] = {
    labels: ['Riz parfumé 25kg', 'Eau 1.5L (x6)', 'Huile 5L', 'Thon boîte'],
    datasets: [
      {
        data: [450, 350, 250, 200],
        backgroundColor: (ctx) => {
          const { chart } = ctx;
          const { ctx: c, chartArea } = chart as any;
          if (!chartArea) {
            return '#FF6B00';
          }
          const gradient = c.createLinearGradient(chartArea.left, 0, chartArea.right, 0);
          gradient.addColorStop(0, '#FF6B00');
          gradient.addColorStop(1, '#FF914D');
          return gradient;
        },
        barThickness: 30,
        hoverBackgroundColor: '#FF914D'
      }
    ]
  };

  public barChartOptions: ChartConfiguration<'bar'>['options'] = {
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
          font: {
            size: 12
          },
          generateLabels: () => [
            {
              text: 'Utilisation(%)',
              fillStyle: '#FF6B00',
              strokeStyle: '#FF6B00',
              lineWidth: 0
            }
          ]
        }
      },
      tooltip: {
        enabled: true
      }
    },
    scales: {
      x: {
        beginAtZero: true,
        max: 500,
        ticks: {
          stepSize: 50,
          font: {
            size: 12
          }
        },
        grid: {
          display: true,
          color: '#F2F5F9'
        }
      },
      y: {
        ticks: {
          font: {
            size: 12
          }
        },
        grid: {
          display: true,
          color: '#F2F5F9'
        }
      }
    }
  };

  // Doughnut Chart Configuration - Répartition des statuts
  public doughnutChartData: ChartConfiguration<'doughnut'>['data'] = {
    labels: ['Actifs', 'Inactifs'],
    datasets: [
      {
        data: [83, 17],
        backgroundColor: ['#22C55F', '#FFD3D3'],
        hoverBackgroundColor: ['#22C55E', '#eeb8b8ff'],
        borderWidth: 2,
        hoverBorderColor: '#FFFFFF'
      }
    ]
  };

  public doughnutChartOptions: ChartConfiguration<'doughnut'>['options'] = {
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
          font: {
            size: 12
          },
          generateLabels: (chart) => {
            const data = chart.data;
            if (data.labels && data.datasets.length) {
              return data.labels.map((label, i) => ({
                text: label as string,
                fillStyle: (data.datasets[0].backgroundColor as string[])[i],
                strokeStyle: (data.datasets[0].backgroundColor as string[])[i],
                lineWidth: 0,
                hidden: false,
                index: i
              }));
            }
            return [];
          }
        }
      },
      tooltip: {
        enabled: true,
        callbacks: {
          label: (context) => {
            return `${context.label}: ${context.parsed}%`;
          }
        }
      }
    }
  };

  ngOnInit() {
    this.productService.products$.subscribe(products => {
      this.products = products;
      this.updateMetrics();
    });
  }

  metricsData: MetricCard[] = [];

  products: Product[] = [];

  updateMetrics() {
    const total = this.products.length;
    const disponibles = this.products.filter(p => p.stock > 0 && p.status === 'Actif').length;
    const modifies = this.products.filter(p => p.updatedAt === '05/10/2025').length;

    this.metricsData = [
      {
        title: 'Total',
        value: String(total).padStart(2, '0'),
        icon: '/icones/commandefour.svg'
      },
      {
        title: 'Disponibilité',
        value: String(disponibles).padStart(2, '0'),
        icon: '/icones/livraisonavenir.svg'
      },
      {
        title: 'Modifiés récemment',
        value: String(modifies).padStart(2, '0'),
        icon: '/icones/temps.svg'
      }
    ];
  }

  get uniqueCategories(): string[] {
    const categories = new Set(this.products.map(p => p.category));
    return ['Toutes les catégories', ...Array.from(categories)];
  }

  get uniqueStatuses(): string[] {
    return ['Tous les status', 'Actif', 'Inactif'];
  }

  get filteredProducts(): Product[] {
    return this.products.filter(product => {
      const matchesSearch =
        product.name.toLowerCase().includes(this.searchText.toLowerCase()) ||
        product.reference.toLowerCase().includes(this.searchText.toLowerCase());

      const matchesCategory = this.selectedCategory === 'Toutes les catégories' || product.category === this.selectedCategory;
      const matchesStatus = this.selectedStatus === 'Tous les status' || product.status === this.selectedStatus;

      return matchesSearch && matchesCategory && matchesStatus;
    });
  }

  toggleCategoryDropdown() {
    this.showCategoryDropdown = !this.showCategoryDropdown;
    this.showStatusDropdown = false;
  }

  toggleStatusDropdown() {
    this.showStatusDropdown = !this.showStatusDropdown;
    this.showCategoryDropdown = false;
  }

  selectCategory(category: string) {
    this.selectedCategory = category;
    this.showCategoryDropdown = false;
  }

  selectStatus(status: string) {
    this.selectedStatus = status;
    this.showStatusDropdown = false;
  }

  getStatusClass(status: string): string {
    return status === 'Actif'
      ? 'bg-[#0A97480F] text-[#0A9748]'
      : 'bg-red-50 text-[#FF0909]';
  }

  getStatusDotClass(status: string): string {
    return status === 'Actif' ? 'bg-[#0A9748]' : 'bg-[#FF0909]';
  }

  nouveauProduit() {
    this.router.navigate(['/admin/add-produit']);
  }

  viewProduct(product: Product) {
    this.selectedProduct = product;
    this.showProductModal = true;
  }

  closeProductModal() {
    this.showProductModal = false;
    this.selectedProduct = null;
  }

  toggleModalProductStatus() {
    if (this.selectedProduct) {
      this.toggleProductStatus(this.selectedProduct);
    }
  }

  editProduct(product: Product) {
    console.log('Modifier produit:', product);
  }

  toggleProductStatus(product: Product) {
    const isActivating = product.status === 'Inactif';
    const titleText = isActivating ? 'Activer ce produit' : 'Désactiver ce produit ?';
    const descriptionText = isActivating ? 'Il sera visible et commandable.' : 'Il ne sera plus affiché ni commandable.';
    const confirmButtonText = isActivating ? 'Activer' : 'Oui';
    const confirmButtonClass = isActivating
      ? 'bg-[#16A34A] hover:bg-[#16A34A] text-white px-8 py-3 rounded-lg font-medium text-base shadow-none border-none'
      : 'bg-[#EF4444] hover:bg-[#DC2626] text-white px-8 py-3 rounded-lg font-medium text-base shadow-none border-none';

    Swal.fire({
      title: titleText,
      text: descriptionText,
      iconHtml: `<img src="/icones/alerte.svg" alt="alert" style=" margin: 0 auto;" />`,
      showCancelButton: true,
      confirmButtonText: confirmButtonText,
      cancelButtonText: 'Annuler',
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-3xl p-6',
        title: 'text-2xl font-medium text-gray-900',
        htmlContainer: 'text-lg text-gray-600',
        confirmButton: confirmButtonClass,
        cancelButton: 'bg-[#F3F4F6] hover:bg-gray-200 text-gray-700 px-8 py-3 rounded-lg font-medium text-base shadow-none border-none',
        actions: 'flex justify-center w-full gap-2',
        icon: 'border-none'
      },
      backdrop: `rgba(0,0,0,0.2)`,
      width: '580px',
      showClass: {
        popup: 'animate__animated animate__fadeIn animate__faster'
      }
    }).then((result) => {
      if (result.isConfirmed) {
        product.status = product.status === 'Actif' ? 'Inactif' : 'Actif';
        this.updateMetrics();
        this.showToggleSuccessMessage(product.status);
      }
    });
  }

  showToggleSuccessMessage(newStatus: 'Actif' | 'Inactif') {
    Swal.fire({
      title: newStatus === 'Inactif' ? 'Le produit a été désactivé' : 'Le produit a été activé',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showConfirmButton: false,
      timer: 1500,
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-3xl p-6',
        title: 'text-xl font-medium text-gray-900',
        icon: 'border-none'
      },
      backdrop: `rgba(0,0,0,0.2)`,
      width: '580px',
      showClass: {
        popup: 'animate__animated animate__fadeIn animate__faster'
      },
      hideClass: {
        popup: 'animate__animated animate__fadeOut animate__faster'
      }
    });
  }

  exportData() {
    console.log('Exporter données');
  }

  previousPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }
}
