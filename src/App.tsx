import React, { useState, useEffect, useRef, useCallback } from "react";
import { Mic, MicOff, KeyRound, Sparkles } from "lucide-react";
import { AssistantStatus, ConfirmationRequest, MemoryTurn, TimerState } from "./types";
import { IntentRouter } from "./services/IntentRouter";
import { MemoryStore } from "./services/MemoryStore";
import { ToolExecutor } from "./services/ToolExecutor";
import { TopBar } from "./components/TopBar";
import { JarvisOrb } from "./components/JarvisOrb";
import { CommandConsole } from "./components/CommandConsole";
import { ActivityFeed } from "./components/ActivityFeed";
import { ConfirmationModal } from "./components/ConfirmationModal";
import { ApiKeyModal } from "./components/ApiKeyModal";
import { TimerHUD } from "./components/TimerHUD";

export const App: React.FC = () => {
  const [status, setStatus] = useState<AssistantStatus>("idle");
  const [statusText, setStatusText] = useState("READY FOR COMMAND");
  const [latestResponse, setLatestResponse] = useState("Hello Boss! Jarvis online hai.");
  const [turns, setTurns] = useState<MemoryTurn[]>([]);
  const [apiKey, setApiKey] = useState("");
  const [isApiKeyModalOpen, setIsApiKeyModalOpen] = useState(false);
  const [confirmationRequest, setConfirmationRequest] = useState<ConfirmationRequest | null>(null);
  const [isMuted, setIsMuted] = useState(false);
  const [timer, setTimer] = useState<TimerState | null>(null);

  const recognitionRef = useRef<any>(null);
  const isListeningRef = useRef(false);

  // Initialize data on mount
  useEffect(() => {
    setTurns(MemoryStore.getTurns());
    const storedKey = localStorage.getItem("gemini_key") || "";
    setApiKey(storedKey);
  }, []);

  // Text to Speech
  const speak = useCallback(
    (text: string) => {
      if (isMuted || !("speechSynthesis" in window)) return;

      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.rate = 1.0;
      utterance.pitch = 1.0;

      // Select natural sounding voice if available
      const voices = window.speechSynthesis.getVoices();
      const preferredVoice =
        voices.find((v) => v.lang.includes("en-IN")) ||
        voices.find((v) => v.lang.includes("en-GB")) ||
        voices.find((v) => v.lang.includes("en-US"));
      if (preferredVoice) {
        utterance.voice = preferredVoice;
      }

      setStatus("speaking");
      setStatusText("SPEAKING...");

      utterance.onend = () => {
        setStatus("idle");
        setStatusText("READY FOR COMMAND");
      };

      utterance.onerror = () => {
        setStatus("idle");
        setStatusText("READY FOR COMMAND");
      };

      window.speechSynthesis.speak(utterance);
    },
    [isMuted]
  );

  const finishInteraction = useCallback(
    (reply: string) => {
      const updated = MemoryStore.remember("assistant", reply);
      setTurns(updated);
      setLatestResponse(reply);
      setStatus("idle");
      setStatusText("READY FOR COMMAND");
      speak(reply);
    },
    [speak]
  );

  const handleExecute = useCallback(
    async (raw: string) => {
      const command = raw.trim();
      if (!command) {
        finishInteraction("Boss, command dijiye.");
        return;
      }

      // Add user turn
      const updatedUserTurns = MemoryStore.remember("user", command);
      setTurns(updatedUserTurns);

      setStatus("executing");
      setStatusText("EXECUTING COMMAND");

      const route = IntentRouter.route(command);

      // Deterministic tool routes
      if (route.type !== "CHAT") {
        ToolExecutor.execute(
          route,
          (req) => setConfirmationRequest(req),
          (result) => {
            if (result.openSettings) {
              setIsApiKeyModalOpen(true);
            }
            if (result.startTimerMinutes) {
              const totalSecs = Math.round(result.startTimerMinutes * 60);
              setTimer({
                active: true,
                totalSeconds: totalSecs,
                remainingSeconds: totalSecs,
                label: `Focus Timer (${result.startTimerMinutes} min)`,
              });
            }
            finishInteraction(result.message);
          }
        );
        return;
      }

      // Chat query routed to Gemini AI via backend proxy
      try {
        const memoryContext = MemoryStore.getContextText();
        const headers: Record<string, string> = {
          "Content-Type": "application/json",
        };
        if (apiKey) {
          headers["x-gemini-key"] = apiKey;
        }

        const res = await fetch("/api/assistant", {
          method: "POST",
          headers,
          body: JSON.stringify({
            message: command,
            memory: memoryContext,
          }),
        });

        if (!res.ok) {
          throw new Error(`HTTP ${res.status}`);
        }

        const data = await res.json();
        const reply = data.reply || "Boss, mujhe response generate karne me issue hui.";
        if (data.needsKey) {
          setIsApiKeyModalOpen(true);
        }
        finishInteraction(reply);
      } catch (err: any) {
        console.error("Gemini assistant call failed:", err);
        finishInteraction(
          "Boss, network ya Gemini assistant connection me issue hui. Check your API key or server status."
        );
      }
    },
    [apiKey, finishInteraction]
  );

  // Web Speech Recognition
  const toggleListening = () => {
    const SpeechRecognition =
      (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;

    if (!SpeechRecognition) {
      finishInteraction("Boss, aapke browser me speech recognition support available nahi hai.");
      return;
    }

    if (isListeningRef.current && recognitionRef.current) {
      recognitionRef.current.stop();
      isListeningRef.current = false;
      setStatus("idle");
      setStatusText("READY FOR COMMAND");
      return;
    }

    try {
      const recognition = new SpeechRecognition();
      recognitionRef.current = recognition;
      recognition.continuous = false;
      recognition.interimResults = false;
      recognition.lang = "en-IN";

      recognition.onstart = () => {
        isListeningRef.current = true;
        setStatus("listening");
        setStatusText("LISTENING...");
      };

      recognition.onresult = (event: any) => {
        isListeningRef.current = false;
        const transcript = event.results[0]?.[0]?.transcript || "";
        if (transcript) {
          // Strip out "Hey Jarvis" prefix if present
          const cleaned = transcript.replace(/^(hey\s+)?jarvis[\s,:.-]*/i, "").trim();
          handleExecute(cleaned || transcript);
        } else {
          setStatus("idle");
          setStatusText("READY FOR COMMAND");
        }
      };

      recognition.onerror = (e: any) => {
        console.warn("Speech recognition error:", e);
        isListeningRef.current = false;
        setStatus("idle");
        setStatusText("READY FOR COMMAND");
        if (e.error !== "no-speech") {
          finishInteraction("Boss, voice capture me issue aayi.");
        }
      };

      recognition.onend = () => {
        isListeningRef.current = false;
        if (status === "listening") {
          setStatus("idle");
          setStatusText("READY FOR COMMAND");
        }
      };

      recognition.start();
    } catch (e) {
      console.error("Speech recognition startup error:", e);
      isListeningRef.current = false;
      setStatus("idle");
      setStatusText("READY FOR COMMAND");
    }
  };

  const handleClearHistory = () => {
    MemoryStore.clear();
    setTurns([]);
    setLatestResponse("Activity cleared, Boss.");
    if (timer) {
      setTimer(null);
    }
  };

  const handleSaveApiKey = (key: string) => {
    setApiKey(key);
    if (key) {
      localStorage.setItem("gemini_key", key);
    } else {
      localStorage.removeItem("gemini_key");
    }
  };

  return (
    <div className="min-h-screen bg-[#06101D] text-white flex flex-col items-center">
      <div className="w-full max-w-xl mx-auto px-4 py-5 flex flex-col items-center min-h-screen">
        {/* Top Header */}
        <TopBar
          hasApiKey={Boolean(apiKey)}
          isMuted={isMuted}
          onToggleMute={() => {
            setIsMuted(!isMuted);
            if (!isMuted) window.speechSynthesis?.cancel();
          }}
          onOpenApiKey={() => setIsApiKeyModalOpen(true)}
        />

        {/* Jarvis Orb Visualizer & Status */}
        <div className="flex flex-col items-center my-6">
          <JarvisOrb status={status} onClick={toggleListening} />
          <div
            id="jarvis-status-text"
            className="mt-3 text-xs sm:text-sm font-hud font-bold tracking-widest text-[#62e6ff] uppercase select-none transition-all drop-shadow-[0_0_8px_rgba(98,230,255,0.4)]"
          >
            {statusText}
          </div>
        </div>

        {/* Focus Timer Widget (if active) */}
        {timer && (
          <TimerHUD
            timer={timer}
            onUpdateTimer={(updated) => setTimer(updated)}
            onDismiss={() => setTimer(null)}
          />
        )}

        {/* Command Console Card */}
        <CommandConsole
          onExecute={handleExecute}
          disabled={status === "executing" || status === "listening"}
        />

        {/* Voice Control Button (Matches original Android UI button) */}
        <div className="w-full mt-4 flex flex-col gap-2.5">
          <button
            id="btn-voice-listen"
            type="button"
            onClick={toggleListening}
            className={`w-full py-3.5 px-4 rounded-2xl font-hud font-bold text-base transition-all flex items-center justify-center gap-2 shadow-lg active:scale-[0.98] ${
              status === "listening"
                ? "bg-rose-500 text-white animate-pulse"
                : "bg-[#BCEFFF] hover:bg-[#d6f5ff] text-[#06101D]"
            }`}
          >
            {status === "listening" ? (
              <>
                <MicOff className="w-5 h-5" />
                <span>LISTENING... TAP TO STOP</span>
              </>
            ) : (
              <>
                <Mic className="w-5 h-5" />
                <span>START LISTENING</span>
              </>
            )}
          </button>

          <button
            id="btn-configure-api-key"
            type="button"
            onClick={() => setIsApiKeyModalOpen(true)}
            className="w-full py-2.5 px-4 rounded-xl bg-[#0F2740] hover:bg-[#153454] border border-[#1b4366] text-xs font-hud tracking-wider text-white font-semibold transition-all flex items-center justify-center gap-2 active:scale-[0.99]"
          >
            <KeyRound className="w-4 h-4 text-[#62e6ff]" />
            <span>⚙ CONFIGURE GEMINI API KEY</span>
          </button>
        </div>

        {/* Activity & Live Feed */}
        <ActivityFeed
          latestResponse={latestResponse}
          turns={turns}
          onClear={handleClearHistory}
          onSpeak={speak}
          isSpeaking={status === "speaking"}
        />
      </div>

      {/* Confirmation Modal */}
      <ConfirmationModal
        request={confirmationRequest}
        onClose={() => setConfirmationRequest(null)}
      />

      {/* API Key Modal */}
      <ApiKeyModal
        isOpen={isApiKeyModalOpen}
        onClose={() => setIsApiKeyModalOpen(false)}
        currentKey={apiKey}
        onSaveKey={handleSaveApiKey}
      />
    </div>
  );
};
