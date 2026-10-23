import { useEffect, useState } from "react";
import { getRecent } from "../api.js";
import EventTable from "./EventTable.jsx";

// The backend has no push channel, so we poll /events/recent.
export default function RecentEvents({ refreshKey, onNew }) {
  const [events, setEvents] = useState([]);
  const [live, setLive] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let lastTop = null;
    async function load() {
      try {
        const d = await getRecent();
        setEvents(d);
        setError("");
        if (lastTop !== null && d[0]?.id !== lastTop) onNew?.();
        lastTop = d[0]?.id ?? null;
      } catch (e) { setError(e.message); }
    }
    load();
    if (!live) return;
    const id = setInterval(load, 3000);
    return () => clearInterval(id);
  }, [live, refreshKey]);

  return (
    <section className="card">
      <div className="toolbar">
        <h2>Recent events</h2>
        <span className={`status ${live && !error ? "ok" : ""}`}>{error ? "error" : live ? "live · polling 3s" : "paused"}</span>
        <button onClick={() => setLive(!live)}>{live ? "Pause" : "Resume"}</button>
      </div>
      {error && <p className="error">{error}</p>}
      <EventTable events={events} empty="No recent events." />
    </section>
  );
}
