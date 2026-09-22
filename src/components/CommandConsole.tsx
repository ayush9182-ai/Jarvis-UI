import React, { useState } from "react";
import { ArrowUpRight } from "lucide-react";

interface CommandConsoleProps {
  onExecute: (command: string) => void;
  disabled?: boolean;
}

export const CommandConsole: React.FC<CommandConsoleProps> = ({ onExecute, disabled }) => {
  const [input, setInput] = useState("");

  const suggestions = [
    "Open YouTube",
    "Call 9876543210",
    "Search the web",
    "Focus timer 5 min",
    "Open WhatsApp",
    "Explain black holes",
  ];

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = input.trim();
    if (!trimmed || disabled) return;
    onExecute(trimmed);
    setInput("");
  };

  const handleChipClick = (suggestion: string) => {
    if (disabled) return;
    setInput(suggestion);
    onExecute(suggestion);
  };

  return (
    <div
      id="command-console-card"
      className="w-full bg-[#0c2236] border border-[#14324d] rounded-3xl p-5 sm:p-6 shadow-xl"
    >
      <div className="flex items-center justify-between mb-1">
        <span className="text-xs font-hud font-bold tracking-widest text-[#62e6ff] uppercase">
          COMMAND CONSOLE
        </span>
      </div>

      <h2 className="text-2xl sm:text-3xl font-bold text-white mb-4 tracking-tight">
        What should I do?
      </h2>

      {/* Input Row */}
      <form onSubmit={handleSubmit} className="relative flex items-center mb-4">
        <div className="w-full flex items-center bg-[#10273d] border border-[#1b3d5e] focus-within:border-[#62e6ff] transition-all rounded-2xl px-4 py-2">
          <span className="text-[#62e6ff] font-hud text-xl font-bold mr-3 select-none">
            &gt;
          </span>
          <input
            id="command-input"
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            disabled={disabled}
            placeholder="Try: call mom, open YouTube, timer 5 min"
            className="w-full bg-transparent text-white placeholder-[#7A98B3] text-base focus:outline-none"
            autoComplete="off"
          />
          <button
            id="btn-submit-command"
            type="submit"
            disabled={disabled || !input.trim()}
            className="ml-2 flex items-center justify-center w-10 h-10 rounded-xl bg-[#CFF8FF] hover:bg-white text-[#06101D] font-bold transition-all disabled:opacity-40 disabled:cursor-not-allowed shadow-md hover:shadow-cyan-500/20 active:scale-95"
            title="Execute Command"
          >
            <ArrowUpRight className="w-6 h-6" />
          </button>
        </div>
      </form>

      {/* Suggestions Chips */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1 scrollbar-none">
        {suggestions.map((suggestion) => (
          <button
            key={suggestion}
            type="button"
            onClick={() => handleChipClick(suggestion)}
            disabled={disabled}
            className="whitespace-nowrap px-3.5 py-1.5 rounded-xl bg-[#162a40] hover:bg-[#1d3752] active:bg-[#254666] border border-[#203c5a] text-xs font-medium text-[#D6E7F8] hover:text-white transition-all disabled:opacity-50"
          >
            {suggestion}
          </button>
        ))}
      </div>
    </div>
  );
};
