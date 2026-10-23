import { useState } from "react";
import { createEvent, EVENT_TYPES } from "../api.js";

export default function PublishForm({ defaultUserId, onCreated }) {
  const [f, setF] = useState({ userId: defaultUserId ?? "", eventType: "VIEW", productId: "", category: "" });
  const [msg, setMsg] = useState({ text: "", error: false });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  async function submit(e) {
    e.preventDefault();
    try {
      const res = await createEvent({
        userId: Number(f.userId),
        eventType: f.eventType,
        productId: f.productId ? Number(f.productId) : null,
        category: f.category || null,
        // timestamp omitted: backend decides
      });
      setMsg({ text: `Created event #${res.id}`, error: false });
      onCreated?.();
    } catch (err) {
      setMsg({ text: err.message, error: true });
    }
  }

  return (
    <section className="card">
      <h2>Publish event</h2>
      <form className="col" onSubmit={submit}>
        <input type="number" placeholder="User ID *" value={f.userId} onChange={set("userId")} required />
        <select value={f.eventType} onChange={set("eventType")}>
          {EVENT_TYPES.map((t) => <option key={t}>{t}</option>)}
        </select>
        <input type="number" placeholder="Product ID" value={f.productId} onChange={set("productId")} />
        <input placeholder="Category" value={f.category} onChange={set("category")} />
        <button className="primary">Publish</button>
      </form>
      {msg.text && <p className={msg.error ? "error" : "note"}>{msg.text}</p>}
    </section>
  );
}
