package com.redressalbot.grievanceredressalbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.List; 
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final GrievanceRepository grievanceRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Transactional
    public ChatResponse processChat(ChatRequest request, User user) {
        try {
            log.info("Processing chat for user: {}, sessionId: {}", user.getUsername(), request.getSessionId());
            
            // Get or create chat session
            ChatSession session = getOrCreateSession(request.getSessionId(), user);
            log.info("Using session: {}", session.getId());
            
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
            
            // Categorize message
            String category = categorizeMessage(request.getMessage());
            response.setCategory(category);
            
            // Check for form submission in AI response
            processFormSubmissionData(aiResponse, response);
            
            // ONLY create grievance when form is completely submitted
            if (response.isSubmitForm() && response.isMandatoryInfoQueried() && response.getFormData() != null) {
                try {
                    ComplaintFormData formData = objectMapper.readValue(response.getFormData(), ComplaintFormData.class);
                    String conversationContext = getAllChatMessages(session);
                    Grievance grievance = createGrievanceFromForm(user, session, conversationContext, aiResponse, category, formData);
                    response.setGrievanceId(grievance.getId());
                    response.setGrievanceNumber(grievance.getGrievanceNumber());
                    log.info("Created grievance: {}", grievance.getGrievanceNumber());
                } catch (Exception e) {
                    log.error("Error creating grievance from form data", e);
                }
            }
            
            // Update session
            session.setUpdatedAt(LocalDateTime.now());
            if (session.getSessionTitle() == null) {
                session.setSessionTitle(generateSessionTitle(request.getMessage()));
            }
            chatSessionRepository.save(session);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error processing chat", e);
            throw new RuntimeException("Error processing chat: " + e.getMessage(), e);
        }
    }
    
    // Helper method to get all chat messages for context
    private String getAllChatMessages(ChatSession session) {
        List<ChatMessage> messages = chatMessageRepository.findByChatSessionOrderByMessageOrderAsc(session);
        StringBuilder conversationContext = new StringBuilder();
        
        for (ChatMessage message : messages) {
            conversationContext.append(message.getMessageType().name()).append(": ")
                              .append(message.getContent()).append("\n");
        }
        
        return conversationContext.toString();
    }
    
    private void processFormSubmissionData(String aiResponse, ChatResponse response) {
        try {
            // Look for JSON structure in the AI response
            Pattern jsonPattern = Pattern.compile("\\{[^{}]*\"submitForm\"[^{}]*\\}", Pattern.DOTALL);
            Matcher matcher = jsonPattern.matcher(aiResponse);
            
            if (matcher.find()) {
                String jsonStr = matcher.group();
                Map<String, Object> jsonData = objectMapper.readValue(jsonStr, Map.class);
                
                response.setSubmitForm(Boolean.TRUE.equals(jsonData.get("submitForm")));
                response.setMandatoryInfoQueried(Boolean.TRUE.equals(jsonData.get("mandatoryInfoQueried")));
                
                if (response.isSubmitForm() && response.isMandatoryInfoQueried()) {
                    response.setFormData(jsonStr);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing form submission data", e);
        }
    }
    
    private Grievance createGrievanceFromForm(User user, ChatSession session, String conversationContext, 
                                             String aiResponse, String category, ComplaintFormData formData) {
        Grievance grievance = new Grievance();
        grievance.setUser(user);
        grievance.setChatSession(session);
        grievance.setUserMessage(conversationContext);
        grievance.setAiResponse(aiResponse);
        grievance.setCategory(category);
        grievance.setGrievanceNumber("GRV-" + System.currentTimeMillis());
        grievance.setPriority(formData.isEmergency() ? "HIGH" : determinePriority(formData));
        grievance.setFormSubmitted(true);
        grievance.setEmergency(formData.isEmergency());
        
        try {
            grievance.setFormDataJson(objectMapper.writeValueAsString(formData));
            
            // Extract key information
            if (formData.getIncidentInformation() != null) {
                grievance.setComplaintType(formData.getIncidentInformation().getIncidentType());
                grievance.setIncidentDate(formData.getIncidentInformation().getIncidentDate());
                if (formData.getIncidentInformation().getIncidentLocation() != null) {
                    grievance.setIncidentLocation(formData.getIncidentInformation().getIncidentLocation().getPlaceName());
                }
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
            
            grievance.setIncidentSummary(formData.getSummary());
            
        } catch (Exception e) {
            log.error("Error processing form data", e);
        }
        
        return grievanceRepository.save(grievance);
    }
    
    private String determinePriority(ComplaintFormData formData) {
        if (formData.getIncidentInformation() == null || formData.getIncidentInformation().getIncidentType() == null) {
            return "MEDIUM";
        }
        
        String incidentType = formData.getIncidentInformation().getIncidentType().toUpperCase();
        
        if (incidentType.contains("FRAUD") || incidentType.contains("THEFT") || 
            incidentType.contains("ASSAULT") || incidentType.contains("ATTACK")) {
            return "HIGH";
        } else if (incidentType.contains("HARASSMENT") || incidentType.contains("STALKING")) {
            return "HIGH";
        } else {
            return "MEDIUM";
        }
    }
    
    // Fix the getOrCreateSession method to handle UUID properly
    private ChatSession getOrCreateSession(UUID sessionId, User user) {
        if (sessionId != null) {
            return chatSessionRepository.findById(sessionId)
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
        List<ChatMessage> existingMessages = chatMessageRepository.findByChatSessionOrderByMessageOrderAsc(session);
        int messageOrder = existingMessages.size() + 1;
        
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
            conversationHistory.append(msg.getMessageType().name())
                              .append(": ")
                              .append(msg.getContent())
                              .append("\n");
        }
        
        String prompt = """
            You are a cyber crime complaint registration assistant. Help users file cyber crime complaints by collecting all necessary information in a structured format.
            
            When ALL required information is collected, respond with a JSON object in this exact format:
            {
              "personal_information": {
                "full_name": {
                  "first_name": "string",
                  "middle_name": "string",
                  "last_name": "string"
                },
                "date_of_birth": "YYYY-MM-DD",
                "gender": "string",
                "adhaar_number": "string",
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
            
            IMPORTANT: Set submitForm and mandatoryInfoQueried to true ONLY when ALL required information (personal, contact, incident) is collected and the user confirms submission. Do not set them prematurely.
            
            Conversation history:
            %s
            
            Current user message: %s
            """.formatted(conversationHistory.toString(), userMessage);
    
        return geminiService.generateResponse(prompt);
    }
    
    private String categorizeMessage(String message) {
        String prompt = """
            Categorize this cyber crime complaint into one of these categories:
            - FINANCIAL_FRAUD
            - IDENTITY_THEFT
            - ONLINE_HARASSMENT
            - PHISHING
            - MALWARE
            - DATA_BREACH
            - SOCIAL_MEDIA_ABUSE
            - OTHER
            
            Message: %s
            
            Return only the category name, nothing else.
            """.formatted(message);
        
        String response = geminiService.generateResponse(prompt);
        return response.trim().toUpperCase();
    }
    
    private boolean isGrievance(String message, String category) {
        return !category.equals("OTHER") && message.toLowerCase().contains("complaint");
    }
    
    private Grievance createGrievance(User user, ChatSession session, String userMessage, String aiResponse, String category) {
        Grievance grievance = new Grievance();
        grievance.setUser(user);
        grievance.setChatSession(session);
        grievance.setUserMessage(userMessage);
        grievance.setAiResponse(aiResponse);
        grievance.setCategory(category);
        grievance.setGrievanceNumber("GRV-" + System.currentTimeMillis());
        grievance.setPriority("MEDIUM");
        
        return grievanceRepository.save(grievance);
    }
    
    private String generateSessionTitle(String firstMessage) {
        return firstMessage.length() > 50 ? 
               firstMessage.substring(0, 47) + "..." : 
               firstMessage;
    }
    
    // Add new method to handle emergency submissions
    public Grievance submitEmergencyGrievance(User user, String emergencyDetails) {
        try {
            log.info("Creating emergency grievance for user: {}", user.getUsername());
            
            Grievance grievance = new Grievance();
            grievance.setUser(user);
            grievance.setUserMessage("EMERGENCY: " + emergencyDetails);
            grievance.setAiResponse("Emergency grievance automatically created and escalated to authorities.");
            grievance.setCategory("EMERGENCY");
            grievance.setGrievanceNumber("EMG-" + System.currentTimeMillis());
            grievance.setPriority("CRITICAL");
            grievance.setEmergency(true);
            grievance.setStatus(GrievanceStatus.IN_PROGRESS); // Immediately escalate
            
            return grievanceRepository.save(grievance);
            
        } catch (Exception e) {
            log.error("Error creating emergency grievance", e);
            throw new RuntimeException("Failed to submit emergency: " + e.getMessage());
        }
    }
    
    private void appendToChatHistory(ChatSession session, String content, MessageType type) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> chatHistory;
            if (session.getChatHistoryJson() != null) {
                chatHistory = mapper.readValue(session.getChatHistoryJson(), new TypeReference<>() {});
            } else {
                chatHistory = new ArrayList<>();
            }
            Map<String, Object> message = new HashMap<>();
            message.put("type", type.name());
            message.put("content", content);
            message.put("timestamp", LocalDateTime.now().toString());
            chatHistory.add(message);
            session.setChatHistoryJson(mapper.writeValueAsString(chatHistory));
            chatSessionRepository.save(session);
        } catch (Exception e) {
            log.error("Failed to update chat history JSON", e);
        }
    }
}