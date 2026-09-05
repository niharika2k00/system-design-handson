import express from "express";
import {
  getAllUsers,
  getUser,
  getUserRaw,
  createUser,
  bulkCreateUsers,
  updateUser,
  deleteUser,
} from "../controllers/userControllers.js";

const router = express.Router();

router.get("/", getAllUsers);
router.post("/", createUser);
router.post("/bulk", bulkCreateUsers);
router.get("/raw/:id", getUserRaw);
router.get("/:id", getUser);
router.put("/:id", updateUser);
router.delete("/:id", deleteUser);

export default router;
