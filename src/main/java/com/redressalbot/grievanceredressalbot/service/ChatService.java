package com.redressalbot.grievanceredressalbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redressalbot.grievanceredressalbot.dto.ChatRequest;
import com.redressalbot.grievanceredressalbot.dto.ChatResponse;
import com.redressalbot.grievanceredressalbot.dto.ComplaintFormData;
import com.redressalbot.grievanceredressalbot.entity.*;
import com.redressalbot.grievanceredressalbot.repository.ChatMessageRepository;
import com.redressalbot.grievanceredressalbot.repository.ChatSessionRepository;
import com.redressalbot.grievanceredressalbot.repository.GrievanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final GrievanceRepository grievanceRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public ChatResponse processChat(ChatRequest request, User user) {
        // Get or create chat session
        ChatSession session = getOrCreateSession(request.getSessionId(), user);
        
        // Save user message
        ChatMessage userMessage = saveMessage(session, request.getMessage(), MessageType.USER);
        
        // Generate AI response with form gathering capabilities
        String aiResponse = generateAIResponse(request.getMessage(), session);
        
        // Save AI response
        ChatMessage assistantMessage = saveMessage(session, aiResponse, MessageType.ASSISTANT);
        
        // Check if it's a grievance and handle accordingly
        ChatResponse response = new ChatResponse();
        response.setSessionId(session.getId());
        response.setUserMessage(request.getMessage());
        response.setAiResponse(aiResponse);
        
        // Categorize and check if it's a grievance
        String category = categorizeMessage(request.getMessage());
        response.setCategory(category);
        
        // Check for form submission in AI response
        processFormSubmissionData(aiResponse, response);
        
        // If form submission is ready, create or update a grievance
        if (response.isSubmitForm() && response.isMandatoryInfoQueried() && response.getFormData() != null) {
            try {
                // Parse the form data
                ComplaintFormData formData = objectMapper.readValue(response.getFormData(), ComplaintFormData.class);
                
                // Create or update grievance with form data
                Grievance grievance = createGrievanceFromForm(user, session, request.getMessage(), aiResponse, category, formData);
                
                response.setGrievanceId(grievance.getId());
                response.setGrievanceNumber(grievance.getGrievanceNumber());
            } catch (Exception e) {
                log.error("Error processing form submission", e);
            }
        } else if (isGrievance(request.getMessage(), category)) {
            // Handle as regular grievance if no form data
            Grievance grievance = createGrievance(user, session, request.getMessage(), aiResponse, category);
            response.setGrievanceId(grievance.getId());
            response.setGrievanceNumber(grievance.getGrievanceNumber());
        }
        
        // Update session
        session.setUpdatedAt(LocalDateTime.now());
        if (session.getSessionTitle() == null) {
            session.setSessionTitle(generateSessionTitle(request.getMessage()));
        }
        chatSessionRepository.save(session);
        
        return response;
    }
    
    private void processFormSubmissionData(String aiResponse, ChatResponse response) {
        // Check if the response contains form data in JSON format
        try {
            // Look for JSON structure in the response
            Pattern pattern = Pattern.compile("\\{[\\s\\S]*?\"submitForm\"\\s*:\\s*(true|false)[\\s\\S]*?\"mandatoryInfoQueried\"\\s*:\\s*(true|false)[\\s\\S]*?\\}");
            Matcher matcher = pattern.matcher(aiResponse);
            
            if (matcher.find()) {
                String jsonStr = matcher.group(0);
                log.info("Found form data JSON: {}", jsonStr);
                
                // Parse the JSON to extract flags
                ComplaintFormData formData = objectMapper.readValue(jsonStr, ComplaintFormData.class);
                
                // Set form submission flags
                response.setSubmitForm(formData.isSubmitForm());
                response.setMandatoryInfoQueried(formData.isMandatoryInfoQueried());
                response.setFormData(jsonStr);
                
                // Clean the AI response to remove the JSON if needed
                String cleanedResponse = aiResponse.replace(jsonStr, "");
                response.setAiResponse(cleanedResponse);
            }
        } catch (Exception e) {
            log.error("Error parsing form data from AI response", e);
        }
    }
    
    private Grievance createGrievanceFromForm(User user, ChatSession session, String userMessage, 
                                             String aiResponse, String category, ComplaintFormData formData) {
        Grievance grievance = new Grievance();
        grievance.setUser(user);
        grievance.setChatSession(session);
        grievance.setUserMessage(userMessage);
        grievance.setAiResponse(aiResponse);
        grievance.setCategory(category);
        grievance.setGrievanceNumber("GRV-" + System.currentTimeMillis());
        grievance.setPriority(determinePriority(formData));
        grievance.setFormSubmitted(true);
        
        try {
            // Store the complete form data as JSON
            grievance.setFormDataJson(objectMapper.writeValueAsString(formData));
            
            // Extract key information for quick access
            if (formData.getIncidentInformation() != null) {
                grievance.setComplaintType(formData.getIncidentInformation().getIncidentType());
                grievance.setIncidentDate(formData.getIncidentInformation().getIncidentDate());
                grievance.setIncidentSummary(formData.getSummary());
                
                if (formData.getIncidentInformation().getIncidentLocation() != null) {
                    grievance.setIncidentLocation(formData.getIncidentInformation().getIncidentLocation().getAddress());
                }
            }
            
            if (formData.getPersonalInformation() != null && formData.getPersonalInformation().getFullName() != null) {
                ComplaintFormData.PersonalInformation.FullName fullName = formData.getPersonalInformation().getFullName();
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
            
        } catch (Exception e) {
            log.error("Error storing form data", e);
        }
        
        return grievanceRepository.save(grievance);
    }
    
    private String determinePriority(ComplaintFormData formData) {
        // Implement logic to determine priority based on incident type
        if (formData.getIncidentInformation() == null || formData.getIncidentInformation().getIncidentType() == null) {
            return "MEDIUM";
        }
        
        String incidentType = formData.getIncidentInformation().getIncidentType().toUpperCase();
        
        if (incidentType.contains("FRAUD") || incidentType.contains("THEFT") || 
            incidentType.contains("ASSAULT") || incidentType.contains("ATTACK")) {
            return "HIGH";
        } else if (incidentType.contains("HARASSMENT") || incidentType.contains("THREAT")) {
            return "HIGH";
        } else if (incidentType.contains("LOST") || incidentType.contains("MISSING")) {
            return "MEDIUM";
        } else {
            return "MEDIUM";
        }
    }
    
    // Keep your existing methods
    private ChatSession getOrCreateSession(Long sessionId, User user) {
        if (sessionId != null) {
            return chatSessionRepository.findById(sessionId)
                    .filter(session -> session.getUser().getId().equals(user.getId()))
                    .orElse(createNewSession(user));
        }
        return createNewSession(user);
    }
    
    private ChatSession createNewSession(User user) {
        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setStatus(SessionStatus.ACTIVE);
        return chatSessionRepository.save(session);
    }
    
    private ChatMessage saveMessage(ChatSession session, String content, MessageType type) {
        int messageOrder = chatMessageRepository.findByChatSessionOrderByMessageOrderAsc(session).size() + 1;
        
        ChatMessage message = new ChatMessage();
        message.setChatSession(session);
        message.setContent(content);
        message.setMessageType(type);
        message.setMessageOrder(messageOrder);
        
        return chatMessageRepository.save(message);
    }
    
    private String generateAIResponse(String userMessage, ChatSession session) {
        // Get conversation history for context
        var messages = chatMessageRepository.findByChatSessionOrderByMessageOrderAsc(session);
        
        StringBuilder conversationHistory = new StringBuilder();
        for (ChatMessage msg : messages) {
            conversationHistory.append(msg.getMessageType().name()).append(": ").append(msg.getContent()).append("\n");
        }
        
        String prompt = """
            [STRICTLY DO NOT ANSWER IRRELEVANT TOPICS/Questions OTHER THAN COMPLAINTS, PROBLEMS ,etc, and YOU WILL BE ASKING MAXIMUM OF 2-3 QUESTION AT A TIME TILL THE END OF THE CONVERSATION]
            
            Role: You are an immediate, quick Cyber crime complaint registerer asking very specific questions (not all questions at once) deeply related to the topic/problem. Ask short and relevant questions, with step-by-step follow-up questions from the person.
            
            Example: What has happened to them? What troubles are they facing?
            
            Based on the situation, act as an Instant police complaint registerer. Ask specific and concise questions to understand the full issue and problem. Then at the end, ask the user whether to create a Case complaint. If yes, collect their name, address, phone number. [STRICTLY ask questions until you have all information]
            
            If the user agrees to submit a complaint, create a full summary of the report with all details the victim/user has provided, structured in this exact JSON format:
            
            {
              "personal_information": {
                "full_name": {
                  "first_name": "string",
                  "middle_name": "string",
                  "last_name": "string"
                },
                "date_of_birth": "YYYY-MM-DD",
                "gender": "string",
                "adhaar number": "string",
                "marital_status": "string"
              },
              "contact_details": {
                "phones": [
                  {
                    "type": "mobile",
                    "number": "string"
                  }
                ],
                "emails": [
                  "string"
                ],
                "address": {
                  "street": "string",
                  "landmark": "string",
                  "city": "string",
                  "district": "string",
                  "state": "string",
                  "postal_code": "string",
                  "country": "string"
                }
              },
              "incident_information": {
                "incident_date": "YYYY-MM-DD",
                "incident_time": "HH:MM",
                "incident_location": {
                  "place_name": "string",
                  "address": "string"
                },
                "incident_type": "string",
                "detailed_description": "string"
              },
              "summary": "string",
              "submitForm": true,
              "mandatoryInfoQueried": true
            }
            
            Set submitForm and mandatoryInfoQueried to true ONLY when you have collected ALL necessary information.
            
            Conversation history:
            %s
            
            Current user message: %s
            """.formatted(conversationHistory.toString(), userMessage);
            
        return geminiService.generateResponse(prompt);
    }
    
    private String categorizeMessage(String message) {
        String prompt = """
            Categorize this message into one of these categories:
            - GENERAL_INQUIRY
            - BILLING_ISSUE
            - SERVICE_COMPLAINT
            - TECHNICAL_SUPPORT
            - PRODUCT_COMPLAINT
            - STAFF_COMPLAINT
            - OTHER
            
            Message: %s
            
            Return only the category name.
            """.formatted(message);
            
        return geminiService.generateResponse(prompt).trim().toUpperCase();
    }
    
    private boolean isGrievance(String message, String category) {
        // Consider it a grievance if it's a complaint or contains complaint indicators
        return category.contains("COMPLAINT") || category.contains("ISSUE") ||
               message.toLowerCase().contains("complaint") ||
               message.toLowerCase().contains("problem") ||
               message.toLowerCase().contains("issue") ||
               message.toLowerCase().contains("wrong") ||
               message.toLowerCase().contains("error");
    }
    
    private Grievance createGrievance(User user, ChatSession session, String userMessage, String aiResponse, String category) {
        Grievance grievance = new Grievance();
        grievance.setUser(user);
        grievance.setChatSession(session);
        grievance.setUserMessage(userMessage);
        grievance.setAiResponse(aiResponse);
        grievance.setCategory(category);
        grievance.setGrievanceNumber("GRV-" + System.currentTimeMillis());
        grievance.setPriority("MEDIUM"); // Default priority
        
        return grievanceRepository.save(grievance);
    }
    
    private String generateSessionTitle(String firstMessage) {
        if (firstMessage.length() > 50) {
            return firstMessage.substring(0, 47) + "...";
        }
        return firstMessage;
    }
}