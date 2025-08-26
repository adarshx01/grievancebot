package com.redressalbot.grievanceredressalbot.controller;

import com.redressalbot.grievanceredressalbot.service.UserService;
import com.redressalbot.grievanceredressalbot.dto.AuthRequest;
import com.redressalbot.grievanceredressalbot.dto.RegisterRequest;
import com.redressalbot.grievanceredressalbot.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            User user = userService.registerUser(request);
            return ResponseEntity.ok().body("User registered successfully with ID: " + user.getId());
        } catch (Exception e) {
            log.error("Registration failed", e);
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        try {
            log.info("Login attempt for username: {}", request.getUsername());
            
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Create session explicitly
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            
            userService.updateLastLogin(request.getUsername());
            
            log.info("Login successful for user: {}, Session ID: {}", request.getUsername(), session.getId());
            
            return ResponseEntity.ok().body("Login successful");
        } catch (Exception e) {
            log.error("Login failed for username: {}", request.getUsername(), e);
            return ResponseEntity.badRequest().body("Login failed: Invalid credentials");
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().body("Logout successful");
    }
    
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = (User) authentication.getPrincipal();
            return ResponseEntity.ok(user);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
    }
    
    @GetMapping("/status")
    public ResponseEntity<?> getAuthStatus(Authentication authentication, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        
        return ResponseEntity.ok().body(String.format(
            "Authenticated: %s, Session ID: %s, User: %s", 
            isAuthenticated, 
            session != null ? session.getId() : "No session",
            isAuthenticated ? authentication.getName() : "Anonymous"
        ));
    }
}