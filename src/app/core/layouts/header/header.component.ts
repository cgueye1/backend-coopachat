import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

type HeaderType = 'filter' | 'simple' | 'back';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './header.component.html'
})
export class HeaderComponent {
  @Input() type: HeaderType = 'filter';
  @Input() title: string = '';
  @Input() breadcrumb: string = '';
  @Input() buttonLabel = '';
  @Input() showSupplierFilter: boolean = true;
  @Input() supplierLabel: string = 'Fournisseurs';

  @Output() actionClick = new EventEmitter<void>();

  // Dropdown states
  isPeriodDropdownOpen = false;
  isSupplierDropdownOpen = false;

  // Selected values
  selectedPeriod = 'Cette année';
  selectedSupplier = 'Tous';

  periodOptions = ['Cette année', 'Ce mois', 'Cette semaine'];
  supplierOptions = ['Tous', 'Fournisseur 1', 'Fournisseur 2'];

  onActionClick() {
    this.actionClick.emit();
  }

  togglePeriodDropdown() {
    this.isPeriodDropdownOpen = !this.isPeriodDropdownOpen;
    this.isSupplierDropdownOpen = false;
  }

  toggleSupplierDropdown() {
    this.isSupplierDropdownOpen = !this.isSupplierDropdownOpen;
    this.isPeriodDropdownOpen = false;
  }

  selectPeriod(period: string) {
    this.selectedPeriod = period;
    this.isPeriodDropdownOpen = false;
  }

  selectSupplier(supplier: string) {
    this.selectedSupplier = supplier;
    this.isSupplierDropdownOpen = false;
  }

  closeDropdowns() {
    this.isPeriodDropdownOpen = false;
    this.isSupplierDropdownOpen = false;
  }
}
