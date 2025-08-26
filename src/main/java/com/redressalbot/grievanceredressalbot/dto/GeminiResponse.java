package com.redressalbot.grievanceredressalbot.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeminiResponse {
    private List<Candidate> candidates;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Candidate {
        private Content content;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Content {
        private List<Part> parts;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Part {
        private String text;
    }
}