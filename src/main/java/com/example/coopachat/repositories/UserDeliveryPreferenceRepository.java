package com.example.coopachat.repositories;

import com.example.coopachat.entities.UserDeliveryPreference;
import com.example.coopachat.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserDeliveryPreferenceRepository extends JpaRepository <UserDeliveryPreference, Long> {

    Optional<UserDeliveryPreference> findByUser(Users users);
}
