public class QuickDeadlockTest {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    
    public static void main(String[] args) {
        System.out.println("🧪 Starting Quick Deadlock Test...");
        
        Thread thread1 = new Thread(() -> {
            synchronized (lock1) {
                System.out.println("Thread-1: Acquired lock1");
                try { lock1.wait(100); } catch (InterruptedException e) {}
                
                synchronized (lock2) {
                    System.out.println("Thread-1: Acquired lock2");
                }
            }
        }, "Worker-Thread-1");
        
        Thread thread2 = new Thread(() -> {
            synchronized (lock2) {
                System.out.println("Thread-2: Acquired lock2");
                try { lock2.wait(100); } catch (InterruptedException e) {}
                
                synchronized (lock1) {
                    System.out.println("Thread-2: Acquired lock1");
                }
            }
        }, "Worker-Thread-2");
        
        thread1.start();
        thread2.start();
        
        System.out.println("💥 Deadlock should occur now!");
        System.out.println("🔍 Check the dashboard at: http://localhost:8080");
        System.out.println("⏱️  Waiting indefinitely...");
        
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            System.out.println("Test interrupted");
        }
    }
}