package com.example.coopachat.repositories;

import com.example.coopachat.entities.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverRepository extends JpaRepository <Driver , Long> {
}
