import { useEffect, useState } from "react";
import { getSummary } from "../api.js";

export default function Summary({ refreshKey }) {
  const [category, setCategory] = useState("");
  const [data, setData] = useState({});
  const [error, setError] = useState("");

  useEffect(() => {
    const t = setTimeout(() => {
      getSummary(category.trim()).then((d) => { setData(d); setError(""); }).catch((e) => setError(e.message));
    }, 300);
    return () => clearTimeout(t);
  }, [category, refreshKey]);

  const entries = Object.entries(data);
  return (
    <section className="card">
      <div className="toolbar">
        <h2>Summary</h2>
        <input placeholder="Filter by category" value={category} onChange={(e) => setCategory(e.target.value)} />
      </div>
      {error && <p className="error">{error}</p>}
      <div className="stats">
        {entries.length === 0 && !error && <p className="note">No events yet.</p>}
        {entries.map(([k, v]) => (
          <div key={k} className="stat"><span>{k}</span><strong>{v}</strong></div>
        ))}
      </div>
    </section>
  );
}
