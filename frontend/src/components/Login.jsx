import { useState } from "react";
import { login, register, setAuth } from "../api.js";

export default function Login({ onLogin }) {
  const [mode, setMode] = useState("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      if (mode === "register") await register(username, password);
      const auth = await login(username, password);
      setAuth(auth);
      onLogin(auth);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login">
      <form className="card" onSubmit={submit}>
        <h1>Event Streaming Service</h1>
        <input placeholder="Username" value={username} onChange={(e) => setUsername(e.target.value)} required />
        <input type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        <button className="primary" disabled={busy}>{mode === "login" ? "Sign in" : "Create account"}</button>
        {error && <p className="error">{error}</p>}
        <p className="note">
          {mode === "login" ? "No account?" : "Have an account?"}{" "}
          <a href="#" onClick={(e) => { e.preventDefault(); setMode(mode === "login" ? "register" : "login"); }}>
            {mode === "login" ? "Register" : "Sign in"}
          </a>
        </p>
      </form>
    </div>
  );
}
