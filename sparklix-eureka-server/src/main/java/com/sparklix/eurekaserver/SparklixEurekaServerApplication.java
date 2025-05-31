package com.sparklix.eurekaserver; // Adjust package to your project's structure

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer // This annotation enables the Eureka Server functionality
public class SparklixEurekaServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SparklixEurekaServerApplication.class, args);
	}

}