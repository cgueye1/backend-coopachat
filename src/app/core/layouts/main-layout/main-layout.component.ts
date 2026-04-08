import { Component, Input, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { MyAccountModalState } from '../../../shared/services/my-account-modal.service';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { UserProfileDetailModalComponent } from '../../../shared/components/user-profile-detail-modal/user-profile-detail-modal.component';
import { MyAccountModalService } from '../../../shared/services/my-account-modal.service';

type Role = 'log' | 'com' | 'admin' | 'commercial';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, SidebarComponent, UserProfileDetailModalComponent],
  templateUrl: './main-layout.component.html',
  styles: [`
    :host {
      display: block;
      min-height: 100vh;
    }
  `]
})
export class MainLayoutComponent {
  @Input() role?: Role;
  @ViewChild(SidebarComponent) sidebar!: SidebarComponent;

  readonly myAccountState$: Observable<MyAccountModalState>;

  isSidebarOpen = false;

  constructor(
    private readonly router: Router,
    private readonly myAccountModalService: MyAccountModalService
  ) {
    this.myAccountState$ = this.myAccountModalService.state$;
  }

  toggleSidebar() {
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  closeSidebar() {
    this.isSidebarOpen = false;
  }

  onMyAccountModalClosed(): void {
    this.myAccountModalService.close();
  }

  onMyAccountModalModify(): void {
    const user = this.myAccountModalService.getSnapshot().user;
    this.myAccountModalService.close();
    if (user?.id != null) {
      this.router.navigate(['/admin/users/edit', user.id]);
    }
  }

  /** Normalisation du rôle */
  get normalizedRole(): 'log' | 'com' | 'admin' {
    if (!this.role) return 'log';
    if (this.role === 'commercial') return 'com';
    if (this.role === 'com' || this.role === 'log' || this.role === 'admin') return this.role;
    return 'log';
  }
}
