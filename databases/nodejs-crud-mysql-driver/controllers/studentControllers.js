import bcrypt from "bcryptjs";
import pool from "../config/db.js";

const db = pool;

// fetch all
// @route: GET /api/students
const getAllStudents = async (req, res) => {
  try {
    let query = `SELECT id, name, email FROM students`;
    const [rows, fields] = await db.execute(query);
    console.log(rows);
    res.status(200).json(rows);
  } catch (err) {
    console.log(err);
    res.status(500).json({ error: err.message });
  }
};

// fetch student
// @route: GET /api/students/:id
const getStudent = async (req, res) => {
  try {
    const [rows, fields] = await db.query(
      `SELECT id, name, email FROM students WHERE id = ?`,
      [req.params.id]
    );
    res.status(200).json(rows[0] || null);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

// @route: POST /api/students
const createStudent = async (req, res) => {
  try {
    const { name, email, password } = req.body;
    const hash = await bcrypt.hash(password, 10);
    const [result] = await db.execute(
      `INSERT INTO students (name, email, password) VALUES (?, ?, ?)`,
      [name, email, hash]
    );
    res.status(201).json({ id: result.insertId, name, email });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

// @route: PUT /api/students/:id
const updateStudent = async (req, res) => {
  try {
    const { name, email, password } = req.body;
    if (password) {
      const hash = await bcrypt.hash(password, 10);
      await db.execute(
        `UPDATE students SET name = ?, email = ?, password = ? WHERE id = ?`,
        [name, email, hash, req.params.id]
      );
    } else {
      await db.execute(
        `UPDATE students SET name = ?, email = ? WHERE id = ?`,
        [name, email, req.params.id]
      );
    }
    res.status(200).json({ id: req.params.id, name, email });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

// @route: DELETE /api/students/:id
const deleteStudent = async (req, res) => {
  try {
    await db.execute(`DELETE FROM students WHERE id = ?`, [req.params.id]);
    res.status(200).json({ message: "deleted", id: req.params.id });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

export { getAllStudents, getStudent, createStudent, updateStudent, deleteStudent };
