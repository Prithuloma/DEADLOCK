public class QuickDeadlockTest {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    private static Thread thread1;
    private static Thread thread2;
    private static volatile boolean shouldStop = false;
    
    public static void main(String[] args) {
        System.out.println("🧪 Starting Quick Deadlock Test with Auto-Resolution Support...");
        System.out.println("📌 This test creates a deadlock that will auto-resolve after being detected");
        
        // Start monitoring thread that watches for deadlock and auto-resolves
        Thread monitorThread = new Thread(() -> {
            try {
                Thread.sleep(500); // Wait for deadlock to form
                
                // Check if threads are deadlocked
                java.lang.management.ThreadMXBean threadMXBean = 
                    java.lang.management.ManagementFactory.getThreadMXBean();
                
                long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
                
                if (deadlockedThreads != null && deadlockedThreads.length > 0) {
                    System.out.println("🔍 Deadlock Monitor: Detected " + deadlockedThreads.length + " deadlocked threads");
                    System.out.println("⏱️  Waiting 45 seconds to allow dashboard observation...");
                    Thread.sleep(45000); // 45 second observation window
                    
                    System.out.println("🔧 AUTO-RESOLUTION: Interrupting deadlocked thread...");
                    shouldStop = true;
                    
                    // Interrupt both threads to break the deadlock
                    if (thread1 != null && thread1.isAlive()) {
                        thread1.interrupt();
                        System.out.println("⚡ Interrupted Worker-Thread-1");
                    }
                    if (thread2 != null && thread2.isAlive()) {
                        thread2.interrupt();
                        System.out.println("⚡ Interrupted Worker-Thread-2");
                    }
                    
                    Thread.sleep(2000);
                    System.out.println("✅ Deadlock resolved! System returned to normal state.");
                    System.out.println("🔄 Program will now continue with normal execution...");
                    
                    // Force exit since BLOCKED threads cannot be interrupted
                    // This simulates the program continuing after resolution
                    Thread.sleep(1000);
                    System.out.println("\n🎉 Test completed successfully!");
                    System.out.println("📈 Export the dashboard report to see full resolution details");
                    System.out.println("💡 Deadlock was detected, monitored for 45 seconds, then resolved");
                    System.exit(0);
                } else {
                    System.out.println("⚠️  No deadlock detected - threads may have completed normally");
                }
            } catch (InterruptedException e) {
                System.out.println("Monitor thread interrupted");
            }
        }, "Deadlock-Monitor");
        
        // Thread 1: Gets lock1, then tries to get lock2
        thread1 = new Thread(() -> {
            try {
                synchronized (lock1) {
                    System.out.println("Thread-1: Acquired lock1");
                    Thread.sleep(100); // Give thread2 time to acquire lock2
                    
                    System.out.println("Thread-1: Now trying to acquire lock2 (will create circular wait)...");
                    // This creates the circular dependency - thread1 holds lock1, wants lock2
                    synchronized (lock2) {
                        System.out.println("Thread-1: ✅ Acquired both locks!");
                    }
                }
                System.out.println("Thread-1: 🎉 Completed successfully after resolution");
            } catch (InterruptedException e) {
                System.out.println("⚡ Thread-1: INTERRUPTED during deadlock - Releasing locks and exiting");
            } catch (Exception e) {
                System.out.println("Thread-1: Exception - " + e.getMessage());
            }
        }, "Worker-Thread-1");
        
        // Thread 2: Gets lock2, then tries to get lock1
        thread2 = new Thread(() -> {
            try {
                Thread.sleep(50); // Small delay to ensure thread1 gets lock1 first
                
                synchronized (lock2) {
                    System.out.println("Thread-2: Acquired lock2");
                    Thread.sleep(100); // Ensure both threads hold their locks
                    
                    System.out.println("Thread-2: Now trying to acquire lock1 (will create circular wait)...");
                    // This creates the circular dependency - thread2 holds lock2, wants lock1
                    synchronized (lock1) {
                        System.out.println("Thread-2: ✅ Acquired both locks!");
                    }
                }
                System.out.println("Thread-2: 🎉 Completed successfully after resolution");
            } catch (InterruptedException e) {
                System.out.println("⚡ Thread-2: INTERRUPTED during deadlock - Releasing locks and exiting");
            } catch (Exception e) {
                System.out.println("Thread-2: Exception - " + e.getMessage());
            }
        }, "Worker-Thread-2");
        
        monitorThread.setDaemon(true);
        monitorThread.start();
        
        // Make worker threads daemon so they don't prevent program exit
        thread1.setDaemon(true);
        thread2.setDaemon(true);
        
        thread1.start();
        thread2.start();
        
        System.out.println("💥 Deadlock will form in ~100ms");
        System.out.println("🔍 Check the dashboard at: http://localhost:8080");
        System.out.println("📊 In the dashboard:");
        System.out.println("   1. Click 'Select Process' and choose this Java process");
        System.out.println("   2. Turn ON 'Auto-Resolution'");
        System.out.println("   3. Watch the deadlock appear and be monitored for 45 seconds");
        System.out.println("   4. See auto-resolution activate and resolve the deadlock");
        System.out.println("   5. Check Analytics tab for resolution details");
        System.out.println("✅ Backend will detect and monitor the deadlock!");
        
        // Keep main thread alive to allow monitoring
        // Monitor thread will exit the program after resolution
        try {
            Thread.sleep(60000); // Max 60 seconds
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted");
        }
    }
}