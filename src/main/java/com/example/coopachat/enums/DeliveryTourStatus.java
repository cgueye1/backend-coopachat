package com.example.coopachat.enums;

/**
 * Statuts possibles d'une tournée de livraison.
 *
 * Cycle de vie normal :
 * ASSIGNEE → EN_COURS → TERMINEE
 *
 * Cas exceptionnel :
 * ASSIGNEE → ANNULEE (si annulation avant démarrage)
 * EN_COURS → ANNULEE (si annulation avant première livraison démarrée)
 */
public enum DeliveryTourStatus {

    /**
     * ASSIGNEE - Tournée créée et assignée à un livreur, en attente de démarrage.
     *
     * Quand ? Immédiatement après création de la tournée par le RL
     * Commandes associées : VALIDEE
     * Qui peut agir ?
     *   - RL : peut modifier la tournée ou l'annuler
     *   - Livreur : doit confirmer la récupération des colis pour démarrer
     * Affichage RL : "Assignée"
     * Affichage Livreur : "À confirmer" avec bouton "Confirmer récupération"
     * Note : Le livreur voit la tournée mais n'a pas encore les colis
     */
    ASSIGNEE("Assignée"),

    /**
     * EN_COURS - Livreur a récupéré les colis et effectue les livraisons.
     *
     * Quand ? Après que le livreur confirme la récupération (swipe "Confirmer récupération")
     * Commandes associées : EN_PREPARATION (puis EN_COURS, ARRIVE, LIVREE au fur et à mesure)
     * Qui peut agir ?
     *   - RL : peut encore annuler si aucune livraison n'a été démarrée (toutes EN_PREPARATION)
     *   - Livreur : effectue les livraisons une par une
     * Affichage RL : "En cours - X/Y livrées"
     * Affichage Livreur : "En cours" avec liste des livraisons à effectuer
     * Note : La tournée reste EN_COURS même si certaines livraisons échouent
     * Passage à TERMINEE : Automatique quand toutes les commandes sont LIVREE
     */
    EN_COURS("En cours"),

    /**
     * TERMINEE - Toutes les livraisons de la tournée ont été effectuées avec succès.
     *
     * Quand ? Automatiquement quand la dernière commande passe à LIVREE
     * Commandes associées : Toutes LIVREE
     *
     * Affichage RL : "Terminée - Y/Y livrées"
     * Affichage Livreur : "Terminée" (historique)
     * Note : Statut final - toutes les livraisons ont réussi
     * Paiements : Tous PAID obligatoirement
     */
    TERMINEE("Terminée"),

    /**
     * ANNULEE - Tournée annulée par le RL avant ou pendant les livraisons.
     *
     * Quand ? Après annulation par le RL (bouton "Annuler la tournée")
     * Conditions d'annulation :
     *   - Si ASSIGNEE : Annulation libre
     *   - Si EN_COURS : Seulement si aucune commande n'est EN_COURS, ARRIVE ou LIVREE
     * Commandes associées : Retournent toutes à EN_ATTENTE (sauf celles déjà livrées)
     * Qui peut agir ?
     *   - Système : notifie tous les salariés concernés
     * Affichage RL : "Annulée - Raison : [MOTIF]"
     * Affichage Livreur : "Annulée" (historique)
     * Raisons possibles : Véhicule en panne, Livreur indisponible, Météo, Grève, etc.
     * Note : Les commandes sont remises en file d'attente pour replanification
     */
    ANNULEE("Annulée");

    private final String label;

    DeliveryTourStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }


}