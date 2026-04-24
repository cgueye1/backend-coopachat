import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { SupplierService } from '../../../shared/services/supplier.service';
import { SupplierDetailsDTO, SupplierListItemDTO, SupplierStatsDTO, SupplierTypeLabels } from '../../../shared/models/supplier.model';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { AdminService, ReferenceItemDTO } from '../../../shared/services/admin.service';

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
  selectedSectorId: number | null = null;
  selectedStatus: boolean | null = null;
  sectors: ReferenceItemDTO[] = [];

  // Stats
  stats: SupplierStatsDTO | null = null;
  typeLabels = SupplierTypeLabels;

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
    this.loadSectors();
    this.loadStats();
  }

  loadSectors(): void {
    this.adminService.getAllCompanySectors().subscribe({
      next: (sectors) => this.sectors = sectors,
      error: (err) => console.error('Error loading sectors', err)
    });
  }

  loadStats(): void {
    this.supplierService.getStats().subscribe({
      next: (stats) => this.stats = stats,
      error: (err) => console.error('Error loading stats', err)
    });
  }

  loadSuppliers(): void {
    this.isLoading = true;
    this.supplierService.getSuppliers({
      page: this.currentPage,
      size: this.pageSize,
      search: this.searchTerm,
      sectorId: this.selectedSectorId || undefined,
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
