import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The Spring backend has CORS disabled, so in dev we proxy /api -> backend (prefix stripped).
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api": {
        target: process.env.VITE_BACKEND || "http://localhost:8080",
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/api/, ""),
      },
    },
  },
});
