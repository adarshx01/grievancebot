package com.redressalbot.grievanceredressalbot.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

@RestController
public class HealthController {
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Backend is running!");
    }

    @GetMapping("/api/health")
    public ResponseEntity<String> apiHealth() {
        return ResponseEntity.ok("API is accessible!");
    }
}