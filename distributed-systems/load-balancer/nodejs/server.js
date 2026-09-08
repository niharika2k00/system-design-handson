import express from "express";

const PORT = Number(process.argv[2] ?? 3001); // 3rd argument from the commandline
const app = express();

app.get("/health", (req, res) => res.json({ ok: true, port: PORT }));

app.use((req, res) => res.json({ backend: PORT, path: req.path }));

app.listen(PORT, () => console.log(`server running on port ${PORT}`));
