
<!-- connection url: redis://redis:6379 -->

# Strings
SET name "Niharika"
GET name
STRLEN name                      # length of string
KEYS *
DEL name
EXISTS name                      # returns 0 (gone) or 1 (exists)
APPEND name " Dutta"

TTL name                         # -1 means no expiry
SETEX profession 30 "engineer"   # expires in 30 secs
SET profession "chef" EX 30


# Increment a number (if value is numeric)
SET counter 10
INCR counter           # +1
INCRBY counter 5       # +5
DECR counter           # -1
DECRBY counter 3       # -3


# Hash (like a mini JSON object)
HSET user:1 name "Alice" age 30 city "NYC"
HEGET user:1 name
HMGET user:1 name city          # get multiple fields
HGETALL user:1
HDEL user:1 city
HEXISTS user:1 name
HLEN user:1                     # count fields
HKEYS user:1                    # only keys
HVALS user:1
HINCRBY user:1 age 1


# List
# RPUSH -> Queue (FIFO) -> push from R (back)
# LPUSH -> Stack (LIFO) -> push from L (front)
LPUSH tasks "buy chocolate" "floor clean"
RPUSH tasks "sleep"
LLEN tasks
LRANGE tasks 0 -1             # all items
LRANGE tasks 0 2              # first 3 items
LPOP tasks                    # remove from left (front)
RPOP tasks                    # remove from right (back)
LINDEX tasks 0                # get by index
LINDEX tasks -1               # get by index last item
LSET tasks 0 "laundry"        # update by index


# Set — unordered, unique values only
SADD tags "redis" "database" "postgres" "cache"
SMEMBERS tags                 # view all items | order not guaranteed
SCARD tags                    # count items | cardinality
SREM tags "cache"             # remove item
SRANDMEMBER tags              # random one
SRANDMEMBER tags 2            # random two
SPOP tags                     # random + remove it


SADD team:backend "Alice" "Bob" "Charlie"
SADD team:frontend "Bob" "Charlie" "Dave"

# Common members (intersection)
SINTER team:backend team:frontend     # Bob, Charlie

# All members combined (union)
SUNION team:backend team:frontend     # Alice, Bob, Charlie, Dave

# In backend but NOT in frontend (difference)
SDIFF team:backend team:frontend      # Alice


# Sorted Set (ZSet) — like a Set but each member has a score for ordering
ZADD leaderboard 100 "Alice"
ZADD leaderboard 250 "Bob"
ZADD leaderboard 75  "Charlie"
ZRANGE leaderboard 0 -1 WITHSCORES    # lowest to highest
ZREVRANGE leaderboard 0 -1            # highest to lowest

# Sorted Sets (leaderboard)
ZADD scores 100 "Alice" 250 "Bob" 80 "Charlie"
ZRANGE scores 0 -1 WITHSCORES REV   # top scores first
