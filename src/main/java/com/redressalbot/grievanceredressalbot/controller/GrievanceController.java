package com.redressalbot.grievanceredressalbot.controller;

import com.redressalbot.grievanceredressalbot.dto.ComplaintFormData;  // Added import for ComplaintFormData
import com.redressalbot.grievanceredressalbot.entity.Grievance;
import com.redressalbot.grievanceredressalbot.entity.User;
import com.redressalbot.grievanceredressalbot.service.GrievanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;  // Added import for AuthenticationPrincipal
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/grievance")
@RequiredArgsConstructor
@Slf4j
public class GrievanceController {

    private final GrievanceService grievanceService;

    @GetMapping("/list")
    public ResponseEntity<List<Grievance>> getUserGrievances(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        User user = (User) authentication.getPrincipal();
        List<Grievance> grievances = grievanceService.getUserGrievances(user);
        return ResponseEntity.ok(grievances);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Grievance> getGrievanceDetails(
            @PathVariable UUID id, Authentication authentication) {  // Change from Long to UUID
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        User user = (User) authentication.getPrincipal();
        return grievanceService.getGrievanceById(id, user)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/admin/list")
    public ResponseEntity<List<Grievance>> getAllGrievances(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        User user = (User) authentication.getPrincipal();
        if (!user.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(403).build();
        }

        List<Grievance> grievances = grievanceService.getAllGrievances();
        return ResponseEntity.ok(grievances);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerGrievance(@RequestBody ComplaintFormData formData, @AuthenticationPrincipal User user) {
        try {
            // Validate user and form data
            if (user == null) {
                return ResponseEntity.status(401).body("User not authenticated");
            }
            // Process registration (e.g., save to database)
            grievanceService.register(formData, user);
            return ResponseEntity.ok("Grievance registered successfully");
        } catch (Exception e) {
            // Log error for debugging
            return ResponseEntity.status(500).body("Registration failed: " + e.getMessage());
        }
    }
}