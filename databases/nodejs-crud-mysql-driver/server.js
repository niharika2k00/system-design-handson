import dotenv from "dotenv/config";
// dotenv.config();
import express from "express";
import studentRoutes from "./routes/studentRoutes.js";
// import instructorRoutes from "./routes/instructorRoutes.js";
import courseRoutes from "./routes/courseRoutes.js";
import authRoutes from "./routes/authRoutes.js";

const app = express();
app.use(express.json());

// app.get(endpoint, callback)
app.get("/", (req, res) => {
  // res.json({ message: "Student Course API is running" });
  res.send("🎉 Server Running !");
});

// app.use(path, callback)
app.use("/api/auth", authRoutes);
app.use("/api/students", studentRoutes);
// app.use("/api/instructors", instructorRoutes);
app.use("/api/courses", courseRoutes);

// listen() function ---->  used to bind and listen the connections on the specified host and port
app.listen(3000, function () {
  console.log("🎉 nodejs-with-mysql-driver server running !!");
  // console.log(`App listening at http://localhost:${port} for ${process.env.NODE_ENV}`);
  // console.log(process.env.PORT);
});
