package com.redressalbot.grievanceredressalbot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "Grievance Redressal Bot API is running! Visit /api/chat/test to test.";
    }
}