import { IntentType, Route } from "../types";

export class IntentRouter {
  public static route(command: string): Route {
    const c = command.trim();
    const l = c.toLowerCase();

    // Greeting patterns (English & Hindi)
    if (/^(hello|hi|hlo|helo|hey|namaste|yo|sup)(\s+jarvis)?[.!?]*$/i.test(l)) {
      return { type: IntentType.GREETING, value: "", originalCommand: c };
    }

    // App launch
    if (l.startsWith("open ") || l.startsWith("launch ")) {
      const target = c.replace(/^(open|launch)\s+/i, "").trim();
      return { type: IntentType.OPEN_APP, value: target, originalCommand: c };
    }

    // Web search
    if (l.startsWith("search ") || l.startsWith("find ") || l.startsWith("google ")) {
      const query = c.replace(/^(search|find|google)\s+/i, "").trim();
      return { type: IntentType.SEARCH, value: query, originalCommand: c };
    }

    // Phone call
    if (l.startsWith("call ") || l.startsWith("dial ")) {
      const target = c.replace(/^(call|dial)\s+/i, "").trim();
      return { type: IntentType.CALL, value: target, originalCommand: c };
    }

    // SMS / Message
    if (l.startsWith("message ") || l.startsWith("sms ") || l.startsWith("text ")) {
      const target = c.replace(/^(message|sms|text)\s+/i, "").trim();
      return { type: IntentType.MESSAGE, value: target, originalCommand: c };
    }

    // Settings
    if (l.includes("settings")) {
      return { type: IntentType.SETTINGS, value: "", originalCommand: c };
    }

    // Focus timer
    if (l.includes("timer") || /.*\d+\s*(minute|min|m|second|sec|s).*/i.test(l)) {
      return { type: IntentType.TIMER, value: c, originalCommand: c };
    }

    // Default to conversational Gemini AI
    return { type: IntentType.CHAT, value: c, originalCommand: c };
  }
}
