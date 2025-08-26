package com.redressalbot.grievanceredressalbot.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private Long sessionId; // Optional: to continue existing session
}