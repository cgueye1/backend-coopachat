import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, FeeDTO } from '../../../../shared/services/admin.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-fee-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './fee-management.component.html'
})
export class FeeManagementComponent implements OnInit {
  items: FeeDTO[] = [];
  loading = false;
  showModal = false;
  currentItem: FeeDTO = { name: '', description: '', amount: 0, isActive: true };

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
    this.currentItem = { name: '', description: '', amount: 0, isActive: true };
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

    this.adminService.createFee(this.currentItem).subscribe({
      next: () => {
        Swal.fire('Succès', 'Frais enregistré avec succès', 'success');
        this.closeModal();
        this.loadItems();
      },
      error: (err) => {
        Swal.fire('Erreur', err.error || 'Une erreur est survenue', 'error');
      }
    });
  }
}
