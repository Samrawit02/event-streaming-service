export default function EventTable({ events, empty = "No events." }) {
  if (!events.length) return <p className="note">{empty}</p>;
  return (
    <div className="scroll">
      <table>
        <thead>
          <tr><th>ID</th><th>User</th><th>Type</th><th>Product</th><th>Category</th><th>Time</th></tr>
        </thead>
        <tbody>
          {events.map((e) => (
            <tr key={e.id}>
              <td>{e.id}</td>
              <td>{e.userId}</td>
              <td><span className={`tag tag-${e.eventType}`}>{e.eventType}</span></td>
              <td>{e.productId ?? "–"}</td>
              <td>{e.category ?? "–"}</td>
              <td>{e.timestamp?.replace("T", " ")}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
