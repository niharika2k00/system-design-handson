# Redis - Real World Use Cases

Redis is an in-memory data store used for speed-critical operations.
It sits between your application and database to make things fast.

## Where Redis is Used in Real Life

### 1. Caching

Avoid repeated expensive DB queries by storing results temporarily.

**Examples:** Product pages, news articles, user profiles

```javascript
const cached = await client.get(`product:${id}`);
if (cached) return JSON.parse(cached);

const product = await db.query(`SELECT * FROM products WHERE id = ${id}`);
await client.set(`product:${id}`, JSON.stringify(product), { EX: 3600 });
return product;
```

---

### 2. Session Storage

Store logged-in user sessions instead of hitting DB on every request.

**Examples:** Login sessions, shopping cart, user preferences

```javascript
// on login
await client.hSet(`session:${token}`, { userId: 1, role: "admin" });
await client.expire(`session:${token}`, 86400); // 24 hrs

// on every request
const session = await client.hGetAll(`session:${token}`);
if (!session.userId) throw new Error("Unauthorized");
```

---

### 3. Rate Limiting

Track how many times a user hits an API and block if too many.

**Examples:** API limits, OTP send limits, login throttling

```javascript
const count = await client.incr(`ratelimit:${userId}`);
if (count === 1) await client.expire(`ratelimit:${userId}`, 60);
if (count > 100) throw new Error("Too many requests");
```

---

### 4. Job Queues

Push slow tasks to a queue and process them in the background.

**Examples:** Sending emails, resizing images, generating PDFs

```javascript
// producer — push job
await client.rPush(
  "queue:emails",
  JSON.stringify({ to: "a@b.com", subject: "Welcome" })
);

// consumer (worker) — process job
const job = await client.lPop("queue:emails");
await sendEmail(JSON.parse(job));
```

---

### 5. Real-time Pub/Sub

Broadcast messages instantly to multiple subscribers.

**Examples:** Live chat, score updates, stock prices

```javascript
// publisher
await client.publish(
  "scores",
  JSON.stringify({ match: "IND vs AUS", score: "272/4" })
);

// subscriber
await subscriber.subscribe("scores", (message) => {
  console.log(JSON.parse(message));
});
```

---

### 6. Leaderboards

Rank users by score instantly using Sorted Sets.

**Examples:** Gaming leaderboards, StackOverflow reputation, sales dashboards

```javascript
// add/update score
await client.zAdd("leaderboard", { score: 5000, value: "Alice" });
await client.zAdd("leaderboard", { score: 8000, value: "Bob" });

// get top 10
const top10 = await client.zRangeWithScores("leaderboard", 0, 9, { REV: true });
```

---

### 7. OTP & Temporary Tokens

Store short-lived codes that auto-expire.

**Examples:** Login OTP, password reset links, email verification

```javascript
// store OTP — expires in 5 mins
await client.set(`otp:${phone}`, "482910", { EX: 300 });

// verify OTP
const stored = await client.get(`otp:${phone}`);
if (stored !== enteredOtp) throw new Error("Invalid OTP");
await client.del(`otp:${phone}`); // delete after use
```

---

### 8. Distributed Locking

Prevent two servers from processing the same thing simultaneously.

**Examples:** Payment processing, inventory deduction, cron jobs

```javascript
// acquire lock — NX means "only set if not exists"
const lock = await client.set(`lock:order:${id}`, "1", { NX: true, EX: 10 });
if (!lock) throw new Error("Already being processed");

// do the work
await processPayment(id);

// release lock
await client.del(`lock:order:${id}`);
```

---

## Setup (Docker)

```bash
docker compose up -d
```

- Redis runs on `localhost:6380`
- RedisInsight UI at `http://localhost:5540`
