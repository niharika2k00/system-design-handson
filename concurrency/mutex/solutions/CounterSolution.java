// Run: java CounterSolution.java [sync|lock|atomic|spin]

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class CounterSolution {
    static final int THREADS = 4;
    static final int INCREMENTS = 100_000;

    static int count = 0;
    static final Object monitor = new Object();
    static final AtomicInteger atomicCount = new AtomicInteger();
    static final ReentrantLock lock = new ReentrantLock();
    static final SpinLock spinLock = new SpinLock();

    // Busy-waits instead of sleeping: good for tiny critical sections, burns CPU otherwise
    static class SpinLock {
        private final AtomicBoolean locked = new AtomicBoolean(false);

        void lock() {
            while (!locked.compareAndSet(false, true))
                Thread.onSpinWait();
        }

        void unlock() {
            locked.set(false);
        }
    }

    // synchronized block | auto release, even on exception
    /* static void incrementSync() {
        synchronized (monitor) {
            count++;
        }
    } */

    // static synchronized method
    public static synchronized void incrementSync() {
        count++;
    }

    // lock-free: single CPU instruction (CAS), no thread ever sleeps
    static void incrementAtomic() {
        atomicCount.incrementAndGet();
    }

    // built-in mutex
    static void incrementSpin() {
        spinLock.lock();
        try {
            count++;
        } finally {
            spinLock.unlock();
        }
    }

    // explicit mutex: unlock MUST be in finally, else an exception leaks the lock forever
    static void incrementLock() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }

    static void increment(String mode) {
        switch (mode) {
            case "sync" -> incrementSync();
            case "atomic" -> incrementAtomic();
            case "spin" -> incrementSpin();
            default -> incrementLock();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        String mode = args.length > 0 ? args[0] : "lock";

        // Previous approach: pick the method once and store it as a Runnable (method references)
        /* Runnable increment = switch (mode) {
            case "sync" -> CounterSolution::incrementSync;
            case "atomic" -> CounterSolution::incrementAtomic;
            case "spin" -> CounterSolution::incrementSpin;
            default -> CounterSolution::incrementLock;
        }; */

        List<Thread> threads = new ArrayList<>();
        long start = System.nanoTime();

        for (int t = 0; t < THREADS; t++) {
            Thread thread = new Thread(() -> {
                for (int i = 0; i < INCREMENTS; i++)
                    increment(mode);
                // increment.run(); // if Runnable is used
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads)
            thread.join();

        long ms = (System.nanoTime() - start) / 1_000_000;
        int actual = mode.equals("atomic") ? atomicCount.get() : count;
        System.out.printf("mode=%s expected=%d actual=%d time=%dms%n", mode, THREADS * INCREMENTS, actual, ms);
    }
}
