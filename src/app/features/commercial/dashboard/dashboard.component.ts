import { Component, OnInit, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

interface MetricCard {
  title: string;
  value: string;
  change: string;
  changeType: 'positive' | 'negative';
  icon: string;
}

interface Prospect {
  company: string;
  contact: string;
  status: string;
  statusColor: string;
  date: string;
}

interface PartnerCompany {
  name: string;
  registeredEmployees: number;
  orders: number;
  totalAmount: string;
  status: 'Active' | 'En attente';
}

@Component({
  selector: 'app-commercial-dashboard',
  standalone: true,
  imports: [CommonModule, MainLayoutComponent],
  template: `
    <app-main-layout [role]="role">
      <!-- Header Section - Responsive -->
      <div class="flex flex-col space-y-4 sm:flex-row sm:items-center sm:justify-between sm:space-y-0 mb-6 lg:mb-8">
        <h1 class="text-2xl sm:text-3xl font-bold text-gray-900">Tableau de bord commercial</h1>
        <div class="flex items-center justify-center sm:justify-end">
          <button class="w-full sm:w-auto bg-blue-950 text-white px-4 py-2 rounded-lg text-sm font-medium flex items-center justify-center hover:bg-blue-700 transition-colors">
            <svg width="17" height="16" viewBox="0 0 17 16" fill="none" xmlns="http://www.w3.org/2000/svg" class="mr-2">
              <path d="M3.87097 8H13.2043" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M8.5376 3.33337V12.6667" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span class="hidden sm:inline">Nouvelle Prospection</span>
            <span class="sm:hidden">Nouveau</span>
          </button>
        </div>
      </div>

      <!-- Metrics Cards - Enhanced Responsivity -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6 lg:mb-8">
        <div *ngFor="let metric of metricsData" class="bg-white rounded-lg p-4 sm:p-6 border border-gray-200 hover:shadow-md transition-shadow">
          <div class="flex items-center justify-between mb-3 sm:mb-4">
            <div class="text-xs sm:text-sm text-gray-600">{{ metric.title }}</div>
            <img [src]="metric.icon" class="w-5 h-5 sm:w-6 sm:h-6" alt="{{ metric.title }}">
          </div>
          <div class="text-2xl sm:text-3xl font-bold text-gray-900 mb-2 sm:mb-4">{{ metric.value }}</div>
          <div class="flex items-center text-xs sm:text-sm">
            <span 
              class="font-medium"
              [class.text-green-600]="metric.changeType === 'positive'"
              [class.text-red-600]="metric.changeType === 'negative'"
            >
              {{ metric.change }}
            </span>
          </div>
        </div>
      </div>

      <!-- Charts and Tables Row -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6 lg:mb-2">
        <!-- Sales Evolution Chart with Chart.js -->
        <div class="lg:col-span-2 bg-white rounded-lg py-6 shadow-sm ">
          <h3 class="text-lg font-semibold text-gray-900 mb-6 pb-2 px-6  border-b border-gray-200">Évolution des ventes</h3>
          <div class="relative 80">
            <canvas #salesChart class="w-full h-full"></canvas>
          </div>
        </div>

        <!-- Recent Prospects -->
        <div class="lg:col-span-1 bg-white rounded-lg py-6 border border-gray-200 shadow-sm">
          <div class="flex items-center justify-between mb-6 pb-2 px-6 relative">
            <h3 class="text-lg font-semibold text-gray-900">Prospects récents</h3>
            <span class="absolute bottom-0 left-0 w-full h-px bg-gray-200"></span>
          </div>
          
          <div class="space-y-4 px-6">
            <div *ngFor="let prospect of recentProspects" class=" border border-gray-200 rounded-lg p-4">
              <div class="flex items-start justify-between">
                <div class="flex-1 min-w-0">
                  <div class="font-medium text-gray-900 text-sm mb-1">{{ prospect.company }}</div>
                  <div class="text-xs text-gray-500 mb-2">Contact: {{ prospect.contact }}</div>
                  <div class="flex items-center space-x-2 w-full ">
                    <span 
                      class="inline-flex items-center px-2 py-1 text-xs font-medium rounded-full"
                      [ngClass]="getStatusClass(prospect.statusColor)"
                    >
                      {{ prospect.status }}
                    </span>
                    <span class="text-xs text-gray-500 justify-end">{{ prospect.date }}</span>
                  </div>
                </div>
                <button class="ml-3 flex items-center text-gray-600 hover:text-gray-900 px-2 py-1 border border-gray-300 rounded text-xs font-medium transition-colors">
                  <svg width="12" height="12" viewBox="0 0 15 14" fill="none" xmlns="http://www.w3.org/2000/svg" class="mr-1">
                    <path d="M3.64172 7H11.8084" stroke="#374151" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M7.7251 2.91663L11.8084 6.99996L7.7251 11.0833" stroke="#374151" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  Détails
                </button>
              </div>
            </div>
          </div>
          
          <div class="text-center mt-6 pt-4 border-t border-gray-100">
            <button class="text-sm hover:text-blue-800 transition-colors font-medium">
              Voir tous les prospects
            </button>
          </div>
        </div>
      </div>

      <!-- Partner Companies Table -->
<div class="bg-white rounded-lg border border-gray-200 shadow-sm">
  <!-- Header -->
  <div class="p-4 sm:p-6 border-b border-gray-200 py-4">
    <h2 class="text-lg sm:text-xl font-semibold text-gray-900  ">Entreprises partenaires</h2>
  </div>
  
  <!-- Table Container -->
  <div class="p-2 sm:p-8 shadow-md">
    <!-- Desktop Table View -->
    <div class="hidden lg:block overflow-x-auto border border-gray-200 rounded-lg shadow-md">
      <table class="w-full rounded-lg border border-gray-200 shadow-md overflow-hidden">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Entreprise</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Salariés inscrits</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Commandes</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Montant total</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Statut</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
          </tr>
        </thead>
        <tbody class="bg-white divide-y divide-gray-200">
          <tr *ngFor="let company of partnerCompanies" class="hover:bg-gray-50">
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
              {{ company.name }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
              {{ company.registeredEmployees }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
              {{ company.orders }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">
              {{ company.totalAmount }}
            </td>
            <td class="px-6 py-4 whitespace-nowrap">
              <span 
                class="inline-flex items-center px-2 py-1 text-xs font-semibold rounded-full"
                [ngClass]="{
                  'bg-green-100 text-green-800': company.status === 'Active',
                  'bg-yellow-100 text-yellow-800': company.status === 'En attente'
                }"
              >
                {{ company.status }}
              </span>
            </td>
            <td class="px-6 py-4 whitespace-nowrap text-sm font-medium space-x-2">
              <button class="text-gray-600 hover:text-gray-900 px-3 py-1 border border-gray-300 rounded text-xs transition-colors">
                Détails
              </button>
              <button class="bg-blue-950 hover:bg-blue-800 text-white px-3 py-1 rounded text-xs transition-colors">
                Gérer
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Mobile Card View -->
    <div class="lg:hidden divide-y divide-gray-200">
      <div *ngFor="let company of partnerCompanies" class="p-4 sm:p-6 hover:bg-gray-50 transition-colors border border-gray-200 rounded-lg shadow-md mb-2">
        <div class="flex flex-col space-y-3">
          <!-- Company Header -->
          <div class="flex items-start justify-between">
            <div class="flex-1 min-w-0">
              <h4 class="text-sm font-medium text-gray-900 truncate">{{ company.name }}</h4>
              <div class="mt-1">
                <span 
                  class="inline-flex items-center px-2 py-1 text-xs font-semibold rounded-full"
                  [ngClass]="{
                    'bg-green-100 text-green-800': company.status === 'Active',
                    'bg-yellow-100 text-yellow-800': company.status === 'En attente'
                  }"
                >
                  {{ company.status }}
                </span>
              </div>
            </div>
            <div class="text-right">
              <div class="text-sm font-semibold text-gray-900">{{ company.totalAmount }}</div>
            </div>
          </div>

          <!-- Company Stats -->
          <div class="grid grid-cols-2 gap-4 text-sm">
            <div>
              <span class="text-gray-500">Salariés:</span>
              <span class="ml-1 font-medium text-gray-900">{{ company.registeredEmployees }}</span>
            </div>
            <div>
              <span class="text-gray-500">Commandes:</span>
              <span class="ml-1 font-medium text-gray-900">{{ company.orders }}</span>
            </div>
          </div>

          <!-- Action Buttons -->
          <div class="flex flex-col sm:flex-row gap-2 sm:gap-3 pt-2">
            <button class="flex-1 text-gray-600 hover:text-gray-900 px-3 py-2 border border-gray-300 rounded text-xs font-medium transition-colors">
              Détails
            </button>
            <button class="flex-1 bg-blue-950 hover:bg-blue-800 text-white px-3 py-2 rounded text-xs font-medium transition-colors">
              Gérer
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>


    </app-main-layout>
  `,
  styles: [`
    .fa, .fas {
      font-family: "Inter";
      font-weight: 600;
    }
    
    /* Ensure smooth scrolling on mobile */
    @media (max-width: 640px) {
      .overflow-x-auto {
        -webkit-overflow-scrolling: touch;
      }
    }
    
    /* Custom scrollbar for webkit browsers */
    .overflow-x-auto::-webkit-scrollbar {
      height: 6px;
    }
    
    .overflow-x-auto::-webkit-scrollbar-track {
      background: #f1f5f9;
      border-radius: 3px;
    }
    
    .overflow-x-auto::-webkit-scrollbar-thumb {
      background: #cbd5e1;
      border-radius: 3px;
    }
    
    .overflow-x-auto::-webkit-scrollbar-thumb:hover {
      background: #94a3b8;
    }
  `]
})
export class DashboardComponent implements AfterViewInit {
  @ViewChild('salesChart', { static: false }) salesChartRef!: ElementRef<HTMLCanvasElement>;
  
  role: 'com' = 'com';
  private salesChart?: Chart;

  metricsData: MetricCard[] = [
    {
      title: 'Entreprises',
      value: '24',
      change: '+2 Partenaires actifs',
      changeType: 'positive',
      icon: 'icones/entreprise.svg'
    },
    {
      title: 'Salariés',
      value: '458',
      change: '+15 Inscrits ce mois',
      changeType: 'positive',
      icon: 'icones/salaries2.svg'
    },
    {
      title: 'Ventes',
      value: '18,250€',
      change: '+12% Ce mois',
      changeType: 'positive',
      icon: 'icones/vente.svg'
    },
    {
      title: 'Promotions',
      value: '3',
      change: 'Actives En cours',
      changeType: 'positive',
      icon: 'icones/promo.svg'
    }
  ];

  recentProspects: Prospect[] = [
    {
      company: 'Entreprise LMN',
      contact: 'Marie Dupont',
      status: 'Intéressée',
      statusColor: 'green',
      date: '19/07/2023'
    },
    {
      company: 'Société PQR',
      contact: 'Jean Martin',
      status: 'A relancer',
      statusColor: 'yellow',
      date: '22/07/2023'
    },
    {
      company: 'Groupe 456',
      contact: 'Sophie Lefebvre',
      status: 'Rendez-vous',
      statusColor: 'blue',
      date: '28/07/2023'
    }
  ];

  partnerCompanies: PartnerCompany[] = [
    {
      name: 'Entreprise ABC',
      registeredEmployees: 85,
      orders: 156,
      totalAmount: '12,450€',
      status: 'Active'
    },
    {
      name: 'Société XYZ',
      registeredEmployees: 42,
      orders: 78,
      totalAmount: '8,320€',
      status: 'Active'
    },
    {
      name: 'Groupe 123',
      registeredEmployees: 23,
      orders: 34,
      totalAmount: '3,670€',
      status: 'En attente'
    },
    {
      name: 'Tech Solutions',
      registeredEmployees: 67,
      orders: 112,
      totalAmount: '9,840€',
      status: 'Active'
    }
  ];

  ngAfterViewInit() {
    this.createSalesChart();
  }

  private createSalesChart() {
    const ctx = this.salesChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    // Données identiques à la capture d'écran
    const data = [4000, 3000, 5000, 3500, 4500, 4200, 5500];
    const labels = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Juil'];

    this.salesChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          data: data,
          borderColor: '#09090aff',
          backgroundColor: 'transparent',
          borderWidth: 1,
          pointBackgroundColor: '#ffffffff',
          pointBorderColor: '#000000ff',
          pointBorderWidth: 1,
          pointRadius: 5,
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
            display: false
          },
          tooltip: {
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            titleColor: '#ffffff',
            bodyColor: '#ffffff',
            borderColor: '#6b7280',
            borderWidth: 1,
            displayColors: false,
            callbacks: {
              label: function(context) {
                return context.parsed.y.toLocaleString() + '€';
              }
            }
          }
        },
        scales: {
          x: {
            grid: {
              display: true,
              color: '#d1d5db',
              drawBorder: true,
              borderDash: [2, 8],
              borderDashOffset: 2
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
            max: 6000,
            grid: {
              display: true,
              color: '#d1d5db',
              drawBorder: true,
              borderDash: [2, 8],
              borderDashOffset: 2
            },
            ticks: {
              color: '#6b7280',
              font: {
                size: 11
              },
              padding: 8,
              stepSize: 1500,
              callback: function(value) {
                return value.toLocaleString();
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

  getStatusClass(color: string): string {
    const classes = {
      'yellow': 'bg-yellow-100 text-yellow-800',
      'blue': 'bg-blue-100 text-blue-800',
      'green': 'bg-green-100 text-green-800'
    };
    return classes[color as keyof typeof classes] || 'bg-gray-100 text-gray-800';
  }

  ngOnDestroy() {
    if (this.salesChart) {
      this.salesChart.destroy();
    }
  }
}