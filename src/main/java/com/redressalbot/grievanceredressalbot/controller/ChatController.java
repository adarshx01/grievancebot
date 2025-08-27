package com.redressalbot.grievanceredressalbot.controller;

import com.redressalbot.grievanceredressalbot.service.ChatService;
import com.redressalbot.grievanceredressalbot.entity.User;
import com.redressalbot.grievanceredressalbot.entity.Grievance; // Add this import
import com.redressalbot.grievanceredressalbot.dto.ChatRequest;
import com.redressalbot.grievanceredressalbot.dto.ChatResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(
            @RequestBody ChatRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            // Log session info for debugging
            log.info("Session ID: {}", httpRequest.getSession().getId());
            log.info("Authentication: {}", authentication != null ? authentication.getName() : "null");

            if (authentication == null || !authentication.isAuthenticated()) {
                log.warn("Unauthenticated request to chat endpoint");
                return ResponseEntity.status(401).build();
            }

            User user = (User) authentication.getPrincipal();
            log.info("Processing chat message for user: {} (ID: {})", user.getUsername(), user.getId());
            log.info("Message: {}, SessionId: {}", request.getMessage(), request.getSessionId());

            ChatResponse response = chatService.processChat(request, user);
            log.info("Chat response generated successfully. SessionId: {}", response.getSessionId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> test(Authentication authentication, HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        String username = authentication != null ? authentication.getName() : "Anonymous";
        String message = String.format("Chat API is working! Session: %s, User: %s", sessionId, username);
        log.info("Test endpoint called: {}", message);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/sessions")
    public ResponseEntity<String> getUserSessions(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("Not authenticated");
        }
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok("Chat sessions for user: " + user.getUsername());
    }

    @PostMapping("/emergency")
    public ResponseEntity<ChatResponse> submitEmergency(@RequestBody Map<String, String> request,
                                                       Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        User user = (User) authentication.getPrincipal();
        String emergencyDetails = request.getOrDefault("details", "Emergency situation reported");

        Grievance grievance = chatService.submitEmergencyGrievance(user, emergencyDetails);

        ChatResponse response = new ChatResponse();
        response.setAiResponse("Your emergency has been reported. Authorities will be contacted immediately.");
        response.setGrievanceId(grievance.getId());
        response.setGrievanceNumber(grievance.getGrievanceNumber());
        response.setCategory("EMERGENCY");

        return ResponseEntity.ok(response);
    }
}