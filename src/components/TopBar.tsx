import React from "react";
import { KeyRound, Volume2, VolumeX } from "lucide-react";

interface TopBarProps {
  hasApiKey: boolean;
  isMuted: boolean;
  onToggleMute: () => void;
  onOpenApiKey: () => void;
}

export const TopBar: React.FC<TopBarProps> = ({
  hasApiKey,
  isMuted,
  onToggleMute,
  onOpenApiKey,
}) => {
  return (
    <header
      id="jarvis-top-bar"
      className="w-full flex items-center justify-between py-4 px-2 sm:px-4 border-b border-[#0c2236]"
    >
      <div className="flex items-center gap-3">
        <span className="font-hud tracking-widest text-xl font-bold text-white uppercase drop-shadow-[0_0_8px_rgba(255,255,255,0.4)]">
          JARVIS
        </span>
        <span className="text-xs text-[#62e6ff] px-2 py-0.5 rounded bg-[#0c2236] border border-[#163654] font-hud">
          v2.0
        </span>
      </div>

      <div className="flex items-center gap-2 sm:gap-3">
        {/* Voice Audio Mute Toggle */}
        <button
          id="btn-toggle-sound"
          type="button"
          onClick={onToggleMute}
          className={`p-2 rounded-full border transition-colors flex items-center justify-center ${
            isMuted
              ? "bg-[#162534] border-[#2c4763] text-[#7A98B3] hover:text-white"
              : "bg-[#0c273e] border-[#1d4c72] text-[#62e6ff] hover:bg-[#12395b]"
          }`}
          title={isMuted ? "Unmute Voice Responses" : "Mute Voice Responses"}
        >
          {isMuted ? <VolumeX className="w-4 h-4" /> : <Volume2 className="w-4 h-4" />}
        </button>

        {/* API Key Config Button */}
        <button
          id="btn-topbar-api-key"
          type="button"
          onClick={onOpenApiKey}
          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-hud transition-all border ${
            hasApiKey
              ? "bg-[#0e273e] border-[#1b4b72] text-[#9ae9f8] hover:bg-[#163a5a]"
              : "bg-[#2b1b17] border-[#723126] text-[#ff9c88] hover:bg-[#3d241e]"
          }`}
          title="Configure Gemini API Key"
        >
          <KeyRound className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">{hasApiKey ? "API KEY CONFIGURED" : "API KEY NEEDED"}</span>
          <span className="sm:hidden">KEY</span>
        </button>

        {/* System Online Badge */}
        <div
          id="system-status-badge"
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-[#12253a] border border-[#1e3c5c]"
        >
          <span className="w-2 h-2 rounded-full bg-[#8BE9D7] animate-pulse" />
          <span className="text-[10px] font-hud tracking-wider font-bold text-[#8BE9D7]">
            SYSTEM ONLINE
          </span>
        </div>
      </div>
    </header>
  );
};
