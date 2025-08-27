package com.redressalbot.grievanceredressalbot.controller;

import com.redressalbot.grievanceredressalbot.dto.ChatRequest;
import com.redressalbot.grievanceredressalbot.dto.ChatResponse;
import com.redressalbot.grievanceredressalbot.entity.User;
import com.redressalbot.grievanceredressalbot.entity.Grievance;
import com.redressalbot.grievanceredressalbot.service.ChatService;
import com.redressalbot.grievanceredressalbot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(
            @RequestBody ChatRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                log.error("Authentication failed for chat message");
                return ResponseEntity.status(401).build();
            }

            String username = authentication.getName();
            User user = userService.loadUserByUsername(username);

            ChatResponse response = chatService.processChat(request, user);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing chat message", e);
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/emergency")
    public ResponseEntity<Map<String, Object>> submitEmergency(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                log.error("Authentication failed for emergency submission");
                return ResponseEntity.status(401).build();
            }

            String username = authentication.getName();
            User user = userService.loadUserByUsername(username);

            String emergencyDetails = request.get("details");
            if (emergencyDetails == null || emergencyDetails.trim().isEmpty()) {
                log.error("Emergency details are required");
                return ResponseEntity.badRequest().build();
            }

            log.info("Processing emergency submission for user: {}", username);
            Grievance grievance = chatService.submitEmergencyGrievance(user, emergencyDetails);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Emergency submitted successfully",
                    "grievanceId", grievance.getId(),
                    "grievanceNumber", grievance.getGrievanceNumber()
            ));

        } catch (Exception e) {
            log.error("Error submitting emergency", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to submit emergency: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Chat controller is working");
    }

    @GetMapping("/sessions")
    public ResponseEntity<String> getSessions(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).build();
            }

            // Implementation for getting chat sessions
            return ResponseEntity.ok("Sessions endpoint - implementation needed");

        } catch (Exception e) {
            log.error("Error getting chat sessions", e);
            return ResponseEntity.status(500).build();
        }
    }
}