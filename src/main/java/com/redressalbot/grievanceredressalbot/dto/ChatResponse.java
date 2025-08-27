package com.redressalbot.grievanceredressalbot.dto;

import lombok.Data;
import java.util.UUID;  // Add this import

@Data
public class ChatResponse {
    private UUID sessionId;  // Change from Long to UUID
    private String userMessage;
    private String aiResponse;
    private String category;
    private boolean submitForm;
    private boolean mandatoryInfoQueried;
    private String formData;
    private UUID grievanceId;  // Change from Long to UUID if Grievance also uses UUID
    private String grievanceNumber;
}