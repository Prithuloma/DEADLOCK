import java.util.Scanner;

/**
 * Controllable Deadlock Demo
 * Allows manual control of deadlock creation and recovery
 */
public class ControllableDeadlock {
    private static final Object lockA = new Object();
    private static final Object lockB = new Object();
    private static Thread thread1, thread2;

    public static void main(String[] args) {
        System.out.println("🎯 CONTROLLABLE DEADLOCK DEMO");
        System.out.println("=============================");
        System.out.println("This demo allows you to:");
        System.out.println("1. Start with NO deadlock");
        System.out.println("2. CREATE a deadlock on command");
        System.out.println("3. RECOVER from deadlock");
        System.out.println("4. Show the visualization differences");
        System.out.println();

        try (Scanner scanner = new Scanner(System.in)) {
            // Phase 1: Normal state (no deadlock)
            demonstrateNormalState();
            
            System.out.println("\n🟢 PHASE 1: NORMAL STATE (No Deadlock)");
            System.out.println("✅ Check your dashboard - should show 'No Deadlock'");
            System.out.println("📊 Graph should be empty or show normal thread activity");
            System.out.print("Press ENTER to CREATE deadlock...");
            scanner.nextLine();

            // Phase 2: Create deadlock
            createDeadlock();
            
            System.out.println("\n🔴 PHASE 2: DEADLOCK CREATED!");
            System.out.println("⚠️  Check your dashboard - should show 'Deadlock Detected'");
            System.out.println("📊 Graph should show:");
            System.out.println("   🔵 Thread-1 (blue circle)");
            System.out.println("   🔵 Thread-2 (blue circle)");
            System.out.println("   🟩 lockA (green rectangle)");
            System.out.println("   🟩 lockB (green rectangle)");
            System.out.println("   🔗 Red edges showing circular dependency");
            System.out.print("Press ENTER to RECOVER from deadlock...");
            scanner.nextLine();

            // Phase 3: Recover from deadlock
            recoverFromDeadlock();
            
            System.out.println("\n🟢 PHASE 3: DEADLOCK RECOVERED!");
            System.out.println("✅ Check your dashboard - should return to 'No Deadlock'");
            System.out.println("📊 Graph should clear or show normal state");
            System.out.println("🎉 Demo complete!");
        }
    }

    private static void demonstrateNormalState() {
        System.out.println("🟢 Starting in normal state...");
        System.out.println("📈 Threads running normally, no circular dependencies");
        
        // Normal thread activity - no deadlock
        Thread normalThread = new Thread(() -> {
            int iterations = 3;
            while (iterations > 0 && !Thread.currentThread().isInterrupted()) {
                synchronized (lockA) {
                    System.out.println("Normal-Thread: Using lockA normally");
                    try { lockA.wait(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                }
                iterations--;
                // Brief pause between iterations using wait
                if (iterations > 0) {
                    synchronized (lockA) {
                        try { lockA.wait(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                    }
                }
            }
        }, "Normal-Thread");
        
        normalThread.start();
        try { normalThread.join(); } catch (InterruptedException e) {}
    }

    private static void createDeadlock() {
        System.out.println("🔴 Creating deadlock scenario...");
        
        thread1 = new Thread(() -> {
            synchronized (lockA) {
                System.out.println("🔵 Thread-1: Acquired lockA");
                try { lockA.wait(100); } catch (InterruptedException e) { return; }
                
                System.out.println("🔵 Thread-1: Now waiting for lockB...");
                synchronized (lockB) {
                    System.out.println("🔵 Thread-1: Acquired lockB");
                }
            }
        }, "DeadlockThread-1");

        thread2 = new Thread(() -> {
            synchronized (lockB) {
                System.out.println("🟠 Thread-2: Acquired lockB");
                try { lockB.wait(100); } catch (InterruptedException e) { return; }
                
                System.out.println("🟠 Thread-2: Now waiting for lockA...");
                synchronized (lockA) {
                    System.out.println("🟠 Thread-2: Acquired lockA");
                }
            }
        }, "DeadlockThread-2");

        thread1.start();
        thread2.start();
        
        // Give threads time to deadlock
        try { Thread.sleep(500); } catch (InterruptedException e) {}
        System.out.println("💥 DEADLOCK ACTIVE! Threads are stuck in circular wait.");
    }

    private static void recoverFromDeadlock() {
        System.out.println("🚑 Initiating deadlock recovery...");
        System.out.println("⚡ Interrupting deadlocked threads...");
        
        if (thread1 != null && thread1.isAlive()) {
            thread1.interrupt();
            System.out.println("⚡ Thread-1 interrupted");
        }
        
        if (thread2 != null && thread2.isAlive()) {
            thread2.interrupt();
            System.out.println("⚡ Thread-2 interrupted");
        }

        // Wait for threads to terminate
        try {
            if (thread1 != null) thread1.join(1000);
            if (thread2 != null) thread2.join(1000);
        } catch (InterruptedException e) {}
        
        System.out.println("✅ Recovery complete! System restored to normal state.");
        System.out.println("📊 Dashboard should now show 'No Deadlock'");
    }
}