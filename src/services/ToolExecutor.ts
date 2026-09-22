import { ConfirmationRequest, Route } from "../types";

export interface ToolExecutionResult {
  message: string;
  handled: boolean;
  openSettings?: boolean;
  startTimerMinutes?: number;
}

export class ToolExecutor {
  private static readonly WEB_APP_URLS: Record<string, string> = {
    youtube: "https://www.youtube.com",
    whatsapp: "https://web.whatsapp.com",
    instagram: "https://www.instagram.com",
    spotify: "https://open.spotify.com",
    chrome: "https://www.google.com",
    browser: "https://www.google.com",
    maps: "https://maps.google.com",
    telegram: "https://web.telegram.org",
    gmail: "https://mail.google.com",
    github: "https://github.com",
    twitter: "https://x.com",
    x: "https://x.com",
    reddit: "https://www.reddit.com",
    chatgpt: "https://chat.openai.com",
  };

  public static execute(
    route: Route,
    confirm: (request: ConfirmationRequest) => void,
    onComplete: (result: ToolExecutionResult) => void
  ): void {
    switch (route.type) {
      case "GREETING":
        onComplete({
          message: "Hello Boss! Jarvis online hai. Aap kya karna chahte ho?",
          handled: true,
        });
        break;

      case "OPEN_APP":
        this.handleOpenApp(route.value, onComplete);
        break;

      case "SEARCH":
        this.handleSearch(route.value, onComplete);
        break;

      case "CALL":
        this.handleCall(route.value, confirm, onComplete);
        break;

      case "MESSAGE":
        this.handleMessage(route.value, confirm, onComplete);
        break;

      case "SETTINGS":
        onComplete({
          message: "Settings open kar diya, Boss.",
          handled: true,
          openSettings: true,
        });
        break;

      case "TIMER":
        this.handleTimer(route.value, onComplete);
        break;

      default:
        onComplete({
          message: "Boss, is action ko handle nahi kar paaya.",
          handled: false,
        });
        break;
    }
  }

  private static handleOpenApp(name: string, onComplete: (res: ToolExecutionResult) => void): void {
    const lower = name.toLowerCase();
    const matchKey = Object.keys(this.WEB_APP_URLS).find((key) => lower.includes(key));

    if (lower.includes("settings")) {
      onComplete({
        message: "Settings open kar diya, Boss.",
        handled: true,
        openSettings: true,
      });
      return;
    }

    if (matchKey) {
      const url = this.WEB_APP_URLS[matchKey];
      window.open(url, "_blank", "noopener,noreferrer");
      onComplete({
        message: `${matchKey.toUpperCase()} open kar diya, Boss.`,
        handled: true,
      });
      return;
    }

    // Fallback web search
    const searchUrl = `https://www.google.com/search?q=${encodeURIComponent(name)}`;
    window.open(searchUrl, "_blank", "noopener,noreferrer");
    onComplete({
      message: `App nahi mila, "${name}" ke liye web search khol diya, Boss.`,
      handled: true,
    });
  }

  private static handleSearch(query: string, onComplete: (res: ToolExecutionResult) => void): void {
    const searchUrl = `https://www.google.com/search?q=${encodeURIComponent(query)}`;
    window.open(searchUrl, "_blank", "noopener,noreferrer");
    onComplete({
      message: "Web search khol diya, Boss.",
      handled: true,
    });
  }

  private static handleCall(
    target: string,
    confirm: (request: ConfirmationRequest) => void,
    onComplete: (res: ToolExecutionResult) => void
  ): void {
    const number = target.replace(/[^0-9+]/g, "");
    if (number.length < 7) {
      onComplete({
        message: "Boss, valid phone number format use kijiye: call 9876543210",
        handled: true,
      });
      return;
    }

    confirm({
      title: `Call ${number}?`,
      message: "Dialer open hoga. Final call aap manually confirm karoge.",
      confirmText: "Call Now",
      action: () => {
        window.location.href = `tel:${number}`;
        onComplete({
          message: `Dialer ready hai, Boss. Calling ${number}.`,
          handled: true,
        });
      },
    });
  }

  private static handleMessage(
    target: string,
    confirm: (request: ConfirmationRequest) => void,
    onComplete: (res: ToolExecutionResult) => void
  ): void {
    const parts = target.trim().split(/\s+/);
    const rawNumber = parts[0] || "";
    const number = rawNumber.replace(/[^0-9+]/g, "");

    if (number.length < 7) {
      onComplete({
        message: "Format: message 9876543210 hello Boss",
        handled: true,
      });
      return;
    }

    const body = parts.slice(1).join(" ").trim() || "Hello Boss!";

    confirm({
      title: `Message ${number}?`,
      message: `SMS composer khulega: "${body}". Send karne se pehle aap confirm karoge.`,
      confirmText: "Open SMS",
      action: () => {
        window.location.href = `sms:${number}?body=${encodeURIComponent(body)}`;
        onComplete({
          message: `SMS draft ready hai, Boss.`,
          handled: true,
        });
      },
    });
  }

  private static handleTimer(text: string, onComplete: (res: ToolExecutionResult) => void): void {
    const numMatch = text.match(/(\d+)/);
    const minutes = numMatch ? parseInt(numMatch[1], 10) : 5;
    const isSeconds = text.toLowerCase().includes("sec");

    const calculatedMins = isSeconds ? Math.max(1, Math.round(minutes / 60)) : minutes;

    onComplete({
      message: `Timer set hai ${minutes} ${isSeconds ? "seconds" : "minute"} ke liye, Boss.`,
      handled: true,
      startTimerMinutes: isSeconds ? minutes / 60 : minutes,
    });
  }
}
