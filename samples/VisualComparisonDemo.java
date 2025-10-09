/**
 * Visual Comparison Demo
 * Creates different deadlock scenarios to show visualization differences
 */
public class VisualComparisonDemo {
    public static void main(String[] args) {
        System.out.println("🎨 VISUAL COMPARISON DEMO");
        System.out.println("=========================");
        System.out.println("This demo shows different deadlock patterns:");
        System.out.println("1. Simple 2-thread deadlock");
        System.out.println("2. Complex 3-thread circular deadlock");
        System.out.println("3. Mixed lock types deadlock");
        System.out.println();
        
        // Demo 1: Simple deadlock
        runSimpleDeadlockDemo();
    }
    
    private static void runSimpleDeadlockDemo() {
        Object resourceX = new Object();
        Object resourceY = new Object();
        
        System.out.println("🎯 Demo 1: Simple 2-Thread Deadlock");
        System.out.println("Expected visualization:");
        System.out.println("  🔵 SimpleThread-A ──🔗── 🟩 resourceX");
        System.out.println("  🔵 SimpleThread-B ──🔗── 🟩 resourceY");
        System.out.println("  Red circular arrows showing deadlock");
        
        Thread threadA = new Thread(() -> {
            synchronized (resourceX) {
                System.out.println("🔵 SimpleThread-A: Got resourceX, need resourceY");
                try { resourceX.wait(100); } catch (InterruptedException e) { return; }
                synchronized (resourceY) {
                    System.out.println("🔵 SimpleThread-A: Got both resources");
                }
            }
        }, "SimpleThread-A");
        
        Thread threadB = new Thread(() -> {
            synchronized (resourceY) {
                System.out.println("🟠 SimpleThread-B: Got resourceY, need resourceX");
                try { resourceY.wait(100); } catch (InterruptedException e) { return; }
                synchronized (resourceX) {
                    System.out.println("🟠 SimpleThread-B: Got both resources");
                }
            }
        }, "SimpleThread-B");
        
        threadA.start();
        threadB.start();
        
        System.out.println("💥 Simple deadlock created!");
        System.out.println("🔍 Check dashboard for 2-node circular pattern");
        
        // Keep deadlock active
        try {
            Thread.sleep(30000); // 30 seconds to observe
        } catch (InterruptedException e) {
            System.out.println("Demo interrupted");
        }
    }
}