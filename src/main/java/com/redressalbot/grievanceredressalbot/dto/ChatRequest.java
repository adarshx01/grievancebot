package com.redressalbot.grievanceredressalbot.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class ChatRequest {
    private UUID sessionId;  // Change from Long to UUID
    private String message;
}