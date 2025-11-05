package com.deadlock.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
}

