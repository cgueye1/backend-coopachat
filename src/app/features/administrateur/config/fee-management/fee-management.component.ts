import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../../core/layouts/header/header.component';
import { AdminService, FeeDTO } from '../../../../shared/services/admin.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-fee-management',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    MainLayoutComponent, 
    HeaderComponent
  ],
  templateUrl: './fee-management.component.html'
})
export class FeeManagementComponent implements OnInit {
  role: 'admin' = 'admin';
  items: FeeDTO[] = [];
  loading = false;
  showModal = false;
  currentItem: FeeDTO = { name: '', description: '', amount: 0, isActive: true };
  isEdit = false;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadItems();
  }

  loadItems(): void {
    this.loading = true;
    this.adminService.getAllFees().subscribe({
      next: (data) => {
        this.items = data;
        this.loading = false;
      },
      error: () => {
        Swal.fire('Erreur', 'Impossible de charger les frais', 'error');
        this.loading = false;
      }
    });
  }

  openAddModal(): void {
    this.isEdit = false;
    this.currentItem = { name: '', description: '', amount: 0, isActive: true };
    this.showModal = true;
  }

  openEditModal(item: FeeDTO): void {
    this.isEdit = true;
    this.currentItem = { ...item };
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveItem(): void {
    if (!this.currentItem.name.trim() || this.currentItem.amount < 0) {
      Swal.fire('Attention', 'Veuillez remplir les champs obligatoires', 'warning');
      return;
    }

    const obs = this.isEdit && this.currentItem.id
      ? this.adminService.updateFee(this.currentItem.id, this.currentItem)
      : this.adminService.createFee(this.currentItem);

    obs.subscribe({
      next: () => {
        Swal.fire('Succès', `Frais ${this.isEdit ? 'mis à jour' : 'enregistré'} avec succès`, 'success');
        this.closeModal();
        this.loadItems();
      },
      error: (err) => {
        Swal.fire('Erreur', err.error || 'Une erreur est survenue', 'error');
      }
    });
  }

  deleteItem(item: FeeDTO): void {
    if (!item.id) return;
    
    Swal.fire({
      title: 'Êtes-vous sûr ?',
      text: `Vous allez supprimer le frais "${item.name}"`,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Oui, supprimer',
      cancelButtonText: 'Annuler'
    }).then((result) => {
      if (result.isConfirmed) {
        this.adminService.deleteFee(item.id!).subscribe({
          next: () => {
            Swal.fire('Supprimé !', 'Le frais a été supprimé.', 'success');
            this.loadItems();
          },
          error: (err) => {
            Swal.fire('Erreur', err.error || 'Impossible de supprimer le frais', 'error');
          }
        });
      }
    });
  }
}
