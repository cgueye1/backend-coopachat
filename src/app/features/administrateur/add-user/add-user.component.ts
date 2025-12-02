import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { UserService } from '../../../shared/services/user.service';
import Swal from 'sweetalert2';

@Component({
    selector: 'app-add-user',
    standalone: true,
    imports: [MainLayoutComponent, HeaderComponent, CommonModule, FormsModule],
    templateUrl: './add-user.component.html',
    styles: ``
})
export class AddUserComponent {
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

    roles = ['Administrateur', 'Commercial', 'Livreur', 'Salarié'];
    showRoleDropdown = false;

    constructor(private router: Router, private userService: UserService) { }

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
        if (this.validateForm()) {
            this.userService.addUser(this.newUser);
            this.showSuccessMessage();
        }
    }

    showSuccessMessage() {
        Swal.fire({
            title: 'Utilisateur ajouté avec succès',
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
