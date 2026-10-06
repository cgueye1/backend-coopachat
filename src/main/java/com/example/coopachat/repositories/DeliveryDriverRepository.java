package com.example.coopachat.repositories;

import com.example.coopachat.entities.Driver;
import com.example.coopachat.entities.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

//Driver
@Repository
public interface DeliveryDriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByUser(Users user);

    /**
     * Recherche paginée des livreurs par nom, prénom ou email.
     */
    @Query("""
            SELECT d FROM Driver d 
            WHERE (:search IS NULL 
               OR LOWER(d.user.firstName) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(d.user.lastName) LIKE LOWER(CONCAT('%', :search, '%')) 
               OR LOWER(d.user.email) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Driver> findWithFilters(@Param("search") String search, Pageable pageable);

}
