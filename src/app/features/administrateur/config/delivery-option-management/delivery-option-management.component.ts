import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../../core/layouts/main-layout/main-layout.component';
import { AdminService, DeliveryOptionDTO } from '../../../../shared/services/admin.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-delivery-option-management',
  standalone: true,
  imports: [CommonModule, FormsModule, MainLayoutComponent],
  templateUrl: './delivery-option-management.component.html'
})
export class DeliveryOptionManagementComponent implements OnInit {
  role: 'admin' = 'admin';
  items: DeliveryOptionDTO[] = [];
  loading = false;
  showModal = false;
  currentItem: DeliveryOptionDTO = { name: '', description: '', isActive: true };

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadItems();
  }

  loadItems(): void {
    this.loading = true;
    this.adminService.getAllDeliveryOptions().subscribe({
      next: (data) => {
        this.items = data;
        this.loading = false;
      },
      error: () => {
        Swal.fire('Erreur', 'Impossible de charger les options de livraison', 'error');
        this.loading = false;
      }
    });
  }

  openAddModal(): void {
    this.currentItem = { name: '', description: '', isActive: true };
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveItem(): void {
    if (!this.currentItem.name.trim()) {
      Swal.fire('Attention', 'Le nom est obligatoire', 'warning');
      return;
    }

    this.adminService.createDeliveryOption(this.currentItem).subscribe({
      next: () => {
        Swal.fire('Succès', 'Option de livraison enregistrée avec succès', 'success');
        this.closeModal();
        this.loadItems();
      },
      error: (err) => {
        Swal.fire('Erreur', err.error || 'Une erreur est survenue', 'error');
      }
    });
  }
}
