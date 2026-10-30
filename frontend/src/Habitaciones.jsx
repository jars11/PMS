import { useState } from 'react';
import { api } from './api';

// Mismos valores y orden que el enum AtributoHabitacion del backend.
const ATRIBUTOS = ['Balcon', 'VistaAlMar', 'Jacuzzi', 'Twin', 'DosAmbientes'];

function formInicial() {
  return {
    numero: '',
    camasMatrimoniales: '1',
    camasIndividuales: '0',
    cunas: '0',
    colchones: '0',
    tarifaPorNoche: '',
    atributos: [],
  };
}

/** Distribución de camas en formato compacto: "2 mat · 1 ind · 0 cunas · 0 colchones". */
function distribucionCamas(h) {
  return [
    `${h.camasMatrimoniales ?? 0} mat`,
    `${h.camasIndividuales ?? 0} ind`,
    `${h.cunas ?? 0} cunas`,
    `${h.colchones ?? 0} colchones`,
  ].join(' · ');
}

/**
 * Lista de ids con `arrastrado` movido a la posición de `destino`. Devuelve
 * null cuando el movimiento no cambia nada (así el llamador no llama a la API).
 */
function mover(ids, arrastrado, destino) {
  const desde = ids.indexOf(arrastrado);
  const hasta = ids.indexOf(destino);
  if (desde < 0 || hasta < 0 || desde === hasta) return null;
  const copia = [...ids];
  copia.splice(desde, 1);
  copia.splice(hasta, 0, arrastrado);
  return copia;
}

/**
 * Sección admin "Habitaciones": lista reordenable por drag & drop (el orden
 * ascendente de filas es la prioridad de asignación, con la primera arriba) y
 * formulario de alta. `habitaciones` ya llega ordenada por prioridad y
 * `onCambio` la vuelve a pedir a la API después de crear o reordenar.
 */
export default function Habitaciones({ habitaciones, onCambio }) {
  const [form, setForm] = useState(formInicial);
  const [errorAlta, setErrorAlta] = useState('');
  const [guardando, setGuardando] = useState(false);
  // Orden optimista mientras el backend confirma: null = el orden del padre.
  const [ordenLocal, setOrdenLocal] = useState(null);
  const [arrastrando, setArrastrando] = useState(null);
  const [sobre, setSobre] = useState(null);
  const [errorOrden, setErrorOrden] = useState('');
  const [reordenando, setReordenando] = useState(false);

  const ordenadas = ordenLocal
    ? ordenLocal.map((id) => habitaciones.find((h) => h.id === id)).filter(Boolean)
    : habitaciones;

  function campo(clave) {
    return (e) => setForm((f) => ({ ...f, [clave]: e.target.value }));
  }

  function alternarAtributo(atributo) {
    setForm((f) => ({
      ...f,
      atributos: f.atributos.includes(atributo)
        ? f.atributos.filter((a) => a !== atributo)
        : [...f.atributos, atributo],
    }));
  }

  async function soltar(destinoId) {
    const arrastrado = arrastrando;
    setArrastrando(null);
    setSobre(null);
    if (arrastrado == null) return;

    const ids = mover(
      ordenadas.map((h) => h.id),
      arrastrado,
      destinoId,
    );
    if (!ids) return;

    // Optimista: la fila se mueve ya y, si el backend rechaza, el orden vuelve
    // al del padre (que sigue siendo el viejo) al limpiar ordenLocal.
    setOrdenLocal(ids);
    setReordenando(true);
    setErrorOrden('');
    try {
      await api.reordenarHabitaciones(ids);
      await onCambio();
    } catch (err) {
      setErrorOrden(err.message);
    } finally {
      setOrdenLocal(null);
      setReordenando(false);
    }
  }

  async function agregar(e) {
    e.preventDefault();
    setErrorAlta('');
    setGuardando(true);
    try {
      await api.crearHabitacion({
        numero: Number(form.numero),
        atributos: form.atributos,
        camasMatrimoniales: Number(form.camasMatrimoniales),
        camasIndividuales: Number(form.camasIndividuales),
        cunas: Number(form.cunas),
        colchones: Number(form.colchones),
        tarifaPorNoche: Number(form.tarifaPorNoche),
      });
      setForm(formInicial());
      await onCambio();
    } catch (err) {
      setErrorAlta(err.message);
    } finally {
      setGuardando(false);
    }
  }

  return (
    <div className="habitaciones">
      <section className="habitaciones-lista-wrap">
        <h2>Habitaciones</h2>
        <p className="habitaciones-ayuda">
          Arrastrá las filas para cambiar el orden. El orden ascendente es la prioridad: la primera
          fila tiene prioridad sobre las de abajo cuando varias pueden entrar en la misma propuesta.
        </p>

        {errorOrden && <div className="error habitaciones-error">{errorOrden}</div>}

        <ul className="habitaciones-lista">
          {ordenadas.map((h, i) => (
            <li
              key={h.id}
              className={
                'habitacion-fila' +
                (arrastrando === h.id ? ' arrastrando' : '') +
                (sobre === h.id && arrastrando != null && arrastrando !== h.id ? ' sobre' : '')
              }
              draggable
              onDragStart={(e) => {
                setArrastrando(h.id);
                // Firefox no inicia el arrastre sin datos en el dataTransfer.
                e.dataTransfer.setData('text/plain', String(h.id));
                e.dataTransfer.effectAllowed = 'move';
              }}
              onDragOver={(e) => {
                if (arrastrando == null || arrastrando === h.id) return;
                e.preventDefault();
                e.dataTransfer.dropEffect = 'move';
                if (sobre !== h.id) setSobre(h.id);
              }}
              onDragLeave={() => setSobre((previo) => (previo === h.id ? null : previo))}
              onDrop={(e) => {
                e.preventDefault();
                soltar(h.id);
              }}
              onDragEnd={() => {
                setArrastrando(null);
                setSobre(null);
              }}
            >
              <span className="habitacion-grip" aria-hidden="true">
                ⠿
              </span>
              <span className="habitacion-prioridad">{i + 1}</span>
              <span className="habitacion-numero">Hab. {h.numero}</span>
              <span className="habitacion-camas">{distribucionCamas(h)}</span>
              <span className="habitacion-atributos">
                {(h.atributos || []).join(', ') || '—'}
              </span>
            </li>
          ))}
          {ordenadas.length === 0 && (
            <li className="habitacion-vacio">No hay habitaciones.</li>
          )}
        </ul>
        {reordenando && <div className="habitaciones-estado">Guardando orden…</div>}
      </section>

      <form className="habitaciones-form" onSubmit={agregar}>
        <h2>Agregar habitación</h2>

        <div className="fila">
          <label>
            Número
            <input
              type="number"
              min="1"
              value={form.numero}
              onChange={campo('numero')}
            />
          </label>
          <label>
            Tarifa por noche
            <input
              type="number"
              min="0"
              step="0.01"
              value={form.tarifaPorNoche}
              onChange={campo('tarifaPorNoche')}
            />
          </label>
        </div>

        <div className="fila">
          <label>
            Matrimoniales
            <input
              type="number"
              min="0"
              value={form.camasMatrimoniales}
              onChange={campo('camasMatrimoniales')}
            />
          </label>
          <label>
            Individuales
            <input
              type="number"
              min="0"
              value={form.camasIndividuales}
              onChange={campo('camasIndividuales')}
            />
          </label>
          <label>
            Cunas
            <input type="number" min="0" value={form.cunas} onChange={campo('cunas')} />
          </label>
          <label>
            Colchones
            <input
              type="number"
              min="0"
              value={form.colchones}
              onChange={campo('colchones')}
            />
          </label>
        </div>

        <fieldset className="habitaciones-atributos">
          <legend>Atributos</legend>
          {ATRIBUTOS.map((atributo) => (
            <label key={atributo} className="check">
              <input
                type="checkbox"
                checked={form.atributos.includes(atributo)}
                onChange={() => alternarAtributo(atributo)}
              />
              {atributo}
            </label>
          ))}
        </fieldset>

        {errorAlta && <div className="error habitaciones-error">{errorAlta}</div>}

        <div className="acciones">
          <button type="submit" disabled={guardando}>
            {guardando ? 'Guardando…' : 'Agregar'}
          </button>
        </div>
      </form>
    </div>
  );
}
