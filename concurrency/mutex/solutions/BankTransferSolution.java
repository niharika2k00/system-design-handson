// Run: java BankTransferSolution.java [naive|global|ordered]
// Interview flow: Sample 1 (correct for 1 thread) -> Sample 2 (thread-safe, simple) -> Sample 3 (thread-safe, scalable)

public class BankTransferSolution {
    static final int TRANSFERS_PER_THREAD = 100_000; // number of times the money transfer takes place

    static class Account {
        final int id;
        long balance; // money as long (paise/cents), never double: 0.1 + 0.2 != 0.3

        Account(int id, long balance) {
            this.id = id;
            this.balance = balance;
        }
    }

    static void validate(Account from, Account to, long amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("amount must be positive");
        if (from == to)
            throw new IllegalArgumentException("cannot transfer to the same account");
    }

    // check + withdraw + deposit; returns false on insufficient funds, so nothing is half-done
    static boolean move(Account from, Account to, long amount) {
        if (from.balance < amount)
            return false; // insufficient fund

        from.balance -= amount;
        to.balance += amount;
        return true;
    }

    // Approach 1: naive. Correct with 1 thread (single-threaded)
    // Breaks with multiple threads due to lost update,
    static boolean transferNaive(Account from, Account to, long amount) {
        validate(from, to, amount);
        return move(from, to, amount);
    }

    // Approach 2: one global lock (simplest thread-safe version)
    // Correct and it can't deadlock bcz only one lock. Only 1 transfer runs at a time in the whole bank.
    static synchronized boolean transferGlobalLock(Account from, Account to, long amount) {
        validate(from, to, amount);
        return move(from, to, amount);
    }

    // Approach 3: one lock per account, always lock the lower id first.
    // Why the ordering ? without it, A→B locks A then B, and B→A locks B then A. Each holds one lock and waits for the other, hence deadlock arrise.
    // Transfers on different accounts run in parallel; A->B and B->A both lock A first, so no deadlock.
    static boolean transferOrdered(Account from, Account to, long amount) {
        validate(from, to, amount);
        Account first = from.id < to.id ? from : to;
        Account second = first == from ? to : from;

        synchronized (first) {
            synchronized (second) {
                return move(from, to, amount);
            }
        }
    }

    static boolean transfer(String mode, Account from, Account to, long amount) {
        switch (mode) {
            case "naive":
                return transferNaive(from, to, amount);
            case "global":
                return transferGlobalLock(from, to, amount);
            default:
                return transferOrdered(from, to, amount);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        String mode = args.length > 0 ? args[0] : "ordered";
        Account a = new Account(1, 1000);
        Account b = new Account(2, 1000);

        // opposite directions at the same time = the case that deadlocked in BankTransfer.java
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < TRANSFERS_PER_THREAD; i++)
                transfer(mode, a, b, 10);
        }, "T1 A->B");
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < TRANSFERS_PER_THREAD; i++)
                transfer(mode, b, a, 10);
        }, "T2 B->A");

        long start = System.nanoTime();
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        long ms = (System.nanoTime() - start) / 1_000_000;

        System.out.printf("mode=%s A=%d B=%d total=%d (expected 2000) time=%dms%n", mode, a.balance, b.balance,
                a.balance + b.balance, ms);
    }
}

// Each thread runs 100,000 times (200,000 in total), transferring 10 rupees per run.
