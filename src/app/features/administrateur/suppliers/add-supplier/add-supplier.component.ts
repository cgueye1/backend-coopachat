import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MainLayoutComponent } from '../../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../../core/layouts/header/header.component';
import { SupplierService } from '../../../../shared/services/supplier.service';
import { AdminService, CategoryListItemDTO } from '../../../../shared/services/admin.service';
import { CreateSupplierDTO, UpdateSupplierDTO, SupplierType, SupplierTypeLabels, SupplierDetailsDTO } from '../../../../shared/models/supplier.model';
import Swal from 'sweetalert2';
import { ErrorHandlerService } from '../../../../core/services/error-handler.service';

@Component({
  selector: 'app-add-supplier',
  standalone: true,
  imports: [CommonModule, FormsModule, MainLayoutComponent, HeaderComponent],
  templateUrl: './add-supplier.component.html',
  styles: []
})
export class AddSupplierComponent implements OnInit {
  supplier: any = {
    name: '',
    type: '',
    categoryIds: [],
    description: '',
    address: '',
    phone: '',
    email: '',
    contactName: '',
    ninea: '',
    deliveryTime: '',
    isActive: true
  };

  categories: CategoryListItemDTO[] = [];
  filteredCategories: CategoryListItemDTO[] = [];
  categorySearchTerm: string = '';
  isDropdownOpen: boolean = false;
  supplierTypes = Object.entries(SupplierTypeLabels).map(([key, label]) => ({ key, label }));
  editId: number | null = null;
  saving: boolean = false;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private supplierService: SupplierService,
    private adminService: AdminService,
    private errorHandler: ErrorHandlerService
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId = +id;
      this.loadSupplier();
    }
  }

  loadCategories(): void {
    this.adminService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.filteredCategories = categories;
      },
      error: (err) => console.error('Error loading categories', err)
    });
  }

  filterCategories(): void {
    if (!this.categorySearchTerm) {
      this.filteredCategories = this.categories;
    } else {
      const search = this.categorySearchTerm.toLowerCase();
      this.filteredCategories = this.categories.filter(cat => 
        cat.name.toLowerCase().includes(search)
      );
    }
  }

  toggleDropdown(): void {
    this.isDropdownOpen = !this.isDropdownOpen;
    if (this.isDropdownOpen) {
      this.categorySearchTerm = '';
      this.filteredCategories = this.categories;
    }
  }

  getSelectedCategoriesLabels(): string {
    if (!this.supplier.categoryIds || this.supplier.categoryIds.length === 0) {
      return 'Sélectionner des catégories';
    }
    const selected = this.categories.filter(cat => this.supplier.categoryIds.includes(cat.id));
    return selected.map(cat => cat.name).join(', ');
  }

  loadSupplier(): void {
    if (this.editId) {
      this.supplierService.getSupplierById(this.editId).subscribe({
        next: (data: SupplierDetailsDTO) => {
          this.supplier = { 
            name: data.name || '',
            type: data.type || '',
            categoryIds: data.categories?.map(c => c.id) || [],
            description: data.description || '',
            address: data.address || '',
            phone: data.phone || '',
            email: data.email || '',
            contactName: data.contactName || '',
            ninea: data.ninea || '',
            deliveryTime: data.deliveryTime || '',
            isActive: data.isActive ?? true
          };
        },
        error: (err) => {
          console.error('Error loading supplier', err);
          this.router.navigate(['/admin/suppliers']);
        }
      });
    }
  }

  onSubmit(): void {
    if (this.saving) return;
    this.saving = true;

    if (this.editId) {
      const dto: UpdateSupplierDTO = { ...this.supplier };
      this.supplierService.updateSupplier(this.editId, dto).subscribe({
        next: () => {
          this.saving = false;
          this.showSuccess('Fournisseur mis à jour avec succès');
        },
        error: (err) => {
          this.saving = false;
          this.showError(err);
        }
      });
    } else {
      const dto: CreateSupplierDTO = { ...this.supplier };
      this.supplierService.createSupplier(dto).subscribe({
        next: () => {
          this.saving = false;
          this.showSuccess('Fournisseur créé avec succès');
        },
        error: (err) => {
          this.saving = false;
          this.showError(err);
        }
      });
    }
  }

  showSuccess(message: string): void {
    Swal.fire({
      title: 'Succès',
      text: message,
      icon: 'success',
      timer: 1500,
      showConfirmButton: false
    }).then(() => {
      this.router.navigate(['/admin/suppliers']);
    });
  }

  showError(err: any): void {
    this.errorHandler.showError(err);
  }

  onCategoryToggle(categoryId: number): void {
    const index = this.supplier.categoryIds.indexOf(categoryId);
    if (index > -1) {
      this.supplier.categoryIds.splice(index, 1);
    } else {
      this.supplier.categoryIds.push(categoryId);
    }
  }

  isCategorySelected(categoryId: number): boolean {
    return this.supplier.categoryIds?.includes(categoryId);
  }

  annuler(): void {
    this.router.navigate(['/admin/suppliers']);
  }
}
