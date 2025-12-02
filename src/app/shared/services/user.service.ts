import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface User {
    id: string;
    name: string;
    initials: string;
    email: string;
    role: string;
    createdAt: string;
    status: 'Actif' | 'Inactif';
    phone?: string;
}

@Injectable({
    providedIn: 'root'
})
export class UserService {
    private usersSubject = new BehaviorSubject<User[]>([
        {
            id: 'US-2025-05',
            name: 'Aminata Ndiaye',
            initials: 'AN',
            email: 'aminata@exemple.sn',
            role: 'Salarié',
            createdAt: '05/10/2025',
            status: 'Actif',
            phone: '70 645 87 92'
        },
        {
            id: 'US-2025-04',
            name: 'Moussa Sarr',
            initials: 'MS',
            email: 'moussa@exemple.sn',
            role: 'Commercial',
            createdAt: '05/10/2025',
            status: 'Actif',
            phone: '77 123 45 67'
        },
        {
            id: 'US-2025-03',
            name: 'Fatou Diop',
            initials: 'FD',
            email: 'fatou@exemple.sn',
            role: 'Livreur',
            createdAt: '05/10/2025',
            status: 'Actif',
            phone: '76 987 65 43'
        },
        {
            id: 'US-2025-02',
            name: 'Ibrahima Ba',
            initials: 'IB',
            email: 'ibrahima@exemple.sn',
            role: 'Salarié',
            createdAt: '05/10/2025',
            status: 'Actif',
            phone: '78 456 78 90'
        },
        {
            id: 'US-2025-01',
            name: 'Lamine Sy',
            initials: 'LS',
            email: 'lamine@exemple.sn',
            role: 'Administrateur',
            createdAt: '05/10/2025',
            status: 'Inactif',
            phone: '77 234 56 78'
        }
    ]);

    users$ = this.usersSubject.asObservable();

    getUsers(): User[] {
        return this.usersSubject.value;
    }

    addUser(userData: { prenom: string; nom: string; email: string; telephone: string; role: string }) {
        const users = this.usersSubject.value;
        const newId = `US-2025-${String(users.length + 1).padStart(2, '0')}`;
        const initials = `${userData.prenom.charAt(0)}${userData.nom.charAt(0)}`.toUpperCase();
        const today = new Date();
        const createdAt = `${String(today.getDate()).padStart(2, '0')}/${String(today.getMonth() + 1).padStart(2, '0')}/${today.getFullYear()}`;

        const newUser: User = {
            id: newId,
            name: `${userData.prenom} ${userData.nom}`,
            initials: initials,
            email: userData.email,
            role: userData.role,
            createdAt: createdAt,
            status: 'Actif',
            phone: userData.telephone
        };

        this.usersSubject.next([newUser, ...users]);
    }

    updateUser(userId: string, updates: Partial<User>) {
        const users = this.usersSubject.value;
        const index = users.findIndex(u => u.id === userId);
        if (index !== -1) {
            users[index] = { ...users[index], ...updates };
            this.usersSubject.next([...users]);
        }
    }

    deleteUser(userId: string) {
        const users = this.usersSubject.value.filter(u => u.id !== userId);
        this.usersSubject.next(users);
    }
}
