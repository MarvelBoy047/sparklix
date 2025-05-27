package com.sparklix.adminservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin") // Base path for admin-related end points
public class AdminController {

    @GetMapping("/hello")
    public String helloAdmin() {
        return "Hello from Sparklix Admin Service!";
    }
}