package com.example.demo; // Or preferably com.sparklix.userservice if you move it

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.sparklix.userservice.SparklixUserServiceApplication; // <--- IMPORTANT: Import YOUR main application class

@SpringBootTest(classes = SparklixUserServiceApplication.class) // <--- THIS IS THE FIX
class SparklixUserServiceApplicationTests {

    @Test
    void contextLoads() {
        // This test verifies if the Spring context can be loaded
    }

}