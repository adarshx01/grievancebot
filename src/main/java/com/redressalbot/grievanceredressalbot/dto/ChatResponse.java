package com.redressalbot.grievanceredressalbot.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {
    private Long sessionId;
    private String userMessage;
    private String aiResponse;
    private String category;
    private Long grievanceId; // If it's a grievance
    private String grievanceNumber;
}