import React, { useState } from "react";
import { Volume2, Copy, Check, Trash2, History, ChevronDown, ChevronUp } from "lucide-react";
import { MemoryTurn } from "../types";

interface ActivityFeedProps {
  latestResponse: string;
  turns: MemoryTurn[];
  onClear: () => void;
  onSpeak: (text: string) => void;
  isSpeaking: boolean;
}

export const ActivityFeed: React.FC<ActivityFeedProps> = ({
  latestResponse,
  turns,
  onClear,
  onSpeak,
  isSpeaking,
}) => {
  const [copied, setCopied] = useState(false);
  const [showHistory, setShowHistory] = useState(false);

  const handleCopy = () => {
    if (!latestResponse) return;
    navigator.clipboard.writeText(latestResponse);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div id="activity-feed-section" className="w-full mt-6">
      {/* Header Row */}
      <div className="flex items-center justify-between mb-1">
        <span className="text-xs font-hud font-bold tracking-widest text-[#62e6ff] uppercase">
          LIVE FEED
        </span>
        <div className="flex items-center gap-3">
          {turns.length > 0 && (
            <button
              type="button"
              onClick={() => setShowHistory(!showHistory)}
              className="text-xs text-[#8AA4BC] hover:text-[#62e6ff] flex items-center gap-1 transition-colors"
            >
              <History className="w-3.5 h-3.5" />
              <span>{turns.length} turns</span>
              {showHistory ? (
                <ChevronUp className="w-3.5 h-3.5" />
              ) : (
                <ChevronDown className="w-3.5 h-3.5" />
              )}
            </button>
          )}
          <button
            id="btn-clear-feed"
            type="button"
            onClick={onClear}
            className="text-xs text-[#8AA4BC] hover:text-[#ff9c88] transition-colors flex items-center gap-1"
          >
            <Trash2 className="w-3.5 h-3.5" />
            <span>Clear</span>
          </button>
        </div>
      </div>

      <h3 className="text-3xl sm:text-4xl font-bold text-white mb-3 tracking-tight">
        Activity
      </h3>

      {/* Featured Primary Response Card (Matches original Android #D9F4FF layout) */}
      <div
        id="latest-response-card"
        className="w-full bg-[#D9F4FF] rounded-2xl p-5 sm:p-6 text-[#0B1B2D] shadow-lg relative group transition-all"
      >
        <div className="flex items-start justify-between gap-4">
          <p className="text-lg sm:text-xl font-medium leading-relaxed select-text whitespace-pre-wrap">
            {latestResponse || "Hello Boss! Jarvis online hai."}
          </p>
          <div className="flex items-center gap-1 shrink-0 pt-1">
            <button
              type="button"
              onClick={() => onSpeak(latestResponse)}
              className={`p-2 rounded-xl transition-all ${
                isSpeaking
                  ? "bg-[#62e6ff] text-[#06101d] animate-pulse"
                  : "bg-white/80 hover:bg-white text-[#0B1B2D]"
              }`}
              title="Speak reply"
            >
              <Volume2 className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={handleCopy}
              className="p-2 rounded-xl bg-white/80 hover:bg-white text-[#0B1B2D] transition-all"
              title="Copy reply"
            >
              {copied ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
            </button>
          </div>
        </div>
      </div>

      {/* Expandable Multi-Turn Memory History */}
      {showHistory && turns.length > 0 && (
        <div className="mt-4 space-y-3 bg-[#091a2a] border border-[#14324d] rounded-2xl p-4 max-h-80 overflow-y-auto">
          <div className="text-xs font-hud text-[#62e6ff] uppercase tracking-wider mb-2">
            Session Memory Buffer (Last {turns.length} turns)
          </div>
          {turns.map((turn, idx) => (
            <div
              key={idx}
              className={`p-3 rounded-xl text-sm leading-relaxed ${
                turn.role === "user"
                  ? "bg-[#10273d] text-[#D6E7F8] border border-[#1c3e60] ml-6"
                  : "bg-[#0f2d48] text-[#cff8ff] border border-[#214b73] mr-6"
              }`}
            >
              <div className="flex items-center justify-between text-[11px] mb-1 opacity-70 font-hud">
                <span>{turn.role === "user" ? "Boss" : "Jarvis"}</span>
                {turn.timestamp && (
                  <span>{new Date(turn.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" })}</span>
                )}
              </div>
              <p className="whitespace-pre-wrap">{turn.text}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
