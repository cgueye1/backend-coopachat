import { Component, OnInit, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
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
  imports: [CommonModule, MainLayoutComponent, HeaderComponent],
  templateUrl: 'dashboard.component.html',
  styleUrls: ['dashboard.component.css']
})

export class DashboardComponent implements AfterViewInit {
  @ViewChild('salesChart', { static: false }) salesChartRef!: ElementRef<HTMLCanvasElement>;

  role: 'com' = 'com';
  private salesChart?: Chart;

  metricsData: MetricCard[] = [
    {
      title: 'Entreprises',
      value: '24',
      changePrimary: '+2',
      changeSecondary: 'Partenaires actifs',
      changeType: 'positive',
      icon: 'icones/entreprise.svg'
    },
    {
      title: 'Salariés',
      value: '458',
      changePrimary: '+15',
      changeSecondary: 'Inscrits ce mois',
      changeType: 'positive',
      icon: 'icones/salaries2.svg'
    },
    {
      title: 'Ventes',
      value: '18,250€',
      changePrimary: '+12%',
      changeSecondary: 'Ce mois',
      changeType: 'positive',
      icon: 'icones/vente.svg'
    },
    {
      title: 'Promotions',
      value: '3',
      changePrimary: 'Actives',
      changeSecondary: 'En cours',
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
    const data = [4000, 3000, 5000, 2900, 1700, 2600, 3500];
    const labels = ['Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Jun', 'Juil'];

    this.salesChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          data: data,
          borderColor: '#2C2D5B',
          backgroundColor: 'transparent',
          borderWidth: 1,
          pointBackgroundColor: '#ffffffff',
          pointBorderColor: '#2C2D5B',
          pointBorderWidth: 1,
          pointRadius: 3,
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
              label: function (context) {
                return context.parsed.y.toLocaleString() + '€';
              }
            }
          }
        },
        scales: {
          x: {
            grid: {
              display: true,
              color: '#CCCCCC',
              drawBorder: true,
              borderDash: [2, 4],
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
              color: '#CCCCCC',
              drawBorder: true,
              borderDash: [2, 4],
              borderDashOffset: 2
            },
            ticks: {
              color: '#666666',
              font: {
                size: 11
              },
              padding: 8,
              stepSize: 1500,
              callback: function (value) {
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
      'yellow': 'bg-[#FEF9C3] text-[#854D0E]',
      'blue': 'bg-[#DBEAFE] text-[#1E40AF]',
      'green': 'bg-[#DCFCE7] text-[#166534]'
    };
    return classes[color as keyof typeof classes] || 'bg-gray-100 text-gray-800';
  }

  getCompanyStatusClass(status: 'Active' | 'En attente'): string {
    return status === 'Active'
      ? 'bg-[#DCFCE7] text-[#166534]'
      : 'bg-[#FEF9C3] text-[#854D0E]';
  }

  ngOnDestroy() {
    if (this.salesChart) {
      this.salesChart.destroy();
    }
  }
}