package com.redressalbot.grievanceredressalbot.service;

import com.redressalbot.grievanceredressalbot.dto.ComplaintFormData;  // Add this import
import com.redressalbot.grievanceredressalbot.entity.Grievance;
import com.redressalbot.grievanceredressalbot.entity.User;
import com.redressalbot.grievanceredressalbot.repository.GrievanceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;  // Add this import
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;  // Add this import

import java.util.List;
import java.util.Optional;
import java.util.UUID;  // Add this import

@Service
@RequiredArgsConstructor
@Slf4j  // Add this annotation
public class GrievanceService {
    
    private final GrievanceRepository grievanceRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();  // Add this field
    
    public List<Grievance> getUserGrievances(User user) {
        return grievanceRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    public Optional<Grievance> getGrievanceById(UUID id, User user) {  // Change from Long to UUID
        return grievanceRepository.findById(id)
                .filter(grievance -> grievance.getUser().getId().equals(user.getId()));
    }
    
    public List<Grievance> getAllGrievances() {
        return grievanceRepository.findAll();
    }
    
    // Add the missing register method
    public Grievance register(ComplaintFormData formData, User user) {
        try {
            Grievance grievance = new Grievance();
            grievance.setUser(user);
            grievance.setGrievanceNumber("GRV-" + System.currentTimeMillis());
            grievance.setPriority(formData.isEmergency() ? "HIGH" : "MEDIUM");
            grievance.setFormSubmitted(true);
            grievance.setEmergency(formData.isEmergency());
            
            // Store form data as JSON
            grievance.setFormDataJson(objectMapper.writeValueAsString(formData));
            
            // Extract key information
            if (formData.getIncidentInformation() != null) {
                grievance.setComplaintType(formData.getIncidentInformation().getIncidentType());
                grievance.setIncidentDate(formData.getIncidentInformation().getIncidentDate());
                grievance.setIncidentSummary(formData.getSummary());
            }
            
            if (formData.getPersonalInformation() != null && formData.getPersonalInformation().getFullName() != null) {
                var fullName = formData.getPersonalInformation().getFullName();
                String name = String.format("%s %s %s", 
                    fullName.getFirstName() != null ? fullName.getFirstName() : "",
                    fullName.getMiddleName() != null ? fullName.getMiddleName() : "",
                    fullName.getLastName() != null ? fullName.getLastName() : ""
                ).trim();
                grievance.setVictimName(name);
            }
            
            if (formData.getContactDetails() != null && formData.getContactDetails().getPhones() != null 
                    && !formData.getContactDetails().getPhones().isEmpty()) {
                grievance.setVictimContact(formData.getContactDetails().getPhones().get(0).getNumber());
            }
            
            return grievanceRepository.save(grievance);
            
        } catch (Exception e) {
            log.error("Error registering grievance", e);
            throw new RuntimeException("Failed to register grievance: " + e.getMessage());
        }
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