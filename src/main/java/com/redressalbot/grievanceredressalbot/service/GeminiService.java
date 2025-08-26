package com.redressalbot.grievanceredressalbot.service;

import com.redressalbot.grievanceredressalbot.dto.GeminiRequest;
import com.redressalbot.grievanceredressalbot.dto.GeminiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import java.util.List;
import java.time.Duration;

@Service
@Slf4j
public class GeminiService {
    
    private final WebClient webClient;
    private final String apiKey;
    
    public GeminiService(@Value("${gemini.api.key}") String apiKey, WebClient.Builder webClientBuilder) {
        this.apiKey = apiKey;
        this.webClient = webClientBuilder
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();
        log.info("GeminiService initialized with API key: {}***", apiKey.substring(0, Math.min(10, apiKey.length())));
    }
    
    public String generateResponse(String prompt) {
        try {
            log.info("Generating response for prompt length: {}", prompt.length());
            
            GeminiRequest request = createRequest(prompt);
            
            GeminiResponse response = webClient.post()
                .uri("/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + apiKey)
                .header("Content-Type", "application/json")
                .body(Mono.just(request), GeminiRequest.class)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), 
                    clientResponse -> {
                        log.error("Gemini API error: {}", clientResponse.statusCode());
                        return clientResponse.bodyToMono(String.class)
                            .doOnNext(body -> log.error("Error body: {}", body))
                            .then(Mono.error(new RuntimeException("Gemini API error: " + clientResponse.statusCode())));
                    })
                .bodyToMono(GeminiResponse.class)
                .timeout(Duration.ofSeconds(30))
                .block();
            
            if (response != null && 
                response.getCandidates() != null && 
                !response.getCandidates().isEmpty() &&
                response.getCandidates().get(0).getContent() != null &&
                response.getCandidates().get(0).getContent().getParts() != null &&
                !response.getCandidates().get(0).getContent().getParts().isEmpty()) {
                
                String result = response.getCandidates().get(0).getContent().getParts().get(0).getText();
                log.info("Successfully generated response of length: {}", result.length());
                return result;
            } else {
                log.warn("Empty or invalid response from Gemini API");
                return "I apologize, but I received an empty response. Please try again.";
            }
            
        } catch (Exception e) {
            log.error("Error generating response from Gemini API", e);
            return "I apologize, but I'm currently unable to process your request. Please try again later. Error: " + e.getMessage();
        }
    }
    
    private GeminiRequest createRequest(String prompt) {
        GeminiRequest.Part part = new GeminiRequest.Part();
        part.setText(prompt);
        
        GeminiRequest.Content content = new GeminiRequest.Content();
        content.setParts(List.of(part));
        
        GeminiRequest request = new GeminiRequest();
        request.setContents(List.of(content));
        
        return request;
    }
}