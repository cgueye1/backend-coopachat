import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';

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
  suppliers: any[] = []; // Placeholder for future data

  constructor(private router: Router) {}

  ngOnInit(): void {
    // Initial loading logic will go here
  }

  nouveauFournisseur(): void {
    this.router.navigate(['/admin/suppliers/add']);
  }

  onSearch(): void {
    // Search logic will go here
  }

  onExport(): void {
    // Export logic will go here
  }
}
