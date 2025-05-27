package com.sparklix.bookingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SparklixBookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SparklixBookingServiceApplication.class, args);
    }
}