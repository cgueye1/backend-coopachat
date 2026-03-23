import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/portail',
    pathMatch: 'full'
  },

  //path for login module
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(c => c.LoginComponent)
  },

  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(c => c.RegisterComponent)
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

  //path for logistique dashboard module
  {
    path: 'log/dashboardlog',
    loadComponent: () => import('./features/logistique/dashboard/dashboardlog.component').then(c => c.DashboardLogComponent)
  },
  {
    path: 'log/fournisseurs',
    loadComponent: () => import('./features/logistique/fournisseur/fournisseur.component').then(c => c.FournisseurComponent)
  },
  {
    path: 'log/stocks',
    loadComponent: () => import('./features/logistique/gestion-stock/gestion-stock.component').then(c => c.GestionStockComponent)
  },
  {
    path: 'log/livraisons',
    loadComponent: () => import('./features/logistique/livraisons/livraisons.component').then(c => c.LivraisonsComponent)
  },
  {
    path: 'log/retours',
    loadComponent: () => import('./features/logistique/gestion-retours/gestion-retours.component').then(c => c.GestionRetoursComponent)
  },
  {
    path: 'log/commandes',
    loadComponent: () => import('./features/logistique/gestion-commandes/gestion-commandes.component').then(c => c.GestionCommandesComponent)
  },
  {
    path: 'admin/dashboardadmin',
    loadComponent: () => import('./features/administrateur/dashboard/dashboard.component').then(c => c.AdminPageComponent)
  },
  {
    path: 'admin/users',
    loadComponent: () => import('./features/administrateur/users/users.component').then(c => c.UsersComponent)
  },
  {
    path: 'admin/users/add',
    loadComponent: () => import('./features/administrateur/add-user/add-user.component').then(c => c.AddUserComponent)
  },
  {
    path: 'admin/users/edit/:id',
    loadComponent: () => import('./features/administrateur/add-user/add-user.component').then(c => c.AddUserComponent)
  },
  {
    path: 'admin/catalogue',
    loadComponent: () => import('./features/administrateur/catalogue/catalogue.component').then(c => c.CatalogueComponent)
  },
  {
    path: 'admin/categories',
    loadComponent: () => import('./features/administrateur/categories/categories.component').then(c => c.CategoriesComponent)
  },
  {
    path: 'admin/add-produit',
    loadComponent: () => import('./features/administrateur/add-produit/add-produit.component').then(c => c.AddProduitComponent)
  },
  //path for commercial dashboard module
  {
    path: 'com/dashboard',
    loadComponent: () => import('./features/commercial/dashboard/dashboard.component').then(c => c.DashboardComponent)
  },
  {
    path: 'com/prospections',
    loadComponent: () => import('./features/commercial/propection/propection.component').then(c => c.ProspectionComponent),
    data: { mode: 'prospects' }
  },
  {
    path: 'com/entreprises',
    loadComponent: () => import('./features/commercial/propection/propection.component').then(c => c.ProspectionComponent),
    data: { mode: 'partenaires' }
  },
  {
    path: 'com/salaries',
    loadComponent: () => import('./features/commercial/salaries/salaries.component').then(c => c.EmployeeManagementComponent)
  },
  {
    path: 'com/statistiques',
    loadComponent: () => import('./features/commercial/statistiques/statistiques.component').then(c => c.SalesStatisticsComponent)
  },
  {
    path: 'com/promotions',
    loadComponent: () => import('./features/commercial/promotions/promotions.component').then(c => c.PromotionsManagementComponent)
  },
  {
    path: 'com/promotions-produits',
    loadComponent: () => import('./features/commercial/promotions-list/promotions-list.component').then(c => c.PromotionsListComponent)
  },
  {
    path: 'com/promotions-produits/create',
    loadComponent: () => import('./features/commercial/promotions-produits/promotions-produits.component').then(c => c.PromotionsProduitsComponent)
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



