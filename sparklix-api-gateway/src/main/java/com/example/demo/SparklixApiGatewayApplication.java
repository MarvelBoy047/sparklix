package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient; // Add this

@SpringBootApplication
@EnableDiscoveryClient
public class SparklixApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(SparklixApiGatewayApplication.class, args);
    }
}