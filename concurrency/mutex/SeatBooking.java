// Drill 2: check-then-act race (in-memory version of your shows/booking tables)
// Run: java SeatBooking.java
// Goal: 500 users hit the same seat at once -> exactly 1 booking succeeds
// Senior twist: booking seat 7 must NOT block someone booking seat 3

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SeatBooking {
    static final int USERS = 500;
    static final int SEATS = 10;

    // seatNo -> userId (null = AVAILABLE)
    static final Integer[] seats = new Integer[SEATS + 1];

    // WHY IT FAILS: check (seat free?) and act (take seat) are separate steps with no lock.
    // During the gap, other threads also see the seat free -> many users book seat 7.
    // TODO: make this safe. Start with one global lock, then switch to one lock per seat
    static boolean book(int seatNo, int userId) throws InterruptedException {
        if (seats[seatNo] == null) {      // check
            Thread.sleep(1);              // simulate work (payment call, validation...)
            seats[seatNo] = userId;       // act
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(50);
        CountDownLatch startGate = new CountDownLatch(1); // release all users at the same instant
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
