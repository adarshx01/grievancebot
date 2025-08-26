package com.redressalbot.grievanceredressalbot.controller;

import com.redressalbot.grievanceredressalbot.entity.Grievance;
import com.redressalbot.grievanceredressalbot.entity.User;
import com.redressalbot.grievanceredressalbot.service.GrievanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @PathVariable Long id, Authentication authentication) {
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
}