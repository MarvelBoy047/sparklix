package com.sparklix.showcatalogservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean // For general internal calls if not load balanced
    public RestTemplate plainRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    @LoadBalanced // For calls to services via Eureka name (e.g., ADMIN-SERVICE)
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }
}