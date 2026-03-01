package com.example.coopachat.enums;

/**
 * Statuts possibles d'une commande dans le système de livraison.
 *
 * Cycle de vie normal :
 * EN_ATTENTE → VALIDEE → EN_PREPARATION → EN_COURS → ARRIVE → LIVREE
 *
 * Cas exceptionnels :
 * - Annulation : EN_ATTENTE → ANNULEE (par le salarié)
 * - Tournée annulée : VALIDEE → EN_ATTENTE (par le RL)
 * - Échec : EN_COURS/ARRIVE → ECHEC_LIVRAISON (par le livreur)
 */
public enum OrderStatus {

    /**
     * EN_ATTENTE - Commande créée par le salarié, en attente d'être planifiée dans une tournée.
     *
     * Quand ? Immédiatement après validation du panier par le salarié
     * Qui peut agir ?
     *   - Salarié : peut annuler sa commande
     *   - RL : peut l'ajouter à une tournée
     * Affichage salarié : "En attente de validation"
     * Tournée : null (pas encore assignée)
     * Paiement : UNPAID
     */
    EN_ATTENTE("En attente "),

    /**
     * VALIDEE - Commande ajoutée à une tournée par le RL, en attente de récupération par le livreur.
     *
     * Quand ? Après que le RL crée une tournée et y ajoute cette commande
     * Qui peut agir ?
     *   - RL : peut annuler la tournée (commande retourne EN_ATTENTE)
     *   - Livreur : doit confirmer la récupération des colis
     * Affichage salarié : "Validée - En préparation"
     * Tournée : ASSIGNEE
     * Paiement : UNPAID (ou PAID si paiement en ligne effectué)
     */
    VALIDEE("Validée"),

    /**
     * EN_PREPARATION - Livreur a récupéré les colis, la tournée est en cours.
     *
     * Quand ? Après que le livreur confirme la récupération (swipe "Confirmer récupération")
     * Qui peut agir ?
     *   - RL : peut encore annuler la tournée (si aucune livraison démarrée)
     *   - Livreur : doit démarrer chaque livraison individuellement
     * Affichage salarié : "En préparation"
     * Tournée : EN_COURS
     * Paiement : UNPAID (ou PAID si paiement en ligne)
     */
    EN_PREPARATION("À livrer"),

    /**
     * EN_COURS - Livreur a démarré la livraison de cette commande spécifique.
     *
     * Quand ? Après que le livreur démarre la livraison (swipe "Démarrer livraison")
     * Qui peut agir ?
     *   - Livreur : peut confirmer son arrivée, ou signaler un problème
     * Affichage salarié : "En cours de livraison"
     * Tournée : EN_COURS
     * Paiement : UNPAID (ou PAID si paiement en ligne)
     * Note : À partir d'ici, la tournée ne peut plus être annulée
     */
    EN_COURS("En cours"),

    /**
     * ARRIVE - Livreur est arrivé chez le client.
     *
     * Quand ? Après que le livreur confirme son arrivée (swipe "Confirmer arrivée")
     * Qui peut agir ?
     *   - Livreur : peut signaler un problème, ou finaliser la livraison
     *   - Client : doit être présent pour réceptionner
     * Affichage salarié : "Livreur arrivé"
     * Tournée : EN_COURS
     * Paiement : Doit être PAID avant finalisation (paiement à la livraison si non payé)
     */
    ARRIVE("Livreur arrivé"),

    /**
     * LIVREE - Commande livrée avec succès au client.
     *
     * Quand ? Après que le livreur finalise (swipe "Finaliser livraison")
     * Qui peut agir ?
     *   - Salarié : peut créer une réclamation si problème avec produits
     *   - Système : passe la tournée en TERMINEE si toutes les commandes sont livrées
     * Affichage salarié : "Livrée"
     * Tournée : EN_COURS (devient TERMINEE quand toutes sont livrées)
     * Paiement : PAID (obligatoire)
     * Note : Statut final positif
     */
    LIVREE("Livrée"),

    /**
     * ECHEC_LIVRAISON - Livraison impossible (client absent, adresse introuvable, etc.).
     *
     * Quand ? Après que le livreur signale un problème (bouton "Signaler un problème")
     * Qui peut agir ?
     *   - RL : doit replanifier cette commande dans une nouvelle tournée
     *   - Système : notifie le salarié et le RL
     * Affichage salarié : "Échec de livraison - Problème signalé"
     * Tournée : EN_COURS (autres livraisons continuent)
     * Paiement : UNPAID
     * Raisons possibles : Client absent, Adresse introuvable, Accès impossible, Client refuse, Colis endommagé
     * Note : Nécessite une action du RL pour replanifier
     */
    ECHEC_LIVRAISON("Échec de livraison"),

    /**
     * ANNULEE - Commande annulée par le salarié avant d'être planifiée.
     *
     * Quand ? Quand le salarié annule sa commande (uniquement si statut EN_ATTENTE)
     * Qui peut agir ?
     *   - Personne (statut final)
     * Affichage salarié : "Annulée"
     * Tournée : null
     * Paiement : UNPAID (ou REFUNDED si paiement avait été effectué)
     * Note : Statut final négatif - la commande ne sera jamais livrée
     */
    ANNULEE("Annulée");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }




   
}