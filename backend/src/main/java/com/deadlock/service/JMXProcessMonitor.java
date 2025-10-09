package com.deadlock.service;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.springframework.stereotype.Service;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

/**
 * Service to monitor external Java processes via JMX
 * Allows connecting to and monitoring any running Java application
 */
@Service
public class JMXProcessMonitor {
    
    private static final String CONNECTOR_ADDRESS = 
        "com.sun.management.jmxremote.localConnectorAddress";
    
    private final Map<String, JMXConnector> activeConnections = new ConcurrentHashMap<>();
    private Map<String, ThreadMXBean> threadBeans = new ConcurrentHashMap<>();
    
    /**
     * Scan for all running Java processes on the system
     */
    public List<JavaProcessInfo> scanJavaProcesses() {
        List<JavaProcessInfo> processes = new ArrayList<>();
        
        try {
            List<VirtualMachineDescriptor> vms = VirtualMachine.list();
            
            for (VirtualMachineDescriptor vmd : vms) {
                try {
                    String pid = vmd.id();
                    String displayName = vmd.displayName();
                    
                    // Skip our own Spring Boot process
                    if (displayName.contains("DeadlockApplication") || 
                        displayName.contains("spring-boot") ||
                        displayName.isEmpty()) {
                        continue;
                    }
                    
                    processes.add(new JavaProcessInfo(pid, displayName));
                    System.out.println("📋 Found Java process: " + displayName + " (PID: " + pid + ")");
                    
                } catch (Exception e) {
                    System.err.println("⚠️ Error scanning process: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error listing Java processes: " + e.getMessage());
        }
        
        return processes;
    }
    
    /**
     * Connect to a specific Java process via JMX and get its ThreadMXBean
     */
    public ThreadMXBean connectToProcess(String pid) throws Exception {
        // Check if already connected
        if (threadBeans.containsKey(pid)) {
            return threadBeans.get(pid);
        }
        
        VirtualMachine vm = null;
        try {
            System.out.println("🔌 Connecting to process PID: " + pid);
            
            // Attach to the target JVM
            vm = VirtualMachine.attach(pid);
            
            // Get or create JMX connector address
            String connectorAddr = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
            
            if (connectorAddr == null) {
                // ✅ FIXED: Use startLocalManagementAgent() instead of loading jar file
                System.out.println("🔧 Starting local management agent for PID: " + pid);
                
                try {
                    // This method starts the management agent without needing the jar file
                    connectorAddr = vm.startLocalManagementAgent();
                    System.out.println("✅ Management agent started successfully");
                } catch (Exception e) {
                    System.err.println("⚠️ Could not start management agent: " + e.getMessage());
                    throw new RuntimeException(
                        "Failed to start JMX agent for PID: " + pid + 
                        ". Please run target process with: java -Dcom.sun.management.jmxremote YourClass"
                    );
                }
                
                // Wait a bit for agent to fully start
                Thread.sleep(500);
                
                // Verify we got the connector address
                if (connectorAddr == null || connectorAddr.isEmpty()) {
                    connectorAddr = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
                    if (connectorAddr == null) {
                        throw new RuntimeException("Failed to obtain JMX connector address for PID: " + pid);
                    }
                }
            }
            
            // Connect via JMX
            JMXServiceURL url = new JMXServiceURL(connectorAddr);
            JMXConnector connector = JMXConnectorFactory.connect(url);
            activeConnections.put(pid, connector);
            
            // Get MBean server connection
            MBeanServerConnection mbsc = connector.getMBeanServerConnection();
            
            // Get ThreadMXBean from remote process
            ThreadMXBean threadBean = ManagementFactory.newPlatformMXBeanProxy(
                mbsc, 
                ManagementFactory.THREAD_MXBEAN_NAME, 
                ThreadMXBean.class
            );
            
            threadBeans.put(pid, threadBean);
            
            System.out.println("✅ Connected to process PID: " + pid);
            
            return threadBean;
            
        } finally {
            if (vm != null) {
                try {
                    vm.detach();
                } catch (Exception e) {
                    // Ignore detach errors
                }
            }
        }
    }
    
    /**
     * Check a specific process for deadlocks
     */
    public DeadlockInfo checkProcessForDeadlocks(String pid) {
        try {
            ThreadMXBean threadBean = connectToProcess(pid);
            
            // Find deadlocked threads
            long[] deadlockedThreadIds = threadBean.findDeadlockedThreads();
            
            if (deadlockedThreadIds != null && deadlockedThreadIds.length > 0) {
                System.out.println("🔴 DEADLOCK DETECTED in PID " + pid + "! Threads: " + Arrays.toString(deadlockedThreadIds));
                
                // Get detailed thread information
                ThreadInfo[] threadInfos = threadBean.getThreadInfo(
                    deadlockedThreadIds, 
                    true,  // locked monitors
                    true   // locked synchronizers
                );
                
                return new DeadlockInfo(pid, true, threadInfos, threadBean);
            } else {
                // No deadlock, but get all thread info for dashboard
                long[] allThreadIds = threadBean.getAllThreadIds();
                ThreadInfo[] allThreadInfos = threadBean.getThreadInfo(allThreadIds, false, false);
                
                return new DeadlockInfo(pid, false, allThreadInfos, threadBean);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error monitoring process " + pid + ": " + e.getMessage());
            // Process might have terminated
            disconnect(pid);
            return null;
        }
    }
    
    /**
     * Disconnect from a specific process
     */
    public void disconnect(String pid) {
        threadBeans.remove(pid);
        
        JMXConnector connector = activeConnections.remove(pid);
        if (connector != null) {
            try {
                connector.close();
                System.out.println("🔌 Disconnected from PID: " + pid);
            } catch (Exception e) {
                // Ignore close errors
            }
        }
    }
    
    /**
     * Disconnect from all processes
     */
    public void disconnectAll() {
        for (String pid : new ArrayList<>(activeConnections.keySet())) {
            disconnect(pid);
        }
    }
    
    /**
     * Information about a Java process
     */
    public static class JavaProcessInfo {
        public String pid;
        public String displayName;
        
        public JavaProcessInfo(String pid, String displayName) {
            this.pid = pid;
            this.displayName = displayName;
        }
        
        public String getPid() { return pid; }
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Deadlock information from a process
     */
    public static class DeadlockInfo {
        public String pid;
        public boolean hasDeadlock;
        public ThreadInfo[] threadInfos;
        public ThreadMXBean threadBean;
        
        public DeadlockInfo(String pid, boolean hasDeadlock, ThreadInfo[] threadInfos, ThreadMXBean threadBean) {
            this.pid = pid;
            this.hasDeadlock = hasDeadlock;
            this.threadInfos = threadInfos;
            this.threadBean = threadBean;
        }
    }
}
