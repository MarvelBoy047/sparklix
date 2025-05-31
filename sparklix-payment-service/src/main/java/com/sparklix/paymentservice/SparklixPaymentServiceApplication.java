package com.sparklix.paymentservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.cloud.client.loadbalancer.LoadBalanced; // If calling other services

@SpringBootApplication
@EnableDiscoveryClient
public class SparklixPaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SparklixPaymentServiceApplication.class, args);
    }

    // If this service needs to call other services (e.g., booking-service to update status)
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RestTemplate plainRestTemplate() {
        return new RestTemplate();
    }
}