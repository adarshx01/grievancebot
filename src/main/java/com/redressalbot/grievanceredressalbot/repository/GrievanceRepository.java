package com.redressalbot.grievanceredressalbot.repository;

import com.redressalbot.grievanceredressalbot.entity.Grievance;
import com.redressalbot.grievanceredressalbot.entity.User;
import com.redressalbot.grievanceredressalbot.entity.GrievanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GrievanceRepository extends JpaRepository<Grievance, Long> {
    List<Grievance> findByStatusOrderByCreatedAtDesc(GrievanceStatus status);
    List<Grievance> findByUserOrderByCreatedAtDesc(User user);
    List<Grievance> findByUserAndStatusOrderByCreatedAtDesc(User user, GrievanceStatus status);
}