import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../../core/layouts/header/header.component';
import { AdminService, DeliveryOptionDTO } from '../../../../shared/services/admin.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-delivery-option-management',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    MainLayoutComponent, 
    HeaderComponent
  ],
  templateUrl: './delivery-option-management.component.html'
})
export class DeliveryOptionManagementComponent implements OnInit {
  role: 'admin' = 'admin';
  items: DeliveryOptionDTO[] = [];
  loading = false;
  showModal = false;
  currentItem: DeliveryOptionDTO = { name: '', description: '', isActive: true };
  isEdit = false;

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
    this.isEdit = false;
    this.currentItem = { name: '', description: '', isActive: true };
    this.showModal = true;
  }

  openEditModal(item: DeliveryOptionDTO): void {
    this.isEdit = true;
    this.currentItem = { ...item };
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

    const obs = this.isEdit && this.currentItem.id
      ? this.adminService.updateDeliveryOption(this.currentItem.id, this.currentItem)
      : this.adminService.createDeliveryOption(this.currentItem);

    obs.subscribe({
      next: () => {
        Swal.fire('Succès', `Option de livraison ${this.isEdit ? 'mise à jour' : 'enregistrée'} avec succès`, 'success');
        this.closeModal();
        this.loadItems();
      },
      error: (err) => {
        Swal.fire('Erreur', err.error || 'Une erreur est survenue', 'error');
      }
    });
  }

  deleteItem(item: DeliveryOptionDTO): void {
    if (!item.id) return;
    
    Swal.fire({
      title: 'Êtes-vous sûr ?',
      text: `Vous allez supprimer l'option "${item.name}"`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Oui, supprimer',
      cancelButtonText: 'Annuler'
    }).then((result) => {
      if (result.isConfirmed) {
        this.adminService.deleteDeliveryOption(item.id!).subscribe({
          next: () => {
            Swal.fire('Supprimé !', 'L\'option a été supprimée.', 'success');
            this.loadItems();
          },
          error: (err) => {
            Swal.fire('Erreur', err.error || 'Impossible de supprimer l\'option', 'error');
          }
        });
      }
    });
  }
}
