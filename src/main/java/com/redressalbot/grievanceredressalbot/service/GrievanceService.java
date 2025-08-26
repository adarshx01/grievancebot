package com.redressalbot.grievanceredressalbot.service;

import com.redressalbot.grievanceredressalbot.entity.Grievance;
import com.redressalbot.grievanceredressalbot.entity.User;
import com.redressalbot.grievanceredressalbot.repository.GrievanceRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GrievanceService {
    
    private final GrievanceRepository grievanceRepository;
    private final GeminiService geminiService;
    
    public List<Grievance> getUserGrievances(User user) {
        return grievanceRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public Optional<Grievance> getGrievanceById(Long id, User user) {
        return grievanceRepository.findById(id)
                .filter(grievance -> grievance.getUser().getId().equals(user.getId()));
    }
    
    public List<Grievance> getAllGrievances() {
        return grievanceRepository.findAll();
    }
    
    public Grievance processGrievance(String userMessage) {
        // Create and save grievance
        Grievance grievance = new Grievance();
        grievance.setUserMessage(userMessage);
        
        // Get AI response
        String aiResponse = getAIResponse(userMessage);
        grievance.setAiResponse(aiResponse);
        
        // Categorize grievance
        String category = categorizeGrievance(userMessage);
        grievance.setCategory(category);
        
        return grievanceRepository.save(grievance);
    }
    
    private String getAIResponse(String message) {
        String prompt = """
            You are a professional grievance redressal assistant for a company. 
            Provide helpful, empathetic responses to user complaints.
            Keep responses concise but comprehensive.
            Be professional and offer actionable solutions when possible.
            
            User grievance: %s
            
            Please provide a professional response:
            """.formatted(message);
            
        return geminiService.generateResponse(prompt);
    }
    
    private String categorizeGrievance(String message) {
        String prompt = """
            Categorize this grievance into one of these categories:
            - BILLING
            - SERVICE_QUALITY
            - TECHNICAL_ISSUE
            - STAFF_BEHAVIOR
            - PRODUCT_DEFECT
            - OTHER
            
            Grievance: %s
            
            Return only the category name, nothing else.
            """.formatted(message);
            
        String response = geminiService.generateResponse(prompt);
        return response.trim().toUpperCase();
    }
}