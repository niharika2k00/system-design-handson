// Drill 1: race condition on a shared counter
// Goal: make `count` always equal THREADS * INCREMENTS

import java.util.ArrayList;
import java.util.List;

public class Counter {
    static final int THREADS = 4;
    static final int INCREMENTS = 100_000;
    static int count = 0;

    // WHY IT FAILS: all threads share `count` with no lock. count++ = read, add, write.
    // Two threads can read the same value (e.g. 5) and both write 6. Both threads incremented, so count should be 7, but it's 6. B overwrote A's result.
    // This is a lost update, caused by a race condition: the result depends on which thread happens to run when.
    // Sample broken output (changes every run): expected=400000 actual=113242 lost=286758
    // TODO: protect this critical section (try synchronized, ReentrantLock, AtomicInteger)
    static void increment() {
        count++; // read -> add -> write: 3 steps, not atomic
    }

    // Calling run() instead of start() would run the code on the main thread, with no concurrency. This comes up in interviews.
    public static void main(String[] args) throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        long start = System.nanoTime();

        for (int t = 1; t <= THREADS; t++) {
            Thread thread = new Thread(() -> {
                for (int i = 0; i < INCREMENTS; i++)
                    increment();
            });
            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads)
            thread.join();

        long ms = (System.nanoTime() - start) / 1_000_000; // converts to ms
        int expected = THREADS * INCREMENTS;
        System.out.printf("expected=%d actual=%d lost=%d time=%dms%n", expected, count, expected - count, ms);
    }
}


// Short answer: the loop body never runs the increment() loop. It only creates and starts the thread.

// How they actually run at the same time:

// With several CPU cores, the OS puts different threads on different cores, so they run truly in parallel.
// With fewer cores than threads, the OS gives each thread short time slices, switching between them every few milliseconds or less. That is concurrency, and the lost updates still happen.
// Either way, the OS decides who runs when, which is why actual is different on every run.


// AtomicBoolean is a boolean wrapped in an object whose operations are atomic. Each operation happens as one indivisible step, and the new value is immediately visible to all threads.