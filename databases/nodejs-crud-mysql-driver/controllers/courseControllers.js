import pool from "../config/db.js";

const db = pool;

// fetch all courses with instructor name via JOIN
// @route: GET /api/courses
const getAllCourses = async (req, res) => {
  try {
    let query = `SELECT c.*, i.name AS instructor_name
      FROM courses c LEFT JOIN instructors i
      ON c.instructor_id = i.id`;
    const [rows, fields] = await db.execute(query);
    console.log(rows);
    res.status(200).json(rows);
  } catch (err) {
    console.log(err);
    res.status(500).json({ error: err.message });
  }
};

// fetch course
// @route: GET /api/courses/:id
const getCourse = async (req, res) => {
  try {
    let query = `SELECT c.*, i.name AS instructor_name
    FROM courses c LEFT JOIN instructors i
    ON c.instructor_id = i.id
    WHERE c.id = ?`;
    const [rows, fields] = await db.execute(query, [req.params.id]);

    if (rows.length === 0) return res.status(404).json({ error: "Course not found" });
    res.json(rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

// @route: POST /api/courses
const createCourse = async (req, res) => {
  try {
    const { title, description, instructor_id } = req.body;
    let query = `INSERT INTO courses (title, description, instructor_id) VALUES (?, ?, ?)`;
    const [result] = await db.execute(query, [title, description, instructor_id]);

    res.status(201).json({ id: result.insertId, title, description, instructor_id });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

// @route: PUT /api/courses/:id
const updateCourse = async (req, res) => {
  try {
    const { title, description, instructor_id } = req.body;
    let query = `UPDATE courses SET title = ?, description = ?, instructor_id = ? WHERE id = ?`;
    const [result] = await db.query(query, [title, description, instructor_id, req.params.id]);

    if (result.affectedRows === 0) return res.status(404).json({ error: "Course not found" });
    res.status(200).json({ id: req.params.id, title, description, instructor_id });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

// @route: DELETE /api/courses/:id
const deleteCourse = async (req, res) => {
  try {
    const [result] = await db.execute(`DELETE FROM courses WHERE id = ?`, [req.params.id]);

    if (result.affectedRows === 0) return res.status(404).json({ error: "Course not found" });
    res.status(200).json({ message: "Course deleted successfully" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
};

export { getAllCourses, getCourse, createCourse, updateCourse, deleteCourse };
