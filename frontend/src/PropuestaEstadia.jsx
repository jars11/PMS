import { useRef, useState } from 'react';
import { api } from './api';
import CamposTitular from './CamposTitular';
import { useTitular } from './useTitular';

// Seccion "Propuesta de estadia" (paridad con PanelPropuestaEstadia del Swing):
// titular, filas de bloques solicitados (fechas, camas, atributos), busqueda de
// alternativas, preview en gris de la elegida y "Aceptar y crear reserva".

// Mismos valores y orden que el enum AtributoHabitacion del backend.
const ATRIBUTOS = ['Balcon', 'VistaAlMar', 'Jacuzzi', 'Twin', 'DosAmbientes'];
const MAX_CANTIDAD = 50; // Swing: SpinnerNumberModel(0, 0, 50, 1)
const FORMATO_MONTO = new Intl.NumberFormat('es-UY', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
const MONEDA_POR_DEFECTO = 'USD';

function formatearMonto(monto, moneda) {
  const valor = Number(monto);
  return `${moneda || MONEDA_POR_DEFECTO} ${FORMATO_MONTO.format(Number.isFinite(valor) ? valor : 0)}`;
}

function dosDigitos(n) {
  return String(n).padStart(2, '0');
}

function hoyISO() {
  const d = new Date();
  return `${d.getFullYear()}-${dosDigitos(d.getMonth() + 1)}-${dosDigitos(d.getDate())}`;
}

function sumarDiasISO(iso, dias) {
  const [y, m, d] = iso.split('-').map(Number);
  const fecha = new Date(y, m - 1, d + dias);
  return `${fecha.getFullYear()}-${dosDigitos(fecha.getMonth() + 1)}-${dosDigitos(fecha.getDate())}`;
}

function bloqueNuevo(inicio) {
  return {
    fechaInicio: inicio,
    fechaFin: sumarDiasISO(inicio, 1),
    camasMatrimoniales: 0,
    camasIndividuales: 0,
    bebes: 0,
    cunas: 0,
    atributos: [],
  };
}

/** Misma validacion pura que PanelPropuestaEstadia.validarBloques, con sus mensajes. */
function validarBloques(bloques) {
  if (!bloques || bloques.length === 0) {
    throw new Error('Agregá al menos un bloque de estadía.');
  }
  for (const bloque of bloques) {
    if (!bloque) throw new Error('Hay un bloque vacío.');
    if (!bloque.fechaInicio || !bloque.fechaFin) {
      throw new Error('Las fechas de check-in y check-out son obligatorias.');
    }
    // Las fechas ISO YYYY-MM-DD se comparan lexicograficamente.
    if (!(bloque.fechaFin > bloque.fechaInicio)) {
      throw new Error('El check-out debe ser posterior al check-in.');
    }
    if (
      bloque.camasMatrimoniales < 0 ||
      bloque.camasIndividuales < 0 ||
      bloque.bebes < 0 ||
      bloque.cunas < 0
    ) {
      throw new Error('Las cantidades no pueden ser negativas.');
    }
  }
}

function aSolicitud(bloque) {
  return {
    fechaInicio: bloque.fechaInicio,
    fechaFin: bloque.fechaFin,
    atributos: [...bloque.atributos],
    camasMatrimoniales: Number(bloque.camasMatrimoniales) || 0,
    camasIndividuales: Number(bloque.camasIndividuales) || 0,
    bebes: Number(bloque.bebes) || 0,
    cunas: Number(bloque.cunas) || 0,
  };
}

function aBloquesPersistir(bloques) {
  return (bloques || []).map((b) => ({
    habitacionId: Number(b.habitacionId),
    checkIn: b.checkIn,
    checkOut: b.checkOut,
    camasMatrimoniales: Number(b.camasMatrimoniales) || 0,
    camasIndividuales: Number(b.camasIndividuales) || 0,
    cunas: Number(b.cunas) || 0,
    atributos: [...(b.atributos || [])],
  }));
}

/** Resumen legible de una alternativa, igual que PanelPropuestaEstadia.resumen. */
function resumen(propuesta) {
  const bloques = propuesta?.bloques || [];
  const partes = [];
  let total = 0;
  for (const bloque of bloques) {
    const numero = bloque.numero == null ? '?' : bloque.numero;
    partes.push(`N° ${numero} (${formatearMonto(bloque.subtotal, bloque.moneda)})`);
    total += Number(bloque.subtotal) || 0;
  }
  const prefijo = bloques.length === 1 ? '1 habitación: ' : `${bloques.length} habitaciones: `;
  return `${prefijo}${partes.join(', ')} — total ${formatearMonto(total, bloques[0]?.moneda)}`;
}

export default function PropuestaEstadia({ titulares = [], onPrevisualizar, onCreada }) {
  // Flujo documento primero: se busca el documento del titular y, si no existe,
  // se da de alta con POST /api/titulares al aceptar la propuesta.
  const titular = useTitular();
  const [bloques, setBloques] = useState(() => [bloqueNuevo(hoyISO())]);
  const [propuestas, setPropuestas] = useState([]);
  const [seleccion, setSeleccion] = useState(-1);
  // 'inicial' muestra "Buscar propuestas"; 'propuestas' muestra aceptar/cancelar.
  const [paso, setPaso] = useState('inicial');
  const [buscando, setBuscando] = useState(false);
  const [guardando, setGuardando] = useState(false);
  const [estado, setEstado] = useState('');
  const [aviso, setAviso] = useState('');
  const [error, setError] = useState('');
  // Generacion de busqueda: invalida resultados de pedidos ya cancelados
  // (paridad con PanelPropuestaEstadia.generacionBusqueda).
  const generacionRef = useRef(0);

  function previsualizar(bloquesElegidos) {
    if (onPrevisualizar) onPrevisualizar(bloquesElegidos || []);
  }

  function cambiarBloque(indice, campo, valor) {
    setBloques((previos) =>
      previos.map((bloque, i) => (i === indice ? { ...bloque, [campo]: valor } : bloque)),
    );
  }

  function alternarAtributo(indice, atributo) {
    setBloques((previos) =>
      previos.map((bloque, i) => {
        if (i !== indice) return bloque;
        const activo = bloque.atributos.includes(atributo);
        return {
          ...bloque,
          atributos: activo
            ? bloque.atributos.filter((a) => a !== atributo)
            : [...bloque.atributos, atributo],
        };
      }),
    );
  }

  function agregarBloque() {
    setBloques((previos) => {
      const ultimo = previos[previos.length - 1];
      return [...previos, bloqueNuevo(ultimo ? ultimo.fechaFin : hoyISO())];
    });
  }

  function quitarBloque(indice) {
    // Siempre queda al menos un bloque (igual que PanelPropuestaEstadia).
    setBloques((previos) => (previos.length <= 1 ? previos : previos.filter((_, i) => i !== indice)));
  }

  function seleccionar(indice, lista = propuestas) {
    setSeleccion(indice);
    previsualizar(lista[indice]?.bloques || []);
  }

  async function buscar() {
    setError('');
    setAviso('');
    try {
      validarBloques(bloques);
    } catch (err) {
      setError(err.message);
      return; // no se corre la busqueda
    }
    setPaso('propuestas');
    setPropuestas([]);
    setSeleccion(-1);
    previsualizar([]);
    const generacion = generacionRef.current + 1;
    generacionRef.current = generacion;
    setBuscando(true);
    setEstado('Buscando propuestas…');
    try {
      const data = await api.buscarPropuestas(bloques.map(aSolicitud));
      if (generacion !== generacionRef.current) return; // busqueda cancelada
      const encontradas = data?.propuestas || [];
      setPropuestas(encontradas);
      if (encontradas.length === 0) {
        setEstado('Sin propuestas.');
        setAviso('No se encontraron propuestas para los bloques pedidos.');
      } else {
        setEstado(`${encontradas.length} propuesta(s) encontrada(s).`);
        // La primera alternativa queda elegida y se previsualiza en gris.
        seleccionar(0, encontradas);
      }
    } catch (err) {
      if (generacion !== generacionRef.current) return;
      setError(err.message);
      setEstado('Error al buscar propuestas.');
    } finally {
      if (generacion === generacionRef.current) setBuscando(false);
    }
  }

  async function aceptar() {
    setError('');
    setAviso('');
    const elegida = propuestas[seleccion];
    if (!elegida) {
      setError('Seleccioná una propuesta de la lista.');
      return;
    }
    setGuardando(true);
    setEstado('Guardando reserva…');
    try {
      // Busca el documento y, si el titular no existe, lo da de alta antes de
      // persistir la reserva.
      const titularId = await titular.resolverTitularId();
      const reserva = await api.persistirPropuestas(
        titularId,
        aBloquesPersistir(elegida.bloques),
      );
      setEstado(`Reserva #${reserva?.id} creada.`);
      setAviso(`Reserva creada: ${resumen(elegida)}`);
      setPropuestas([]);
      setSeleccion(-1);
      previsualizar([]);
      setPaso('inicial');
      if (onCreada) onCreada();
    } catch (err) {
      setError(err.message);
    } finally {
      setGuardando(false);
    }
  }

  function cancelar() {
    generacionRef.current += 1; // invalida una busqueda en curso
    setBuscando(false);
    setPropuestas([]);
    setSeleccion(-1);
    setPaso('inicial');
    setEstado('');
    setAviso('');
    setError('');
    previsualizar([]);
  }

  return (
    <section className="propuesta">
      <h2 className="propuesta-titulo">Propuesta de estadía</h2>

      <div className="propuesta-titular">
        <CamposTitular titular={titular} titulares={titulares} />
      </div>

      <div className="propuesta-bloques">
        <div className="propuesta-bloques-cabecera">
          <h3>Bloques solicitados</h3>
          <button type="button" className="secundario" onClick={agregarBloque}>
            Agregar bloque
          </button>
        </div>
        {bloques.map((bloque, i) => (
          <div className="propuesta-bloque" key={i}>
            <label>
              Check-in
              <input
                type="date"
                value={bloque.fechaInicio}
                onChange={(e) => cambiarBloque(i, 'fechaInicio', e.target.value)}
              />
            </label>
            <label>
              Check-out
              <input
                type="date"
                value={bloque.fechaFin}
                onChange={(e) => cambiarBloque(i, 'fechaFin', e.target.value)}
              />
            </label>
            <label>
              Matrimonios
              <input
                type="number"
                min="0"
                max={MAX_CANTIDAD}
                value={bloque.camasMatrimoniales}
                onChange={(e) => cambiarBloque(i, 'camasMatrimoniales', e.target.value)}
              />
            </label>
            <label>
              Individuales
              <input
                type="number"
                min="0"
                max={MAX_CANTIDAD}
                value={bloque.camasIndividuales}
                onChange={(e) => cambiarBloque(i, 'camasIndividuales', e.target.value)}
              />
            </label>
            <label>
              Bebés
              <input
                type="number"
                min="0"
                max={MAX_CANTIDAD}
                value={bloque.bebes}
                onChange={(e) => cambiarBloque(i, 'bebes', e.target.value)}
              />
            </label>
            <label>
              Cunas
              <input
                type="number"
                min="0"
                max={MAX_CANTIDAD}
                value={bloque.cunas}
                onChange={(e) => cambiarBloque(i, 'cunas', e.target.value)}
              />
            </label>
            <fieldset className="propuesta-atributos">
              <legend>Atributos</legend>
              {ATRIBUTOS.map((atributo) => (
                <label key={atributo} className="check">
                  <input
                    type="checkbox"
                    checked={bloque.atributos.includes(atributo)}
                    onChange={() => alternarAtributo(i, atributo)}
                  />
                  {atributo}
                </label>
              ))}
            </fieldset>
            <div className="propuesta-bloque-acciones">
              <button
                type="button"
                className="secundario"
                onClick={() => quitarBloque(i)}
                disabled={bloques.length <= 1}
              >
                Quitar bloque
              </button>
            </div>
          </div>
        ))}
      </div>

      {propuestas.length > 0 && (
        <div className="propuesta-lista-wrap">
          <h3>Propuestas encontradas</h3>
          <ul className="propuesta-lista">
            {propuestas.map((propuesta, i) => (
              <li key={i}>
                <label className={`propuesta-item${seleccion === i ? ' activo' : ''}`}>
                  <input
                    type="radio"
                    name="propuesta-elegida"
                    checked={seleccion === i}
                    onChange={() => seleccionar(i)}
                  />
                  <span>{resumen(propuesta)}</span>
                </label>
              </li>
            ))}
          </ul>
        </div>
      )}

      {aviso && <div className="propuesta-aviso">{aviso}</div>}
      {error && <div className="error">{error}</div>}

      <div className="propuesta-acciones">
        <span className="propuesta-estado">{estado}</span>
        {paso === 'inicial' ? (
          <button type="button" onClick={buscar} disabled={buscando || guardando}>
            Buscar propuestas
          </button>
        ) : (
          <>
            <button
              type="button"
              onClick={aceptar}
              disabled={buscando || guardando || propuestas.length === 0}
            >
              Aceptar y crear reserva
            </button>
            <button type="button" className="secundario" onClick={cancelar} disabled={guardando}>
              Cancelar
            </button>
          </>
        )}
      </div>
    </section>
  );
}
