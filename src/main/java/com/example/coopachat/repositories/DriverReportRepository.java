package com.example.coopachat.repositories;

import com.example.coopachat.entities.DriverReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverReportRepository extends JpaRepository<DriverReport, Long> {
}
