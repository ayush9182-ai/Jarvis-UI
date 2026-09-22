export enum IntentType {
  GREETING = "GREETING",
  OPEN_APP = "OPEN_APP",
  SEARCH = "SEARCH",
  CALL = "CALL",
  MESSAGE = "MESSAGE",
  SETTINGS = "SETTINGS",
  TIMER = "TIMER",
  CHAT = "CHAT",
}

export interface Route {
  type: IntentType;
  value: string;
  originalCommand: string;
}

export interface MemoryTurn {
  role: "user" | "assistant";
  text: string;
  timestamp?: number;
}

export type AssistantStatus = "idle" | "listening" | "executing" | "speaking";

export interface ConfirmationRequest {
  title: string;
  message: string;
  action: () => void;
  confirmText?: string;
  cancelText?: string;
}

export interface TimerState {
  active: boolean;
  totalSeconds: number;
  remainingSeconds: number;
  label: string;
}
