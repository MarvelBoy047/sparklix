package com.sparklix.bookingservice.config;

 import org.springframework.cloud.client.loadbalancer.LoadBalanced;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.web.client.RestTemplate;

 @Configuration
 public class AppConfig {

     @Bean
     @LoadBalanced // This enables client-side load balancing with Eureka
     public RestTemplate restTemplate() {
         return new RestTemplate();
     }
 }