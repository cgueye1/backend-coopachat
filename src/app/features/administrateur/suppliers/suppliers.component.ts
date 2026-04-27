import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { SupplierService } from '../../../shared/services/supplier.service';
import { SupplierDetailsDTO, SupplierListItemDTO, SupplierStatsDTO, SupplierTypeLabels } from '../../../shared/models/supplier.model';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { AdminService, CategoryListItemDTO } from '../../../shared/services/admin.service';

@Component({
  selector: 'app-suppliers',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, MainLayoutComponent, HeaderComponent],
  templateUrl: './suppliers.component.html',
  styleUrls: []
})
export class SuppliersComponent implements OnInit {
  searchTerm: string = '';
  isLoading: boolean = false;
  suppliers: SupplierListItemDTO[] = [];
  
  // Pagination
  currentPage: number = 0;
  pageSize: number = 10;
  totalElements: number = 0;
  totalPages: number = 0;
  pageSizeOptions: number[] = [5, 10, 20, 50];

  // Filters
  selectedCategoryId: number | null = null;
  selectedStatus: boolean | null = null;
  categories: CategoryListItemDTO[] = [];

  // Stats
  stats: SupplierStatsDTO | null = null;
  typeLabels = SupplierTypeLabels;
  metricsData: { title: string, value: string, icon: string }[] = [];

  private searchSubject = new Subject<string>();

  constructor(
    private router: Router,
    private supplierService: SupplierService,
    private adminService: AdminService
  ) {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(() => {
      this.currentPage = 0;
      this.loadSuppliers();
    });
  }

  ngOnInit(): void {
    this.loadSuppliers();
    this.loadCategories();
    this.loadStats();
  }

  loadCategories(): void {
    this.adminService.getCategories().subscribe({
      next: (categories) => this.categories = categories,
      error: (err) => console.error('Error loading categories', err)
    });
  }

  loadStats(): void {
    this.supplierService.getStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.metricsData = [
          { title: 'Fournisseurs', value: String(stats.totalSuppliers || 0).padStart(2, '0'), icon: '/icones/utilisateurs.svg' },
          { title: 'Actifs', value: String(stats.activeSuppliers || 0).padStart(2, '0'), icon: '/icones/GreenUser.svg' },
          { title: 'Inactifs', value: String(stats.inactiveSuppliers || 0).padStart(2, '0'), icon: '/icones/OrangeUser.svg' }
        ];
      },
      error: (err) => console.error('Error loading stats', err)
    });
  }

  loadSuppliers(): void {
    this.isLoading = true;
    this.supplierService.getSuppliers({
      page: this.currentPage,
      size: this.pageSize,
      search: this.searchTerm,
      categoryId: this.selectedCategoryId || undefined,
      status: this.selectedStatus !== null ? this.selectedStatus : undefined
    }).subscribe({
      next: (response) => {
        this.suppliers = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error loading suppliers', error);
        this.isLoading = false;
      }
    });
  }

  nouveauFournisseur(): void {
    this.router.navigate(['/admin/suppliers/add']);
  }

  onSearch(): void {
    this.searchSubject.next(this.searchTerm);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadSuppliers();
  }

  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadSuppliers();
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadSuppliers();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadSuppliers();
    }
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.loadSuppliers();
  }

  onExport(): void {
    this.supplierService.exportSuppliers(this.searchTerm).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `fournisseurs_${new Date().getTime()}.xlsx`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Error exporting suppliers', error);
      }
    });
  }

  editSupplier(id: number): void {
    this.router.navigate(['/admin/suppliers/edit', id]);
  }

  viewSupplier(id: number): void {
    this.router.navigate(['/admin/suppliers/view', id]);
  }
}
