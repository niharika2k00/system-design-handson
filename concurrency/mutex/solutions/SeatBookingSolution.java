// Run: java SeatBookingSolution.java [naive|global|perseat|atomic]
// Interview flow: Approach 1 (correct for 1 thread) -> 2 (one lock) -> 3 (lock per seat) -> 4 (no lock, atomic map op)

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SeatBookingSolution {
    static final int USERS = 500; // 500 different users (one thread each); user n tries seat (n % 10 + 1), so 50 users fight for each seat
    static final int SEATS = 10;
    static final long START = System.currentTimeMillis(); // log timestamps are ms since program start

    static final Map<Integer, Integer> bookings = new ConcurrentHashMap<>(); // seatNo -> userId
    static final Object[] seatLocks = new Object[SEATS + 1];

    // it creates one lock object for each seat, before any thread runs.
    static {
        for (int i = 1; i <= SEATS; i++)
            seatLocks[i] = new Object();
    }

    static void validate(int seatNo) {
        if (seatNo < 1 || seatNo > SEATS)
            throw new IllegalArgumentException("invalid seat " + seatNo);
    }

    static boolean tryBook(int seatNo, int userId) throws InterruptedException {
        if (bookings.containsKey(seatNo)) // if the seat is already taken
            return false;

        Thread.sleep(2); // waits 2ms for other process (payment, validation)
        bookings.put(seatNo, userId);
        return true;
    }

    // Approach 1: naive. Correct with 1 thread.
    // Breaks with multiple threads: all of them pass the check before anyone writes, so many users "get" the same seat
    // A thread-safe map doesn't help: each call is safe, but check + act together are not
    static boolean bookNaive(int seatNo, int userId) throws InterruptedException {
        validate(seatNo);
        return tryBook(seatNo, userId);
    }

    // Approach 2: one global lock. Correct, but booking one seat blocks users booking any other seat
    static synchronized boolean bookGlobalLock(int seatNo, int userId) throws InterruptedException {
        validate(seatNo);
        return tryBook(seatNo, userId);
    }

    // Approach 3: one lock per seat. Only users fighting for the same seat wait; different seats run in parallel. No deadlock is possible
    static boolean bookPerSeatLock(int seatNo, int userId) throws InterruptedException {
        validate(seatNo);
        synchronized (seatLocks[seatNo]) {
            return tryBook(seatNo, userId);
        }
    }

    // Approach 4: no explicit lock. putIfAbsent does check + act as one atomic step
    // putIfAbsent returns null if there was no mapping for the key, means user got the seat
    static boolean bookAtomic(int seatNo, int userId) {
        validate(seatNo);
        return bookings.putIfAbsent(seatNo, userId) == null;
    }

    static boolean book(String mode, int seatNo, int userId) throws InterruptedException {
        switch (mode) {
        case "naive":
            return bookNaive(seatNo, userId);
        case "global":
            return bookGlobalLock(seatNo, userId);
        case "perseat":
            return bookPerSeatLock(seatNo, userId);
        default:
            return bookAtomic(seatNo, userId);
        }
    }

    static void logBooking(int userId, int seatNo, boolean isBooked) {
        System.out.printf("[%4dms] %s user %d -> seat %d: %s%n", System.currentTimeMillis() - START,
                Thread.currentThread().getName(), userId, seatNo, isBooked ? "BOOKED" : "SEAT_TAKEN");
    }

    public static void main(String[] args) throws InterruptedException {
        String mode = args.length > 0 ? args[0] : "atomic";
        AtomicInteger bookedUsersCount = new AtomicInteger();
        List<Thread> threads = new ArrayList<>();

        Map<Integer, Queue<Integer>> winners = new HashMap<>();
        for (int seatNo = 1; seatNo <= SEATS; seatNo++)
            winners.put(seatNo, new ConcurrentLinkedQueue<>());

        for (int u = 1; u <= USERS; u++) {
            int userId = u;
            int seatNo = userId % SEATS + 1; // spreads users over seats 1..10

            Thread thread = new Thread(() -> {
                try {
                    boolean isBooked = book(mode, seatNo, userId);
                    // logBooking(userId, seatNo, isBooked);

                    if (isBooked) { // track bookings
                        bookedUsersCount.incrementAndGet();
                        winners.get(seatNo).add(userId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (IllegalArgumentException e) {
                    System.out.println("user " + userId + ": " + e.getMessage());
                }
            });

            threads.add(thread);
            thread.start();
        }

        for (Thread thread : threads)
            thread.join();

        for (int seatNo = 1; seatNo <= SEATS; seatNo++)
            System.out.printf("seat %d: winners=%s final owner=%s%n", seatNo, winners.get(seatNo), bookings.get(seatNo));

        System.out.printf("\nmode=%s successful bookings=%d (expected %d, one per seat)%n", mode, bookedUsersCount.get(), SEATS);
    }
}

// Both (InterruptedException, IllegalArgumentException) exceptions are handled in catch separately. Instead can use parent Exception class to handle everything
