import express from "express";
import {
  getAllStudents,
  getStudent,
  createStudent,
  updateStudent,
  deleteStudent,
} from "../controllers/studentControllers.js";
import auth from "../middleware/auth.js";

const router = express.Router();

// public
router.post("/", createStudent);

// protected
router.get("/", auth, getAllStudents);
router.get("/:id", auth, getStudent);
router.put("/:id", auth, updateStudent);
router.delete("/:id", auth, deleteStudent);

export default router;
