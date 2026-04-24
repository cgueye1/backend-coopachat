import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MainLayoutComponent } from '../../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../../core/layouts/header/header.component';
import { SupplierService } from '../../../../shared/services/supplier.service';
import { AdminService, ReferenceItemDTO } from '../../../../shared/services/admin.service';
import { CreateSupplierDTO, UpdateSupplierDTO, SupplierType, SupplierTypeLabels } from '../../../../shared/models/supplier.model';
import Swal from 'sweetalert2';

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
    sectorId: null,
    description: '',
    address: '',
    phone: '',
    email: '',
    contactName: '',
    ninea: '',
    deliveryTime: '',
    isActive: true
  };

  sectors: ReferenceItemDTO[] = [];
  supplierTypes = Object.entries(SupplierTypeLabels).map(([key, label]) => ({ key, label }));
  editId: number | null = null;
  saving: boolean = false;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private supplierService: SupplierService,
    private adminService: AdminService
  ) {}

  ngOnInit(): void {
    this.loadSectors();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId = +id;
      this.loadSupplier();
    }
  }

  loadSectors(): void {
    this.adminService.getAllCompanySectors().subscribe({
      next: (sectors) => this.sectors = sectors,
      error: (err) => console.error('Error loading sectors', err)
    });
  }

  loadSupplier(): void {
    if (this.editId) {
      this.supplierService.getSupplierById(this.editId).subscribe({
        next: (data) => {
          this.supplier = { ...data };
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
    const msg = err.error?.message || err.error || 'Une erreur est survenue';
    Swal.fire({
      title: 'Erreur',
      text: msg,
      icon: 'error',
      confirmButtonText: 'OK'
    });
  }

  annuler(): void {
    this.router.navigate(['/admin/suppliers']);
  }
}
