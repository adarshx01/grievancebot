package com.redressalbot.grievanceredressalbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "grievances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Grievance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // Use GenerationType.UUID for PostgreSQL
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id")
    private ChatSession chatSession;
    
    @Column(nullable = false)
    private String userMessage;
    
    @Column(columnDefinition = "TEXT")
    private String aiResponse;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Enumerated(EnumType.STRING)
    private GrievanceStatus status = GrievanceStatus.OPEN;
    
    private String category;
    private String priority;
    
    @Column(name = "grievance_number", unique = true)
    private String grievanceNumber;
    
    // Add these fields for structured form data
    @Column(columnDefinition = "TEXT")
    private String formDataJson;
    
    private boolean formSubmitted = false;
    
    private boolean emergency = false; // Add emergency flag
    
    private String complaintType;
    private String victimName;
    private String victimContact;
    private String incidentDate;
    private String incidentLocation;
    
    @Column(columnDefinition = "TEXT")
    private String incidentSummary;
}