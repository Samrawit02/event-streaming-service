// Matches the Spring Boot backend:
//   POST /auth/login, /auth/register
//   POST /events (single object or array) | GET /events (search) | GET /events/recent | GET /events/summary
const BASE = "/api";
export const EVENT_TYPES = ["VIEW", "ADD_TO_CART", "PURCHASE"];

const KEY = "es_auth";
export const getAuth = () => { try { return JSON.parse(localStorage.getItem(KEY)); } catch { return null; } };
export const setAuth = (a) => (a ? localStorage.setItem(KEY, JSON.stringify(a)) : localStorage.removeItem(KEY));

let onUnauthorized = () => {};
export const setUnauthorizedHandler = (fn) => (onUnauthorized = fn);

async function request(path, { method = "GET", body, params } = {}) {
  const qs = params
    ? "?" + new URLSearchParams(Object.entries(params).filter(([, v]) => v !== "" && v != null)).toString()
    : "";
  const auth = getAuth();
  const res = await fetch(`${BASE}${path}${qs}`, {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(auth?.token ? { Authorization: `Bearer ${auth.token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 401 && !path.startsWith("/auth")) onUnauthorized();
  const data = await res.json().catch(() => null);
  if (!res.ok) throw new Error(data?.message || data?.error || `Request failed (${res.status})`);
  return { data, cache: res.headers.get("X-Cache") };
}

export const login = (username, password) => request("/auth/login", { method: "POST", body: { username, password } }).then((r) => r.data);
export const register = (username, password) => request("/auth/register", { method: "POST", body: { username, password } }).then((r) => r.data);
export const getRecent = () => request("/events/recent").then((r) => r.data);
export const getSummary = (category) => request("/events/summary", { params: { category } }).then((r) => r.data);
export const searchEvents = (params) => request("/events", { params });
export const createEvent = (evt) => request("/events", { method: "POST", body: evt }).then((r) => r.data);

// Backend expects yyyy-MM-ddTHH:mm:ss
export const toBackendTime = (d) => (d ? new Date(d).toISOString().slice(0, 19) : undefined);
