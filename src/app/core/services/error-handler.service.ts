import { Injectable } from '@angular/core';
import Swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class ErrorHandlerService {

  constructor() { }

  /**
   * Extrait un message d'erreur lisible pour l'utilisateur final
   * @param err L'objet erreur reçu du backend
   * @returns Un message string clair
   */
  getErrorMessage(err: any): string {
    const DEFAULT_MSG = 'Une erreur inattendue est survenue. Veuillez vérifier votre saisie.';
    
    if (!err) return DEFAULT_MSG;

    // Fonction utilitaire pour vérifier si un texte est technique
    const isTechnical = (text: string): boolean => {
      if (!text) return false;
      const lower = text.toLowerCase();
      return lower.includes('http failure') || 
             lower.includes('localhost') || 
             lower.includes('status code') ||
             lower.includes('bad request') ||
             lower.includes('internal server error');
    };

    // 1. Extraction du corps de l'erreur (Message spécifique du Backend)
    if (err.error) {
      let body = err.error;

      // Si c'est du JSON (même en string)
      if (typeof body === 'string') {
        try {
          const parsed = JSON.parse(body);
          if (parsed.message && !isTechnical(parsed.message)) return parsed.message;
          if (parsed.errors && Array.isArray(parsed.errors)) return parsed.errors.join('\n');
        } catch (e) {
          // Si c'est du texte brut propre (pas technique)
          if (body.length < 300 && !isTechnical(body) && !body.includes('<!DOCTYPE')) {
            return body;
          }
        }
      } 
      // Si c'est un objet JSON déjà parsé
      else {
        if (body.message && !isTechnical(body.message)) return body.message;
        if (body.errors && Array.isArray(body.errors)) return body.errors.join('\n');
      }
    }

    // 2. Traduction des codes HTTP (Fallback si aucun message backend n'est trouvé)
    if (err.status === 400) return 'Les données envoyées sont incorrectes. Vérifiez les champs obligatoires et le format du téléphone.';
    if (err.status === 401) return 'Session expirée. Veuillez vous reconnecter.';
    if (err.status === 403) return 'Accès refusé. Vous n\'avez pas les permissions nécessaires.';
    if (err.status === 404) return 'La ressource est introuvable.';
    if (err.status === 409) return 'Un conflit est survenu (ex: cette donnée existe déjà).';
    if (err.status === 0) return 'Le serveur ne répond pas. Vérifiez votre connexion internet.';
    if (err.status >= 500) return 'Erreur serveur. Nous travaillons à sa résolution.';

    // 3. Fallback final sur err.message (uniquement si ce n'est pas technique)
    if (err.message && typeof err.message === 'string' && !isTechnical(err.message)) {
      return err.message;
    }

    return DEFAULT_MSG;
  }

  /**
   * Affiche une modal d'erreur SweetAlert2 avec le message extrait
   */
  showError(err: any, title: string = 'Erreur'): void {
    const message = this.getErrorMessage(err);
    
    Swal.fire({
      title: title,
      text: message,
      icon: 'error',
      confirmButtonColor: '#2B3674',
      confirmButtonText: 'OK'
    });
  }
}
