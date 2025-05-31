package com.sparklix.adminservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SparklixAdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SparklixAdminServiceApplication.class, args);
    }
}