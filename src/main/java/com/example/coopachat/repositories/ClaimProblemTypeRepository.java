package com.example.coopachat.repositories;

import com.example.coopachat.entities.ClaimProblemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClaimProblemTypeRepository extends JpaRepository<ClaimProblemType, Long> {
}
