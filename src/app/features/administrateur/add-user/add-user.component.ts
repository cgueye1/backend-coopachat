import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { map, of, switchMap } from 'rxjs';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { AdminService } from '../../../shared/services/admin.service';
import { AuthService } from '../../../shared/services/auth.service';
import Swal from 'sweetalert2';

/** Libellé affiché → enum backend (POST/PUT /users). */
const ROLE_LABEL_TO_ENUM: Record<string, string> = {
    'Administrateur': 'ADMINISTRATOR',
    'Commercial': 'COMMERCIAL',
    'Livreur': 'DELIVERY_DRIVER',
    'Responsable Logistique': 'LOGISTICS_MANAGER',
    'Fournisseur': 'SUPPLIER'
};

@Component({
    selector: 'app-add-user',
    standalone: true,
    imports: [MainLayoutComponent, HeaderComponent, CommonModule, FormsModule],
    templateUrl: './add-user.component.html',
    styles: ``
})
export class AddUserComponent implements OnInit {
    newUser = {
        prenom: '',
        nom: '',
        email: '',
        telephone: '',
        role: '',
        /** Obligatoire si rôle = Commercial (admin uniquement). */
        entreprise: ''
    };

    errors = {
        prenom: '',
        nom: '',
        email: '',
        telephone: '',
        role: '',
        entreprise: ''
    };

    /** Fichier photo de profil sélectionné (création ou modification). */
    profilePhotoFile: File | null = null;
    /** URL de prévisualisation (object URL) pour la photo. */
    profilePhotoPreview: string | null = null;
    /** URL actuelle de la photo en mode édition (affichée si pas de nouvelle sélection). */
    currentProfilePhotoUrl: string | null = null;

    /** Pas de « Salarié » : création via le flux commercial. */
    roles = ['Administrateur', 'Commercial', 'Livreur', 'Responsable Logistique', 'Fournisseur'];
    showRoleDropdown = false;
    editId: number | null = null;
    saving = false;
    /** Appel DELETE photo en cours (édition utilisateur). */
    removingPhoto = false;

    /** Route /profile/edit : commercial / RL modifient leur compte via PUT /api/auth/me. */
    selfEditMode = false;
    /** Rôle pour la sidebar (même layout que les écrans log / com). */
    mainLayoutRole: 'log' | 'com' | 'admin' = 'admin';

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private adminService: AdminService,
        private authService: AuthService
    ) { }

    get headerTitle(): string {
        if (this.selfEditMode) {
            return 'Modifier mon profil';
        }
        return this.editId ? 'Modifier l\'utilisateur' : 'Nouveau utilisateur';
    }

    get headerBreadcrumb(): string {
        if (this.selfEditMode) {
            return 'Pages / Mon compte / Modifier';
        }
        return this.editId ? 'Pages / Gestion des utilisateurs / Modifier' : 'Pages / Gestion des utilisateurs / Nouveau utilisateur';
    }

    ngOnInit() {
        if (this.router.url.includes('/profile/edit')) {
            this.selfEditMode = true;
            this.mainLayoutRole = this.guessLayoutFromSessionRole();
            this.authService.getCurrentUserProfile().subscribe({
                next: (u) => {
                    this.mainLayoutRole = this.mapBackendRoleToLayoutRole(u.role);
                    this.newUser = {
                        prenom: u.firstName ?? '',
                        nom: u.lastName ?? '',
                        email: u.email ?? '',
                        telephone: u.phoneNumber ?? '',
                        role: u.roleLabel ?? '',
                        entreprise: u.companyCommercial ?? ''
                    };
                    if (u.profilePhotoUrl) {
                        this.currentProfilePhotoUrl = this.adminService.getProfilePhotoUrl(u.profilePhotoUrl);
                    }
                },
                error: () => this.router.navigate([this.dashboardHomePath()])
            });
            return;
        }

        const id = this.route.snapshot.paramMap.get('id');
        if (id) {
            this.editId = +id;
            if (!isNaN(this.editId)) {
                this.adminService.getUserById(this.editId).subscribe({
                    next: (u) => {
                        this.newUser = {
                            prenom: u.firstName ?? '',
                            nom: u.lastName ?? '',
                            email: u.email ?? '',
                            telephone: u.phoneNumber ?? '',
                            role: u.roleLabel ?? '',
                            entreprise: u.companyCommercial ?? ''
                        };
                        if (u.profilePhotoUrl) {
                            this.currentProfilePhotoUrl = this.adminService.getProfilePhotoUrl(u.profilePhotoUrl);
                        }
                    },
                    error: () => this.router.navigate(['/admin/users'])
                });
            }
        }
    }

    private guessLayoutFromSessionRole(): 'log' | 'com' | 'admin' {
        const r = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem('role') || '' : '';
        if (r.includes('Commercial')) {
            return 'com';
        }
        if (r.includes('Logistique')) {
            return 'log';
        }
        return 'log';
    }

    private mapBackendRoleToLayoutRole(role: string): 'log' | 'com' | 'admin' {
        const r = (role || '').toUpperCase();
        if (r === 'COMMERCIAL') {
            return 'com';
        }
        if (r === 'LOGISTICS_MANAGER') {
            return 'log';
        }
        return 'admin';
    }

    private dashboardHomePath(): string {
        if (this.mainLayoutRole === 'com') {
            return '/com/dashboard';
        }
        if (this.mainLayoutRole === 'log') {
            return '/log/dashboardlog';
        }
        return '/admin/dashboardadmin';
    }

    goBack() {
        if (this.selfEditMode) {
            this.router.navigate([this.dashboardHomePath()]);
        } else {
            this.router.navigate(['/admin/users']);
        }
    }

    toggleRoleDropdown() {
        this.showRoleDropdown = !this.showRoleDropdown;
    }

    selectRole(role: string) {
        this.newUser.role = role;
        if (role !== 'Commercial') {
            this.newUser.entreprise = '';
        }
        this.showRoleDropdown = false;
        this.errors.role = '';
        this.errors.entreprise = '';
    }

    onProfilePhotoChange(event: Event): void {
        const input = event.target as HTMLInputElement;
        const file = input?.files?.[0];
        if (!file) return;
        if (!file.type.startsWith('image/')) {
            return;
        }
        if (this.profilePhotoPreview) {
            URL.revokeObjectURL(this.profilePhotoPreview);
        }
        this.profilePhotoFile = file;
        this.profilePhotoPreview = URL.createObjectURL(file);
    }

    clearProfilePhoto(input?: HTMLInputElement): void {
        if (this.profilePhotoPreview || this.profilePhotoFile) {
            if (this.profilePhotoPreview) {
                URL.revokeObjectURL(this.profilePhotoPreview);
            }
            this.profilePhotoFile = null;
            this.profilePhotoPreview = null;
            if (input) input.value = '';
            return;
        }
        if (this.editId != null && this.currentProfilePhotoUrl) {
            this.removingPhoto = true;
            this.adminService.deleteUserProfilePhoto(this.editId).subscribe({
                next: () => {
                    this.removingPhoto = false;
                    this.currentProfilePhotoUrl = null;
                    if (input) input.value = '';
                    Swal.fire({
                        title: 'Photo retirée',
                        icon: 'success',
                        timer: 1500,
                        showConfirmButton: false
                    });
                },
                error: (err: { error?: string }) => {
                    this.removingPhoto = false;
                    const msg = typeof err.error === 'string' ? err.error : 'Impossible de retirer la photo';
                    Swal.fire({ title: 'Erreur', text: msg, icon: 'error', confirmButtonText: 'OK' });
                }
            });
            return;
        }
        if (input) input.value = '';
    }

    validateForm(): boolean {
        let isValid = true;
        this.errors = {
            prenom: '',
            nom: '',
            email: '',
            telephone: '',
            role: '',
            entreprise: ''
        };

        if (!this.newUser.prenom.trim()) {
            this.errors.prenom = 'Le prénom est requis';
            isValid = false;
        }

        if (!this.newUser.nom.trim()) {
            this.errors.nom = 'Le nom est requis';
            isValid = false;
        }

        if (!this.newUser.email.trim()) {
            this.errors.email = "L'email est requis";
            isValid = false;
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.newUser.email)) {
            this.errors.email = 'Email invalide';
            isValid = false;
        }

        if (!this.newUser.telephone.trim()) {
            this.errors.telephone = 'Le téléphone est requis';
            isValid = false;
        }

        if (!this.selfEditMode && !this.newUser.role) {
            this.errors.role = 'Le rôle est requis';
            isValid = false;
        }

        if (!this.selfEditMode && this.newUser.role === 'Commercial' && !this.newUser.entreprise.trim()) {
            this.errors.entreprise = 'L’entreprise / entité est obligatoire pour un commercial';
            isValid = false;
        }

        return isValid;
    }

    enregistrer() {
        if (!this.validateForm() || this.saving) return;
        if (this.selfEditMode) {
            this.saving = true;
            this.authService.updateMyProfile({
                firstName: this.newUser.prenom.trim(),
                lastName: this.newUser.nom.trim(),
                email: this.newUser.email.trim(),
                phoneNumber: this.newUser.telephone.trim()
            }).pipe(
                switchMap((res) => {
                    this.authService.applyProfileUpdateResponse(res);
                    if (this.profilePhotoFile) {
                        return this.authService.updateMyProfilePhoto(this.profilePhotoFile).pipe(map(() => void 0));
                    }
                    return of(undefined);
                })
            ).subscribe({
                next: () => {
                    this.saving = false;
                    this.showSuccessMessage('Profil mis à jour avec succès');
                },
                error: (err) => {
                    this.saving = false;
                    this.showApiError(err);
                }
            });
            return;
        }
        const roleEnum = ROLE_LABEL_TO_ENUM[this.newUser.role];
        if (!roleEnum) {
            this.errors.role = 'Rôle invalide';
            return;
        }
        this.saving = true;
        if (this.editId != null) {
            this.adminService.updateUser(this.editId, {
                firstName: this.newUser.prenom.trim(),
                lastName: this.newUser.nom.trim(),
                email: this.newUser.email.trim(),
                phoneNumber: this.newUser.telephone.trim(),
                role: this.newUser.role,
                companyCommercial: this.newUser.role === 'Commercial' ? this.newUser.entreprise.trim() : undefined,
                profilePhoto: this.profilePhotoFile ?? undefined
            }).subscribe({
                next: () => {
                    this.saving = false;
                    const msg = this.profilePhotoFile
                        ? 'Utilisateur mis à jour avec succès. La photo de profil sera visible pour cet utilisateur après qu\'il ait rechargé sa page ou se soit reconnecté.'
                        : 'Utilisateur mis à jour avec succès';
                    this.showSuccessMessage(msg);
                },
                error: (err) => {
                    this.saving = false;
                    this.showApiError(err);
                }
            });
        } else {
            this.adminService.createUser({
                firstName: this.newUser.prenom.trim(),
                lastName: this.newUser.nom.trim(),
                email: this.newUser.email.trim(),
                phoneNumber: this.newUser.telephone.trim(),
                role: roleEnum,
                companyCommercial: this.newUser.role === 'Commercial' ? this.newUser.entreprise.trim() : undefined,
                profilePhoto: this.profilePhotoFile ?? undefined
            }).subscribe({
                next: () => {
                    this.saving = false;
                    this.showSuccessMessage('Utilisateur créé avec succès.');
                },
                error: (err) => {
                    this.saving = false;
                    this.showApiError(err);
                }
            });
        }
    }

     // Message d'erreur en cas d'erreur
    private showApiError(err: { error?: string; message?: string; status?: number }) {
        const raw = typeof err.error === 'string' ? err.error : (err.message || '');
        const msg = this.getFriendlyErrorMessage(raw, err.status);
        Swal.fire({
            title: 'Erreur',
            text: msg,
            icon: 'error',
            confirmButtonText: 'OK'
        });
    }

    private getFriendlyErrorMessage(raw: string, status?: number): string {
        if (!raw) {
            if (status === 0) return 'Impossible de contacter le serveur. Vérifiez votre connexion.';
            if (status === 403) return 'Vous n\'avez pas les droits pour effectuer cette action.';
            if (status === 500) return 'Une erreur interne s\'est produite. Veuillez réessayer.';
            return 'Une erreur est survenue lors de l\'enregistrement.';
        }
        // Messages connus côté backend → on les affiche tels quels
        const knownMessages = [
            'Le prénom est obligatoire',
            'Le nom est obligatoire',
            'L\'adresse email est obligatoire',
            'Le téléphone est obligatoire',
            'Le rôle est obligatoire',
            'Cet email est déjà utilisé',
            'Ce numéro de téléphone est déjà utilisé',
            'L\'entreprise / entité est obligatoire pour un commercial',
            'Seul un administrateur peut créer un utilisateur',
        ];
        for (const known of knownMessages) {
            if (raw.includes(known)) return known;
        }
        // Messages techniques (SQL, Java) → message générique
        if (raw.includes('Data truncated') || raw.includes('could not execute') ||
            raw.includes('constraint') || raw.includes('duplicate') ||
            raw.includes('Exception') || raw.includes('stack') ||
            raw.includes('Hibernate') || raw.includes('JDBC')) {
            return 'Une erreur technique est survenue. Veuillez vérifier les informations saisies ou contacter l\'administrateur.';
        }
        // Si le message est court et lisible, on l'affiche
        if (raw.length < 120) return raw;
        return 'Une erreur est survenue lors de l\'enregistrement.';
    }

    showSuccessMessage(title: string) {
        Swal.fire({
            title,
            iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
            showConfirmButton: false,
            timer: 1500,
            buttonsStyling: false,
            customClass: {
                popup: 'rounded-3xl p-6',
                title: 'text-xl font-medium text-gray-900',
                icon: 'border-none'
            },
            backdrop: `rgba(0,0,0,0.2)`,
            width: '580px',
            showClass: {
                popup: 'animate__animated animate__fadeIn animate__faster'
            },
            hideClass: {
                popup: 'animate__animated animate__fadeOut animate__faster'
            }
        }).then(() => {
            this.router.navigate([this.selfEditMode ? this.dashboardHomePath() : '/admin/users']);
        });
    }

    annuler() {
        if (this.selfEditMode) {
            this.router.navigate([this.dashboardHomePath()]);
        } else {
            this.router.navigate(['/admin/users']);
        }
    }
}
