import React, { useState, useEffect } from "react";
import { KeyRound, ExternalLink, Check, AlertCircle, HelpCircle, X } from "lucide-react";

interface ApiKeyModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentKey: string;
  onSaveKey: (key: string) => void;
}

export const ApiKeyModal: React.FC<ApiKeyModalProps> = ({
  isOpen,
  onClose,
  currentKey,
  onSaveKey,
}) => {
  const [keyInput, setKeyInput] = useState(currentKey);
  const [showGuide, setShowGuide] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  useEffect(() => {
    setKeyInput(currentKey);
    setStatusMessage(null);
  }, [currentKey, isOpen]);

  if (!isOpen) return null;

  const handleSave = () => {
    const trimmed = keyInput.trim();
    onSaveKey(trimmed);
    setStatusMessage(trimmed ? "API key saved successfully, Boss." : "API key cleared.");
    setTimeout(() => {
      onClose();
    }, 1200);
  };

  const handleClear = () => {
    setKeyInput("");
    onSaveKey("");
    setStatusMessage("API key cleared.");
  };

  return (
    <div
      id="api-key-modal-backdrop"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 animate-in fade-in duration-200"
    >
      <div
        id="api-key-modal"
        className="w-full max-w-lg bg-[#0c2236] border border-[#1f496e] rounded-3xl p-6 shadow-2xl text-white relative max-h-[90vh] overflow-y-auto"
      >
        <button
          type="button"
          onClick={onClose}
          className="absolute top-5 right-5 text-[#7A98B3] hover:text-white p-1 rounded-lg"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex items-center gap-3 mb-2">
          <div className="w-10 h-10 rounded-xl bg-[#143350] border border-[#235684] flex items-center justify-center text-[#62e6ff]">
            <KeyRound className="w-5 h-5" />
          </div>
          <div>
            <span className="text-[11px] font-hud text-[#62e6ff] tracking-widest uppercase">
              AI CONFIGURATION
            </span>
            <h3 className="text-xl font-bold text-white">Gemini API Key</h3>
          </div>
        </div>

        <p className="text-xs text-[#7A98B3] mb-4">
          Key is stored safely in your browser local storage. Never committed to source control.
        </p>

        {statusMessage && (
          <div className="mb-4 p-3 rounded-xl bg-emerald-950/60 border border-emerald-500/40 text-emerald-300 text-xs flex items-center gap-2">
            <Check className="w-4 h-4" />
            <span>{statusMessage}</span>
          </div>
        )}

        <div className="mb-4">
          <label className="block text-xs font-hud text-[#62e6ff] mb-2 uppercase tracking-wider">
            Paste Google Gemini Key
          </label>
          <input
            id="input-gemini-key"
            type="password"
            value={keyInput}
            onChange={(e) => setKeyInput(e.target.value)}
            placeholder="AIzaSy..."
            className="w-full bg-[#10273d] border border-[#1b3d5e] focus:border-[#62e6ff] text-white rounded-xl px-4 py-3 text-sm focus:outline-none transition-all font-mono"
          />
        </div>

        {/* Action Buttons */}
        <div className="flex items-center justify-between gap-3 mb-6">
          <button
            type="button"
            onClick={handleClear}
            className="text-xs text-[#7A98B3] hover:text-[#ff9c88] transition-colors"
          >
            Clear key
          </button>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-xl bg-[#162a40] hover:bg-[#1d3752] text-xs text-[#D6E7F8] font-medium transition-colors"
            >
              Cancel
            </button>
            <button
              id="btn-save-gemini-key"
              type="button"
              onClick={handleSave}
              className="px-5 py-2 rounded-xl bg-[#62e6ff] hover:bg-[#85eeff] text-[#06101D] text-xs font-bold transition-all shadow-md active:scale-95"
            >
              Save Key
            </button>
          </div>
        </div>

        {/* How to get API key guide */}
        <div className="border-t border-[#14324d] pt-4">
          <button
            type="button"
            onClick={() => setShowGuide(!showGuide)}
            className="flex items-center justify-between w-full text-left text-xs font-hud text-[#62e6ff] hover:text-[#9feeff] transition-colors"
          >
            <span className="flex items-center gap-1.5 font-bold tracking-wider">
              <HelpCircle className="w-4 h-4" />
              HOW TO GET GEMINI API KEY
            </span>
            <span className="text-[11px] underline">
              {showGuide ? "Hide Steps" : "Show Steps"}
            </span>
          </button>

          {showGuide && (
            <div className="mt-3 p-4 rounded-2xl bg-[#091a2a] border border-[#163654] text-xs text-[#b8d2eb] space-y-2.5">
              <ol className="list-decimal list-inside space-y-2">
                <li>
                  Open{" "}
                  <a
                    href="https://aistudio.google.com/apikey"
                    target="_blank"
                    rel="noreferrer"
                    className="text-[#62e6ff] underline inline-flex items-center gap-1 font-semibold"
                  >
                    Google AI Studio <ExternalLink className="w-3 h-3" />
                  </a>
                </li>
                <li>Sign in with your Google account.</li>
                <li>Click <strong>Create API key</strong>.</li>
                <li>Select <strong>Create API key in new project</strong> if prompted.</li>
                <li>Click <strong>Copy</strong> beside the generated key.</li>
                <li>Paste it in the box above and click <strong>Save Key</strong>.</li>
              </ol>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
