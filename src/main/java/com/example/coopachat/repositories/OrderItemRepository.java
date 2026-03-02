package com.example.coopachat.repositories;

import com.example.coopachat.entities.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Top 5 produits les plus commandés (en quantité) depuis une date.
     * Le graphique affiche le nombre d'utilisations en % : voir getTop5ProductUsage() dans AdminServiceImpl.
     *
     * <p>Requête expliquée :
     * <ul>
     *   <li>SELECT oi.product.name : nom du produit (via la relation OrderItem → Product)</li>
     *   <li>SUM(oi.quantity) : somme des quantités commandées pour ce produit sur toutes les lignes de commande</li>
     *   <li>FROM OrderItem oi : on part des lignes de commande (chaque ligne = 1 produit + quantité)</li>
     *   <li>WHERE oi.order.createdAt >= :dateDebut : uniquement les commandes créées depuis cette date</li>
     *   <li>GROUP BY oi.product.name : on regroupe par produit pour sommer les quantités</li>
     *   <li>ORDER BY SUM(oi.quantity) DESC : les plus commandés en premier</li>
     *   <li>Pageable (size=5) : on ne garde que les 5 premiers (équivalent LIMIT 5 en SQL)</li>
     * </ul>
     * Le résultat est une liste de tableaux [nomProduit, sommeQuantité]. Le pourcentage d’utilisation
     * est calculé dans le service : (sommeQuantité du produit / total général) × 100.
     *
     * @param dateDebut date de début de période (ex: il y a 30 jours)
     * @param pageable  PageRequest.of(0, 5) pour limiter à 5 lignes
     * @return liste de Object[] : [0] = String (nom produit), [1] = Long (somme des quantités)
     */
    @Query("""
        SELECT oi.product.name, SUM(oi.quantity)
        FROM OrderItem oi
        WHERE oi.order.createdAt >= :dateDebut
        GROUP BY oi.product.name
        ORDER BY SUM(oi.quantity) DESC
        """)
    List<Object[]> findTop5ProductsByQuantitySince(
            @Param("dateDebut") LocalDateTime dateDebut,
            Pageable pageable);

    /**
     * Somme totale des quantités commandées (tous produits confondus) depuis une date.
     * Utilisé comme dénominateur pour calculer le % d’utilisation de chaque produit du top 5.
     *
     * <p>Requête expliquée :
     * <ul>
     *   <li>SUM(oi.quantity) : total de toutes les quantités sur les lignes de commande</li>
     *   <li>WHERE oi.order.createdAt >= :dateDebut : même période que le top 5</li>
     * </ul>
     *
     * @param dateDebut date de début de période
     * @return somme des quantités (Long), ou 0 si aucune commande
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.order.createdAt >= :dateDebut")
    long sumQuantityByOrderCreatedAtAfter(@Param("dateDebut") LocalDateTime dateDebut);
}
