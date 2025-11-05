package com.deadlock.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resolution")
public class ResolutionController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    private boolean autoResolutionEnabled = true;
    private final Queue<Map<String, Object>> resolutionHistory = new ConcurrentLinkedQueue<>();
    
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> currentStats = new HashMap<>();
        
        // Calculate statistics from history
        int totalResolutions = resolutionHistory.size();
        long successfulResolutions = resolutionHistory.stream()
                .mapToLong(r -> Boolean.TRUE.equals(r.get("success")) ? 1 : 0)
                .sum();
        
        double successRate = totalResolutions > 0 ? (double) successfulResolutions / totalResolutions * 100 : 0.0;
        
        double avgResolutionTime = resolutionHistory.stream()
                .mapToDouble(r -> ((Number) r.getOrDefault("resolutionTime", 0)).doubleValue())
                .average()
                .orElse(0.0);
        
        String lastDetection = resolutionHistory.isEmpty() ? "Never" : 
                new Date(resolutionHistory.stream()
                        .skip(Math.max(0, resolutionHistory.size() - 1))
                        .findFirst()
                        .map(r -> ((Number) r.getOrDefault("timestamp", System.currentTimeMillis())).longValue())
                        .orElse(System.currentTimeMillis())).toString();
        
        currentStats.put("totalResolutions", totalResolutions);
        currentStats.put("successRate", Math.round(successRate * 10.0) / 10.0);
        currentStats.put("avgResolutionTime", Math.round(avgResolutionTime * 10.0) / 10.0);
        currentStats.put("lastDetection", lastDetection);
        currentStats.put("autoResolutionEnabled", autoResolutionEnabled);
        
        return currentStats;
    }
    
    @GetMapping("/history")
    public List<Map<String, Object>> getHistory() {
        // Return last 50 events in reverse order (newest first)
        List<Map<String, Object>> historyList = new ArrayList<>(resolutionHistory);
        Collections.reverse(historyList);
        return historyList.stream()
                .limit(50)
                .collect(Collectors.toList());
    }
    
    @PostMapping("/toggle")
    public Map<String, Object> toggleAutoResolution() {
        autoResolutionEnabled = !autoResolutionEnabled;
        
        Map<String, Object> response = new HashMap<>();
        response.put("autoResolutionEnabled", autoResolutionEnabled);
        response.put("message", "Auto-resolution " + (autoResolutionEnabled ? "enabled" : "disabled"));
        response.put("timestamp", System.currentTimeMillis());
        
        // Broadcast update
        messagingTemplate.convertAndSend("/topic/resolution", response);
        
        System.out.println("🔄 Auto-resolution toggled: " + autoResolutionEnabled);
        
        return response;
    }
    
    @PostMapping("/trigger")
    public Map<String, Object> triggerManualResolution() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Manual resolution triggered successfully");
        result.put("timestamp", System.currentTimeMillis());
        result.put("resolutionTime", 150 + (int)(Math.random() * 300)); // Simulated resolution time
        result.put("type", "MANUAL");
        
        // Add to history
        addToHistory(result);
        
        // Broadcast update
        messagingTemplate.convertAndSend("/topic/resolution", result);
        
        System.out.println("⚡ Manual resolution triggered");
        
        return result;
    }
    
    @PostMapping("/simulate")
    public Map<String, Object> simulateResolution(@RequestBody Map<String, Object> request) {
        boolean success = (Boolean) request.getOrDefault("success", true);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("message", success ? "Deadlock resolved automatically" : "Resolution failed");
        result.put("timestamp", System.currentTimeMillis());
        result.put("resolutionTime", success ? 80 + (int)(Math.random() * 200) : 0);
        result.put("type", "AUTO");
        result.put("status", success ? "RESOLVED" : "FAILED");
        
        // Add to history
        addToHistory(result);
        
        // Broadcast update
        messagingTemplate.convertAndSend("/topic/resolution", result);
        
        System.out.println("🤖 Simulated resolution: " + (success ? "SUCCESS" : "FAILED"));
        
        return result;
    }
    
    private void addToHistory(Map<String, Object> event) {
        resolutionHistory.offer(event);
        
        // Keep only last 100 events
        while (resolutionHistory.size() > 100) {
            resolutionHistory.poll();
        }
    }
}