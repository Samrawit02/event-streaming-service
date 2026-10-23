# Event Streaming Service: Frontend

React + Vite dashboard for the Spring Boot backend (`com.challenge.eventstream`).

## Run
1. Start the backend (default http://localhost:8080; needs PostgreSQL per `application.yml`).
2. `npm install && npm run dev`  → http://localhost:5173

Dev requests go to `/api/*`, which Vite proxies to the backend with the `/api` prefix stripped.
(The backend has `cors` disabled, so the proxy is required in dev.)
Use `VITE_BACKEND=http://host:port npm run dev` to point at a different backend.

## Login
The backend seeds `admin` / `password123` on first start (DataInitializer). You can also register.

## Features
- JWT login/register (token kept in localStorage, auto-logout on 401)
- Summary counts (optionally by category)
- Recent events, polled every 3s (the backend has no SSE/WebSocket)
- Search by user, type, category, product, date range, with X-Cache HIT/MISS shown
- Publish a single event
