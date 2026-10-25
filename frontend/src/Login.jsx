import { useState } from 'react';
import { api } from './api';

export default function Login({ onLogin }) {
  const [usuario, setUsuario] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);

  async function manejarSubmit(e) {
    e.preventDefault();
    setError('');
    setCargando(true);
    try {
      const sesion = await api.login(usuario, password);
      localStorage.setItem('pms.sesion', JSON.stringify(sesion));
      onLogin(sesion);
    } catch (err) {
      setError(err.message);
    } finally {
      setCargando(false);
    }
  }

  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={manejarSubmit}>
        <h1>PMS — Web</h1>
        <p className="login-hint">PoC de acceso. Usuarios: admin/a o recepcionista/r.</p>
        <label>
          Usuario
          <input
            value={usuario}
            onChange={(e) => setUsuario(e.target.value)}
            autoFocus
            placeholder="admin"
          />
        </label>
        <label>
          Contraseña
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••"
          />
        </label>
        {error && <div className="error">{error}</div>}
        <button type="submit" disabled={cargando}>
          {cargando ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>
    </div>
  );
}
