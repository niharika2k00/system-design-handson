import bcrypt from "bcryptjs";
import jwt from "jsonwebtoken";
import pool from "../config/db.js";

const db = pool;

const signToken = (user) =>
  jwt.sign({ id: user.id, email: user.email }, process.env.JWT_SECRET, {
    expiresIn: "1d",
  });

// @route: POST /api/auth/register
const register = async (req, res) => {
  try {
    const { name, email, password } = req.body;
    if (!name || !email || !password) {
      return res.status(400).json({ error: "name, email, password required" });
    }
    const hash = await bcrypt.hash(password, 10);
    const [result] = await db.execute(
      `INSERT INTO students (name, email, password) VALUES (?, ?, ?)`,
      [name, email, hash]
    );
    const user = { id: result.insertId, name, email };
    const token = signToken(user);
    res.status(201).json({ user, token });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

// @route: POST /api/auth/login
const login = async (req, res) => {
  try {
    const { email, password } = req.body;
    const [rows] = await db.execute(
      `SELECT id, name, email, password FROM students WHERE email = ?`,
      [email]
    );
    const user = rows[0];
    if (!user) return res.status(401).json({ error: "invalid credentials" });

    const ok = await bcrypt.compare(password, user.password);
    if (!ok) return res.status(401).json({ error: "invalid credentials" });

    const token = signToken(user);
    res.status(200).json({
      user: { id: user.id, name: user.name, email: user.email },
      token,
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

export { register, login };
