package com.deadlock.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.deadlock.model.DeadlockSnapshot;
import com.deadlock.service.DeadlockService;

/**
 * REST API controller for deadlock detection and management
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow cross-origin requests for development
public class DeadlockController {

    private final DeadlockService deadlockService;

    public DeadlockController(DeadlockService deadlockService) {
        this.deadlockService = deadlockService;
    }

    /**
     * GET /api/state - Returns current deadlock state
     */
    @GetMapping("/state")
    public ResponseEntity<DeadlockSnapshot> getCurrentState() {
        try {
            DeadlockSnapshot snapshot = deadlockService.detectDeadlocks();
            return ResponseEntity.ok(snapshot);
        } catch (Exception e) {
            System.err.println("❌ Error getting deadlock state: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/system - Returns system information
     */
    @GetMapping("/system")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        try {
            Map<String, Object> systemInfo = deadlockService.getSystemInfo();
            return ResponseEntity.ok(systemInfo);
        } catch (Exception e) {
            System.err.println("❌ Error getting system info: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/interrupt/{threadId} - Attempts to interrupt a specific thread
     */
    @PostMapping("/interrupt/{threadId}")
    public ResponseEntity<Map<String, Object>> interruptThread(@PathVariable long threadId) {
        try {
            Map<String, Object> result = deadlockService.interruptThread(threadId);
            
            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            System.err.println("❌ Error interrupting thread: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/health - Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Deadlock Detection Tool",
            "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }
    
    /**
     * 🚀 NEW: GET /api/resolution/history - Get resolution history
     */
    @GetMapping("/resolution/history")
    public ResponseEntity<Map<String, Object>> getResolutionHistory() {
        try {
            Map<String, Object> history = deadlockService.getResolutionHistory();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            System.err.println("❌ Error getting resolution history: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 🌐 NEW: POST /api/monitor/{pid} - Select process to monitor
     */
    @PostMapping("/monitor/{pid}")
    public ResponseEntity<Map<String, String>> selectProcess(@PathVariable String pid) {
        try {
            deadlockService.setMonitoredProcess(pid);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Now monitoring process PID: " + pid,
                "pid", pid
            ));
        } catch (Exception e) {
            System.err.println("❌ Error selecting process: " + e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * 🌐 NEW: GET /api/monitor/current - Get currently monitored process
     */
    @GetMapping("/monitor/current")
    public ResponseEntity<Map<String, String>> getCurrentProcess() {
        try {
            String pid = deadlockService.getMonitoredProcess();
            if (pid != null) {
                return ResponseEntity.ok(Map.of(
                    "pid", pid,
                    "status", "monitoring"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "status", "none",
                    "message", "No external process selected"
                ));
            }
        } catch (Exception e) {
            System.err.println("❌ Error getting current process: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 🚀 NEW: GET /api/resolution/stats - Get resolution statistics
     */
    @GetMapping("/resolution/stats")
    public ResponseEntity<Map<String, Object>> getResolutionStats() {
        try {
            Map<String, Object> stats = deadlockService.getResolutionStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            System.err.println("❌ Error getting resolution stats: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 🚀 NEW: POST /api/resolution/toggle - Toggle auto-resolution on/off
     */
    @PostMapping("/resolution/toggle")
    public ResponseEntity<Map<String, Object>> toggleAutoResolution() {
        try {
            Map<String, Object> result = deadlockService.toggleAutoResolution();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("❌ Error toggling auto-resolution: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 🚀 NEW: POST /api/resolution/trigger - Manually trigger resolution for current deadlocks
     */
    @PostMapping("/resolution/trigger")
    public ResponseEntity<Map<String, Object>> triggerManualResolution() {
        try {
            Map<String, Object> result = deadlockService.triggerManualResolution();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("❌ Error triggering manual resolution: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}