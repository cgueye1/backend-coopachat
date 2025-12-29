// portail.component.ts
import { Component, OnInit, OnDestroy, PLATFORM_ID, inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-portail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './portail.component.html',
  styleUrls: ['./portail.component.css']
})
export class PortailComponent implements OnInit, OnDestroy {
  private platformId = inject(PLATFORM_ID);
  private router = inject(Router);
  private autoScrollInterval: any;

  isMobileMenuOpen = false;

  logo = '/images/logo.png';
  heroImage = '/images/image-portail.png';

  features = [
    { label: 'Livraison 24-72h' },
    { label: 'Paiement sécurisé' },
    { label: 'Support WhatsApp' }
  ];

  navigationItems = [
    { label: 'Accueil', sectionId: 'accueil' },
    { label: 'Fonctionnalités', sectionId: 'fonctionnalites' },
    { label: 'Profils', sectionId: 'profils' },
    { label: 'Tarifs', sectionId: 'tarifs' },
    { label: 'Téléchargement', sectionId: 'telechargement' },
  ];

  partnerLogos = [
    { src: '/icones/connect.svg', alt: 'Connect' },
    { src: '/icones/modern.svg', alt: 'Modern' },
    { src: '/icones/medical.svg', alt: 'Medical' },
    { src: '/icones/business.svg', alt: 'Business' },
    { src: '/icones/cityrise.svg', alt: 'Cityrise' },
    { src: '/icones/laptop.svg', alt: 'Laptop' },
    { src: '/icones/computer.svg', alt: 'Computer' },
    { src: '/icones/brand.svg', alt: 'Brand' },
    { src: '/icones/fast-market.svg', alt: 'Fast Market' }
  ];

  featureCards = [
    {
      badge: 'Client',
      title: 'Catalogue de produits',
      description: 'Prix avantageux pour salariés, filtres, comparateur, fiches détaillées.',
      image: '/images/phone-catalogue.png'
    },
    {
      badge: 'Client - Livreur',
      title: 'Livraison optimisée',
      description: 'De la commande jusqu\'à la livraion avec des notifications temps réel, preuve de livraison...',
      image: '/images/phone-livraison.png'
    },
    {
      badge: 'Client - Livreur',
      title: 'Paiement sécurisé',
      description: 'Carte bancaire, mobile money, facture téléchargeable.',
      image: '/images/phone-paiement.png'
    }
  ];

  logisticsCards = [
    {
      badge: 'Logistique',
      title: 'Tableau de bord',
      description: 'Comparaison des commandes vs livraisons, alerts des seuil...',
      image: '/images/dashboard-tableau.png',
      type: 'desktop'
    },
    {
      badge: 'Logistique',
      title: 'Gestion des stocks',
      description: 'Suivi des stocks, achats fournisseurs, gestion des alertes et réapprovisionnement...',
      image: '/images/dashboard-stocks.png',
      type: 'desktop'
    }
  ];

  profiles = [
    {
      id: 1,
      title: 'Client',
      description: 'Découvrir les catalogues de produits, commander, suivi de livraison',
      image: '/images/profile-client.png'
    },
    {
      id: 2,
      title: 'Livreur',
      description: 'Prise en charge, gerer les commandes, Tableau de bord, Statistiques et reporting...',
      image: '/images/profile-livreur.png'
    },
    {
      id: 3,
      title: 'Commercial',
      description: 'Promotions, Gestion des salariés, Tableau de bord, Statistiques et reporting...',
      image: '/images/profile-commercial.png'
    },
    {
      id: 4,
      title: 'Logistique',
      description: 'Gestion des commandes, stocks, planifications des livreur, Tableau de bord, Statistiques et reporting...',
      image: '/images/profile-logistique.png'
    }
  ];

  howItWorksSteps = [
    {
      number: '1',
      title: 'Découvrez',
      description: 'Parcourez les catégories, comparez et ajoutez au panier.'
    },
    {
      number: '2',
      title: 'Commandez',
      description: 'Validez et payez en toute sécurité (carte, mobile money).'
    },
    {
      number: '3',
      title: 'Livraison',
      description: 'Le livreur prend en charge, vous suivez l\'arrivée en temps réel.'
    }
  ];

  howItWorksImage = '/images/comment-ca-marche.png';

  pricingPlans = [
    {
      name: 'Basic',
      subtitle: 'Tous utilisateurs',
      price: '0',
      currency: 'FCFA',
      period: '',
      popular: false,
      features: [
        'Commandes',
        'Suivi en temps réel',
        'Historique des commandes'
      ],
      buttonText: 'Essayer gratuitement',
      buttonStyle: 'outline'
    },
    {
      name: 'Livreur',
      subtitle: 'Livreurs',
      price: '9 900',
      currency: 'FCFA',
      period: 'mois',
      popular: true,
      features: [
        'Gestion des commandes',
        'Gérer ses disponibilités',
        'Tableau de bord, Statistiques et Reporting'
      ],
      buttonText: 'Souscrire',
      buttonStyle: 'solid'
    },
    {
      name: 'Entreprise',
      subtitle: 'Responsable logistique - Commercial',
      price: '4 900',
      currency: 'FCFA',
      period: 'mois',
      popular: false,
      features: [
        'Gestion des salariés',
        'Gestion des promotions',
        'Gestion des stocks',
        'Tableau de bord, Statistiques et Reporting'
      ],
      buttonText: 'Souscrire',
      buttonStyle: 'outline'
    }
  ];

  appStoreLink = 'https://apps.apple.com/your-app';
  playStoreLink = 'https://play.google.com/store/apps/your-app';

  appStoreBadge = '/icones/app-store-badge.svg';
  playStoreBadge = '/icones/play-store-badge.svg';
  mobileAppPreview = '/images/mobile-app.png';

  stats = [
    { value: '+500', label: 'Clients' },
    { value: '+240', label: 'Livreurs' },
    { value: '+150', label: 'Entreprises' }
  ];

  // Testimonials data
  testimonials = [
    {
      quote: 'Process simple, promos vraiment intéressantes...',
      company: 'Entreprise BTP',
      role: 'RH',
      avatar: '/images/avatar-rh.png',
      rating: 5
    },
    {
      quote: 'Livraison rapide, suivi clair, facture immédiate.',
      company: 'Comptabilité',
      role: 'PME Tech',
      avatar: '/images/avatar-comptable.png',
      rating: 5
    },
    {
      quote: 'Gain de temps énorme pour nos achats groupés.',
      company: 'Agence Marketing',
      role: 'Manager',
      avatar: '/images/avatar-manager.png',
      rating: 5
    }
  ];

  // FAQ data
  faqItems = [
    {
      question: 'Comment créer un compte salarié ?',
      answer: 'Cliquez sur « Créer mon compte » puis suivez les étapes. Un email de validation vous sera envoyé.',
      isOpen: true
    },
    {
      question: 'Comment suivre ma livraison ?',
      answer: 'Connectez-vous à votre espace client et accédez à la section "Mes commandes" pour suivre votre livraison en temps réel.',
      isOpen: false
    },
    {
      question: 'Quels sont les moyens de paiement ?',
      answer: 'Nous acceptons les cartes bancaires, le mobile money (Orange Money, Wave, etc.) et le paiement à la livraison.',
      isOpen: false
    }
  ];

  toggleFaq(index: number): void {
    this.faqItems[index].isOpen = !this.faqItems[index].isOpen;
  }

  currentTestimonialIndex = 0;

  ngOnInit(): void {
    if (isPlatformBrowser(this.platformId)) {
      this.startTestimonialAutoScroll();
    }
  }

  ngOnDestroy(): void {
    if (this.autoScrollInterval) {
      clearInterval(this.autoScrollInterval);
    }
  }

  startTestimonialAutoScroll(): void {
    this.autoScrollInterval = setInterval(() => {
      this.currentTestimonialIndex =
        (this.currentTestimonialIndex + 1) % this.testimonials.length;
    }, 5000);
  }

  goToTestimonial(index: number): void {
    this.currentTestimonialIndex = index;
  }

  prevTestimonial(): void {
    this.currentTestimonialIndex =
      this.currentTestimonialIndex === 0
        ? this.testimonials.length - 1
        : this.currentTestimonialIndex - 1;
  }

  nextTestimonial(): void {
    this.currentTestimonialIndex =
      (this.currentTestimonialIndex + 1) % this.testimonials.length;
  }

  // Footer data
  footerLogo = '/images/logo.png';

  socialLinks = [
    { name: 'Facebook', icon: 'facebook', url: 'https://facebook.com' },
    { name: 'Instagram', icon: 'instagram', url: 'https://instagram.com' },
    { name: 'Twitter', icon: 'twitter', url: 'https://twitter.com' },
    { name: 'LinkedIn', icon: 'linkedin', url: 'https://linkedin.com' }
  ];

  footerProductLinks = [
    { label: 'Fonctionnalités', sectionId: 'fonctionnalites' },
    { label: 'Tarifs', sectionId: 'tarifs' },
    { label: 'Témoignages', sectionId: 'temoignages' },
    { label: 'FAQ', sectionId: 'faq' }
  ];

  footerLegalLinks = [
    { label: 'Mentions légales', route: '#mentions' },
    { label: 'Politique de confidentialité', route: '#confidentialite' },
    { label: 'CGU', route: '#cgu' },
    { label: 'Cookies', route: '#cookies' }
  ];

  scrollToSection(sectionId: string): void {
    if (isPlatformBrowser(this.platformId)) {
      const element = document.getElementById(sectionId);
      if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    }
    this.closeMobileMenu();
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
    this.closeMobileMenu();
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }
}