import express from "express";
import { createClient } from "redis";
import db from "./fakeDatabase.js";

const app = express();
app.use(express.json());

const redisClient = await createClient({ url: "redis://localhost:6380" })
  .on("error", (err) => console.log("Redis Client Error", err))
  .connect();

// fetch
app.get("/product/:id", async (req, res) => {
  const { id } = req.params;
  const cacheKey = `product:${id}`;
  const startTime = Date.now();

  // check for cache hit
  const cachedData = await redisClient.get(cacheKey);

  if (cachedData) {
    console.log("✅ Cache HIT for product ", id);
    return res.json({
      source: "redis cache",
      retrievalTime: `${Date.now() - startTime}ms`,
      data: cachedData,
    });
  }

  // cache miss - fetch from database
  console.log(`❌ Cache MISS for product ${id}`);
  const product = await db.getProduct(id);

  if (!product) return res.status(404).send({ msg: "Product not found" });

  res.json({
    source: "database",
    retrievalTime: `${Date.now() - startTime}ms`,
    data: product,
  });

  // save data to redis cache with a TTL(seconds) for future use
  await redisClient.set(cacheKey, JSON.stringify(product), { EX: 30 });
});

// update data
app.put("/product/:id", async (req, res) => {
  const { id } = req.params;

  // update db here
  // clear/delete old data from cache
  await redisClient.del(`product:${id}`);
  console.log(`🗑️  Cache cleared for product ${id}`);

  res.json({ message: `Product ${id} updated` });
});

app.listen(3000, () => console.log("🚀 Server running on http://localhost:3000"));

/*
curl http://localhost:3000/product/1
curl -X PUT http://localhost:3000/product/1
*/
