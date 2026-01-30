package com.example.coopachat.repositories;

import com.example.coopachat.entities.Driver;
import com.example.coopachat.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryDriverRepository extends JpaRepository <Driver, Long> {

    Optional <Driver> findByUser(Users user);
}
