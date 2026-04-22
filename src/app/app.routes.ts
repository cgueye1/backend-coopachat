import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { administratorRoleGuard } from './core/guards/administrator-role.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/portail',
    pathMatch: 'full'
  },

  //path for login module
  {
    path: 'profile/edit',
    canActivate: [authGuard],
    loadComponent: () => import('./features/administrateur/add-user/add-user.component').then(c => c.AddUserComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(c => c.LoginComponent)
  },

  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(c => c.RegisterComponent)
  },
  {
    path: 'activate-account',
    redirectTo: 'register',
    pathMatch: 'full'
  },
  {
    path: 'create-password', // ✅ Route corrigée pour CreatePasswordComponent
    loadComponent: () => import('./features/auth/create-password/create-password.component').then(c => c.CreatePasswordComponent)
  },
  {
    path: 'otp-verification',
    loadComponent: () => import('./features/auth/otp-verification/otp-verification.component').then(c => c.OtpVerificationComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./features/auth/reset-password/reset-password.component').then(c => c.ResetPasswordComponent)
  },

  //path for logistique dashboard module (JWT requis)
  {
    path: 'log/dashboardlog',
    canActivate: [authGuard],
    loadComponent: () => import('./features/logistique/dashboard/dashboardlog.component').then(c => c.DashboardLogComponent)
  },
  {
    path: 'log/fournisseurs',
    canActivate: [authGuard],
    loadComponent: () => import('./features/logistique/fournisseur/fournisseur.component').then(c => c.FournisseurComponent)
  },
  {
    path: 'log/stocks',
    canActivate: [authGuard],
    loadComponent: () => import('./features/logistique/gestion-stock/gestion-stock.component').then(c => c.GestionStockComponent)
  },
  {
    path: 'log/livraisons',
    canActivate: [authGuard],
    loadComponent: () => import('./features/logistique/livraisons/livraisons.component').then(c => c.LivraisonsComponent)
  },
  {
    path: 'log/retours',
    canActivate: [authGuard],
    loadComponent: () => import('./features/logistique/gestion-retours/gestion-retours.component').then(c => c.GestionRetoursComponent)
  },
  {
    path: 'log/commandes',
    canActivate: [authGuard],
    loadComponent: () => import('./features/logistique/gestion-commandes/gestion-commandes.component').then(c => c.GestionCommandesComponent)
  },
  {
    path: 'admin/dashboardadmin',
    canActivate: [authGuard, administratorRoleGuard],
    loadComponent: () => import('./features/administrateur/dashboard/dashboard.component').then(c => c.AdminPageComponent)
  },
  {
    path: 'admin/users',
    canActivate: [authGuard, administratorRoleGuard],
    loadComponent: () => import('./features/administrateur/users/users.component').then(c => c.UsersComponent)
  },
  {
    path: 'admin/users/add',
    canActivate: [authGuard, administratorRoleGuard],
    loadComponent: () => import('./features/administrateur/add-user/add-user.component').then(c => c.AddUserComponent)
  },
  {
    path: 'admin/users/edit/:id',
    canActivate: [authGuard, administratorRoleGuard],
    loadComponent: () => import('./features/administrateur/add-user/add-user.component').then(c => c.AddUserComponent)
  },
  {
    path: 'admin/catalogue',
    canActivate: [authGuard, administratorRoleGuard],
    loadComponent: () => import('./features/administrateur/catalogue/catalogue.component').then(c => c.CatalogueComponent)
  },
  {
    path: 'admin/categories',
    canActivate: [authGuard, administratorRoleGuard],
    loadComponent: () => import('./features/administrateur/categories/categories.component').then(c => c.CategoriesComponent)
  },
  {
    path: 'admin/add-produit',
    canActivate: [authGuard, administratorRoleGuard],
    loadComponent: () => import('./features/administrateur/add-produit/add-produit.component').then(c => c.AddProduitComponent)
  },
  // --- Admin Configuration Routes ---
  {
    path: 'admin/config/delivery-options',
    canActivate: [authGuard, administratorRoleGuard],
    loadComponent: () => import('./features/administrateur/config/delivery-option-management/delivery-option-management.component').then(c => c.DeliveryOptionManagementComponent)
  },
  {
    path: 'admin/config/fees',
    canActivate: [authGuard, administratorRoleGuard],
    loadComponent: () => import('./features/administrateur/config/fee-management/fee-management.component').then(c => c.FeeManagementComponent)
  },
  {
    path: 'admin/config/claim-types',
    canActivate: [authGuard, administratorRoleGuard],
    loadComponent: () => import('./features/administrateur/config/reference-management/reference-management.component').then(c => c.ReferenceManagementComponent)
  },
  {
    path: 'admin/config/delivery-reasons',
    canActivate: [authGuard, administratorRoleGuard],
    loadComponent: () => import('./features/administrateur/config/reference-management/reference-management.component').then(c => c.ReferenceManagementComponent)
  },
  {
    path: 'admin/config/employee-reasons',
    canActivate: [authGuard, administratorRoleGuard],
    loadComponent: () => import('./features/administrateur/config/reference-management/reference-management.component').then(c => c.ReferenceManagementComponent)
  },
  {
    path: 'admin/config/activity-sectors',
    canActivate: [authGuard, administratorRoleGuard],
    loadComponent: () => import('./features/administrateur/config/reference-management/reference-management.component').then(c => c.ReferenceManagementComponent)
  },
  //path for commercial dashboard module (JWT requis)
  {
    path: 'com/dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/commercial/dashboard/dashboard.component').then(c => c.DashboardComponent)
  },
  {
    path: 'com/prospections',
    canActivate: [authGuard],
    loadComponent: () => import('./features/commercial/propection/propection.component').then(c => c.ProspectionComponent),
    data: { mode: 'prospects' }
  },
  {
    path: 'com/entreprises/:companyId/salaries',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/commercial/company-employees/company-employees.component').then(c => c.CompanyEmployeesComponent)
  },
  {
    path: 'com/entreprises',
    canActivate: [authGuard],
    loadComponent: () => import('./features/commercial/propection/propection.component').then(c => c.ProspectionComponent),
    data: { mode: 'partenaires' }
  },
  {
    path: 'com/statistiques',
    canActivate: [authGuard],
    loadComponent: () => import('./features/commercial/statistiques/statistiques.component').then(c => c.SalesStatisticsComponent)
  },
  {
    path: 'com/promotions',
    canActivate: [authGuard],
    loadComponent: () => import('./features/commercial/promotions/promotions.component').then(c => c.PromotionsManagementComponent)
  },
  {
    path: 'com/promotions-produits',
    canActivate: [authGuard],
    loadComponent: () => import('./features/commercial/promotions-list/promotions-list.component').then(c => c.PromotionsListComponent)
  },
  {
    path: 'com/promotions-produits/create',
    canActivate: [authGuard],
    loadComponent: () => import('./features/commercial/promotions-produits/promotions-produits.component').then(c => c.PromotionsProduitsComponent)
  },

  // ===== ESPACE ENTREPRISE =====
  {
    path: 'entreprise/dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/entreprise/entreprise-dashboard/entreprise-dashboard.component').then(c => c.EntrepriseDashboardComponent)
  },
  {
    path: 'portail',
    loadComponent: () => import('./features/portail/portail.component').then(c => c.PortailComponent)
  },
  {
    path: '**',
    redirectTo: '/portail'
  }
];



