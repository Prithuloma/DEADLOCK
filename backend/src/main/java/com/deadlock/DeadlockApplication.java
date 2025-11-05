package com.deadlock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Spring Boot application for the Deadlock Detection Tool.
 * This application provides REST APIs and WebSocket endpoints for 
 * real-time deadlock monitoring and visualization.
 */
@SpringBootApplication(scanBasePackages = "com.deadlock")
@EnableScheduling
public class DeadlockApplication {

    public static void main(String[] args) {
        System.out.println("🚀 Starting Deadlock Detection Tool...");
        SpringApplication.run(DeadlockApplication.class, args);
        System.out.println("✅ Deadlock Detection Tool is running!");
        System.out.println("📊 Dashboard: http://localhost:8080");
        System.out.println("🔍 API Endpoint: http://localhost:8080/api/state");
    }
}