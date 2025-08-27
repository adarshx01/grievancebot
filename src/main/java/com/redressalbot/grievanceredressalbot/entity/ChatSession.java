package com.redressalbot.grievanceredressalbot.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

@Entity
@Table(name = "chat_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // Use GenerationType.UUID for PostgreSQL
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "session_title")
    private String sessionTitle;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.ACTIVE;
    
    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatMessage> messages;
    
    @Column(name = "chat_history_json", columnDefinition = "TEXT")
    private String chatHistoryJson;

    @JsonIgnore
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Method to get chat history as a list of messages
    public List<Map<String, Object>> getChatHistory() {
        if (chatHistoryJson == null || chatHistoryJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(chatHistoryJson, TypeFactory.defaultInstance().constructCollectionType(List.class, Map.class));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    // Method to add a new message and update JSON
    public void addMessage(String sender, String message) {
        List<Map<String, Object>> history = getChatHistory();
        Map<String, Object> newMessage = Map.of("sender", sender, "message", message, "timestamp", LocalDateTime.now().toString());
        history.add(newMessage);
        try {
            this.chatHistoryJson = objectMapper.writeValueAsString(history);
            this.updatedAt = LocalDateTime.now();
        } catch (JsonProcessingException e) {
            // Log error and handle gracefully
            System.err.println("Error updating chat history JSON: " + e.getMessage());
        }
    }
}