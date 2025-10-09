import java.util.concurrent.locks.ReentrantLock;

/**
 * ReentrantLock deadlock example
 * 
 * This demonstrates deadlock using java.util.concurrent.locks.ReentrantLock
 * instead of synchronized blocks.
 * 
 * To run: java ReentrantLockDeadlock
 */
public class ReentrantLockDeadlock {
    private static final ReentrantLock lock1 = new ReentrantLock();
    private static final ReentrantLock lock2 = new ReentrantLock();

    public static void main(String[] args) {
        System.out.println("🚀 Starting ReentrantLock Deadlock Example...");
        System.out.println("🔒 Using ReentrantLock instead of synchronized blocks");

        Thread workerA = new Thread(ReentrantLockDeadlock::workerA, "ReentrantWorker-A");
        Thread workerB = new Thread(ReentrantLockDeadlock::workerB, "ReentrantWorker-B");

        workerA.start();
        workerB.start();

        try {
            Thread.sleep(3000);
            System.out.println("⚠️ ReentrantLock deadlock should be active!");
            
            // Try to check lock states
            System.out.println("🔍 Lock1 is locked: " + lock1.isLocked());
            System.out.println("🔍 Lock2 is locked: " + lock2.isLocked());
            
        } catch (InterruptedException e) {
            System.out.println("🛑 Main thread interrupted");
        }
    }

    private static void workerA() {
        try {
            System.out.println("🔵 Worker-A: Attempting to acquire Lock1...");
            lock1.lock();
            System.out.println("🔵 Worker-A: Acquired Lock1, working...");
            
            Thread.sleep(1000); // Simulate work
            
            System.out.println("🔵 Worker-A: Now trying to acquire Lock2...");
            lock2.lock(); // This will cause deadlock
            
            System.out.println("🔵 Worker-A: Got both locks! (Should not reach here)");
            
        } catch (InterruptedException e) {
            System.out.println("🔵 Worker-A: Interrupted!");
        } finally {
            // This finally block might never execute due to deadlock
            if (lock2.isHeldByCurrentThread()) {
                lock2.unlock();
                System.out.println("🔵 Worker-A: Released Lock2");
            }
            if (lock1.isHeldByCurrentThread()) {
                lock1.unlock();
                System.out.println("🔵 Worker-A: Released Lock1");
            }
        }
    }

    private static void workerB() {
        try {
            System.out.println("🟠 Worker-B: Attempting to acquire Lock2...");
            lock2.lock();
            System.out.println("🟠 Worker-B: Acquired Lock2, working...");
            
            Thread.sleep(1000); // Simulate work
            
            System.out.println("🟠 Worker-B: Now trying to acquire Lock1...");
            lock1.lock(); // This will cause deadlock
            
            System.out.println("🟠 Worker-B: Got both locks! (Should not reach here)");
            
        } catch (InterruptedException e) {
            System.out.println("🟠 Worker-B: Interrupted!");
        } finally {
            // This finally block might never execute due to deadlock
            if (lock1.isHeldByCurrentThread()) {
                lock1.unlock();
                System.out.println("🟠 Worker-B: Released Lock1");
            }
            if (lock2.isHeldByCurrentThread()) {
                lock2.unlock();
                System.out.println("🟠 Worker-B: Released Lock2");
            }
        }
    }
}