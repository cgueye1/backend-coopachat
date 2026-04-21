import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AdminService, ReferenceItemDTO } from '../../../../shared/services/admin.service';
import Swal from 'sweetalert2';


//Composant générique pour la gestion des référentiels similaires 
@Component({
  selector: 'app-reference-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reference-management.component.html'
})
export class ReferenceManagementComponent implements OnInit {
  title = '';
  type: 'claim-types' | 'delivery-reasons' | 'employee-reasons' | 'activity-sectors' = 'claim-types';
  items: ReferenceItemDTO[] = [];
  loading = false;

  showModal = false;
  isEdit = false;
  currentItem: ReferenceItemDTO = { name: '', description: '' };

  constructor(
    private adminService: AdminService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.url.subscribe(url => {
      const path = url[url.length - 1].path;
      this.setupType(path);
      this.loadItems();
    });
  }

  setupType(path: string): void {
    switch (path) {
      case 'claim-types':
        this.type = 'claim-types';
        this.title = 'Types de réclamation';
        break;
      case 'delivery-reasons':
        this.type = 'delivery-reasons';
        this.title = 'Motifs incidents (Livreur)';
        break;
      case 'employee-reasons':
        this.type = 'employee-reasons';
        this.title = 'Motifs incidents (Salarié)';
        break;
      case 'activity-sectors':
        this.type = 'activity-sectors';
        this.title = 'Secteurs d\'activité';
        break;
    }
  }

  loadItems(): void {
    this.loading = true;
    let obs$;
    switch (this.type) {
      case 'claim-types': obs$ = this.adminService.getAllClaimProblemTypes(); break;
      case 'delivery-reasons': obs$ = this.adminService.getAllDeliveryIssueReasons(); break;
      case 'employee-reasons': obs$ = this.adminService.getAllEmployeeDeliveryIssueReasons(); break;
      case 'activity-sectors': obs$ = this.adminService.getAllCompanySectors(); break;
    }

    obs$.subscribe({
      next: (data) => {
        this.items = data;
        this.loading = false;
      },
      error: () => {
        Swal.fire('Erreur', 'Impossible de charger les données', 'error');
        this.loading = false;
      }
    });
  }

  openAddModal(): void {
    this.isEdit = false;
    this.currentItem = { name: '', description: '' };
    this.showModal = true;
  }

  openEditModal(item: ReferenceItemDTO): void {
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

    let obs$;
    if (this.isEdit && this.currentItem.id) {
      switch (this.type) {
        case 'claim-types': obs$ = this.adminService.updateClaimProblemType(this.currentItem.id, this.currentItem); break;
        case 'delivery-reasons': obs$ = this.adminService.updateDeliveryIssueReason(this.currentItem.id, this.currentItem); break;
        case 'employee-reasons': obs$ = this.adminService.updateEmployeeDeliveryIssueReason(this.currentItem.id, this.currentItem); break;
        case 'activity-sectors': obs$ = this.adminService.updateCompanySector(this.currentItem.id, this.currentItem); break;
      }
    } else {
      switch (this.type) {
        case 'claim-types': obs$ = this.adminService.createClaimProblemType(this.currentItem); break;
        case 'delivery-reasons': obs$ = this.adminService.createDeliveryIssueReason(this.currentItem); break;
        case 'employee-reasons': obs$ = this.adminService.createEmployeeDeliveryIssueReason(this.currentItem); break;
        case 'activity-sectors': obs$ = this.adminService.createCompanySector(this.currentItem); break;
      }
    }

    obs$.subscribe({
      next: () => {
        Swal.fire('Succès', 'Enregistré avec succès', 'success');
        this.closeModal();
        this.loadItems();
      },
      error: (err) => {
        Swal.fire('Erreur', err.error || 'Une erreur est survenue', 'error');
      }
    });
  }

  deleteItem(item: ReferenceItemDTO): void {
    if (!item.id) return;

    Swal.fire({
      title: 'Êtes-vous sûr ?',
      text: 'Cette action est irréversible',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Oui, supprimer',
      cancelButtonText: 'Annuler'
    }).then((result) => {
      if (result.isConfirmed) {
        let obs$;
        switch (this.type) {
          case 'claim-types': obs$ = this.adminService.deleteClaimProblemType(item.id!); break;
          case 'delivery-reasons': obs$ = this.adminService.deleteDeliveryIssueReason(item.id!); break;
          case 'employee-reasons': obs$ = this.adminService.deleteEmployeeDeliveryIssueReason(item.id!); break;
          case 'activity-sectors': obs$ = this.adminService.deleteCompanySector(item.id!); break;
        }

        obs$.subscribe({
          next: () => {
            Swal.fire('Supprimé', 'L\'élément a été supprimé', 'success');
            this.loadItems();
          },
          error: (err) => {
            Swal.fire('Erreur', err.error || 'Impossible de supprimer cet élément', 'error');
          }
        });
      }
    });
  }
}
