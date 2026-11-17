// Run: java SeatBookingSolution.java
// Lock striping: one mutex per seat, so different seats never wait on each other

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SeatBookingSolution {
    static final int USERS = 500;
    static final int SEATS = 10;

    static final Integer[] seats = new Integer[SEATS + 1];
    static final Object[] seatLocks = new Object[SEATS + 1];

    static {
        for (int i = 1; i <= SEATS; i++) seatLocks[i] = new Object();
    }

    // check + act inside the same lock = atomic (same idea as SELECT ... FOR UPDATE on one row)
    static boolean book(int seatNo, int userId) throws InterruptedException {
        synchronized (seatLocks[seatNo]) {
            if (seats[seatNo] != null) return false;
            Thread.sleep(1);
            seats[seatNo] = userId;
            return true;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(50);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        for (int u = 1; u <= USERS; u++) {
            int userId = u;
            pool.submit(() -> {
                startGate.await();
                if (book(7, userId)) successes.incrementAndGet();
                return null;
            });
        }

        startGate.countDown();
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        System.out.printf("successful bookings=%d (expected 1), seat 7 owner=%s%n", successes.get(), seats[7]);
    }
}
