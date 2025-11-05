package com.deadlock.service;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
        System.out.println("🚀 Auto-resolution enabled: " + autoResolutionEnabled);
    }

    // 🔧 [Rest of your methods remain unchanged until markCurrentDeadlocksResolved()]

    /**
     * ✅ UPDATED: Marks current deadlocks as resolved and pushes update to frontend
     */
    private void markCurrentDeadlocksResolved() {
        if (!deadlockEvents.isEmpty()) {
            DeadlockEvent lastEvent = deadlockEvents.get(deadlockEvents.size() - 1);
            if (!lastEvent.wasResolved) {
                lastEvent.markResolved("AUTOMATIC_RECOVERY");
                lastEvent.addResolutionStep("System recovered automatically ✅");

                // 🟢 Log backend event
                System.out.println("🟢 Deadlock resolved automatically and marked as recovered.");

                // 🔔 Send WebSocket resolution update
                broadcastResolutionUpdate("RESOLVED", "All deadlocks cleared automatically");

                // 📡 Force dashboard update to show Healthy
                DeadlockSnapshot freshSnapshot = getLastSnapshot();
                freshSnapshot.setDeadlockDetected(false);
                messagingTemplate.convertAndSend("/topic/deadlock", freshSnapshot);

                System.out.println("📡 Live dashboard updated to 'Healthy' state.");
            }
        }
    }

    // 🧠 [Keep all other methods (detectDeadlocks, broadcastResolutionUpdate, monitorDeadlocks, etc.) the same]
    // Just make sure this new markCurrentDeadlocksResolved() replaces the old one

    public String createSampleDeadlock() {
        Thread t1 = new Thread(() -> {
            synchronized ("A") {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                synchronized ("B") {
                    System.out.println("Thread 1 acquired locks A and B");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized ("B") {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                synchronized ("A") {
                    System.out.println("Thread 2 acquired locks B and A");
                }
            }
        });

        t1.start();
        t2.start();

        return "Deadlock simulation started (check logs)";
    }
}
