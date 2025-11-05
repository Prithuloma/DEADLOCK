package com.deadlock.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MonitorController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    private String currentMonitoredPid = null;
    private String currentProcessName = null;
    
    @PostMapping("/monitor/{pid}")
    public Map<String, Object> selectProcess(@PathVariable String pid, @RequestParam(required = false) String name) {
        currentMonitoredPid = pid;
        currentProcessName = name != null ? name : "Process " + pid;
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Now monitoring process " + currentProcessName + " (PID: " + pid + ")");
        response.put("pid", pid);
        response.put("processName", currentProcessName);
        response.put("timestamp", System.currentTimeMillis());
        
        // Broadcast process selection update
        messagingTemplate.convertAndSend("/topic/monitor", response);
        
        System.out.println("🎯 Selected process for monitoring: " + currentProcessName + " (PID: " + pid + ")");
        
        return response;
    }
    
    @GetMapping("/monitor/current")
    public Map<String, Object> getCurrentProcess() {
        Map<String, Object> response = new HashMap<>();
        response.put("pid", currentMonitoredPid);
        response.put("processName", currentProcessName);
        response.put("monitoring", currentMonitoredPid != null);
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }
    
    @GetMapping("/processes")
    public List<Map<String, Object>> getProcessList() {
        // Simulate process list - in real implementation, this would get actual Java processes
        List<Map<String, Object>> processes = new ArrayList<>();
        
        // Add some sample processes
        addProcess(processes, "12345", "DeadlockTestApp", "java -cp test.jar DeadlockTestApp");
        addProcess(processes, "67890", "SampleApplication", "java -jar sample-app.jar");
        addProcess(processes, "11111", "TestDeadlock", "java TestDeadlock");
        addProcess(processes, "22222", "MultiThreadApp", "java -Xmx512m MultiThreadApp");
        
        return processes;
    }
    
    @DeleteMapping("/monitor/stop")
    public Map<String, Object> stopMonitoring() {
        String previousPid = currentMonitoredPid;
        String previousName = currentProcessName;
        
        currentMonitoredPid = null;
        currentProcessName = null;
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Stopped monitoring " + (previousName != null ? previousName : "process"));
        response.put("previousPid", previousPid);
        response.put("timestamp", System.currentTimeMillis());
        
        // Broadcast stop monitoring update
        messagingTemplate.convertAndSend("/topic/monitor", response);
        
        System.out.println("⏹️ Stopped monitoring process: " + previousName);
        
        return response;
    }
    
    private void addProcess(List<Map<String, Object>> processes, String pid, String name, String command) {
        Map<String, Object> process = new HashMap<>();
        process.put("pid", pid);
        process.put("displayName", name);
        process.put("name", name);
        process.put("command", command);
        process.put("status", "RUNNING");
        process.put("cpuUsage", Math.random() * 20); // Simulated CPU usage
        process.put("memoryUsage", Math.random() * 1024); // Simulated memory usage in MB
        processes.add(process);
    }
}