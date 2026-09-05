import "dotenv/config";
import express from "express";
import sequelize from "./config/db.js";
import userRoutes from "./routes/userRoutes.js";

const app = express();
app.use(express.json());

app.get("/", (req, res) => {
  res.send("🎉 Server Running !");
});

app.use("/api/users", userRoutes);

const start = async () => {
  try {
    await sequelize.authenticate();
    console.log("✅ DB connected");
    await sequelize.sync(); // auto creates tables from models
    app.listen(3000, () => {
      console.log("🎉 nodejs-with-mysql-sequelize server running !!");
    });
  } catch (err) {
    console.error("❌ DB connection failed:", err.message);
  }
};

start();
