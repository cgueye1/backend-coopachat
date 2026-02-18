package com.example.coopachat.repositories;

import com.example.coopachat.entities.PaymentTiming;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTimingRepository extends JpaRepository<PaymentTiming, Long> {

    boolean existsByName(String name);
}
