package com.redressalbot.grievanceredressalbot.service;

import com.redressalbot.grievanceredressalbot.entity.*;
import com.redressalbot.grievanceredressalbot.repository.*;
import com.redressalbot.grievanceredressalbot.dto.ChatRequest;
import com.redressalbot.grievanceredressalbot.dto.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final GrievanceRepository grievanceRepository;
    private final GeminiService geminiService;
    
    @Transactional
    public ChatResponse processChat(ChatRequest request, User user) {
        // Get or create chat session
        ChatSession session = getOrCreateSession(request.getSessionId(), user);
        
        // Save user message
        ChatMessage userMessage = saveMessage(session, request.getMessage(), MessageType.USER);
        
        // Generate AI response
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
        
        if (isGrievance(request.getMessage(), category)) {
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
            You are a professional customer service assistant. You help with general inquiries and grievances.
            
            Conversation history:
            %s
            
            Current user message: %s
            
            Please provide a helpful, professional response. If this appears to be a complaint or grievance, 
            acknowledge it appropriately and explain that it will be logged for resolution.
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