import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { AuthService } from '../../../shared/services/auth.service';
import { UserDetailsDTO } from '../../../shared/services/admin.service';
import { environment } from '../../../../environments/environment';

type LayoutRole = 'log' | 'com' | 'admin' | 'commercial';

@Component({
  selector: 'app-mon-profil',
  standalone: true,
  imports: [CommonModule, MainLayoutComponent, HeaderComponent],
  templateUrl: './mon-profil.component.html',
  styles: [``]
})
export class MonProfilComponent implements OnInit {
  profile: UserDetailsDTO | null = null;
  loading = true;
  error: string | null = null;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  /** Rôle pour le layout (sidebar) à partir du storage. */
  get layoutRole(): LayoutRole {
    const r =
      typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('role') : null
      || typeof localStorage !== 'undefined' ? localStorage.getItem('role') : null
      || '';
    const s = (r as string).toLowerCase();
    if (s.includes('administrateur') || s === 'admin') return 'admin';
    if (s.includes('commercial') || s === 'com') return 'com';
    if (s.includes('logistique') || s.includes('responsable') || s.includes('livreur') || s === 'log') return 'log';
    return 'log';
  }

  ngOnInit(): void {
    this.authService.getCurrentUserProfile().subscribe({
      next: (data) => {
        this.profile = data;
        this.loading = false;
        this.error = null;
      },
      error: () => {
        this.error = 'Impossible de charger le profil.';
        this.loading = false;
      }
    });
  }

  get initials(): string {
    if (!this.profile) return '';
    const f = (this.profile.firstName || '').trim();
    const l = (this.profile.lastName || '').trim();
    if (f && l) return (f[0] + l[0]).toUpperCase();
    if (f) return f.slice(0, 2).toUpperCase();
    if (l) return l.slice(0, 2).toUpperCase();
    if (this.profile.email) return this.profile.email.slice(0, 2).toUpperCase();
    return '?';
  }

  get fullName(): string {
    if (!this.profile) return '';
    return [this.profile.firstName, this.profile.lastName].filter(Boolean).join(' ') || this.profile.email || '';
  }

  get createdAtIndex(): string {
    if (!this.profile?.createdAt) return '';
    const d = new Date(this.profile.createdAt);
    return isNaN(d.getTime()) ? String(this.profile.createdAt) : d.toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  get profilePhotoUrl(): string | null {
    if (!this.profile?.profilePhotoUrl) return null;
    const base = (environment as { imageServerUrl?: string }).imageServerUrl ?? '';
    return base ? `${base}/files/${this.profile.profilePhotoUrl}` : null;
  }

  back(): void {
    if (typeof history !== 'undefined' && history.length > 1) {
      history.back();
    } else {
      this.router.navigate(['/portail']);
    }
  }
}
