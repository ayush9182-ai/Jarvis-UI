import React, { useEffect, useState, useRef } from "react";
import { Timer, Play, Pause, X, RotateCcw, CheckCircle } from "lucide-react";
import { TimerState } from "../types";

interface TimerHUDProps {
  timer: TimerState;
  onUpdateTimer: (updated: TimerState) => void;
  onDismiss: () => void;
}

export const TimerHUD: React.FC<TimerHUDProps> = ({
  timer,
  onUpdateTimer,
  onDismiss,
}) => {
  const [isRunning, setIsRunning] = useState(true);
  const audioContextRef = useRef<AudioContext | null>(null);

  const playChime = () => {
    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();
      audioContextRef.current = ctx;

      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = "sine";
      osc.frequency.setValueAtTime(587.33, ctx.currentTime); // D5
      osc.frequency.setValueAtTime(880, ctx.currentTime + 0.15); // A5

      gain.gain.setValueAtTime(0.3, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 1.2);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start();
      osc.stop(ctx.currentTime + 1.3);
    } catch (e) {
      console.warn("Audio chime error:", e);
    }
  };

  useEffect(() => {
    if (!timer.active || !isRunning) return;

    const interval = setInterval(() => {
      if (timer.remainingSeconds <= 1) {
        clearInterval(interval);
        onUpdateTimer({
          ...timer,
          remainingSeconds: 0,
        });
        playChime();
      } else {
        onUpdateTimer({
          ...timer,
          remainingSeconds: timer.remainingSeconds - 1,
        });
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [timer, isRunning, onUpdateTimer]);

  if (!timer.active) return null;

  const minutes = Math.floor(timer.remainingSeconds / 60);
  const seconds = timer.remainingSeconds % 60;
  const formattedTime = `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;

  const progress =
    timer.totalSeconds > 0
      ? ((timer.totalSeconds - timer.remainingSeconds) / timer.totalSeconds) * 100
      : 100;

  const isCompleted = timer.remainingSeconds === 0;

  return (
    <div
      id="timer-hud-container"
      className="w-full bg-[#0a1e30] border border-[#1b4366] rounded-2xl p-4 mb-4 flex items-center justify-between gap-4 shadow-xl"
    >
      <div className="flex items-center gap-3">
        <div className="relative w-12 h-12 flex items-center justify-center">
          <svg className="w-12 h-12 transform -rotate-90">
            <circle
              cx="24"
              cy="24"
              r="20"
              stroke="#132d47"
              strokeWidth="4"
              fill="transparent"
            />
            <circle
              cx="24"
              cy="24"
              r="20"
              stroke={isCompleted ? "#8BE9D7" : "#62e6ff"}
              strokeWidth="4"
              fill="transparent"
              strokeDasharray={125.6}
              strokeDashoffset={125.6 - (125.6 * progress) / 100}
              strokeLinecap="round"
              className="transition-all duration-1000 ease-linear"
            />
          </svg>
          <div className="absolute inset-0 flex items-center justify-center">
            {isCompleted ? (
              <CheckCircle className="w-5 h-5 text-[#8BE9D7]" />
            ) : (
              <Timer className="w-5 h-5 text-[#62e6ff]" />
            )}
          </div>
        </div>

        <div>
          <div className="text-[11px] font-hud text-[#62e6ff] uppercase tracking-wider">
            {isCompleted ? "TIMER FINISHED" : timer.label || "FOCUS TIMER"}
          </div>
          <div className="text-2xl font-hud font-bold text-white tracking-wider">
            {formattedTime}
          </div>
        </div>
      </div>

      <div className="flex items-center gap-2">
        {!isCompleted && (
          <button
            type="button"
            onClick={() => setIsRunning(!isRunning)}
            className="p-2 rounded-xl bg-[#143350] hover:bg-[#1d476f] text-[#62e6ff] transition-colors"
            title={isRunning ? "Pause" : "Resume"}
          >
            {isRunning ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
          </button>
        )}
        <button
          type="button"
          onClick={() => {
            onUpdateTimer({
              ...timer,
              remainingSeconds: timer.totalSeconds,
            });
            setIsRunning(true);
          }}
          className="p-2 rounded-xl bg-[#143350] hover:bg-[#1d476f] text-[#b4d2ed] transition-colors"
          title="Reset timer"
        >
          <RotateCcw className="w-4 h-4" />
        </button>
        <button
          type="button"
          onClick={onDismiss}
          className="p-2 rounded-xl bg-[#143350] hover:bg-[#ff5555]/20 text-[#8AA4BC] hover:text-[#ff9c88] transition-colors"
          title="Close timer"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
