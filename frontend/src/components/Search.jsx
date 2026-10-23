import { useState } from "react";
import { searchEvents, toBackendTime, EVENT_TYPES } from "../api.js";
import EventTable from "./EventTable.jsx";

const blank = { userId: "", eventType: "", category: "", productId: "", from: "", to: "" };

export default function Search() {
  const [f, setF] = useState(blank);
  const [results, setResults] = useState(null);
  const [cache, setCache] = useState(null);
  const [error, setError] = useState("");
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function run(e) {
    e.preventDefault();
    try {
      const { data, cache } = await searchEvents({ ...f, from: toBackendTime(f.from), to: toBackendTime(f.to) });
      setResults(data); setCache(cache); setError("");
    } catch (err) { setError(err.message); }
  }

  return (
    <section className="card">
      <h2>Search</h2>
      <form className="grid" onSubmit={run}>
        <input placeholder="User ID" type="number" value={f.userId} onChange={set("userId")} />
        <select value={f.eventType} onChange={set("eventType")}>
          <option value="">Any type</option>
          {EVENT_TYPES.map((t) => <option key={t}>{t}</option>)}
        </select>
        <input placeholder="Category" value={f.category} onChange={set("category")} />
        <input placeholder="Product ID" type="number" value={f.productId} onChange={set("productId")} />
        <input type="datetime-local" title="From" value={f.from} onChange={set("from")} />
        <input type="datetime-local" title="To" value={f.to} onChange={set("to")} />
        <button className="primary">Search</button>
        <button type="button" onClick={() => { setF(blank); setResults(null); }}>Reset</button>
      </form>
      {error && <p className="error">{error}</p>}
      {results && (
        <>
          <p className="note">{results.length} result(s){cache && <> · cache <strong>{cache}</strong></>}</p>
          <EventTable events={results} />
        </>
      )}
    </section>
  );
}
