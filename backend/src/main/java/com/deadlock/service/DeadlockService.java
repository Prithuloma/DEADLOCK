package com.deadlock.service;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
 * Complete DeadlockService - simplified, robust, and exposes controller-required methods.
 */
@Service
public class DeadlockService{

    private final ThreadMXBean threadMXBean;
    private final SimpMessagingTemplate messagingTemplate;

    // Last snapshot published to UI
    private DeadlockSnapshot lastSnapshot;

    // Resolution history and stats
    private final Map<Long, ResolutionEvent> resolutionHistory = new ConcurrentHashMap<>();
    private final AtomicInteger resolutionCounter = new AtomicInteger(0);
    private final List<DeadlockEvent> deadlockEvents = Collections.synchronizedList(new ArrayList<>());

    // Simple state
    private final Set<Long> knownDeadlockedThreads = ConcurrentHashMap.newKeySet();
    private boolean autoResolutionEnabled = true;

    // optional external JMX monitor (can be null in some builds)
    private final JMXProcessMonitor jmxMonitor;

    public DeadlockService(SimpMessagingTemplate messagingTemplate, JMXProcessMonitor jmxMonitor) {
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.messagingTemplate = messagingTemplate;
        this.jmxMonitor = jmxMonitor;
        this.lastSnapshot = new DeadlockSnapshot();

        // Debug
        System.out.println("DeadlockService initialized. Auto-resolution = " + autoResolutionEnabled);
    }

    // ---- Small inner models ----
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

    public static class DeadlockEvent {
        public final String id;
        public final LocalDateTime detectedAt;
        public final Set<Long> deadlockedThreads;
        public final List<String> resolutionSteps = new ArrayList<>();
        public LocalDateTime resolvedAt;
        public String resolutionMethod;
        public boolean wasResolved = false;
        public long totalResolutionTime = 0;

        public DeadlockEvent(Set<Long> threads) {
            this.id = "DL-" + System.currentTimeMillis();
            this.detectedAt = LocalDateTime.now();
            this.deadlockedThreads = new HashSet<>(threads);
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

    // ---- Public API used by controllers/UI ----

    /**
     * Create a small sample deadlock on the current JVM (used for demo).
     */
    public String createSampleDeadlock() {
        Thread t1 = new Thread(() -> {
            synchronized ("SAMPLE_LOCK_A") {
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                synchronized ("SAMPLE_LOCK_B") {
                    System.out.println("Sample Thread-1 acquired both locks");
                }
            }
        }, "Sample-Deadlock-1");

        Thread t2 = new Thread(() -> {
            synchronized ("SAMPLE_LOCK_B") {
                try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                synchronized ("SAMPLE_LOCK_A") {
                    System.out.println("Sample Thread-2 acquired both locks");
                }
            }
        }, "Sample-Deadlock-2");

        t1.start();
        t2.start();

        return "Deadlock simulation started (check backend logs)";
    }

    /**
     * Return last snapshot (used by REST controller).
     */
    public DeadlockSnapshot getLastSnapshot() {
        if (lastSnapshot == null) {
            return detectDeadlocks();
        }
        return lastSnapshot;
    }

    /**
     * Expose resolution history summary.
     */
    public Map<String, Object> getResolutionHistory() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("totalEvents", deadlockEvents.size());
        ret.put("totalResolutions", resolutionCounter.get());
        ret.put("resolutionEvents", new ArrayList<>(resolutionHistory.values()));
        return ret;
    }

    /**
     * Toggle auto-resolution on/off.
     */
    public Map<String, Object> toggleAutoResolution() {
        autoResolutionEnabled = !autoResolutionEnabled;
        Map<String, Object> result = new HashMap<>();
        result.put("autoResolutionEnabled", autoResolutionEnabled);
        result.put("message", autoResolutionEnabled ? "enabled" : "disabled");
        broadcastResolutionUpdate("CONFIG_CHANGE", "Auto-resolution " + (autoResolutionEnabled ? "enabled" : "disabled"));
        return result;
    }

    /**
     * Interrupt a thread by id (best-effort).
     */
    public Map<String, Object> interruptThread(long threadId) {
        Map<String, Object> res = new HashMap<>();
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getId() == threadId) {
                try {
                    t.interrupt();
                    res.put("success", true);
                    res.put("message", "Interrupt sent to thread: " + t.getName());
                    return res;
                } catch (Exception e) {
                    res.put("success", false);
                    res.put("message", "Failed to interrupt: " + e.getMessage());
                    return res;
                }
            }
        }
        res.put("success", false);
        res.put("message", "Thread not found: " + threadId);
        return res;
    }

    /**
     * Basic system info endpoint.
     */
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("totalThreads", Thread.activeCount());
        info.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        info.put("timestamp", System.currentTimeMillis());
        info.put("autoResolutionEnabled", autoResolutionEnabled);
        info.put("totalResolutions", resolutionCounter.get());
        return info;
    }

    /**
     * Public broadcast method — controllers sometimes call this. Made public intentionally.
     */
    public void broadcastResolutionUpdate(String status, String message) {
        try {
            Map<String, Object> update = new HashMap<>();
            update.put("type", "RESOLUTION_UPDATE");
            update.put("status", status);
            update.put("message", message);
            update.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS")));
            messagingTemplate.convertAndSend("/topic/resolution", update);
        } catch (Exception e) {
            System.err.println("Failed to broadcast resolution update: " + e.getMessage());
        }
    }

    // ---- Detection + periodic publishing ----

    /**
     * Detect deadlocks in current JVM (or via JMX if implemented).
     * Returns a DeadlockSnapshot object for the UI.
     */
    public DeadlockSnapshot detectDeadlocks() {
        try {
            long[] deadlocked = threadMXBean.findDeadlockedThreads(); // returns IDs

            DeadlockSnapshot snapshot = new DeadlockSnapshot();
            snapshot.setTimestamp(System.currentTimeMillis());

            if (deadlocked == null || deadlocked.length == 0) {
                snapshot.setDeadlockDetected(false);
                // publish thread list (light)
                ThreadInfo[] all = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), true, true);

                for (ThreadInfo ti : all) {
                    if (ti == null) continue;
                    DeadlockSnapshot.ThreadData td = new DeadlockSnapshot.ThreadData();
                    td.id = ti.getThreadId();
                    td.name = ti.getThreadName();
                    td.state = ti.getThreadState().toString();
                    td.isDeadlocked = false;
                    snapshot.getThreads().add(td);
                }

                // If we previously knew of deadlocks, mark resolved and broadcast
                if (!knownDeadlockedThreads.isEmpty()) {
                    markCurrentDeadlocksResolved();
                    knownDeadlockedThreads.clear();
                }
            } else {
                // Deadlock detected
                snapshot.setDeadlockDetected(true);
                ThreadInfo[] infos = threadMXBean.getThreadInfo(deadlocked, true, true);
                for (ThreadInfo ti : infos) {
                    if (ti == null) continue;
                    DeadlockSnapshot.ThreadData td = new DeadlockSnapshot.ThreadData();
                    td.id = ti.getThreadId();
                    td.name = ti.getThreadName();
                    td.state = ti.getThreadState().toString();
                    td.isDeadlocked = true;
                    Arrays.stream(ti.getStackTrace()).limit(10).forEach(st -> td.stackTrace.add(st.toString()));
                    snapshot.getThreads().add(td);
                }

                // bookkeeping and event creation
                Set<Long> cur = new HashSet<>();
                for (long id : deadlocked) cur.add(id);
                Set<Long> newFound = new HashSet<>(cur);
                newFound.removeAll(knownDeadlockedThreads);
                if (!newFound.isEmpty()) {
                    DeadlockEvent ev = new DeadlockEvent(cur);
                    ev.addResolutionStep("Detected deadlock with threads: " + cur);
                    deadlockEvents.add(ev);

                    // Auto-resolution: attempt simple interrupt of one candidate (best-effort)
                    if (autoResolutionEnabled) {
                        new Thread(() -> {
                            try {
                                Thread.sleep(6000); // short wait so dashboard shows detection
                                attemptSimpleResolution(cur, infos, ev);
                            } catch (InterruptedException ignored) {}
                        }, "DeadlockResolver").start();
                    }
                }
                knownDeadlockedThreads.addAll(cur);
            }

            // add minimal resolution history to snapshot for UI convenience
            Map<String, Object> extra = new HashMap<>();
            extra.put("totalResolutions", resolutionCounter.get());
            extra.put("activeDeadlocks", knownDeadlockedThreads.size());
            snapshot.setAdditionalData(extra);

            // save last snapshot and return
            lastSnapshot = snapshot;
            return snapshot;

        } catch (Exception e) {
            System.err.println("Error in detectDeadlocks: " + e.getMessage());
            DeadlockSnapshot s = new DeadlockSnapshot();
            s.setDeadlockDetected(false);
            s.setAdditionalData(Map.of("error", e.getMessage()));
            lastSnapshot = s;
            return s;
        }
    }

    /**
     * Attempt a simple resolution: interrupt one candidate thread (best-effort, only works for same JVM).
     */
    private void attemptSimpleResolution(Set<Long> deadlockedThreads, ThreadInfo[] infos, DeadlockEvent event) {
        event.addResolutionStep("Attempting simple interruption of a candidate thread");
        try {
            // choose the first deadlocked thread's id (best-effort)
            long targetId = -1;
            for (ThreadInfo ti : infos) {
                if (ti != null) { targetId = ti.getThreadId(); break; }
            }
            if (targetId == -1) {
                event.addResolutionStep("No candidate found for interruption");
                recordResolution("NONE", deadlockedThreads, "FAILED", "No candidate", 0);
                return;
            }

            // find Thread object and interrupt
            Thread target = findThreadById(targetId);
            if (target != null) {
                target.interrupt();
                event.addResolutionStep("Interrupted thread id=" + targetId);
                recordResolution("INTERRUPT", deadlockedThreads, "SUCCESS", "Interrupted thread " + target.getName(), 200);
                // mark resolved and broadcast
                event.markResolved("INTERRUPT");
                knownDeadlockedThreads.removeAll(deadlockedThreads);
                broadcastResolutionUpdate("RESOLVED", "Deadlock cleared by interrupting a thread");
                // publish fresh snapshot
                DeadlockSnapshot fresh = detectDeadlocks();
                messagingTemplate.convertAndSend("/topic/deadlock", fresh);
            } else {
                event.addResolutionStep("Target thread object not found to interrupt");
                recordResolution("INTERRUPT", deadlockedThreads, "FAILED", "Thread object missing", 0);
            }

        } catch (Exception e) {
            event.addResolutionStep("Resolution attempt failed: " + e.getMessage());
            recordResolution("INTERRUPT", deadlockedThreads, "FAILED", e.getMessage(), 0);
        }
    }

    /**
     * Record a resolution event entry
     */
    private void recordResolution(String method, Set<Long> threads, String status, String details, long timeMs) {
        ResolutionEvent re = new ResolutionEvent(method, threads, status, details, timeMs);
        long key = resolutionCounter.incrementAndGet();
        resolutionHistory.put(key, re);
        System.out.println("Resolution recorded: " + method + " -> " + status);
    }

    /**
     * Helper to find live Thread by id (best-effort).
     */
    private Thread findThreadById(long threadId) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getId() == threadId) return t;
        }
        return null;
    }

    /**
     * Mark current deadlocks as resolved and notify the UI.
     */
    private void markCurrentDeadlocksResolved() {
        if (!deadlockEvents.isEmpty()) {
            DeadlockEvent last = deadlockEvents.get(deadlockEvents.size() - 1);
            if (!last.wasResolved) {
                last.markResolved("AUTOMATIC_RECOVERY");
                last.addResolutionStep("System recovered automatically");
                recordResolution("AUTOMATIC_RECOVERY", last.deadlockedThreads, "SUCCESS", "Auto resolved", last.totalResolutionTime);
                broadcastResolutionUpdate("RESOLVED", "All known deadlocks have been resolved");
                // push an updated snapshot so UI turns healthy
                DeadlockSnapshot fresh = detectDeadlocks();
                messagingTemplate.convertAndSend("/topic/deadlock", fresh);
            }
        }
    }

    // ---- scheduled monitor that publishes only when changed ----

    @Scheduled(fixedRate = 2000)
    public void monitorDeadlocks() {
        try {
            DeadlockSnapshot snapshot = detectDeadlocks();
            // only publish when state changed or at first run
            if (lastSnapshot == null || lastSnapshot.isDeadlockDetected() != snapshot.isDeadlockDetected()
                    || lastSnapshot.getThreads().size() != snapshot.getThreads().size()) {
                lastSnapshot = snapshot;
                messagingTemplate.convertAndSend("/topic/deadlock", snapshot);
                if (snapshot.isDeadlockDetected()) {
                    System.out.println("Published deadlock state -> DETECTED");
                } else {
                    System.out.println("Published deadlock state -> CLEAR");
                }
            }
        } catch (Exception e) {
            System.err.println("Error in monitorDeadlocks: " + e.getMessage());
        }
    }

    // ---- simple helpers for controllers to set which process to monitor (if you use JMX) ----
    public void setMonitoredProcess(String pid) { this.selectedProcessPid = pid; }
    public String getMonitoredProcess() { return this.selectedProcessPid; }
    private String selectedProcessPid = null;

    // ---- manual trigger for demo purposes ----
    public Map<String, Object> triggerManualResolution() {
        Map<String, Object> res = new HashMap<>();
        if (knownDeadlockedThreads.isEmpty()) {
            res.put("success", false);
            res.put("message", "No deadlocks known");
            return res;
        }
        long[] ids = threadMXBean.findDeadlockedThreads();
        if (ids == null) {
            res.put("success", false);
            res.put("message", "No deadlocked threads at query time");
            return res;
        }
        ThreadInfo[] infos = threadMXBean.getThreadInfo(ids, true, true);
        DeadlockEvent ev = new DeadlockEvent(new HashSet<>(knownDeadlockedThreads));
        deadlockEvents.add(ev);
        attemptSimpleResolution(new HashSet<>(knownDeadlockedThreads), infos, ev);
        res.put("success", true);
        res.put("message", "Manual resolution attempted");
        return res;
    }
}
