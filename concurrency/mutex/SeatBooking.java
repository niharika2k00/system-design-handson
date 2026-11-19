// Drill 2: check-then-act race (in-memory version of your shows/booking tables)

// Problem: a theatre has 10 seats and 500 different users. Each user is one thread. User n tries to book seat (n % 10 + 1), so every seat is wanted by 50 users at the same moment.
// Each seat can have only ONE owner; the other 49 users of that seat must be told "seat taken".
// Goal: every seat has exactly 1 winner (no double booking), so total successful bookings = 10
// Senior twist: the lock must be per seat. A user booking seat 3 must NOT wait for users booking seat 7.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SeatBooking {
    static final int USERS = 500;
    static final int SEATS = 10;

    // array for tracking the seatNo(idx) -> userId (null = AVAILABLE)
    static final Integer[] seats = new Integer[SEATS + 1];

    // WHY IT FAILS ? check seat availability and book seat are 2 separate steps with no lock. Hence, during the gap other threads also see the seat free -> many users book seat X. Lost update.
    // here if (seats[seatNo] == null) condition satisfies for more than 1 userId for same seatNo (incorrect) as multiple threads updates at same time as no locking here
    // TODO: make this safe. Start with one global lock, then switch to one lock per seat
    static boolean book(int seatNo, int userId) throws InterruptedException {
        if (seats[seatNo] == null) { // check if seat is available
            Thread.sleep(2); // waits 2ms for other process (payment, validation)
            seats[seatNo] = userId;
            return true;
        }

        return false;
    }

    public static void main(String[] args) throws InterruptedException {
        AtomicInteger bookedUsersCount = new AtomicInteger(); // shared thread-safe counter for tracking user, so it must be thread-safe
        List<Thread> threads = new ArrayList<>();

        // seatNo -> users who got "true" for that seat (ideally should be only 1 per seat)
        Map<Integer, Queue<Integer>> winners = new HashMap<>();
        for (int seatNo = 1; seatNo <= SEATS; seatNo++)
            winners.put(seatNo, new ConcurrentLinkedQueue<>());

        for (int i = 1; i <= USERS; i++) {
            int userId = i;
            int seatNo = userId % SEATS + 1; // spreads users over seats 1..10

            Thread thread = new Thread(() -> {
                try {
                    if (book(seatNo, userId)) {
                        bookedUsersCount.incrementAndGet();
                        winners.get(seatNo).add(userId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads)
            thread.join();

        for (int seatNo = 1; seatNo <= SEATS; seatNo++)
            System.out.printf("seat %d: winners=%s final owner=%s%n", seatNo, winners.get(seatNo), seats[seatNo]);

        System.out.printf("\nsuccessful bookings=%d (expected %d, one per seat)%n", bookedUsersCount.get(), SEATS);
    }
}
