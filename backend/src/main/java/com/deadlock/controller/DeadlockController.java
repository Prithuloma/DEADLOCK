package com.deadlock.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.deadlock.service.DeadlockService;

@RestController
public class DeadlockController {

    @Autowired
    private DeadlockService deadlockService;

    @GetMapping("/api/run-deadlock")
    public ResponseEntity<String> runDeadlock() {
        String response = deadlockService.createSampleDeadlock();
        return ResponseEntity.ok(response);
    }
    // ✅ Returns the current deadlock state snapshot
    @GetMapping("/api/state")
    public ResponseEntity<?> getCurrentState() {
        try {
            return ResponseEntity.ok(deadlockService.getLastSnapshot());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching state: " + e.getMessage());
        }
    }

    // ✅ Returns the full list/history of detected deadlocks
    @GetMapping("/api/deadlocks")
    public ResponseEntity<?> getAllDeadlocks() {
        try {
            return ResponseEntity.ok(deadlockService.getResolutionHistory());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error fetching deadlocks: " + e.getMessage());
        }
    }

}


