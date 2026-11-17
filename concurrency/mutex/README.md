# Mutex Hands-on (Java 17)

Each drill is a broken single-file program. Run it, see the bug, fix the `TODO`, re-run.
Don't open `solutions/` until you've tried.

```bash
cd concurrency/mutex
java Counter.java
```

## Drill 1 — Race condition (`Counter.java`)

4 threads × 100k `count++` → final count is way below 400000.

- [ ] Fix with `synchronized`
- [ ] Fix with `ReentrantLock` (unlock in `finally`)
- [ ] Fix with `AtomicInteger`
- [ ] Compare timings: `java solutions/CounterSolution.java sync|lock|atomic|spin`

**Why it fails**
- `count` is shared by all 4 threads, and nothing stops two threads from updating it at once.
- `count++` is 3 CPU steps: read, add, write. A thread can be paused between them.
- A and B both read 5, both write 6. Two increments happened but only one counted.
- Both threads incremented, so count should be 7, but it's 6. B overwrote A's result. This is a **lost update**, caused by a **race condition**: the result depends on which thread happens to run when.
- Sample broken output (changes every run): `expected=400000 actual=113242 lost=286758`

**Fix**: make read-add-write **atomic**, so no thread can step in midway.
- `synchronized` / `ReentrantLock`: one thread at a time inside `count++`, the rest wait.
- `AtomicInteger`: no lock, the CPU does read-add-write as one instruction (CAS).
- `volatile` does **not** fix it: it gives visibility (latest value), not atomicity.

## Drill 2 — Check-then-act (`SeatBooking.java`)

500 users book seat 7 at the same instant → many "succeed".

- [ ] Fix with one global lock
- [ ] Replace with one lock per seat (lock striping) — why is this better?

**Why it fails**
- `book()` first **checks** `seats[7] == null`, then later **acts** `seats[7] = userId`.
- Between check and act (the `sleep`, like a payment call), other threads also check and still see the seat free.
- So many threads pass the check and all "book" seat 7; the last writer wins and the others think they succeeded (**double booking**).
- Same race as Drill 1, but across 2 separate statements: a **check-then-act** race.

**Fix**: put the check and the act inside the **same lock**, so they run as one unit.
- One global lock works, but booking seat 3 then waits for seat 7, so all seats are serialised.
- One lock per seat (**lock striping**): only threads for the same seat wait, different seats run in parallel.
- DB equivalent: `SELECT ... FOR UPDATE` on that one row.

## Drill 3 — Deadlock (`BankTransfer.java`)

A→B and B→A each hold one lock and wait for the other → program hangs; main prints `DEADLOCK` after 3s.

- [ ] Fix with lock ordering (always lock lower `id` first)
- [ ] Fix with `tryLock(timeout)` + back-off + jitter
- [ ] Explain why ordering is faster than tryLock here

**Why it fails**
- A transfer needs 2 locks: `from` and `to`.
- T1 (A->B) locks A, then wants B. T2 (B->A) locks B, then wants A.
- Each holds what the other needs and waits forever: **deadlock** (circular wait).
- `synchronized` has no timeout, so nobody gives up and the threads hang.

**Fix**: break the circular wait.
- **Lock ordering**: always lock the account with the lower `id` first. Both threads then try A first, so one waits for the other instead of crossing. Simple and fast.
- **`tryLock(timeout)`**: if the second lock isn't free in time, release the first, wait a random moment (jitter) and retry. Nobody waits forever. Slower because of retries; jitter stops both threads retrying in lockstep (**livelock**).

## Bonus — Build your own mutex

- [ ] Implement `SpinLock` with `AtomicBoolean.compareAndSet` (see `CounterSolution.java`)
- [ ] Make it reentrant (track owner thread + hold count)

## Interview questions to answer out loud

1. Why is `count++` not atomic? Would `volatile int count` fix it?
2. `synchronized` vs `ReentrantLock` — when do you need the latter? (tryLock, timeout, fairness, interruptible, multiple `Condition`s)
3. Why must `unlock()` be in `finally`?
4. What does "reentrant" mean? What happens if a non-reentrant lock is re-acquired by the same thread?
5. 4 Coffman conditions for deadlock. Which one does lock ordering break? Which one does tryLock break?
6. Deadlock vs livelock vs starvation. Why add jitter in the tryLock retry?
7. Mutex vs semaphore(1) — difference? (ownership: only the holder can unlock a mutex)
8. When is a spinlock better than a blocking mutex?
9. Global lock vs lock striping vs `ConcurrentHashMap` — trade-offs?
10. Seat booking runs on 3 app servers behind a load balancer. Does your Java lock still work? (No — use DB `FOR UPDATE`, optimistic `version`, or a distributed lock like Redis `SET NX PX`)
11. Hot counter under heavy contention: `AtomicInteger` vs `LongAdder` — why is `LongAdder` faster?
12. Visibility vs atomicity: what does `volatile` guarantee, and what doesn't it?
13. In a wait/`await()` loop, why `while (condition)` and not `if`? (spurious wakeups, competing consumers)

## Coding rounds seen in senior interviews (next drills)

- Bounded blocking queue with `ReentrantLock` + 2 `Condition`s (`notFull`, `notEmpty`), plus shutdown
- Thread-safe LRU cache — then "remove the single global lock"
- Thread-safe token-bucket rate limiter
- Print "first, second, third" in order from 3 threads (semaphores / `CountDownLatch`)
- Thread-safe lazy singleton (double-checked locking + `volatile`)
- Read-heavy cache with `ReentrantReadWriteLock`




Rule: use synchronized by default.
Switch to ReentrantLock only when you need a timeout, cancellation, fairness or several conditions. That’s why the bank transfer tryLock fix uses it.


“No fairness” means Java doesn’t promise to hand the lock to the thread that has waited longest. When the lock is released, any thread can take it, including a thread that only just arrived, or the very thread that just released it. This is called barging (jumping the queue).

