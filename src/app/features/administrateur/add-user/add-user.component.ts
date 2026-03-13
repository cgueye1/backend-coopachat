import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { AdminService } from '../../../shared/services/admin.service';
import Swal from 'sweetalert2';

/** Libellé affiché → enum backend (POST/PUT /users). */
const ROLE_LABEL_TO_ENUM: Record<string, string> = {
    'Administrateur': 'ADMINISTRATOR',
    'Commercial': 'COMMERCIAL',
    'Livreur': 'DELIVERY_DRIVER',
    'Salarié': 'EMPLOYEE',
    'Responsable Logistique': 'LOGISTICS_MANAGER'
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
        role: ''
    };

    errors = {
        prenom: '',
        nom: '',
        email: '',
        telephone: '',
        role: ''
    };

    /** Fichier photo de profil sélectionné (création ou modification). */
    profilePhotoFile: File | null = null;
    /** URL de prévisualisation (object URL) pour la photo. */
    profilePhotoPreview: string | null = null;
    /** URL actuelle de la photo en mode édition (affichée si pas de nouvelle sélection). */
    currentProfilePhotoUrl: string | null = null;

    roles = ['Administrateur', 'Commercial', 'Livreur', 'Salarié', 'Responsable Logistique'];
    showRoleDropdown = false;
    editId: number | null = null;
    saving = false;

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private adminService: AdminService
    ) { }

    ngOnInit() {
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
                            role: u.roleLabel ?? ''
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

    goBack() {
        this.router.navigate(['/admin/users']);
    }

    toggleRoleDropdown() {
        this.showRoleDropdown = !this.showRoleDropdown;
    }

    selectRole(role: string) {
        this.newUser.role = role;
        this.showRoleDropdown = false;
        this.errors.role = '';
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
        if (this.profilePhotoPreview) {
            URL.revokeObjectURL(this.profilePhotoPreview);
        }
        this.profilePhotoFile = null;
        this.profilePhotoPreview = null;
        if (input) input.value = '';
    }

    validateForm(): boolean {
        let isValid = true;
        this.errors = {
            prenom: '',
            nom: '',
            email: '',
            telephone: '',
            role: ''
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

        if (!this.newUser.role) {
            this.errors.role = 'Le rôle est requis';
            isValid = false;
        }

        return isValid;
    }

    enregistrer() {
        if (!this.validateForm() || this.saving) return;
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
                profilePhoto: this.profilePhotoFile ?? undefined
            }).subscribe({
                next: () => {
                    this.saving = false;
                    this.showSuccessMessage('Utilisateur créé avec succès. Un code d\'activation a été envoyé par email.');
                },
                error: (err) => {
                    this.saving = false;
                    this.showApiError(err);
                }
            });
        }
    }

    private showApiError(err: { error?: string; message?: string; status?: number }) {
        const msg = typeof err.error === 'string' ? err.error : (err.message || 'Erreur lors de l\'enregistrement');
        Swal.fire({
            title: 'Erreur',
            text: msg,
            icon: 'error',
            confirmButtonText: 'OK'
        });
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
            this.router.navigate(['/admin/users']);
        });
    }

    annuler() {
        this.router.navigate(['/admin/users']);
    }
}
