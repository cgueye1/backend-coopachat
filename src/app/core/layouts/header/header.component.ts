import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

type HeaderType = 'filter' | 'simple';

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

  @Output() actionClick = new EventEmitter<void>();

  onActionClick() {
    this.actionClick.emit();
  }
}
