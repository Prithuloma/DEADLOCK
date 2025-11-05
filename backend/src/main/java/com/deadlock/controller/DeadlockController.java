package com.deadlock.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.deadlock.service.DeadlockService;

/**
 * REST Controller for Deadlock Detection and Resolution API.
 * Provides endpoints for simulation, state, and resolution history.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DeadlockController {

    private final DeadlockService deadlockService;

    public DeadlockController(DeadlockService deadlockService) {
        this.deadlockService = deadlockService;
    }

    /**
     * ✅ Triggers a sample deadlock simulation.
     */
    @GetMapping("/run-deadlock")
    public ResponseEntity<?> runDeadlock() {
        try {
            String response = deadlockService.createSampleDeadlock();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating deadlock: " + e.getMessage());
        }
    }

    /**
     * ✅ Returns the current JVM deadlock snapshot.
     */
    @GetMapping("/state")
    public ResponseEntity<?> getCurrentState() {
        try {
            return ResponseEntity.ok(deadlockService.getLastSnapshot());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching state: " + e.getMessage());
        }
    }

    /**
     * ✅ Returns resolution and deadlock history.
     */
    @GetMapping("/deadlocks")
    public ResponseEntity<?> getAllDeadlocks() {
        try {
            return ResponseEntity.ok(deadlockService.getResolutionHistory());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching deadlocks: " + e.getMessage());
        }
    }
}


