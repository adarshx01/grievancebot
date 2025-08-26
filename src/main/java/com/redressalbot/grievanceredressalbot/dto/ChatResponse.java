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
    private Long grievanceId;
    private String grievanceNumber;
    
    // Form submission flags
    private boolean submitForm = false;
    private boolean mandatoryInfoQueried = false;
    private String formData; // JSON string containing the structured form data
}