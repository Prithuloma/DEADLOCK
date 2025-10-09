/**
 * Simple deadlock example using synchronized blocks
 * 
 * This creates a classic deadlock scenario where:
 * - Thread 1 acquires Lock A, then tries to acquire Lock B
 * - Thread 2 acquires Lock B, then tries to acquire Lock A
 * 
 * To run: java SimpleDeadlock
 */
public class SimpleDeadlock {
    private static final Object lockA = new Object();
    private static final Object lockB = new Object();

    public static void main(String[] args) {
        System.out.println("🚀 Starting Simple Deadlock Example...");
        System.out.println("💡 This will create a deadlock within 1-2 seconds");
        System.out.println("🔍 Start your deadlock detector to see it in action!");

        Thread thread1 = new Thread(SimpleDeadlock::task1, "DeadlockThread-1");
        Thread thread2 = new Thread(SimpleDeadlock::task2, "DeadlockThread-2");

        thread1.start();
        thread2.start();

        // Wait a bit, then print status
        try {
            Thread.sleep(3000);
            System.out.println("⚠️ Deadlock should have occurred by now!");
            System.out.println("📊 Check your deadlock detector dashboard");
            
            // Keep the main thread alive
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            System.out.println("🛑 Main thread interrupted");
        }
    }

    private static void task1() {
        try {
            System.out.println("🔵 Thread-1: Acquiring Lock A...");
            synchronized (lockA) {
                System.out.println("🔵 Thread-1: Got Lock A, sleeping...");
                lockA.wait(500); // Hold the lock for a while
                
                System.out.println("🔵 Thread-1: Now trying to acquire Lock B...");
                synchronized (lockB) {
                    System.out.println("🔵 Thread-1: Got both locks! (This should not print)");
                }
            }
        } catch (InterruptedException e) {
            System.out.println("🔵 Thread-1: Interrupted!");
        }
    }

    private static void task2() {
        try {
            System.out.println("🟠 Thread-2: Acquiring Lock B...");
            synchronized (lockB) {
                System.out.println("🟠 Thread-2: Got Lock B, sleeping...");
                lockB.wait(500); // Hold the lock for a while
                
                System.out.println("🟠 Thread-2: Now trying to acquire Lock A...");
                synchronized (lockA) {
                    System.out.println("🟠 Thread-2: Got both locks! (This should not print)");
                }
            }
        } catch (InterruptedException e) {
            System.out.println("🟠 Thread-2: Interrupted!");
        }
    }
}