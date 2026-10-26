import { useCallback, useEffect, useState } from 'react';
import Login from './Login';
import ReservasLista from './ReservasLista';
import Calendario from './Calendario';
import PropuestaEstadia from './PropuestaEstadia';
import SplitVertical from './SplitVertical';
import ReservaForm from './ReservaForm';
import { api } from './api';
import './App.css';

export default function App() {
  const [sesion, setSesion] = useState(null);
  const [pestana, setPestana] = useState('calendario');
  const [habitaciones, setHabitaciones] = useState([]);
  const [titulares, setTitulares] = useState([]);
  const [reservas, setReservas] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [formAbierto, setFormAbierto] = useState(false);
  const [reservaEnEdicion, setReservaEnEdicion] = useState(null);
  const [precarga, setPrecarga] = useState(null);
  const [calendarioVersion, setCalendarioVersion] = useState(0);
  // Preview gris de la propuesta elegida en el asistente de abajo.
  const [previsualizacion, setPrevisualizacion] = useState([]);

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

  const refrescarReservas = useCallback(async () => {
    try {
      setReservas(await api.reservas());
    } catch (err) {
      setError(err.message);
    }
  }, []);

  function salir() {
    localStorage.removeItem('pms.sesion');
    setSesion(null);
  }

  // El asistente se desmonta al cambiar de pestana: su preview no debe quedar.
  function cambiarPestana(destino) {
    if (destino !== 'calendario') setPrevisualizacion([]);
    setPestana(destino);
  }

  async function guardar(reservaOriginal, payload, lateCheckout) {
    const guardada = reservaOriginal
      ? await api.editarReserva(reservaOriginal.id, payload)
      : await api.crearReserva(payload);

    // Paridad con DialogoReserva.aplicarLateCheckout: en alta se persiste solo
    // si esta marcado (el default ya es false); en edicion siempre, para poder
    // destildar. Si el backend rechaza el marcado (otra reserva con check-in el
    // mismo dia en la habitacion) la reserva ya guardada NO se revierte: se
    // avisa y el checkbox vuelve destildado porque el flag quedo en false.
    let aviso = '';
    if (reservaOriginal || lateCheckout) {
      try {
        await api.marcarLateCheckout(guardada.id, lateCheckout);
      } catch (err) {
        aviso = err.message;
      }
    }

    setError(aviso);
    setFormAbierto(false);
    setReservaEnEdicion(null);
    setPrecarga(null);
    await refrescarReservas();
    setCalendarioVersion((v) => v + 1);
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
        <button
          className={pestana === 'calendario' ? 'activo' : ''}
          onClick={() => cambiarPestana('calendario')}
        >
          Calendario
        </button>
        <button className={pestana === 'lista' ? 'activo' : ''} onClick={() => cambiarPestana('lista')}>
          Reservas
        </button>
        <button className="nueva" onClick={() => { setReservaEnEdicion(null); setPrecarga(null); setFormAbierto(true); }}>
          + Nueva reserva
        </button>
      </nav>

      {error && <div className="error banner">{error}</div>}
      {cargando && <div className="cargando">Cargando…</div>}

      {!cargando && pestana === 'lista' && (
        <ReservasLista
          reservas={reservas}
          onEditar={(r) => { setReservaEnEdicion(r); setPrecarga(null); setFormAbierto(true); }}
        />
      )}

      {!cargando && pestana === 'calendario' && (
        // Paridad con RecepcionistaFrame: calendario arriba y "Propuesta de
        // estadía" abajo, en un split vertical redimensionable.
        <SplitVertical
          arriba={
            <Calendario
              habitaciones={habitaciones}
              onNuevaPrecargada={(p) => { setReservaEnEdicion(null); setPrecarga(p); setFormAbierto(true); }}
              onEditarReserva={(r) => { setReservaEnEdicion(r); setPrecarga(null); setFormAbierto(true); }}
              onCambio={refrescarReservas}
              refreshSignal={calendarioVersion}
              propuestasPreview={previsualizacion}
            />
          }
          abajo={
            <PropuestaEstadia
              titulares={titulares}
              onPrevisualizar={setPrevisualizacion}
              onCreada={() => {
                refrescarReservas();
                setCalendarioVersion((v) => v + 1);
              }}
            />
          }
        />
      )}

      {formAbierto && (
        <ReservaForm
          titulares={titulares}
          habitaciones={habitaciones}
          reserva={reservaEnEdicion}
          precarga={precarga}
          onCancelar={() => { setFormAbierto(false); setReservaEnEdicion(null); setPrecarga(null); }}
          onGuardado={guardar}
        />
      )}
    </div>
  );
}
