package com.deadlock.model;

import java.lang.management.ThreadInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data model representing a snapshot of the current deadlock state.
 * Contains information about threads, locks, and their relationships.
 */
public class DeadlockSnapshot {
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("deadlockDetected")
    private boolean deadlockDetected;
    
    @JsonProperty("threads")
    private List<ThreadData> threads;
    
    @JsonProperty("locks")
    private List<LockData> locks;
    
    @JsonProperty("edges")
    private List<EdgeData> edges;
    
    @JsonProperty("deadlockCycles")
    private List<List<Long>> deadlockCycles;
    
    @JsonProperty("additionalData")
    private Map<String, Object> additionalData;

    public DeadlockSnapshot() {
        this.timestamp = System.currentTimeMillis();
        this.threads = new ArrayList<>();
        this.locks = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.deadlockCycles = new ArrayList<>();
        this.additionalData = new HashMap<>();
    }

    /**
     * Creates a DeadlockSnapshot from ThreadInfo array (from ThreadMXBean)
     */
    public static DeadlockSnapshot from(ThreadInfo[] threadInfos) {
        DeadlockSnapshot snapshot = new DeadlockSnapshot();
        
        if (threadInfos == null || threadInfos.length == 0) {
            snapshot.deadlockDetected = false;
            return snapshot;
        }
        
        snapshot.deadlockDetected = true;
        Set<String> processedLocks = new HashSet<>();
        
        // Process each deadlocked thread
        for (ThreadInfo info : threadInfos) {
            if (info == null) continue;
            
            // Add thread data
            ThreadData thread = new ThreadData();
            thread.id = info.getThreadId();
            thread.name = info.getThreadName();
            thread.state = info.getThreadState().toString();
            thread.isDeadlocked = true;
            snapshot.threads.add(thread);
            
            // Process locks this thread is waiting for
            if (info.getLockInfo() != null) {
                String lockId = info.getLockInfo().getClassName() + "@" + 
                               Integer.toHexString(info.getLockInfo().getIdentityHashCode());
                
                if (!processedLocks.contains(lockId)) {
                    LockData lock = new LockData();
                    lock.id = lockId;
                    lock.className = info.getLockInfo().getClassName();
                    lock.type = "MONITOR";
                    snapshot.locks.add(lock);
                    processedLocks.add(lockId);
                }
                
                // Add waiting edge
                EdgeData waitingEdge = new EdgeData();
                waitingEdge.from = String.valueOf(info.getThreadId());
                waitingEdge.to = lockId;
                waitingEdge.type = "WAITING";
                snapshot.edges.add(waitingEdge);
            }
            
            // Process locks this thread owns
            if (info.getLockedMonitors() != null) {
                for (var monitor : info.getLockedMonitors()) {
                    String lockId = monitor.getClassName() + "@" + 
                                   Integer.toHexString(monitor.getIdentityHashCode());
                    
                    if (!processedLocks.contains(lockId)) {
                        LockData lock = new LockData();
                        lock.id = lockId;
                        lock.className = monitor.getClassName();
                        lock.type = "MONITOR";
                        snapshot.locks.add(lock);
                        processedLocks.add(lockId);
                    }
                    
                    // Add holding edge
                    EdgeData holdingEdge = new EdgeData();
                    holdingEdge.from = lockId;
                    holdingEdge.to = String.valueOf(info.getThreadId());
                    holdingEdge.type = "HOLDING";
                    snapshot.edges.add(holdingEdge);
                }
            }
        }
        
        return snapshot;
    }

    // Getters and Setters
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public boolean isDeadlockDetected() { return deadlockDetected; }
    public void setDeadlockDetected(boolean deadlockDetected) { this.deadlockDetected = deadlockDetected; }
    
    public List<ThreadData> getThreads() { return threads; }
    public void setThreads(List<ThreadData> threads) { this.threads = threads; }
    
    public List<LockData> getLocks() { return locks; }
    public void setLocks(List<LockData> locks) { this.locks = locks; }
    
    public List<EdgeData> getEdges() { return edges; }
    public void setEdges(List<EdgeData> edges) { this.edges = edges; }
    
    public List<List<Long>> getDeadlockCycles() { return deadlockCycles; }
    public void setDeadlockCycles(List<List<Long>> deadlockCycles) { this.deadlockCycles = deadlockCycles; }
    
    public Map<String, Object> getAdditionalData() { return additionalData; }
    public void setAdditionalData(Map<String, Object> additionalData) { this.additionalData = additionalData; }

    /**
     * Represents a thread in the deadlock graph
     */
    public static class ThreadData {
        @JsonProperty("id")
        public long id;
        
        @JsonProperty("name")
        public String name;
        
        @JsonProperty("state")
        public String state;
        
        @JsonProperty("isDeadlocked")
        public boolean isDeadlocked;
        
        @JsonProperty("stackTrace")
        public List<String> stackTrace = new ArrayList<>();
    }

    /**
     * Represents a lock in the deadlock graph
     */
    public static class LockData {
        @JsonProperty("id")
        public String id;
        
        @JsonProperty("className")
        public String className;
        
        @JsonProperty("type")
        public String type; // MONITOR, REENTRANT_LOCK, etc.
    }

    /**
     * Represents an edge in the deadlock graph (waiting or holding relationship)
     */
    public static class EdgeData {
        @JsonProperty("from")
        public String from;
        
        @JsonProperty("to")
        public String to;
        
        @JsonProperty("type")
        public String type; // WAITING, HOLDING
    }
}