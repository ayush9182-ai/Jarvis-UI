import React from "react";
import { AlertCircle, PhoneCall, Send, ShieldAlert } from "lucide-react";
import { ConfirmationRequest } from "../types";

interface ConfirmationModalProps {
  request: ConfirmationRequest | null;
  onClose: () => void;
}

export const ConfirmationModal: React.FC<ConfirmationModalProps> = ({ request, onClose }) => {
  if (!request) return null;

  const isCall = request.title.toLowerCase().includes("call");
  const isMessage = request.title.toLowerCase().includes("message");

  const handleConfirm = () => {
    request.action();
    onClose();
  };

  return (
    <div
      id="confirmation-modal-backdrop"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm p-4 animate-in fade-in duration-200"
    >
      <div
        id="confirmation-modal"
        className="w-full max-w-md bg-[#0c2236] border border-[#1f496e] rounded-3xl p-6 shadow-2xl text-white transform transition-all"
      >
        <div className="flex items-center gap-3 mb-4">
          <div className="w-12 h-12 rounded-2xl bg-[#143350] border border-[#235684] flex items-center justify-center text-[#62e6ff]">
            {isCall ? (
              <PhoneCall className="w-6 h-6 text-[#62e6ff]" />
            ) : isMessage ? (
              <Send className="w-6 h-6 text-[#62e6ff]" />
            ) : (
              <ShieldAlert className="w-6 h-6 text-[#62e6ff]" />
            )}
          </div>
          <div>
            <span className="text-[11px] font-hud text-[#62e6ff] tracking-widest uppercase">
              CONFIRMATION REQUIRED
            </span>
            <h3 className="text-xl font-bold text-white">{request.title}</h3>
          </div>
        </div>

        <p className="text-sm text-[#b0c8de] mb-6 leading-relaxed bg-[#10273d] p-3.5 rounded-xl border border-[#1b3d5e]">
          {request.message}
        </p>

        <div className="flex items-center justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2.5 rounded-xl bg-[#162a40] hover:bg-[#1d3752] text-sm text-[#D6E7F8] font-medium transition-colors"
          >
            {request.cancelText || "Cancel"}
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            className="px-5 py-2.5 rounded-xl bg-[#62e6ff] hover:bg-[#85eeff] text-[#06101D] text-sm font-bold shadow-lg transition-transform active:scale-95"
          >
            {request.confirmText || "Continue"}
          </button>
        </div>
      </div>
    </div>
  );
};
