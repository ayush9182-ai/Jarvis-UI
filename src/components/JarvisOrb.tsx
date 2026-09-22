import React, { useEffect, useRef } from "react";
import { AssistantStatus } from "../types";

interface JarvisOrbProps {
  status: AssistantStatus;
  onClick?: () => void;
}

export const JarvisOrb: React.FC<JarvisOrbProps> = ({ status, onClick }) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let animationFrameId: number;
    let rotation = 0;
    let pulsePhase = 0;

    const render = () => {
      const width = canvas.width;
      const height = canvas.height;
      ctx.clearRect(0, 0, width, height);

      const cx = width / 2;
      const cy = height / 2;
      const baseRadius = Math.min(width, height) * 0.36;

      // Dynamic rotation speed based on status
      let rotationSpeed = 0.015;
      if (status === "executing") rotationSpeed = 0.05;
      if (status === "listening") rotationSpeed = 0.03;
      if (status === "speaking") rotationSpeed = 0.025;

      rotation = (rotation + rotationSpeed) % (Math.PI * 2);
      pulsePhase = (pulsePhase + 0.05) % (Math.PI * 2);

      const pulseScale =
        status === "listening"
          ? 1 + Math.sin(pulsePhase * 2) * 0.06
          : status === "speaking"
          ? 1 + Math.sin(pulsePhase * 1.5) * 0.04
          : 1 + Math.sin(pulsePhase * 0.5) * 0.02;

      const radius = baseRadius * pulseScale;

      // 1. Outer Glow / Radial Background (matches RadialGradient in JarvisOrbView.kt)
      const grad = ctx.createRadialGradient(cx, cy, 0, cx, cy, radius * 1.45);
      grad.addColorStop(0, "rgba(25, 101, 137, 0.45)");
      grad.addColorStop(0.55, "rgba(7, 29, 45, 0.7)");
      grad.addColorStop(1, "rgba(6, 16, 29, 0)");
      ctx.fillStyle = grad;
      ctx.beginPath();
      ctx.arc(cx, cy, radius * 1.45, 0, Math.PI * 2);
      ctx.fill();

      // Audio ripples when listening or speaking
      if (status === "listening" || status === "speaking") {
        const rippleCount = 3;
        for (let i = 0; i < rippleCount; i++) {
          const ripplePhase = (pulsePhase + (i * Math.PI * 2) / rippleCount) % (Math.PI * 2);
          const rippleRadius = radius * (1.1 + (ripplePhase / (Math.PI * 2)) * 0.45);
          const opacity = (1 - ripplePhase / (Math.PI * 2)) * 0.4;
          ctx.strokeStyle =
            status === "listening"
              ? `rgba(98, 230, 255, ${opacity})`
              : `rgba(139, 233, 215, ${opacity})`;
          ctx.lineWidth = 1.5;
          ctx.beginPath();
          ctx.arc(cx, cy, rippleRadius, 0, Math.PI * 2);
          ctx.stroke();
        }
      }

      // 2. Primary Outer Ring (Color: #62E6FF)
      ctx.strokeStyle = "#62E6FF";
      ctx.lineWidth = 2.5;
      ctx.shadowColor = "#62E6FF";
      ctx.shadowBlur = status === "idle" ? 8 : 18;
      ctx.beginPath();
      ctx.arc(cx, cy, radius, 0, Math.PI * 2);
      ctx.stroke();
      ctx.shadowBlur = 0;

      // 3. Secondary Inner Ring (Color: #BDF7FF opacity ~0.65)
      ctx.strokeStyle = "rgba(189, 247, 255, 0.65)";
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.arc(cx, cy, radius * 0.76, 0, Math.PI * 2);
      ctx.stroke();

      // 4. Rotating Outer Arc and 12 Satellite Radar Ticks
      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(rotation);

      // Arc: 94 degrees (1.64 radians)
      ctx.strokeStyle = "rgba(98, 230, 255, 0.85)";
      ctx.lineWidth = 3.5;
      ctx.lineCap = "round";
      ctx.beginPath();
      ctx.arc(0, 0, radius * 1.18, 0.2, 0.2 + 1.64);
      ctx.stroke();

      // Secondary counter-arc for high-tech HUD look
      ctx.strokeStyle = "rgba(139, 233, 215, 0.6)";
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(0, 0, radius * 1.25, Math.PI + 0.3, Math.PI + 1.2);
      ctx.stroke();

      // 12 Satellite Radar Dots (Color: #E2FF78 / Lime Mint)
      ctx.fillStyle = "#E2FF78";
      for (let i = 0; i < 12; i++) {
        const angle = (i * 30 * Math.PI) / 180;
        const dotX = Math.cos(angle) * (radius * 1.08);
        const dotY = Math.sin(angle) * (radius * 1.08);
        ctx.beginPath();
        ctx.arc(dotX, dotY, 3.2, 0, Math.PI * 2);
        ctx.fill();
      }
      ctx.restore();

      // 5. Central glowing core
      const coreGrad = ctx.createRadialGradient(cx, cy, 0, cx, cy, radius * 0.5);
      coreGrad.addColorStop(0, "rgba(98, 230, 255, 0.3)");
      coreGrad.addColorStop(1, "rgba(6, 16, 29, 0.8)");
      ctx.fillStyle = coreGrad;
      ctx.beginPath();
      ctx.arc(cx, cy, radius * 0.5, 0, Math.PI * 2);
      ctx.fill();

      // 6. Central Sci-Fi "J" Glyph
      ctx.fillStyle = "#DDF9FF";
      ctx.shadowColor = "#62E6FF";
      ctx.shadowBlur = 12;
      ctx.font = `bold ${Math.round(radius * 0.68)}px 'Chakra Petch', sans-serif, system-ui`;
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillText("J", cx, cy + radius * 0.04);
      ctx.shadowBlur = 0;

      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      cancelAnimationFrame(animationFrameId);
    };
  }, [status]);

  return (
    <div
      id="jarvis-orb-container"
      className="relative flex flex-col items-center justify-center cursor-pointer select-none group"
      onClick={onClick}
      title="Click to toggle voice listening"
    >
      <div className="relative w-64 h-64 sm:w-72 sm:h-72 flex items-center justify-center">
        <canvas
          ref={canvasRef}
          width={300}
          height={300}
          className="w-full h-full transform transition-transform duration-300 group-hover:scale-105"
        />
      </div>
    </div>
  );
};
