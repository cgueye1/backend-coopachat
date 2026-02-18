package com.example.coopachat.repositories;

import com.example.coopachat.entities.DriverReview;
import com.example.coopachat.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DriverReviewRepository extends JpaRepository<DriverReview, Long> {

    boolean existsByOrderId(Long orderId);

    Optional<DriverReview> findByOrder(Order order);
}
