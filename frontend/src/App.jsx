import { useEffect, useState } from "react";
import { getAuth, setAuth, setUnauthorizedHandler } from "./api.js";
import Login from "./components/Login.jsx";
import Summary from "./components/Summary.jsx";
import RecentEvents from "./components/RecentEvents.jsx";
import Search from "./components/Search.jsx";
import PublishForm from "./components/PublishForm.jsx";

export default function App() {
  const [auth, setAuthState] = useState(getAuth());
  const [tick, setTick] = useState(0);
  const bump = () => setTick((t) => t + 1);

  const logout = () => { setAuth(null); setAuthState(null); };
  useEffect(() => setUnauthorizedHandler(logout), []);

  if (!auth) return <Login onLogin={setAuthState} />;

  return (
    <div className="app">
      <header>
        <h1>Event Streaming Service</h1>
        <div><span className="note">{auth.username}</span> <button onClick={logout}>Log out</button></div>
      </header>
      <main>
        <div className="col">
          <Summary refreshKey={tick} />
          <RecentEvents refreshKey={tick} onNew={bump} />
          <Search />
        </div>
        <PublishForm defaultUserId={auth.userId} onCreated={bump} />
      </main>
    </div>
  );
}
