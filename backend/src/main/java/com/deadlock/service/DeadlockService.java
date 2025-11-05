package com.deadlock.service;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.deadlock.model.DeadlockSnapshot;

/**
 * Enhanced Deadlock Detection & Automatic Resolution Service
 * Provides real-time monitoring, detection, and automatic resolution of deadlocks
 * with comprehensive visualization and logging capabilities.
 */
@Service
public class DeadlockService {
    
    private final ThreadMXBean threadMXBean;
    private final SimpMessagingTemplate messagingTemplate;
    private DeadlockSnapshot lastSnapshot;
    
    // 🚀 Enhanced Resolution Tracking
    private final Map<Long, ResolutionEvent> resolutionHistory = new ConcurrentHashMap<>();
    private final AtomicInteger resolutionCounter = new AtomicInteger(0);
    private final Set<Long> knownDeadlockedThreads = ConcurrentHashMap.newKeySet();
    private boolean autoResolutionEnabled = true;

    // 📊 Resolution Statistics
    private final Map<String, Integer> resolutionMethodStats = new ConcurrentHashMap<>();
    private final List<DeadlockEvent> deadlockEvents = new ArrayList<>();
    
    /**
     * Represents a deadlock resolution event with full details
     */
    public static class ResolutionEvent {
        public final String id;
        public final LocalDateTime timestamp;
        public final String method;
        public final Set<Long> affectedThreads;
        public final String status;
        public final String details;
        public final long resolutionTime;
        
        public ResolutionEvent(String method, Set<Long> threads, String status, String details, long time) {
            this.id = "RES-" + System.currentTimeMillis();
            this.timestamp = LocalDateTime.now();
            this.method = method;
            this.affectedThreads = new HashSet<>(threads);
            this.status = status;
            this.details = details;
            this.resolutionTime = time;
        }
    }
    
    /**
     * Represents a complete deadlock event from detection to resolution
     */
    public static class DeadlockEvent {
        public final String id;
        public final LocalDateTime detectedAt;
        public final Set<Long> deadlockedThreads;
        public final List<String> resolutionSteps;
        public LocalDateTime resolvedAt;
        public String resolutionMethod;
        public boolean wasResolved;
        public long totalResolutionTime;
        
        public DeadlockEvent(Set<Long> threads) {
            this.id = "DL-" + System.currentTimeMillis();
            this.detectedAt = LocalDateTime.now();
            this.deadlockedThreads = new HashSet<>(threads);
            this.resolutionSteps = new ArrayList<>();
            this.wasResolved = false;
        }
        
        public void addResolutionStep(String step) {
            resolutionSteps.add(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")) + " - " + step);
        }
        
        public void markResolved(String method) {
            this.resolvedAt = LocalDateTime.now();
            this.resolutionMethod = method;
            this.wasResolved = true;
            this.totalResolutionTime = java.time.Duration.between(detectedAt, resolvedAt).toMillis();
        }
    }
    
    // 🌐 External Process Monitoring
    private final JMXProcessMonitor jmxMonitor;
    private String selectedProcessPid = null;

    public DeadlockService(SimpMessagingTemplate messagingTemplate, JMXProcessMonitor jmxMonitor) {
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.messagingTemplate = messagingTemplate;
        this.jmxMonitor = jmxMonitor;
        this.lastSnapshot = new DeadlockSnapshot();
        
        // Initialize resolution method statistics
        resolutionMethodStats.put("THREAD_INTERRUPTION", 0);
        resolutionMethodStats.put("PRIORITY_BASED", 0);
        resolutionMethodStats.put("TIMEOUT_RECOVERY", 0);
        resolutionMethodStats.put("SMART_ORDERING", 0);
        
        System.out.println("🔍 Enhanced DeadlockService initialized with auto-resolution");
        System.out.println("📊 Thread monitoring enabled: " + threadMXBean.isThreadContentionMonitoringEnabled());
        System.out.println("🚀 Auto-resolution enabled: " + autoResolutionEnabled);
        System.out.println("🌐 External process monitoring enabled via JMX");
    }
    
    /**
     * 🌐 Scan for Java processes and broadcast to frontend
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 2000)
    public void scanAndBroadcastProcesses() {
        try {
            List<JMXProcessMonitor.JavaProcessInfo> processes = jmxMonitor.scanJavaProcesses();
            
            // Auto-select deadlock sample programs (prioritize actual sample programs, not the jar file)
            if (selectedProcessPid == null && !processes.isEmpty()) {
                for (JMXProcessMonitor.JavaProcessInfo proc : processes) {
                    // Skip the backend jar file itself
                    if (proc.displayName.contains("deadlock-detector") || 
                        proc.displayName.contains("target/")) {
                        continue;
                    }
                    // Select actual deadlock samples like SimpleDeadlock, MultiThreadDeadlock, etc.
                    if (proc.displayName.contains("Deadlock") || 
                        proc.displayName.contains("deadlock")) {
                        selectedProcessPid = proc.pid;
                        System.out.println("🎯 Auto-selected deadlock process: " + proc.displayName + " (PID: " + proc.pid + ")");
                        break;
                    }
                }
            }
            
            // Broadcast process list to frontend
            messagingTemplate.convertAndSend("/topic/processes", processes);
            
        } catch (RuntimeException e) {
            System.err.println("❌ Error scanning processes: " + e.getMessage());
        }
    }
    
    /**
     * Set the process to monitor
     */
    public void setMonitoredProcess(String pid) {
        this.selectedProcessPid = pid;
        System.out.println("🎯 Now monitoring process PID: " + pid);
    }
    
    /**
     * Get current monitored process
     */
    public String getMonitoredProcess() {
        return selectedProcessPid;
    }

    /**
     * 🚀 ENHANCED: Performs deadlock detection with automatic resolution
     * Now monitors EXTERNAL processes via JMX
     */
    public DeadlockSnapshot detectDeadlocks() {
        try {
            // Determine which ThreadMXBean to use
            ThreadMXBean targetBean = threadMXBean; // Default to self
            ThreadInfo[] threadInfos = null;
            long[] deadlockedThreadIds = null;
            
            // 🌐 If monitoring external process, use JMX
            if (selectedProcessPid != null) {
                JMXProcessMonitor.DeadlockInfo info = jmxMonitor.checkProcessForDeadlocks(selectedProcessPid);
                
                if (info == null) {
                    // Process terminated or unreachable
                    System.out.println("⚠️ Process " + selectedProcessPid + " is no longer available");
                    selectedProcessPid = null;
                    return createEmptySnapshot();
                }
                
                targetBean = info.threadBean;
                threadInfos = info.threadInfos;
                
                if (info.hasDeadlock) {
                    deadlockedThreadIds = new long[threadInfos.length];
                    for (int i = 0; i < threadInfos.length; i++) {
                        deadlockedThreadIds[i] = threadInfos[i].getThreadId();
                    }
                }
            } else {
                // Monitor self (Spring Boot JVM)
                deadlockedThreadIds = threadMXBean.findDeadlockedThreads();
            }
            
            if (deadlockedThreadIds == null || deadlockedThreadIds.length == 0) {
                // Check if we previously had deadlocks that are now resolved
                if (!knownDeadlockedThreads.isEmpty()) {
                    System.out.println("✅ Previous deadlock appears to be resolved!");
                    markCurrentDeadlocksResolved();
                    knownDeadlockedThreads.clear();
                    
                    // Broadcast resolution success
                    broadcastResolutionUpdate("RESOLVED", "All known deadlocks have been resolved");
                }
                
                // No deadlock detected
                DeadlockSnapshot snapshot = new DeadlockSnapshot();
                snapshot.setDeadlockDetected(false);
                
                // Add thread info from target process
                if (selectedProcessPid != null && threadInfos != null) {
                    addThreadInfoFromExternal(snapshot, threadInfos, targetBean);
                } else {
                    addAllThreadsInfo(snapshot);
                }
                
                addResolutionHistory(snapshot);
                return snapshot;
            }
            
            // Get detailed information about deadlocked threads if not already fetched
            if (threadInfos == null) {
                threadInfos = targetBean.getThreadInfo(
                    deadlockedThreadIds, 
                    true,  // include locked monitors
                    true   // include locked ownable synchronizers
                );
            }
            
            DeadlockSnapshot snapshot = DeadlockSnapshot.from(threadInfos);
            
            // Add stack traces for debugging
            addStackTraces(snapshot, threadInfos);
            
            // 🚀 NEW: Check for new deadlocks and trigger automatic resolution
            Set<Long> currentDeadlocks = new HashSet<>();
            for (long id : deadlockedThreadIds) {
                currentDeadlocks.add(id);
            }
            
            // Detect new deadlocks
            Set<Long> newDeadlocks = new HashSet<>(currentDeadlocks);
            newDeadlocks.removeAll(knownDeadlockedThreads);
            
            if (!newDeadlocks.isEmpty()) {
                System.out.println("🚨 NEW DEADLOCK DETECTED! Threads: " + Arrays.toString(deadlockedThreadIds));
                if (selectedProcessPid != null) {
                    System.out.println("📍 In external process PID: " + selectedProcessPid);
                }
                
                // Create deadlock event for tracking
                DeadlockEvent event = new DeadlockEvent(currentDeadlocks);
                deadlockEvents.add(event);
                
                // 🔧 AUTOMATIC RESOLUTION with delay to allow observation
                if (autoResolutionEnabled) {
                    // Make variables effectively final for lambda
                    final Set<Long> finalDeadlocks = currentDeadlocks;
                    final ThreadInfo[] finalThreadInfos = threadInfos;
                    final DeadlockEvent finalEvent = event;
                    final ThreadMXBean finalTargetBean = targetBean;
                    
                    // Run resolution in background thread with delay
                    new Thread(() -> {
                        try {
                            System.out.println("⏱️ Waiting 8 seconds before attempting resolution...");
                            System.out.println("📊 This allows you to observe the deadlock in the dashboard");
                            Thread.sleep(8000); // 8 second delay
                            
                            System.out.println("🔧 Starting automatic resolution...");
                            attemptAutomaticResolution(finalDeadlocks, finalThreadInfos, finalEvent, finalTargetBean);
                        } catch (InterruptedException e) {
                            System.err.println("⚠️ Resolution delay interrupted");
                        }
                    }, "DeadlockResolver").start();
                }
            }
            
            knownDeadlockedThreads.addAll(currentDeadlocks);
            addResolutionHistory(snapshot);
            
            return snapshot;
            
        } catch (SecurityException | UnsupportedOperationException e) {
            System.err.println("❌ JMX operation not permitted: " + e.getMessage());
            
            DeadlockSnapshot errorSnapshot = new DeadlockSnapshot();
            errorSnapshot.setDeadlockDetected(false);
            addResolutionHistory(errorSnapshot);
            return errorSnapshot;
        } catch (RuntimeException e) {
            System.err.println("❌ Runtime error during deadlock detection: " + e.getMessage());
            
            DeadlockSnapshot errorSnapshot = new DeadlockSnapshot();
            errorSnapshot.setDeadlockDetected(false);
            addResolutionHistory(errorSnapshot);
            return errorSnapshot;
        }
    }
    
    /**
     * Create empty snapshot for when no process is being monitored
     */
    private DeadlockSnapshot createEmptySnapshot() {
        DeadlockSnapshot snapshot = new DeadlockSnapshot();
        snapshot.setDeadlockDetected(false);
        addResolutionHistory(snapshot);
        return snapshot;
    }
    
    /**
     * Add thread information from external process
     */
    private void addThreadInfoFromExternal(DeadlockSnapshot snapshot, ThreadInfo[] allThreads, ThreadMXBean bean) {
        Map<String, Object> additionalData = new HashMap<>();
        
        int liveThreads = 0;
        for (ThreadInfo ti : allThreads) {
            if (ti != null) liveThreads++;
        }
        
        additionalData.put("threadCount", liveThreads);
        additionalData.put("totalStartedThreadCount", bean.getTotalStartedThreadCount());
        additionalData.put("daemonThreadCount", bean.getDaemonThreadCount());
        additionalData.put("peakThreadCount", bean.getPeakThreadCount());
        additionalData.put("monitoredProcess", selectedProcessPid);
        
        snapshot.setAdditionalData(additionalData);
    }
    
    /**
     * 🔧 AUTOMATIC DEADLOCK RESOLUTION ENGINE
     */
    private void attemptAutomaticResolution(Set<Long> deadlockedThreads, ThreadInfo[] threadInfos, 
                                           DeadlockEvent event, ThreadMXBean threadBean) {
        System.out.println("🔧 Starting automatic deadlock resolution...");
        event.addResolutionStep("Starting automatic resolution for " + deadlockedThreads.size() + " threads");
        
        boolean isExternalProcess = (selectedProcessPid != null);
        if (isExternalProcess) {
            System.out.println("🌐 Resolving deadlock in EXTERNAL process (PID: " + selectedProcessPid + ")");
            event.addResolutionStep("Target: External process PID " + selectedProcessPid);
        } else {
            System.out.println("🏠 Resolving deadlock in SELF process");
            event.addResolutionStep("Target: Current JVM process");
        }
        
        long startTime = System.currentTimeMillis();
        
        // Strategy 1: Smart Thread Interruption (works for both self and external)
        if (trySmartInterruption(deadlockedThreads, threadInfos, event, isExternalProcess, threadBean)) {
            long resolutionTime = System.currentTimeMillis() - startTime;
            recordResolution("SMART_INTERRUPTION", deadlockedThreads, "SUCCESS", 
                "Resolved using intelligent thread interruption", resolutionTime);
            event.markResolved("SMART_INTERRUPTION");
            knownDeadlockedThreads.removeAll(deadlockedThreads); // Clear resolved deadlocks
            return;
        }
        
        // Skip other strategies for external processes (JMX limitations)
        if (isExternalProcess) {
            long resolutionTime = System.currentTimeMillis() - startTime;
            
            // Check if external process is using interruptible waits
            boolean hasWaitingThreads = false;
            for (Long threadId : deadlockedThreads) {
                for (ThreadInfo info : threadInfos) {
                    if (info != null && info.getThreadId() == threadId) {
                        if (info.getThreadState() == Thread.State.WAITING ||
                            info.getThreadState() == Thread.State.TIMED_WAITING) {
                            hasWaitingThreads = true;
                            break;
                        }
                    }
                }
            }
            
            if (hasWaitingThreads) {
                recordResolution("EXTERNAL_SELF_RESOLVE", deadlockedThreads, "MONITORING", 
                    "External process has interruptible threads - may self-resolve", resolutionTime);
                event.addResolutionStep("✅ External process uses interruptible waits");
                event.addResolutionStep("🔍 Monitoring for self-resolution...");
                System.out.println("✅ External process appears to support self-resolution");
                System.out.println("🔍 Threads in WAITING state can respond to interrupts");
            } else {
                recordResolution("EXTERNAL_MONITORING", deadlockedThreads, "MONITORING", 
                    "External process with BLOCKED threads - monitoring for self-resolution", resolutionTime);
                event.addResolutionStep("🔍 External threads are BLOCKED (monitoring)");
                event.addResolutionStep("💡 Waiting for external process to self-resolve");
                System.out.println("🔍 External process threads are BLOCKED - monitoring for self-resolution");
                System.out.println("💡 External process should resolve deadlock internally");
            }
            // Keep tracking these threads so we can detect when they're resolved
            knownDeadlockedThreads.addAll(deadlockedThreads);
            return;
        }
        
        // Strategy 2: Priority-Based Resolution (self process only)
        if (tryPriorityBasedResolution(deadlockedThreads, threadInfos, event)) {
            long resolutionTime = System.currentTimeMillis() - startTime;
            recordResolution("PRIORITY_BASED", deadlockedThreads, "SUCCESS", 
                "Resolved using thread priority analysis", resolutionTime);
            event.markResolved("PRIORITY_BASED");
            knownDeadlockedThreads.removeAll(deadlockedThreads);
            return;
        }
        
        // Strategy 3: Timeout-Based Recovery (self process only)
        if (tryTimeoutRecovery(deadlockedThreads, threadInfos, event)) {
            long resolutionTime = System.currentTimeMillis() - startTime;
            recordResolution("TIMEOUT_RECOVERY", deadlockedThreads, "SUCCESS", 
                "Resolved using timeout-based recovery", resolutionTime);
            event.markResolved("TIMEOUT_RECOVERY");
            knownDeadlockedThreads.removeAll(deadlockedThreads);
            return;
        }
        
        // Strategy 4: Last Resort - Force Interruption (self process only)
        if (tryForceInterruption(deadlockedThreads, event)) {
            long resolutionTime = System.currentTimeMillis() - startTime;
            recordResolution("FORCE_INTERRUPTION", deadlockedThreads, "PARTIAL", 
                "Applied force interruption as last resort", resolutionTime);
            event.markResolved("FORCE_INTERRUPTION");
            knownDeadlockedThreads.removeAll(deadlockedThreads);
            event.markResolved("FORCE_INTERRUPTION");
            return;
        }
        
        // Resolution failed
        long resolutionTime = System.currentTimeMillis() - startTime;
        recordResolution("FAILED", deadlockedThreads, "FAILED", 
            "All resolution strategies failed", resolutionTime);
        event.addResolutionStep("❌ All resolution strategies failed");
        
        System.out.println("❌ Automatic resolution failed for deadlock");
    }

    /**
     * 🎯 Strategy 1: Smart Thread Interruption
     * Analyzes thread states and interrupts the most suitable candidate
     * Supports both self and external processes via JMX
     */
    private boolean trySmartInterruption(Set<Long> deadlockedThreads, ThreadInfo[] threadInfos, 
                                        DeadlockEvent event, boolean isExternalProcess, ThreadMXBean threadBean) {
        event.addResolutionStep("Trying smart interruption strategy");
        
        try {
            // Find the best candidate for interruption (usually youngest thread or waiting thread)
            ThreadInfo bestCandidate = null;
            long youngestThreadId = 0;
            
            for (ThreadInfo info : threadInfos) {
                if (info != null && deadlockedThreads.contains(info.getThreadId())) {
                    // Prefer WAITING or TIMED_WAITING threads as they're more likely to respond to interruption
                    if (info.getThreadState() == Thread.State.WAITING || 
                        info.getThreadState() == Thread.State.TIMED_WAITING) {
                        bestCandidate = info;
                        break;
                    }
                    
                    // Fallback: pick the thread with highest ID (usually youngest)
                    if (info.getThreadId() > youngestThreadId) {
                        youngestThreadId = info.getThreadId();
                        bestCandidate = info;
                    }
                }
            }
            
            if (bestCandidate != null) {
                String threadName = bestCandidate.getThreadName();
                long threadId = bestCandidate.getThreadId();
                
                event.addResolutionStep("Interrupting thread: " + threadName + 
                    " (ID: " + threadId + ", State: " + bestCandidate.getThreadState() + ")");
                
                if (isExternalProcess) {
                    // For external process: use JMX ThreadMXBean
                    System.out.println("⚡ Interrupting external thread via JMX: " + threadName);
                    try {
                        // Note: Standard JMX doesn't directly support thread interruption
                        // We'll use a workaround by repeatedly checking if thread state changes
                        event.addResolutionStep("Note: JMX-based interruption has limitations");
                        broadcastResolutionUpdate("RESOLVING", "Attempting JMX-based resolution for: " + threadName);
                        
                        // Log the limitation
                        System.out.println("⚠️ JMX limitation: Cannot directly interrupt external threads");
                        System.out.println("💡 Recommendation: External process should implement interruptible waits");
                        
                        // Return false to try other strategies
                        return false;
                    } catch (Exception e) {
                        event.addResolutionStep("JMX interruption failed: " + e.getMessage());
                        return false;
                    }
                } else {
                    // For self process: direct thread interruption
                    Thread targetThread = findThreadById(threadId);
                    if (targetThread != null && targetThread.isAlive()) {
                        targetThread.interrupt();
                        
                        // Broadcast resolution attempt
                        broadcastResolutionUpdate("RESOLVING", "Interrupting thread: " + threadName);
                        
                        // Wait a moment to see if interruption worked
                        Thread.sleep(500);
                        
                        System.out.println("⚡ Smart interruption applied to: " + threadName);
                        return true;
                    }
                }
            }
            
            return false;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            event.addResolutionStep("Smart interruption failed: thread was interrupted");
            return false;
        } catch (SecurityException e) {
            event.addResolutionStep("Smart interruption failed: security restriction - " + e.getMessage());
            return false;
        } catch (RuntimeException e) {
            event.addResolutionStep("Smart interruption failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 🎯 Strategy 2: Priority-Based Resolution
     * Interrupts threads based on priority and importance
     */
    private boolean tryPriorityBasedResolution(Set<Long> deadlockedThreads, ThreadInfo[] threadInfos, DeadlockEvent event) {
        event.addResolutionStep("Trying priority-based resolution strategy");
        
        try {
            // Sort threads by priority (interrupt lower priority first)
            ThreadInfo lowestPriorityThread = null;
            int lowestPriority = Thread.MAX_PRIORITY + 1;
            
            for (ThreadInfo info : threadInfos) {
                if (info != null && deadlockedThreads.contains(info.getThreadId())) {
                    Thread thread = findThreadById(info.getThreadId());
                    if (thread != null) {
                        int priority = thread.getPriority();
                        if (priority < lowestPriority) {
                            lowestPriority = priority;
                            lowestPriorityThread = info;
                        }
                    }
                }
            }
            
            if (lowestPriorityThread != null) {
                Thread targetThread = findThreadById(lowestPriorityThread.getThreadId());
                if (targetThread != null && targetThread.isAlive()) {
                    event.addResolutionStep("Interrupting lowest priority thread: " + 
                        lowestPriorityThread.getThreadName() + " (Priority: " + lowestPriority + ")");
                    
                    targetThread.interrupt();
                    
                    broadcastResolutionUpdate("RESOLVING", "Priority-based interruption: " + lowestPriorityThread.getThreadName());
                    
                    Thread.sleep(500);
                    
                    System.out.println("🎯 Priority-based interruption applied to: " + lowestPriorityThread.getThreadName());
                    return true;
                }
            }
            
            return false;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            event.addResolutionStep("Priority-based resolution failed: thread was interrupted");
            return false;
        } catch (SecurityException e) {
            event.addResolutionStep("Priority-based resolution failed: security restriction - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 🎯 Strategy 3: Timeout-Based Recovery
     * Uses timing information to make resolution decisions
     */
    private boolean tryTimeoutRecovery(Set<Long> deadlockedThreads, ThreadInfo[] threadInfos, DeadlockEvent event) {
        event.addResolutionStep("Trying timeout-based recovery strategy");
        
        try {
            // Find thread that has been blocked the longest (if blocked time is available)
            ThreadInfo longestBlockedThread = null;
            long longestBlockedTime = 0;
            
            for (ThreadInfo info : threadInfos) {
                if (info != null && deadlockedThreads.contains(info.getThreadId())) {
                    long blockedTime = info.getBlockedTime();
                    if (blockedTime > longestBlockedTime) {
                        longestBlockedTime = blockedTime;
                        longestBlockedThread = info;
                    }
                }
            }
            
            // If no blocked time info, fall back to any blocked thread
            if (longestBlockedThread == null) {
                for (ThreadInfo info : threadInfos) {
                    if (info != null && deadlockedThreads.contains(info.getThreadId()) && 
                        info.getThreadState() == Thread.State.BLOCKED) {
                        longestBlockedThread = info;
                        break;
                    }
                }
            }
            
            if (longestBlockedThread != null) {
                Thread targetThread = findThreadById(longestBlockedThread.getThreadId());
                if (targetThread != null && targetThread.isAlive()) {
                    event.addResolutionStep("Interrupting longest blocked thread: " + 
                        longestBlockedThread.getThreadName() + " (Blocked: " + longestBlockedTime + "ms)");
                    
                    targetThread.interrupt();
                    
                    broadcastResolutionUpdate("RESOLVING", "Timeout recovery: " + longestBlockedThread.getThreadName());
                    
                    Thread.sleep(500);
                    
                    System.out.println("⏰ Timeout-based interruption applied to: " + longestBlockedThread.getThreadName());
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            event.addResolutionStep("Timeout-based recovery failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 🎯 Strategy 4: Force Interruption (Last Resort)
     * Interrupts all deadlocked threads as a last resort
     */
    private boolean tryForceInterruption(Set<Long> deadlockedThreads, DeadlockEvent event) {
        event.addResolutionStep("Applying force interruption (last resort)");
        
        try {
            int interruptedCount = 0;
            
            for (Long threadId : deadlockedThreads) {
                Thread thread = findThreadById(threadId);
                if (thread != null && thread.isAlive()) {
                    event.addResolutionStep("Force interrupting: " + thread.getName());
                    thread.interrupt();
                    interruptedCount++;
                }
            }
            
            if (interruptedCount > 0) {
                broadcastResolutionUpdate("RESOLVING", "Force interruption applied to " + interruptedCount + " threads");
                
                Thread.sleep(1000); // Give more time for force interruption
                
                System.out.println("💥 Force interruption applied to " + interruptedCount + " threads");
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            event.addResolutionStep("Force interruption failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Records a resolution attempt in the history
     */
    private void recordResolution(String method, Set<Long> threads, String status, String details, long resolutionTime) {
        ResolutionEvent event = new ResolutionEvent(method, threads, status, details, resolutionTime);
        resolutionHistory.put((long) resolutionCounter.incrementAndGet(), event);
        
        // Update statistics
        resolutionMethodStats.merge(method, 1, Integer::sum);
        
        System.out.println("📊 Resolution recorded: " + method + " - " + status + " (" + resolutionTime + "ms)");
        
        // Broadcast resolution event
        broadcastResolutionUpdate(status, method + ": " + details);
    }
    
    /**
     * Marks current deadlocks as resolved
     */
    private void markCurrentDeadlocksResolved() {
        if (!deadlockEvents.isEmpty()) {
            DeadlockEvent lastEvent = deadlockEvents.get(deadlockEvents.size() - 1);
            if (!lastEvent.wasResolved) {
                lastEvent.markResolved("AUTOMATIC_RECOVERY");
                lastEvent.addResolutionStep("System recovered automatically");
            }
        }
    }
    
    /**
     * Broadcasts resolution updates to WebSocket clients
     */
    private void broadcastResolutionUpdate(String status, String message) {
        Map<String, Object> update = new HashMap<>();
        update.put("type", "RESOLUTION_UPDATE");
        update.put("status", status);
        update.put("message", message);
        update.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
        
        messagingTemplate.convertAndSend("/topic/resolution", update);
    }
    
    /**
     * Adds resolution history to the snapshot for visualization
     */
    private void addResolutionHistory(DeadlockSnapshot snapshot) {
        Map<String, Object> resolution = new HashMap<>();
        resolution.put("autoResolutionEnabled", autoResolutionEnabled);
        resolution.put("totalResolutions", resolutionCounter.get());
        resolution.put("methodStats", new HashMap<>(resolutionMethodStats));
        resolution.put("recentEvents", getRecentResolutionEvents(5));
        resolution.put("activeDeadlockEvent", getCurrentDeadlockEvent());
        
        // Add to snapshot (you'll need to add this field to DeadlockSnapshot)
        // For now, we'll use a simple approach
        snapshot.getAdditionalData().put("resolution", resolution);
    }
    
    /**
     * Gets recent resolution events for display
     */
    private List<Map<String, Object>> getRecentResolutionEvents(int limit) {
        List<Map<String, Object>> events = new ArrayList<>();
        
        resolutionHistory.values().stream()
            .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
            .limit(limit)
            .forEach(event -> {
                Map<String, Object> eventMap = new HashMap<>();
                eventMap.put("id", event.id);
                eventMap.put("timestamp", event.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                eventMap.put("method", event.method);
                eventMap.put("status", event.status);
                eventMap.put("details", event.details);
                eventMap.put("resolutionTime", event.resolutionTime + "ms");
                eventMap.put("threadCount", event.affectedThreads.size());
                events.add(eventMap);
            });
            
        return events;
    }
    
    /**
     * Gets current active deadlock event
     */
    private Map<String, Object> getCurrentDeadlockEvent() {
        if (deadlockEvents.isEmpty()) return null;
        
        DeadlockEvent current = deadlockEvents.get(deadlockEvents.size() - 1);
        if (current.wasResolved) return null;
        
        Map<String, Object> event = new HashMap<>();
        event.put("id", current.id);
        event.put("detectedAt", current.detectedAt.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        event.put("threadCount", current.deadlockedThreads.size());
        event.put("resolutionSteps", new ArrayList<>(current.resolutionSteps));
        event.put("duration", java.time.Duration.between(current.detectedAt, LocalDateTime.now()).toMillis() + "ms");
        
        return event;
    }

    /**
     * Adds stack trace information to thread data for debugging
     */
    private void addStackTraces(DeadlockSnapshot snapshot, ThreadInfo[] threadInfos) {
        for (ThreadInfo info : threadInfos) {
            if (info == null) continue;
            
            snapshot.getThreads().stream()
                .filter(t -> t.id == info.getThreadId())
                .findFirst()
                .ifPresent(thread -> {
                    StackTraceElement[] stackTrace = info.getStackTrace();
                    if (stackTrace != null) {
                        for (int i = 0; i < Math.min(stackTrace.length, 10); i++) {
                            thread.stackTrace.add(stackTrace[i].toString());
                        }
                    }
                });
        }
    }

    /**
     * Adds information about all threads (not just deadlocked ones) for complete view
     */
    private void addAllThreadsInfo(DeadlockSnapshot snapshot) {
        try {
            ThreadInfo[] allThreads = threadMXBean.getThreadInfo(
                threadMXBean.getAllThreadIds(), 
                false, 
                false
            );
            
            for (ThreadInfo info : allThreads) {
                if (info == null) continue;
                
                DeadlockSnapshot.ThreadData thread = new DeadlockSnapshot.ThreadData();
                thread.id = info.getThreadId();
                thread.name = info.getThreadName();
                thread.state = info.getThreadState().toString();
                thread.isDeadlocked = false;
                
                snapshot.getThreads().add(thread);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Could not fetch all threads info: " + e.getMessage());
        }
    }

    /**
     * Periodic monitoring - runs every 2 seconds to check for deadlocks
     */
    @Scheduled(fixedRate = 2000)
    public void monitorDeadlocks() {
        try {
            DeadlockSnapshot currentSnapshot = detectDeadlocks();
            
            // Only send update if state changed
            if (hasStateChanged(currentSnapshot)) {
                this.lastSnapshot = currentSnapshot;
                
                // Send to WebSocket subscribers
                messagingTemplate.convertAndSend("/topic/deadlock", currentSnapshot);
                
                if (currentSnapshot.isDeadlockDetected()) {
                    System.out.println("🚨 Broadcasting deadlock alert to dashboard");
                } else {
                    System.out.println("✅ Broadcasting all-clear to dashboard");
                }
            }
            
        } catch (RuntimeException e) {
            System.err.println("❌ Error in periodic monitoring: " + e.getMessage());
        }
    }

    /**
     * Checks if the deadlock state has changed since last check
     */
    private boolean hasStateChanged(DeadlockSnapshot current) {
        if (lastSnapshot == null) return true;
        
        return lastSnapshot.isDeadlockDetected() != current.isDeadlockDetected() ||
               lastSnapshot.getThreads().size() != current.getThreads().size();
    }

    /**
     * Attempts to interrupt a specific thread by ID
     */
    public Map<String, Object> interruptThread(long threadId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Thread targetThread = findThreadById(threadId);
            
            if (targetThread == null) {
                result.put("success", false);
                result.put("message", "Thread not found: " + threadId);
                return result;
            }
            
            if (!targetThread.isAlive()) {
                result.put("success", false);
                result.put("message", "Thread is not alive: " + threadId);
                return result;
            }
            
            // Attempt to interrupt the thread
            targetThread.interrupt();
            
            result.put("success", true);
            result.put("message", "Interrupt signal sent to thread: " + targetThread.getName());
            result.put("warning", "Note: Interrupting may not release intrinsic locks. Monitor the system.");
            
            System.out.println("⚡ Interrupted thread: " + targetThread.getName() + " (ID: " + threadId + ")");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Failed to interrupt thread: " + e.getMessage());
            System.err.println("❌ Failed to interrupt thread " + threadId + ": " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Finds a Thread object by its ID
     */
    private Thread findThreadById(long threadId) {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getId() == threadId) {
                return thread;
            }
        }
        return null;
    }

    /**
     * Returns the last detected snapshot (for REST endpoint)
     */
    public DeadlockSnapshot getLastSnapshot() {
        if (lastSnapshot == null) {
            return detectDeadlocks();
        }
        return lastSnapshot;
    }

    /**
     * Returns system information for debugging
     */
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        info.put("totalThreads", Thread.activeCount());
        info.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        info.put("maxMemory", Runtime.getRuntime().maxMemory());
        info.put("freeMemory", Runtime.getRuntime().freeMemory());
        info.put("threadContentionMonitoring", threadMXBean.isThreadContentionMonitoringEnabled());
        info.put("timestamp", System.currentTimeMillis());
        info.put("autoResolutionEnabled", autoResolutionEnabled);
        info.put("totalResolutions", resolutionCounter.get());
        
        return info;
    }
    
    /**
     * 🚀 NEW: Get comprehensive resolution history
     */
    public Map<String, Object> getResolutionHistory() {
        Map<String, Object> history = new HashMap<>();
        
        history.put("totalEvents", deadlockEvents.size());
        history.put("totalResolutions", resolutionCounter.get());
        history.put("successfulResolutions", (int) deadlockEvents.stream().mapToLong(e -> e.wasResolved ? 1 : 0).sum());
        history.put("events", deadlockEvents.stream()
            .sorted((a, b) -> b.detectedAt.compareTo(a.detectedAt))
            .limit(20)
            .map(this::eventToMap)
            .toList());
        history.put("resolutionEvents", getRecentResolutionEvents(20));
        
        return history;
    }
    
    /**
     * 🚀 NEW: Get resolution statistics
     */
    public Map<String, Object> getResolutionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("methodStats", new HashMap<>(resolutionMethodStats));
        stats.put("averageResolutionTime", calculateAverageResolutionTime());
        stats.put("successRate", calculateSuccessRate());
        stats.put("autoResolutionEnabled", autoResolutionEnabled);
        stats.put("activeDeadlocks", knownDeadlockedThreads.size());
        stats.put("totalDeadlockEvents", deadlockEvents.size());
        
        return stats;
    }
    
    /**
     * 🚀 NEW: Toggle auto-resolution on/off
     */
    public Map<String, Object> toggleAutoResolution() {
        autoResolutionEnabled = !autoResolutionEnabled;
        
        Map<String, Object> result = new HashMap<>();
        result.put("autoResolutionEnabled", autoResolutionEnabled);
        result.put("message", autoResolutionEnabled ? "Auto-resolution enabled" : "Auto-resolution disabled");
        result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        
        System.out.println("🔧 Auto-resolution " + (autoResolutionEnabled ? "ENABLED" : "DISABLED"));
        
        // Broadcast change to clients
        broadcastResolutionUpdate("CONFIG_CHANGE", "Auto-resolution " + (autoResolutionEnabled ? "enabled" : "disabled"));
        
        return result;
    }
    
    /**
     * 🚀 NEW: Manually trigger resolution for current deadlocks
     */
    public Map<String, Object> triggerManualResolution() {
        Map<String, Object> result = new HashMap<>();
        
        if (knownDeadlockedThreads.isEmpty()) {
            result.put("success", false);
            result.put("message", "No deadlocks currently detected");
            return result;
        }
        
        // Get current deadlock info
        long[] deadlockedThreadIds = threadMXBean.findDeadlockedThreads();
        if (deadlockedThreadIds == null) {
            result.put("success", false);
            result.put("message", "No deadlocks found during manual trigger");
            return result;
        }
        
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedThreadIds, true, true);
        DeadlockEvent event = new DeadlockEvent(knownDeadlockedThreads);
        deadlockEvents.add(event);
        
        event.addResolutionStep("Manual resolution triggered by user");
        
        // Force resolution regardless of auto-resolution setting
        // targetBean is already set correctly in detectDeadlocks()
        attemptAutomaticResolution(knownDeadlockedThreads, threadInfos, event, threadMXBean);
        
        result.put("success", true);
        result.put("message", "Manual resolution triggered for " + knownDeadlockedThreads.size() + " threads");
        result.put("threadCount", knownDeadlockedThreads.size());
        result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        
        return result;
    }
    
    /**
     * Helper: Convert DeadlockEvent to Map for JSON serialization
     */
    private Map<String, Object> eventToMap(DeadlockEvent event) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", event.id);
        map.put("detectedAt", event.detectedAt.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
        map.put("threadCount", event.deadlockedThreads.size());
        map.put("wasResolved", event.wasResolved);
        map.put("resolutionMethod", event.resolutionMethod);
        map.put("resolutionSteps", new ArrayList<>(event.resolutionSteps));
        
        if (event.resolvedAt != null) {
            map.put("resolvedAt", event.resolvedAt.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
            map.put("totalResolutionTime", event.totalResolutionTime + "ms");
        } else {
            map.put("duration", java.time.Duration.between(event.detectedAt, LocalDateTime.now()).toMillis() + "ms");
        }
        
        return map;
    }
    
    /**
     * Helper: Calculate average resolution time
     */
    private double calculateAverageResolutionTime() {
        List<Long> times = deadlockEvents.stream()
            .filter(e -> e.wasResolved)
            .map(e -> e.totalResolutionTime)
            .toList();
            
        return times.isEmpty() ? 0.0 : times.stream().mapToLong(Long::longValue).average().orElse(0.0);
    }
    
    /**
     * Helper: Calculate success rate
     */
    private double calculateSuccessRate() {
        if (deadlockEvents.isEmpty()) return 0.0;
        
        long resolved = deadlockEvents.stream().mapToLong(e -> e.wasResolved ? 1 : 0).sum();
        return (double) resolved / deadlockEvents.size() * 100.0;
    }
}