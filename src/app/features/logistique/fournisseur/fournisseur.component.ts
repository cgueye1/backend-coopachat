import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';

interface Commande {
  reference: string;
  fournisseur: string;
  date: string;
  produits: string;
  statut: 'En cours' | 'Livrée' | 'En attente' | 'Annulée';
}

@Component({
  selector: 'app-fournisseur',
  standalone: true,
  imports: [MainLayoutComponent, CommonModule, FormsModule],
  styles: [`
    .statut-en-cours {
      background-color: #FEF3C7;
      color: #D97706;
    }
    
    .statut-livree {
      background-color: #D1FAE5;
      color: #059669;
    }
    
    .statut-en-attente {
      background-color: #E5E7EB;
      color: #6B7280;
    }
    
    .statut-annulee {
      background-color: #FEE2E2;
      color: #DC2626;
    }
  `],
  template: `
    <app-main-layout>
  <div class="p-6 bg-gray-100 min-h-screen">
    <!-- Header -->
    <div class="mb-6">
      <div class="flex items-center gap-2 text-gray-500 text-sm mb-2">
        <span>Pages</span>
        <span>/</span>
        <span>Commandes Fournisseurs</span>
      </div>
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <h1 class="text-2xl sm:text-3xl font-bold text-[#2d3561]">Commandes Fournisseurs</h1>
        <button
          (click)="nouvelleCommande()"
          class="flex items-center gap-2 bg-[#2E2E5D] hover:bg-[#25254B] text-white font-medium px-5 py-2 rounded-full shadow-sm transition-colors"
        >
          <svg class="w-5 h-5" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M10 18.75C5.175 18.75 1.25 14.825 1.25 10C1.25 5.175 5.175 1.25 10 1.25C14.825 1.25 18.75 5.175 18.75 10C18.75 14.825 14.825 18.75 10 18.75ZM10 2.5C5.8625 2.5 2.5 5.8625 2.5 10C2.5 14.1375 5.8625 17.5 10 17.5C14.1375 17.5 17.5 14.1375 17.5 10C17.5 5.8625 14.1375 2.5 10 2.5Z" fill="white"/>
            <path d="M10 14.375C9.65 14.375 9.375 14.1 9.375 13.75V6.25C9.375 5.9 9.65 5.625 10 5.625C10.35 5.625 10.625 5.9 10.625 6.25V13.75C10.625 14.1 10.35 14.375 10 14.375Z" fill="white"/>
            <path d="M13.75 10.625H6.25C5.9 10.625 5.625 10.35 5.625 10C5.625 9.65 5.9 9.375 6.25 9.375H13.75C14.1 9.375 14.375 9.65 14.375 10C14.375 10.35 14.1 10.625 13.75 10.625Z" fill="white"/>
          </svg>
          Nouvelle commande
        </button>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="grid gap-5 mb-6 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
      <div class="bg-white rounded-xl p-5 shadow flex justify-between items-center">
        <div>
          <p class="text-gray-500 text-sm mb-2">Total commandes</p>
          <h2 class="text-2xl font-bold text-[#2d3561]">{{ totalCommandes }}</h2>
        </div>
        <div>
          <svg width="33" height="32" viewBox="0 0 33 32" fill="none" xmlns="http://www.w3.org/2000/svg">
<g clip-path="url(#clip0_1180_8177)">
<path d="M14.5 28C14.5 28 12.5 28 12.5 26C12.5 24 14.5 18 22.5 18C30.5 18 32.5 24 32.5 26C32.5 28 30.5 28 30.5 28H14.5ZM22.5 16C24.0913 16 25.6174 15.3679 26.7426 14.2426C27.8679 13.1174 28.5 11.5913 28.5 10C28.5 8.4087 27.8679 6.88258 26.7426 5.75736C25.6174 4.63214 24.0913 4 22.5 4C20.9087 4 19.3826 4.63214 18.2574 5.75736C17.1321 6.88258 16.5 8.4087 16.5 10C16.5 11.5913 17.1321 13.1174 18.2574 14.2426C19.3826 15.3679 20.9087 16 22.5 16ZM10.932 28C10.6356 27.3756 10.4878 26.6911 10.5 26C10.5 23.29 11.86 20.5 14.372 18.56C13.1184 18.1729 11.812 17.9839 10.5 18C2.5 18 0.5 24 0.5 26C0.5 28 2.5 28 2.5 28H10.932ZM9.5 16C10.8261 16 12.0979 15.4732 13.0355 14.5355C13.9732 13.5979 14.5 12.3261 14.5 11C14.5 9.67392 13.9732 8.40215 13.0355 7.46447C12.0979 6.52678 10.8261 6 9.5 6C8.17392 6 6.90215 6.52678 5.96447 7.46447C5.02678 8.40215 4.5 9.67392 4.5 11C4.5 12.3261 5.02678 13.5979 5.96447 14.5355C6.90215 15.4732 8.17392 16 9.5 16Z" fill="#2C2D5B"/>
</g>
<defs>
<clipPath id="clip0_1180_8177">
<rect width="32" height="32" fill="white" transform="translate(0.5)"/>
</clipPath>
</defs>
</svg>
        </div>
      </div>

      <div class="bg-white rounded-xl p-5 shadow flex justify-between items-center">
        <div>
          <p class="text-gray-500 text-sm mb-2">En attente</p>
          <h2 class="text-2xl font-bold text-[#2d3561]">{{ enAttente < 10 ? '0' + enAttente : enAttente }}</h2>
        </div>
        <div>
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none" xmlns="http://www.w3.org/2000/svg">
<path d="M21 28C22.8565 28 24.637 27.2625 25.9497 25.9497C27.2625 24.637 28 22.8565 28 21C28 19.1435 27.2625 17.363 25.9497 16.0503C24.637 14.7375 22.8565 14 21 14C19.1435 14 17.363 14.7375 16.0503 16.0503C14.7375 17.363 14 19.1435 14 21C14 22.8565 14.7375 24.637 16.0503 25.9497C17.363 27.2625 19.1435 28 21 28ZM24.358 19.014L21.688 23.466C21.5707 23.6616 21.4103 23.8279 21.219 23.9521C21.0278 24.0764 20.8107 24.1554 20.5843 24.1832C20.3579 24.211 20.1282 24.1868 19.9126 24.1124C19.6969 24.038 19.5011 23.9154 19.34 23.754L17.792 22.208C17.6042 22.0202 17.4987 21.7656 17.4987 21.5C17.4987 21.2344 17.6042 20.9798 17.792 20.792C17.9798 20.6042 18.2344 20.4987 18.5 20.4987C18.7656 20.4987 19.0202 20.6042 19.208 20.792L20.302 21.888L22.642 17.986C22.7095 17.8733 22.7985 17.775 22.904 17.6968C23.0095 17.6185 23.1294 17.5618 23.2568 17.5299C23.3842 17.4979 23.5166 17.4914 23.6465 17.5106C23.7765 17.5299 23.9013 17.5745 24.014 17.642C24.1267 17.7095 24.225 17.7985 24.3032 17.904C24.3815 18.0095 24.4382 18.1294 24.4701 18.2568C24.5021 18.3842 24.5086 18.5166 24.4894 18.6465C24.4701 18.7765 24.4255 18.9013 24.358 19.014ZM18 6C18 7.5913 17.3679 9.11742 16.2426 10.2426C15.1174 11.3679 13.5913 12 12 12C10.4087 12 8.88258 11.3679 7.75736 10.2426C6.63214 9.11742 6 7.5913 6 6C6 4.4087 6.63214 2.88258 7.75736 1.75736C8.88258 0.632141 10.4087 0 12 0C13.5913 0 15.1174 0.632141 16.2426 1.75736C17.3679 2.88258 18 4.4087 18 6Z" fill="#318F3F"/>
<path d="M0 22C0 24 2 24 2 24H12.512C12.1723 23.0363 11.9991 22.0218 12 21C12 19.7139 12.2756 18.4428 12.8083 17.2722C13.3409 16.1016 14.1183 15.0588 15.088 14.214C14.1547 14.074 13.1253 14.0027 12 14C2 14 0 20 0 22Z" fill="#318F3F"/>
</svg>
        </div>
      </div>

      <div class="bg-white rounded-xl p-5 shadow flex justify-between items-center">
        <div>
          <p class="text-gray-500 text-sm mb-2">Livrées</p>
          <h2 class="text-2xl font-bold text-[#2d3561]">{{ livrees < 10 ? '0' + livrees : livrees }}</h2>
        </div>
        <div>
          <svg width="29" height="28" viewBox="0 0 29 28" fill="none" xmlns="http://www.w3.org/2000/svg">
<path d="M21.5 28C23.3565 28 25.137 27.2625 26.4497 25.9497C27.7625 24.637 28.5 22.8565 28.5 21C28.5 19.1435 27.7625 17.363 26.4497 16.0503C25.137 14.7375 23.3565 14 21.5 14C19.6435 14 17.863 14.7375 16.5503 16.0503C15.2375 17.363 14.5 19.1435 14.5 21C14.5 22.8565 15.2375 24.637 16.5503 25.9497C17.863 27.2625 19.6435 28 21.5 28ZM24.858 19.014L22.188 23.466C22.0707 23.6616 21.9103 23.8279 21.719 23.9521C21.5278 24.0764 21.3107 24.1554 21.0843 24.1832C20.8579 24.211 20.6282 24.1868 20.4126 24.1124C20.1969 24.038 20.0011 23.9154 19.84 23.754L18.292 22.208C18.1042 22.0202 17.9987 21.7656 17.9987 21.5C17.9987 21.2344 18.1042 20.9798 18.292 20.792C18.4798 20.6042 18.7344 20.4987 19 20.4987C19.2656 20.4987 19.5202 20.6042 19.708 20.792L20.802 21.888L23.142 17.986C23.2095 17.8733 23.2985 17.775 23.404 17.6968C23.5095 17.6185 23.6294 17.5618 23.7568 17.5299C23.8842 17.4979 24.0166 17.4914 24.1465 17.5106C24.2765 17.5299 24.4013 17.5745 24.514 17.642C24.6267 17.7095 24.725 17.7985 24.8032 17.904C24.8815 18.0095 24.9382 18.1294 24.9701 18.2568C25.0021 18.3842 25.0086 18.5166 24.9894 18.6465C24.9701 18.7765 24.9255 18.9013 24.858 19.014ZM18.5 6C18.5 7.5913 17.8679 9.11742 16.7426 10.2426C15.6174 11.3679 14.0913 12 12.5 12C10.9087 12 9.38258 11.3679 8.25736 10.2426C7.13214 9.11742 6.5 7.5913 6.5 6C6.5 4.4087 7.13214 2.88258 8.25736 1.75736C9.38258 0.632141 10.9087 0 12.5 0C14.0913 0 15.6174 0.632141 16.7426 1.75736C17.8679 2.88258 18.5 4.4087 18.5 6Z" fill="#318F3F"/>
<path d="M0.5 22C0.5 24 2.5 24 2.5 24H13.012C12.6723 23.0363 12.4991 22.0218 12.5 21C12.5 19.7139 12.7756 18.4428 13.3083 17.2722C13.8409 16.1016 14.6183 15.0588 15.588 14.214C14.6547 14.074 13.6253 14.0027 12.5 14C2.5 14 0.5 20 0.5 22Z" fill="#318F3F"/>
</svg>

        </div>
      </div>

      <div class="bg-white rounded-xl p-5 shadow flex justify-between items-center">
        <div>
          <p class="text-gray-500 text-sm mb-2">Annulées</p>
          <h2 class="text-2xl font-bold text-[#2d3561]">{{ annulees < 10 ? '0' + annulees : annulees }}</h2>
        </div>
        <div>
          <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
<g clip-path="url(#clip0_1180_8199)">
<path d="M27.758 20.828C26.7962 20.1933 25.6448 19.9101 24.4983 20.0263C23.3518 20.1425 22.2807 20.651 21.4658 21.4658C20.651 22.2807 20.1425 23.3518 20.0263 24.4983C19.9101 25.6448 20.1933 26.7962 20.828 27.758L27.758 20.828ZM29.172 22.242L22.242 29.172C23.2038 29.8067 24.3552 30.0899 25.5017 29.9737C26.6482 29.8575 27.7193 29.349 28.5342 28.5342C29.349 27.7193 29.8575 26.6482 29.9737 25.5017C30.0899 24.3552 29.8067 23.2038 29.172 22.242ZM20.052 20.05C20.6997 19.3894 21.4719 18.8637 22.3239 18.5034C23.176 18.143 24.0911 17.9551 25.0162 17.9506C25.9413 17.9461 26.8582 18.125 27.7138 18.4769C28.5693 18.8289 29.3466 19.347 30.0007 20.0013C30.6549 20.6555 31.1728 21.4329 31.5246 22.2886C31.8764 23.1442 32.0551 24.0611 32.0504 24.9862C32.0457 25.9113 31.8576 26.8264 31.4971 27.6784C31.1366 28.5304 30.6108 29.3025 29.95 29.95C28.6372 31.2628 26.8566 32.0004 25 32.0004C23.1434 32.0004 21.3628 31.2628 20.05 29.95C18.7372 28.6372 17.9996 26.8566 17.9996 25C17.9996 23.1434 18.7372 21.3628 20.05 20.05H20.052ZM22 10C22 11.5913 21.3679 13.1174 20.2426 14.2426C19.1174 15.3679 17.5913 16 16 16C14.4087 16 12.8826 15.3679 11.7574 14.2426C10.6321 13.1174 10 11.5913 10 10C10 8.4087 10.6321 6.88258 11.7574 5.75736C12.8826 4.63214 14.4087 4 16 4C17.5913 4 19.1174 4.63214 20.2426 5.75736C21.3679 6.88258 22 8.4087 22 10ZM4 26C4 28 6 28 6 28H16.512C16.1723 27.0363 15.9991 26.0218 16 25C16 23.7139 16.2756 22.4428 16.8083 21.2722C17.3409 20.1016 18.1183 19.0588 19.088 18.214C18.1547 18.074 17.1253 18.0027 16 18C6 18 4 24 4 26Z" fill="url(#paint0_linear_1180_8199)"/>
</g>
<defs>
<linearGradient id="paint0_linear_1180_8199" x1="4" y1="18.0002" x2="32.0505" y2="18.0002" gradientUnits="userSpaceOnUse">
<stop stop-color="#FF6B00"/>
<stop offset="1" stop-color="#FF914D"/>
</linearGradient>
<clipPath id="clip0_1180_8199">
<rect width="32" height="32" fill="white"/>
</clipPath>
</defs>
</svg>

        </div>
      </div>
    </div>

    <!-- Filters & Actions -->
    <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6 flex-wrap">
      <div class="relative flex-1 min-w-[250px]">
        <svg class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-500 w-5 h-5" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
          <circle cx="11" cy="11" r="8"/>
          <path d="m21 21-4.35-4.35"/>
        </svg>
        <input type="text" placeholder="Rechercher par référence ou produit..." [(ngModel)]="searchText"
          class="w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:border-[#2d3561] text-sm"
        >
      </div>
      <div class="flex gap-3 flex-wrap">
        <button class="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg text-gray-700 hover:border-[#2d3561] hover:text-[#2d3561] text-sm">
          <svg width="20" height="21" viewBox="0 0 20 21" fill="none" xmlns="http://www.w3.org/2000/svg">
<path d="M16.6673 5.16671C16.6673 4.70004 16.6673 4.46671 16.5757 4.28837C16.496 4.13171 16.3688 4.00424 16.2123 3.92421C16.034 3.83337 15.8007 3.83337 15.334 3.83337H4.66732C4.20065 3.83337 3.96732 3.83337 3.78898 3.92421C3.63219 4.0041 3.50471 4.13158 3.42482 4.28837C3.33398 4.46671 3.33398 4.70004 3.33398 5.16671V5.78087C3.33398 5.98504 3.33398 6.08671 3.35732 6.18254C3.37772 6.26786 3.41147 6.34942 3.45732 6.42421C3.50815 6.50754 3.58065 6.58004 3.72398 6.72421L7.94315 10.9425C8.08732 11.0867 8.15982 11.1592 8.21065 11.2425C8.25676 11.3181 8.2901 11.3987 8.31065 11.4842C8.33398 11.5792 8.33398 11.68 8.33398 11.8792V15.8425C8.33398 16.5567 8.33398 16.9142 8.48398 17.1292C8.54906 17.2222 8.63245 17.3009 8.72903 17.3605C8.82562 17.4201 8.93336 17.4594 9.04565 17.4759C9.30482 17.5142 9.62482 17.355 10.2632 17.035L10.9298 16.7017C11.1982 16.5684 11.3315 16.5017 11.429 16.4017C11.5155 16.3134 11.5812 16.2069 11.6215 16.09C11.6673 15.9584 11.6673 15.8084 11.6673 15.5092V11.8859C11.6673 11.6817 11.6673 11.58 11.6907 11.4842C11.7111 11.3989 11.7448 11.3173 11.7907 11.2425C11.8407 11.1592 11.9132 11.0875 12.0548 10.9459L12.0582 10.9425L16.2773 6.72421C16.4207 6.58004 16.4923 6.50754 16.544 6.42421C16.5901 6.34865 16.6234 6.2681 16.644 6.18254C16.6673 6.08837 16.6673 5.98671 16.6673 5.78754V5.16671Z" stroke="#A3AED0" stroke-width="1.66667" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
        Tous les fournisseurs
        </button>
        <button class="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg text-gray-700 hover:border-[#2d3561] hover:text-[#2d3561] text-sm">
          <svg width="20" height="21" viewBox="0 0 20 21" fill="none" xmlns="http://www.w3.org/2000/svg">
<path d="M16.6673 5.16671C16.6673 4.70004 16.6673 4.46671 16.5757 4.28837C16.496 4.13171 16.3688 4.00424 16.2123 3.92421C16.034 3.83337 15.8007 3.83337 15.334 3.83337H4.66732C4.20065 3.83337 3.96732 3.83337 3.78898 3.92421C3.63219 4.0041 3.50471 4.13158 3.42482 4.28837C3.33398 4.46671 3.33398 4.70004 3.33398 5.16671V5.78087C3.33398 5.98504 3.33398 6.08671 3.35732 6.18254C3.37772 6.26786 3.41147 6.34942 3.45732 6.42421C3.50815 6.50754 3.58065 6.58004 3.72398 6.72421L7.94315 10.9425C8.08732 11.0867 8.15982 11.1592 8.21065 11.2425C8.25676 11.3181 8.2901 11.3987 8.31065 11.4842C8.33398 11.5792 8.33398 11.68 8.33398 11.8792V15.8425C8.33398 16.5567 8.33398 16.9142 8.48398 17.1292C8.54906 17.2222 8.63245 17.3009 8.72903 17.3605C8.82562 17.4201 8.93336 17.4594 9.04565 17.4759C9.30482 17.5142 9.62482 17.355 10.2632 17.035L10.9298 16.7017C11.1982 16.5684 11.3315 16.5017 11.429 16.4017C11.5155 16.3134 11.5812 16.2069 11.6215 16.09C11.6673 15.9584 11.6673 15.8084 11.6673 15.5092V11.8859C11.6673 11.6817 11.6673 11.58 11.6907 11.4842C11.7111 11.3989 11.7448 11.3173 11.7907 11.2425C11.8407 11.1592 11.9132 11.0875 12.0548 10.9459L12.0582 10.9425L16.2773 6.72421C16.4207 6.58004 16.4923 6.50754 16.544 6.42421C16.5901 6.34865 16.6234 6.2681 16.644 6.18254C16.6673 6.08837 16.6673 5.98671 16.6673 5.78754V5.16671Z" stroke="#A3AED0" stroke-width="1.66667" stroke-linecap="round" stroke-linejoin="round"/>
</svg>
        Tous les statuts
        </button>
        <button class="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 rounded-lg text-gray-700 hover:border-[#2d3561] hover:text-[#2d3561] text-sm">
  <svg width="20" height="21" viewBox="0 0 20 21" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path fill-rule="evenodd" clip-rule="evenodd" d="M3.75 2.375C3.75 2.20924 3.81585 2.05027 3.93306 1.93306C4.05027 1.81585 4.20924 1.75 4.375 1.75H11.25C11.3321 1.74985 11.4135 1.7659 11.4894 1.79721C11.5653 1.82853 11.6344 1.8745 11.6925 1.9325L16.0675 6.3075C16.1255 6.36565 16.1715 6.43466 16.2028 6.51059C16.2341 6.58651 16.2501 6.66787 16.25 6.75V9.875H15V7.375H11.25C11.0842 7.375 10.9253 7.30915 10.8081 7.19194C10.6908 7.07473 10.625 6.91576 10.625 6.75V3H5V18H11.875V19.25H4.375C4.20924 19.25 4.05027 19.1842 3.93306 19.0669C3.81585 18.9497 3.75 18.7908 3.75 18.625V2.375ZM11.875 3.88375L14.1163 6.125H11.875V3.88375ZM15.4425 11.9338L17.9425 14.4338C18.0597 14.551 18.1255 14.7099 18.1255 14.8756C18.1255 15.0414 18.0597 15.2003 17.9425 15.3175L15.4425 17.8175L14.5575 16.9325L15.9913 15.5H10V14.25H15.9913L14.5575 12.8175L15.4425 11.9338Z" fill="#A3AED0"/>
  </svg>
  Exporter
</button>

      </div>
    </div>

    <!-- Table -->
    <div class="bg-white rounded-xl shadow overflow-x-auto mb-5">
      <table class="min-w-full border-collapse">
        <thead class="bg-gray-100">
          <tr>
            <th class="text-left text-xs font-semibold text-gray-600 uppercase p-4 border-b border-gray-200">Référence</th>
            <th class="text-left text-xs font-semibold text-gray-600 uppercase p-4 border-b border-gray-200">Fournisseur</th>
            <th class="text-left text-xs font-semibold text-gray-600 uppercase p-4 border-b border-gray-200">Date</th>
            <th class="text-left text-xs font-semibold text-gray-600 uppercase p-4 border-b border-gray-200">Produits</th>
            <th class="text-left text-xs font-semibold text-gray-600 uppercase p-4 border-b border-gray-200">Statut</th>
            <th class="text-left text-xs font-semibold text-gray-600 uppercase p-4 border-b border-gray-200">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let commande of commandes" class="hover:bg-gray-100">
            <td class="font-semibold text-[#2d3561] p-4">{{ commande.reference }}</td>
            <td class="p-4">{{ commande.fournisseur }}</td>
            <td class="p-4">{{ commande.date }}</td>
            <td class="p-4">{{ commande.produits }}</td>
            <td class="p-4">
              <span [class]="'inline-flex items-center gap-2 px-3 py-1 rounded-full text-sm font-medium ' + getStatutClass(commande.statut)">
                <span class="w-2 h-2 rounded-full bg-current"></span>
                {{ commande.statut }}
              </span>
            </td>
            <td class="flex gap-2 p-4">
              <button class="p-2 rounded-md hover:bg-gray-100 text-gray-500" (click)="viewCommande(commande.reference)">
                <svg width="22" height="22" viewBox="0 0 22 22" fill="none" xmlns="http://www.w3.org/2000/svg">
<path d="M11.0007 6.19493C12.6072 6.18959 14.1825 6.63768 15.5456 7.48769C16.9087 8.3377 18.0042 9.55512 18.7063 11C17.2648 13.9442 14.3206 15.8051 11.0007 15.8051C7.68089 15.8051 4.7367 13.9442 3.29518 11C3.99726 9.55512 5.09282 8.3377 6.45593 7.48769C7.81903 6.63768 9.39434 6.18959 11.0007 6.19493ZM11.0007 4.44763C6.63251 4.44763 2.90204 7.16468 1.39062 11C2.90204 14.8353 6.63251 17.5523 11.0007 17.5523C15.369 17.5523 19.0995 14.8353 20.6109 11C19.0995 7.16468 15.369 4.44763 11.0007 4.44763ZM11.0007 8.81587C11.58 8.81587 12.1356 9.04598 12.5452 9.45558C12.9548 9.86519 13.1849 10.4207 13.1849 11C13.1849 11.5793 12.9548 12.1348 12.5452 12.5444C12.1356 12.954 11.58 13.1841 11.0007 13.1841C10.4215 13.1841 9.86595 12.954 9.45634 12.5444C9.04674 12.1348 8.81663 11.5793 8.81663 11C8.81663 10.4207 9.04674 9.86519 9.45634 9.45558C9.86595 9.04598 10.4215 8.81587 11.0007 8.81587ZM11.0007 7.06858C8.8341 7.06858 7.06933 8.83334 7.06933 11C7.06933 13.1666 8.8341 14.9314 11.0007 14.9314C13.1674 14.9314 14.9322 13.1666 14.9322 11C14.9322 8.83334 13.1674 7.06858 11.0007 7.06858Z" fill="#4B4848"/>
</svg>
              </button>
              <button class="p-2 rounded-md hover:bg-gray-100 text-gray-500" (click)="editCommande(commande.reference)">
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
<path d="M4.16667 15.8333H5.35417L13.5 7.6875L12.3125 6.5L4.16667 14.6458V15.8333ZM2.5 17.5V13.9583L13.5 2.97917C13.6667 2.82639 13.8508 2.70833 14.0525 2.625C14.2542 2.54167 14.4658 2.5 14.6875 2.5C14.9092 2.5 15.1244 2.54167 15.3333 2.625C15.5422 2.70833 15.7228 2.83333 15.875 3L17.0208 4.16667C17.1875 4.31944 17.3092 4.5 17.3858 4.70833C17.4625 4.91667 17.5006 5.125 17.5 5.33333C17.5 5.55556 17.4619 5.7675 17.3858 5.96917C17.3097 6.17083 17.1881 6.35472 17.0208 6.52083L6.04167 17.5H2.5ZM12.8958 7.10417L12.3125 6.5L13.5 7.6875L12.8958 7.10417Z" fill="#F68647"/>
</svg>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="bg-white rounded-xl shadow flex flex-col sm:flex-row justify-between items-center p-4 gap-4">
      <p class="text-gray-500 text-sm">Page {{ currentPage }} sur {{ totalPages }}</p>
      <div class="flex gap-2">
        <button class="px-4 py-2 bg-white border border-gray-300 rounded-lg text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed hover:border-[#2d3561] hover:text-[#2d3561]" 
                [disabled]="currentPage === 1" (click)="previousPage()">Précédent</button>
        <button class="px-4 py-2 bg-white border border-gray-300 rounded-lg text-gray-700 disabled:opacity-50 disabled:cursor-not-allowed hover:border-[#2d3561] hover:text-[#2d3561]" 
                [disabled]="currentPage === totalPages" (click)="nextPage()">Suivant</button>
      </div>
    </div>
  </div>
</app-main-layout>
  `,
})
export class FournisseurComponent {
  totalCommandes = 12;
  enAttente = 3;
  livrees = 7;
  annulees = 2;

  searchText = '';
  selectedFournisseur = 'Tous les fournisseurs';
  selectedStatut = 'Tous les statuts';

  commandes: Commande[] = [
    {
      reference: 'CMD-0012',
      fournisseur: 'Fourniture Express',
      date: '03/10/2025',
      produits: 'Riz (100), Huile (50)',
      statut: 'En cours'
    },
    {
      reference: 'CMD-0011',
      fournisseur: 'Stock Pro',
      date: '03/10/2025',
      produits: 'Sucre (80), Sel (40)',
      statut: 'Livrée'
    },
    {
      reference: 'CMD-0010',
      fournisseur: 'Stock Pro',
      date: '03/10/2025',
      produits: 'Sucre (80), Sel (40)',
      statut: 'En attente'
    },
    {
      reference: 'CMD-0009',
      fournisseur: 'Stock Pro',
      date: '03/10/2025',
      produits: 'Sucre (80), Sel (40)',
      statut: 'Annulée'
    }
  ];

  currentPage = 1;
  totalPages = 12;

  getStatutClass(statut: string): string {
    const classes: { [key: string]: string } = {
      'En cours': 'statut-en-cours',
      'Livrée': 'statut-livree',
      'En attente': 'statut-en-attente',
      'Annulée': 'statut-annulee'
    };
    return classes[statut] || '';
  }

  viewCommande(reference: string): void {
    console.log('Voir commande:', reference);
  }

  editCommande(reference: string): void {
    console.log('Modifier commande:', reference);
  }

  nouvelleCommande(): void {
    console.log('Nouvelle commande');
  }

  exportCommandes(): void {
    console.log('Exporter les commandes');
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }
}