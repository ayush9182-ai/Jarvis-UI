import express from "express";
import helmet from "helmet";
import rateLimit from "express-rate-limit";

const app = express();
app.use(helmet());
app.use(express.json({ limit: "16kb" }));
app.use(rateLimit({ windowMs: 60_000, limit: 30, standardHeaders: true, legacyHeaders: false }));

app.get("/health", (_req, res) => res.json({ ok: true, service: "jarvis-api" }));
app.post("/v1/assistant", async (req, res) => {
  try {
    const message = String(req.body?.message ?? "").trim();
    const memory = String(req.body?.memory ?? "").slice(-12000);
    if (!message || message.length > 4000) return res.status(400).json({ error: "Invalid message" });
    const upstream = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${process.env.GEMINI_MODEL ?? "gemini-1.5-flash"}:generateContent?key=${process.env.GEMINI_API_KEY}`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ contents: [{ parts: [{ text: `You are Jarvis. Address the user as Boss. Reply briefly in Hinglish. Conversation memory:\n${memory}\nUser:\n${message}` }] }] })
    });
    if (!upstream.ok) return res.status(502).json({ error: "LLM upstream failed" });
    const data = await upstream.json();
    const reply = data?.candidates?.[0]?.content?.parts?.[0]?.text;
    if (!reply) return res.status(502).json({ error: "Empty LLM response" });
    res.json({ reply: String(reply).trim() });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: "Assistant unavailable" });
  }
});

app.listen(process.env.PORT ?? 8787, "0.0.0.0", () => console.log("Jarvis API online"));
