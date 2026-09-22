import { MemoryTurn } from "../types";

export class MemoryStore {
  private static readonly STORAGE_KEY = "jarvis_memory";
  private static readonly MAX_TURNS = 10;

  public static getTurns(): MemoryTurn[] {
    try {
      const data = localStorage.getItem(this.STORAGE_KEY);
      if (!data) return [];
      const parsed = JSON.parse(data);
      if (Array.isArray(parsed)) {
        return parsed;
      }
      return [];
    } catch {
      return [];
    }
  }

  public static remember(role: "user" | "assistant", text: string): MemoryTurn[] {
    const current = this.getTurns();
    const newTurn: MemoryTurn = {
      role,
      text: text.slice(0, 4000),
      timestamp: Date.now(),
    };
    const updated = [...current, newTurn].slice(-this.MAX_TURNS);
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(updated));
    } catch (e) {
      console.warn("Failed to persist to localStorage:", e);
    }
    return updated;
  }

  public static clear(): void {
    try {
      localStorage.removeItem(this.STORAGE_KEY);
    } catch (e) {
      console.warn("Failed to clear localStorage:", e);
    }
  }

  public static getContextText(): string {
    return this.getTurns()
      .map((t) => `${t.role}: ${t.text}`)
      .join("\n");
  }
}
