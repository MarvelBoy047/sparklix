package com.sparklix.bookingservice; // Your booking service package

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
        "com.sparklix.bookingservice",
        "com.sparklix.commons.aop"
})
public class SparklixBookingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SparklixBookingServiceApplication.class, args);
    }
}