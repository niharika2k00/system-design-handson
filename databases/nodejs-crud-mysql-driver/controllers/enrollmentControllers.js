const express = require("express");
const router = express.Router();
const pool = require("../db");

// This is a junction table with a COMPOSITE key (student_id + course_id).
// So there is no single :id. We identify a row by BOTH ids.

// CREATE (enroll a student in a course)
router.post("/", async (req, res) => {
  try {
    const { student_id, course_id, enrolled_date } = req.body;
    // enrolled_date is optional; the DB defaults it to today if omitted.
    const [result] = await pool.query(
      "INSERT INTO enrollments (student_id, course_id, enrolled_date) VALUES (?, ?, ?)",
      [student_id, course_id, enrolled_date || new Date()]
    );
    res.status(201).json({ student_id, course_id, enrolled_date: enrolled_date || "today" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// READ ALL (with student name + course title via JOINs)
router.get("/", async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT e.student_id, e.course_id, e.enrolled_date,
              s.name AS student_name, c.title AS course_title
       FROM enrollments e
       JOIN students s ON e.student_id = s.id
       JOIN courses  c ON e.course_id  = c.id`
    );
    res.json(rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// READ ONE (needs both ids)
router.get("/:studentId/:courseId", async (req, res) => {
  try {
    const { studentId, courseId } = req.params;
    const [rows] = await pool.query("SELECT * FROM enrollments WHERE student_id = ? AND course_id = ?", [
      studentId,
      courseId,
    ]);
    if (rows.length === 0) return res.status(404).json({ error: "Enrollment not found" });
    res.json(rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// UPDATE (only enrolled_date can change; the ids are the key)
router.put("/:studentId/:courseId", async (req, res) => {
  try {
    const { studentId, courseId } = req.params;
    const { enrolled_date } = req.body;
    const [result] = await pool.query(
      "UPDATE enrollments SET enrolled_date = ? WHERE student_id = ? AND course_id = ?",
      [enrolled_date, studentId, courseId]
    );
    if (result.affectedRows === 0) return res.status(404).json({ error: "Enrollment not found" });
    res.json({ student_id: Number(studentId), course_id: Number(courseId), enrolled_date });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// DELETE (unenroll)
router.delete("/:studentId/:courseId", async (req, res) => {
  try {
    const { studentId, courseId } = req.params;
    const [result] = await pool.query("DELETE FROM enrollments WHERE student_id = ? AND course_id = ?", [
      studentId,
      courseId,
    ]);
    if (result.affectedRows === 0) return res.status(404).json({ error: "Enrollment not found" });
    res.json({ message: "Enrollment deleted" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
