/**
 * Multi-thread circular deadlock example
 * 
 * This creates a more complex deadlock scenario with 3 threads:
 * - Thread A: Resource1 → Resource2
 * - Thread B: Resource2 → Resource3  
 * - Thread C: Resource3 → Resource1
 * 
 * This forms a circular dependency that results in deadlock.
 * 
 * To run: java MultiThreadDeadlock
 */
public class MultiThreadDeadlock {
    private static final Object resource1 = new Object();
    private static final Object resource2 = new Object();
    private static final Object resource3 = new Object();

    public static void main(String[] args) {
        System.out.println("🚀 Starting Multi-Thread Circular Deadlock Example...");
        System.out.println("🔄 This creates a 3-way circular deadlock");
        System.out.println("📈 More complex scenario for testing your detector!");

        Thread threadA = new Thread(MultiThreadDeadlock::taskA, "CircularThread-A");
        Thread threadB = new Thread(MultiThreadDeadlock::taskB, "CircularThread-B");
        Thread threadC = new Thread(MultiThreadDeadlock::taskC, "CircularThread-C");
        

        // Start all threads with slight delays to ensure proper ordering
        threadA.start();
        
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        threadB.start();
        
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        threadC.start();

        try {
            Thread.sleep(5000);
            System.out.println("⚠️ Circular deadlock should be active!");
            System.out.println("🔍 Check your detector - it should show 3 threads in deadlock");
            
            // Keep main thread alive
            threadA.join();
            threadB.join();
            threadC.join();
            
        } catch (InterruptedException e) {
            System.out.println("🛑 Main thread interrupted");
        }
    }

    // Thread A: Resource1 → Resource2
    private static void taskA() {
        try {
            System.out.println("🔵 Thread-A: Acquiring Resource1...");
            synchronized (resource1) {
                System.out.println("🔵 Thread-A: Got Resource1, processing...");
                resource1.wait(1000);
                
                System.out.println("🔵 Thread-A: Now need Resource2...");
                synchronized (resource2) {
                    System.out.println("🔵 Thread-A: Got both resources! (Should not print)");
                    resource2.wait(1000);
                }
            }
        } catch (InterruptedException e) {
            System.out.println("🔵 Thread-A: Interrupted!");
        }
    }

    // Thread B: Resource2 → Resource3
    private static void taskB() {
        try {
            System.out.println("🟠 Thread-B: Acquiring Resource2...");
            synchronized (resource2) {
                System.out.println("🟠 Thread-B: Got Resource2, processing...");
                resource2.wait(1000);
                
                System.out.println("🟠 Thread-B: Now need Resource3...");
                synchronized (resource3) {
                    System.out.println("🟠 Thread-B: Got both resources! (Should not print)");
                    resource3.wait(1000);
                }
            }
        } catch (InterruptedException e) {
            System.out.println("🟠 Thread-B: Interrupted!");
        }
    }

    // Thread C: Resource3 → Resource1
    private static void taskC() {
        try {
            System.out.println("🟢 Thread-C: Acquiring Resource3...");
            synchronized (resource3) {
                System.out.println("🟢 Thread-C: Got Resource3, processing...");
                resource3.wait(1000);
                
                System.out.println("🟢 Thread-C: Now need Resource1...");
                synchronized (resource1) {
                    System.out.println("🟢 Thread-C: Got both resources! (Should not print)");
                    resource1.wait(1000);
                }
            }
        } catch (InterruptedException e) {
            System.out.println("🟢 Thread-C: Interrupted!");
        }
    }
}