import express from "express";
import path from "path";
import cors from "cors";
import { createServer as createViteServer } from "vite";
import { GoogleGenAI } from "@google/genai";

async function startServer() {
  const app = express();
  const PORT = 3000;

  app.use(cors());
  app.use(express.json({ limit: "64kb" }));

  // Health check endpoint
  app.get("/api/health", (_req, res) => {
    res.json({ ok: true, service: "jarvis-api", status: "online" });
  });

  // Assistant endpoint
  app.post("/api/assistant", async (req, res) => {
    try {
      const message = String(req.body?.message ?? "").trim();
      const memory = String(req.body?.memory ?? "").slice(-12000);
      const clientApiKey = String(req.headers["x-gemini-key"] ?? "").trim();
      const effectiveApiKey = clientApiKey || process.env.GEMINI_API_KEY || "";

      if (!message || message.length > 4000) {
        return res.status(400).json({ error: "Invalid message" });
      }

      if (!effectiveApiKey) {
        return res.json({
          reply: "Boss, pehle API KEY save kijiye. Top-right 'API KEY' button par click karke key enter karein.",
          needsKey: true,
        });
      }

      const model = process.env.GEMINI_MODEL || "gemini-3.8-flash";

      try {
        const ai = new GoogleGenAI({ apiKey: effectiveApiKey });
        const systemPrompt = `You are Jarvis, a highly intelligent and charismatic AI assistant inspired by Tony Stark's JARVIS. 
Always address the user as 'Boss'. 
Answer conversation, reasoning, tech, and code questions accurately, crisply, and with a touch of polite wit. 
Reply in natural Hinglish (mix of Hindi and English) unless the Boss explicitly asks to speak in pure English, Hindi, or another language.
Keep responses concise, conversational, and direct for voice/speech.
Context of recent conversation:
${memory}
User command/query:
${message}`;

        const response = await ai.models.generateContent({
          model,
          contents: systemPrompt,
        });

        const reply = response.text?.trim();
        if (!reply) {
          return res.status(502).json({ error: "Empty response from Gemini" });
        }

        return res.json({ reply });
      } catch (geminiError: any) {
        console.error("Gemini SDK error, trying direct REST fallback:", geminiError?.message);
        
        // Fallback to direct REST endpoint if SDK encountered version mismatch
        const restUrl = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${encodeURIComponent(effectiveApiKey)}`;
        const upstream = await fetch(restUrl, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            contents: [
              {
                parts: [
                  {
                    text: `You are Jarvis. Address the user as Boss. Handle conversation, reasoning and code. Reply in natural Hinglish unless requested otherwise.\nContext:\n${memory}\nBoss:\n${message}`,
                  },
                ],
              },
            ],
          }),
        });

        if (!upstream.ok) {
          const errText = await upstream.text();
          console.error("Upstream REST error:", errText);
          return res.status(502).json({
            error: "Gemini connection error",
            reply: `Boss, Gemini connection me dikkat aayi: ${upstream.statusText || "Request failed"}. Kripya API key check karein.`,
          });
        }

        const data: any = await upstream.json();
        const reply = data?.candidates?.[0]?.content?.parts?.[0]?.text?.trim();
        if (!reply) {
          return res.status(502).json({ error: "Empty reply from Gemini" });
        }
        return res.json({ reply });
      }
    } catch (err: any) {
      console.error("Assistant endpoint error:", err);
      res.status(500).json({
        error: "Assistant unavailable",
        reply: "Boss, abhi system me thoda glitch ho gaya. Phir se koshish karein.",
      });
    }
  });

  // Vite middleware in dev or static files in production
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (_req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Jarvis Server running on http://0.0.0.0:${PORT}`);
  });
}

startServer();
