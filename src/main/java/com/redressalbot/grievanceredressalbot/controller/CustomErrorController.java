package com.redressalbot.grievanceredressalbot.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<String> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        String message = (String) request.getAttribute("javax.servlet.error.message");
        
        if (statusCode != null) {
            return ResponseEntity.status(statusCode)
                .body(String.format("Error %d: %s", statusCode, message != null ? message : "Unknown error"));
        }
        
        return ResponseEntity.badRequest().body("An error occurred");
    }
}