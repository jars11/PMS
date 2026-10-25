import { useCallback, useEffect, useState } from 'react';
import Login from './Login';
import ReservasLista from './ReservasLista';
import Calendario from './Calendario';
import ReservaForm from './ReservaForm';
import { api } from './api';
import './App.css';

export default function App() {
  const [sesion, setSesion] = useState(null);
  const [pestana, setPestana] = useState('lista');
  const [habitaciones, setHabitaciones] = useState([]);
  const [titulares, setTitulares] = useState([]);
  const [reservas, setReservas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [formAbierto, setFormAbierto] = useState(false);
  const [reservaEnEdicion, setReservaEnEdicion] = useState(null);

  const cargarDatos = useCallback(async () => {
    setError('');
    try {
      const [habs, tits, res] = await Promise.all([
        api.habitaciones(),
        api.titulares(),
        api.reservas(),
      ]);
      setHabitaciones(habs);
      setTitulares(tits);
      setReservas(res);
    } catch (err) {
      setError(err.message);
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => {
    const guardada = localStorage.getItem('pms.sesion');
    if (guardada) setSesion(JSON.parse(guardada));
  }, []);

  useEffect(() => {
    if (sesion) cargarDatos();
  }, [sesion, cargarDatos]);

  function salir() {
    localStorage.removeItem('pms.sesion');
    setSesion(null);
  }

  async function guardar(reservaOriginal, payload) {
    if (reservaOriginal) {
      await api.editarReserva(reservaOriginal.id, payload);
    } else {
      await api.crearReserva(payload);
    }
    setFormAbierto(false);
    setReservaEnEdicion(null);
    await cargarDatos();
  }

  if (!sesion) {
    return <Login onLogin={setSesion} />;
  }

  return (
    <div className="app">
      <header className="topbar">
        <h1>PMS — Web</h1>
        <div className="topbar-der">
          <span className="rol">
            {sesion.rol} {sesion.admin ? '(admin)' : '(recepcionista)'}
          </span>
          <button className="secundario" onClick={salir}>
            Salir
          </button>
        </div>
      </header>

      <nav className="tabs">
        <button className={pestana === 'lista' ? 'activo' : ''} onClick={() => setPestana('lista')}>
          Reservas
        </button>
        <button
          className={pestana === 'calendario' ? 'activo' : ''}
          onClick={() => setPestana('calendario')}
        >
          Calendario
        </button>
        <button className="nueva" onClick={() => { setReservaEnEdicion(null); setFormAbierto(true); }}>
          + Nueva reserva
        </button>
      </nav>

      {error && <div className="error banner">{error}</div>}
      {cargando && <div className="cargando">Cargando…</div>}

      {!cargando && pestana === 'lista' && (
        <ReservasLista
          reservas={reservas}
          onEditar={(r) => { setReservaEnEdicion(r); setFormAbierto(true); }}
        />
      )}

      {!cargando && pestana === 'calendario' && (
        <Calendario habitaciones={habitaciones} reservas={reservas} />
      )}

      {formAbierto && (
        <ReservaForm
          titulares={titulares}
          habitaciones={habitaciones}
          reserva={reservaEnEdicion}
          onCancelar={() => { setFormAbierto(false); setReservaEnEdicion(null); }}
          onGuardado={guardar}
        />
      )}
    </div>
  );
}
