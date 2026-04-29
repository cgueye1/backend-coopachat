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
import Swal from 'sweetalert2';
import { ErrorHandlerService } from '../../../core/services/error-handler.service';

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
  toggleLoadingId: number | null = null;

  // Detail modal
  showDetailModal: boolean = false;
  selectedSupplierDetails: SupplierDetailsDTO | null = null;
  loadingDetails: boolean = false;

  // Category filter dropdown
  isCategoryDropdownOpen: boolean = false;
  categoryFilterSearch: string = '';
  filteredCategoriesForFilter: CategoryListItemDTO[] = [];

  // Stats
  stats: SupplierStatsDTO | null = null;
  typeLabels = SupplierTypeLabels;
  metricsData: { title: string, value: string, icon: string }[] = [];

  private searchSubject = new Subject<string>();

  constructor(
    private router: Router,
    private supplierService: SupplierService,
    private adminService: AdminService,
    private errorHandler: ErrorHandlerService
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
      next: (categories) => {
        this.categories = categories;
        this.filteredCategoriesForFilter = categories;
      },
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

  // ---- Category filter dropdown ----
  toggleCategoryFilterDropdown(): void {
    this.isCategoryDropdownOpen = !this.isCategoryDropdownOpen;
    if (this.isCategoryDropdownOpen) {
      this.categoryFilterSearch = '';
      this.filteredCategoriesForFilter = this.categories;
    }
  }

  selectCategoryFilter(id: number | null): void {
    this.selectedCategoryId = id;
    this.isCategoryDropdownOpen = false;
    this.categoryFilterSearch = '';
    this.filteredCategoriesForFilter = this.categories;
    this.currentPage = 0;
    this.loadSuppliers();
  }

  getSelectedCategoryLabel(): string {
    if (!this.selectedCategoryId) return 'Toutes les catégories';
    const found = this.categories.find(c => c.id === this.selectedCategoryId);
    return found ? found.name : 'Toutes les catégories';
  }

  filterCategoryDropdown(): void {
    const term = this.categoryFilterSearch.toLowerCase();
    this.filteredCategoriesForFilter = term
      ? this.categories.filter(c => c.name.toLowerCase().includes(term))
      : this.categories;
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
    this.showDetailModal = true;
    this.loadingDetails = true;
    this.selectedSupplierDetails = null;
    this.supplierService.getSupplierById(id).subscribe({
      next: (details) => {
        this.selectedSupplierDetails = details;
        this.loadingDetails = false;
      },
      error: (err) => {
        console.error('Error loading supplier details', err);
        this.loadingDetails = false;
      }
    });
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedSupplierDetails = null;
  }

  toggleStatusFromModal(): void {
    if (!this.selectedSupplierDetails) return;
    
    const supplier = this.selectedSupplierDetails;
    const newStatus = !supplier.isActive;
    const action = newStatus ? 'activer' : 'désactiver';

    Swal.fire({
      title: 'Êtes-vous sûr ?',
      text: `Voulez-vous vraiment ${action} le fournisseur ${supplier.name} ?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#2B3674',
      cancelButtonColor: '#CBD5E1',
      confirmButtonText: `Oui, ${action} !`,
      cancelButtonText: 'Annuler'
    }).then((result) => {
      if (result.isConfirmed) {
        this.toggleLoadingId = supplier.id!;
        this.supplierService.updateStatus(supplier.id!, { isActive: newStatus }).subscribe({
          next: (response) => {
            this.selectedSupplierDetails!.isActive = newStatus;
            
            // Synchroniser dans la liste
            const s = this.suppliers.find(x => x.id === supplier.id);
            if (s) s.isActive = newStatus;
            
            this.toggleLoadingId = 0;
            this.loadStats();

            // Modal de succès centrée
            Swal.fire({
              title: 'Succès',
              text: response || `Fournisseur ${newStatus ? 'activé' : 'désactivé'} avec succès`,
              icon: 'success',
              confirmButtonColor: '#2B3674',
              timer: 2000,
              showConfirmButton: false
            });
          },
          error: (err) => {
            console.error('Error toggling status', err);
            this.toggleLoadingId = 0;
            this.errorHandler.showError(err);
          }
        });
      }
    });
  }

  toggleSupplierStatus(supplier: SupplierListItemDTO): void {
    const newStatus = !supplier.isActive;
    const action = newStatus ? 'activer' : 'désactiver';

    Swal.fire({
      title: 'Êtes-vous sûr ?',
      text: `Voulez-vous vraiment ${action} le fournisseur ${supplier.name} ?`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#2B3674',
      cancelButtonColor: '#CBD5E1',
      confirmButtonText: `Oui, ${action} !`,
      cancelButtonText: 'Annuler'
    }).then((result) => {
      if (result.isConfirmed) {
        this.toggleLoadingId = supplier.id;
        this.supplierService.updateStatus(supplier.id, { isActive: newStatus }).subscribe({
          next: (response) => {
            supplier.isActive = newStatus;
            this.toggleLoadingId = null;
            this.loadStats(); // Rafraîchir les compteurs en haut
            
            // Modal de succès centrée
            Swal.fire({
              title: 'Succès',
              text: response || `Fournisseur ${newStatus ? 'activé' : 'désactivé'} avec succès`,
              icon: 'success',
              confirmButtonColor: '#2B3674',
              timer: 2000,
              showConfirmButton: false
            });
          },
          error: (error) => {
            console.error('Error toggling status', error);
            this.toggleLoadingId = null;
            this.errorHandler.showError(error);
          }
        });
      }
    });
  }
}
