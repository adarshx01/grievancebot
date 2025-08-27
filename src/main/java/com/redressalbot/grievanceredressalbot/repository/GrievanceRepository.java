package com.redressalbot.grievanceredressalbot.repository;

import com.redressalbot.grievanceredressalbot.entity.Grievance;
import com.redressalbot.grievanceredressalbot.entity.User;
import com.redressalbot.grievanceredressalbot.entity.GrievanceStatus;
import com.redressalbot.grievanceredressalbot.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GrievanceRepository extends JpaRepository<Grievance, UUID> {
    List<Grievance> findByStatusOrderByCreatedAtDesc(GrievanceStatus status);
    List<Grievance> findByUserOrderByCreatedAtDesc(User user);
    List<Grievance> findByUserAndStatusOrderByCreatedAtDesc(User user, GrievanceStatus status);
    
    // Add this method to check if grievance already exists for a session
    boolean existsByChatSessionAndFormSubmitted(ChatSession chatSession, boolean formSubmitted);
    
    // Add method to find grievance by chat session
    Optional<Grievance> findByChatSessionAndFormSubmitted(ChatSession chatSession, boolean formSubmitted);
}