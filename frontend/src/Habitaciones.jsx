import { useLayoutEffect, useRef, useState } from 'react';
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
 * true cuando la fila en `indice` queda entre el origen y el destino del
 * arrastre, o sea cuando el arrastre le abre lugar y se tiene que correr un
 * escalón hacia arriba o hacia abajo.
 */
function seDesplaza(indice, origen, destino) {
  if (origen == null || destino == null || origen === destino) return false;
  return origen < destino
    ? indice > origen && indice <= destino
    : indice >= destino && indice < origen;
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

  // Firma del orden actual, para distinguir un cambio de orden real de un
  // simple cambio de preview.
  const firmaRef = useRef('');

  // Nodos de cada fila (id → <li>).
  const filasRef = useRef(new Map());
  // Posición base (layout, sin transform) de cada fila: id → offsetTop.
  const topsBaseRef = useRef(new Map());
  // Posición base en coordenadas de viewport, capturada al iniciar el arrastre:
  // en ese momento no hay transforms y las filas no se reordenan hasta soltar.
  const topsCursorRef = useRef(new Map());
  // Filas con un desplazamiento de preview aplicado, para poder limpiarlo.
  const desplazadasRef = useRef(new Set());

  const ordenadas = ordenLocal
    ? ordenLocal.map((id) => habitaciones.find((h) => h.id === id)).filter(Boolean)
    : habitaciones;

  const indiceDe = (id) => {
    if (id == null) return null;
    const i = ordenadas.findIndex((h) => h.id === id);
    return i < 0 ? null : i;
  };
  const indiceArrastrado = indiceDe(arrastrando);
  const indiceSobre = indiceDe(sobre);

  /**
   * Fila objetivo según la POSICIÓN del cursor, no según el elemento que recibe
   * el dragover.
   *
   * El preview mueve las filas con transform, y el transform SÍ cambia el
   * elemento que queda bajo el cursor. Si el destino se leyera del evento, cada
   * movimiento de filas cambiaba el destino, que movía las filas otra vez... y
   * la lista oscilaba sola aunque el mouse estuviera quieto.
   *
   * Se usa la geometría capturada al iniciar el arrastre (sin transforms y sin
   * transiciones en curso): leer las filas ahora devolvería posiciones a medio
   * animar y elegiría la fila vecina.
   */
  function filaBajoCursor(clientY) {
    const ids = ordenLocal ?? habitaciones.map((h) => h.id);
    const tops = topsCursorRef.current;
    let mejor = null;
    let mejorDist = Infinity;
    for (const id of ids) {
      const el = filasRef.current.get(id);
      const top = tops.get(id);
      if (!el || top == null) continue;
      const dist = Math.abs(clientY - (top + el.offsetHeight / 2));
      if (dist < mejorDist) {
        mejorDist = dist;
        mejor = id;
      }
    }
    return mejor;
  }

  /** Captura la geometría de las filas tal como están ahora (sin transforms). */
  function capturarGeometria() {
    const tops = new Map();
    for (const [id, el] of filasRef.current) tops.set(id, el.getBoundingClientRect().top);
    topsCursorRef.current = tops;
  }

  /**
   * Preview del reordenamiento: cada fila se corre a la posición que va a ocupar
   * si se suelta en la fila bajo el cursor, y la arrastrada sigue a ese destino.
   *
   * Claves para que no se trabe:
   * - Las posiciones base se leen con `offsetTop`, que NO depende del transform
   *   (leer `getBoundingClientRect()` sí lo incluiría y realimentaría el bucle).
   * - Nunca se resetea el transform de todas las filas en cada evento: solo se
   *   escribe el valor final, así el navegador interpola desde donde está.
   * - El efecto depende de valores estables, no del array `ordenadas`, que es
   *   nuevo en cada render y hacía correr la animación en cada tecla del form.
   */
  useLayoutEffect(() => {
    const filas = filasRef.current;
    const tops = new Map();
    for (const [id, el] of filas) tops.set(id, el.offsetTop);
    topsBaseRef.current = tops;

    const ids = ordenLocal ?? habitaciones.map((h) => h.id);
    const firma = ids.join(',');
    const cambioElOrden = firma !== firmaRef.current;
    firmaRef.current = firma;

    const destinoPorId = new Map();
    if (arrastrando != null && sobre != null) {
      // Se derivan los ids acá (y no del array `ordenadas`) porque ese array es
      // nuevo en cada render: usarlo como dependencia volvía a correr la
      // animación en cada render y la cortaba a mitad de camino.
      const objetivo = mover(ids, arrastrando, sobre);
      if (objetivo) {
        for (const id of ids) {
          const slot = ids[objetivo.indexOf(id)];
          const delta = (tops.get(slot) ?? 0) - (tops.get(id) ?? 0);
          if (delta) destinoPorId.set(id, delta);
        }
      }
    }

    // Filas que dejan de estar desplazadas.
    const sueltan = [...desplazadasRef.current].filter((id) => !destinoPorId.has(id));
    if (sueltan.length > 0) {
      // Al soltar, el orden del DOM ya cambió: la fila está en su lugar final y
      // su transform era relativo a la posición VIEJA. Si se limpia con
      // transición, el navegador interpola el transform y la fila se desliza
      // 42px de más (eso era el "se mueven las demás" al soltar). Limpiarlo sin
      // transición la deja instantáneamente donde el preview ya la mostraba.
      for (const id of sueltan) {
        const el = filas.get(id);
        if (!el) continue;
        if (cambioElOrden) el.style.transition = 'none';
        el.style.transform = '';
      }
      if (cambioElOrden) {
        // Consolida el salto antes de devolver la transición.
        for (const [, el] of filas) void el.offsetHeight;
        for (const id of sueltan) {
          const el = filas.get(id);
          if (el) el.style.transition = '';
        }
      }
    }
    for (const [id, delta] of destinoPorId) {
      const el = filas.get(id);
      if (el) el.style.transform = `translateY(${delta}px)`;
    }
    desplazadasRef.current = new Set(destinoPorId.keys());
  }, [arrastrando, sobre, ordenLocal, habitaciones]);

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
    // al del padre (que sigue siendo el viejo) al limpiar ordenLocal. El preview
    // del arrastre ya dejó cada fila en su lugar final, así que el cambio de
    // orden no necesita un FLIP extra: al limpiar los transforms las filas
    // quedan exactamente donde se veían.
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
        <div className="habitaciones-cabecera">
          <h2>Habitaciones</h2>
          {reordenando && <span className="habitaciones-estado">Guardando orden…</span>}
        </div>
        <p className="habitaciones-ayuda">
          Arrastrá las filas para cambiar el orden. El orden ascendente es la prioridad: la primera
          fila tiene prioridad sobre las de abajo cuando varias pueden entrar en la misma propuesta.
        </p>

        <span className="habitaciones-prioridad-tope">Más prioritaria</span>

        {errorOrden && <div className="error habitaciones-error">{errorOrden}</div>}

        <ul
          className="habitaciones-lista"
          // Los eventos de arrastre se manejan en el contenedor: así el destino
          // depende solo de la posición del cursor y no de qué fila quedó
          // debajo (ver filaBajoCursor).
          onDragOver={(e) => {
            if (arrastrando == null) return;
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            const id = filaBajoCursor(e.clientY);
            if (id != null && sobre !== id) setSobre(id);
          }}
          onDrop={(e) => {
            if (arrastrando == null) return;
            e.preventDefault();
            const id = filaBajoCursor(e.clientY) ?? sobre ?? arrastrando;
            soltar(id);
          }}
          onDragEnd={() => {
            setArrastrando(null);
            setSobre(null);
          }}
        >
          {ordenadas.map((h, i) => (
            <li
              key={h.id}
              ref={(el) => {
                if (el) filasRef.current.set(h.id, el);
                else filasRef.current.delete(h.id);
              }}
              className={
                'habitacion-fila' +
                (arrastrando === h.id ? ' arrastrando' : '') +
                (sobre === h.id && arrastrando != null && arrastrando !== h.id ? ' sobre' : '') +
                (seDesplaza(i, indiceArrastrado, indiceSobre) ? ' desplazada' : '')
              }
              draggable
              onDragStart={(e) => {
                // Geometría previa al preview: con transforms aplicados las
                // posiciones quedarían a medio animar.
                capturarGeometria();
                setArrastrando(h.id);
                // Firefox no inicia el arrastre sin datos en el dataTransfer.
                e.dataTransfer.setData('text/plain', String(h.id));
                e.dataTransfer.effectAllowed = 'move';

                // Imagen de arrastre propia: la fila se ve OPACA y con el
                // reborde celeste mientras sigue al cursor. Sin esto el navegador
                // usa una copia de la fila tal cual (y, junto con el hueco que
                // deja en la lista, se veía doble).
                const fila = e.currentTarget;
                const caja = fila.getBoundingClientRect();
                const clon = fila.cloneNode(true);
                clon.classList.add('habitacion-ghost');
                clon.style.width = `${caja.width}px`;
                document.body.appendChild(clon);
                e.dataTransfer.setDragImage(
                  clon,
                  Math.max(0, Math.min(caja.width, e.clientX - caja.left)),
                  Math.max(0, Math.min(caja.height, e.clientY - caja.top)),
                );
                // El navegador ya tomó la imagen en este tick.
                setTimeout(() => clon.remove(), 0);
              }}
            >
              <span className="habitacion-grip" aria-hidden="true">
                ⠿
              </span>
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
        <span className="habitaciones-prioridad-tope habitaciones-prioridad-tope--fin">
          Menos prioritaria
        </span>
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
