// Drill 3: two mutexes -> deadlock
// Goal: A->B and B->A run concurrently without hanging, total money stays constant

public class BankTransfer {

    static class Account {
        final int id;
        int balance;

        Account(int id, int balance) {
            this.id = id;
            this.balance = balance;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Account a = new Account(1, 1000);
        Account b = new Account(2, 1000);

        Thread t1 = new Thread(() -> repeat(a, b), "T1 (A->B)");
        Thread t2 = new Thread(() -> repeat(b, a), "T2 (B->A)");
        t1.setDaemon(true); // daemon = JVM can exit even if these threads are stuck
        t2.setDaemon(true);
        t1.start();
        t2.start();

        t1.join(3000);
        t2.join(3000);

        if (t1.isAlive() || t2.isAlive()) {
            System.out.println("DEADLOCK: threads still stuck after 3s");
            return;
        }
        System.out.printf("A=%d B=%d total=%d (expected 2000)%n", a.balance, b.balance, a.balance + b.balance);
    }

    static void repeat(Account from, Account to) {
        try {
            for (int i = 0; i < 50; i++)
                transfer(from, to, 10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // WHY IT FAILS: T1 locks A then waits for B; T2 locks B then waits for A.
    // Each holds what the other needs, and synchronized never times out -> both wait forever.
    // TODO: fix the deadlock (hint 1: lock ordering, hint 2: ReentrantLock.tryLock with timeout)
    static void transfer(Account from, Account to, int amount) throws InterruptedException {
        synchronized (from) {
            System.out.println(Thread.currentThread().getName() + " locked " + from.id + ", sleeping");
            Thread.sleep(10); // always pauses the thread that is running that line
            System.out.println(Thread.currentThread().getName() + " wants " + to.id);

            synchronized (to) {
                if (from.balance < amount)
                    return;
                from.balance -= amount;
                to.balance += amount;
            }
        }
    }
}
