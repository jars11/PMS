import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from './api';

// ---------------------------------------------------------------- fechas

function parseISO(iso) {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}

function fechaISO(d) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function sumarDias(iso, n) {
  const [y, m, d] = iso.split('-').map(Number);
  return fechaISO(new Date(y, m - 1, d + n));
}

function diasEntre(a, b) {
  return Math.round((parseISO(b) - parseISO(a)) / 86400000);
}

function formatearDMA(iso) {
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}

function formatearDia(iso) {
  const [, m, d] = iso.split('-');
  return `${d}/${m}`;
}

// ------------------------------------------------------- tooltip de reserva
// Replica la tarjeta que TablaCalendario.htmlReserva pinta en la app Swing.
// Monto con formato es-UY (miles con punto, decimales con coma) = "USD 7.400,00".

const FORMATO_MONTO = new Intl.NumberFormat('es-UY', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});
const SEPARADOR_DATO = ' \u00B7 ';
const MONEDA_POR_DEFECTO = 'USD';

function formatearMonto(monto, moneda) {
  return `${moneda || MONEDA_POR_DEFECTO} ${FORMATO_MONTO.format(monto)}`;
}

/** Lineas del tooltip de una barra, en el mismo orden que el Swing. */
function lineasTooltipReserva(bloque, habitacion) {
  if (!bloque || !bloque.checkIn || !bloque.checkOut) return [];
  const lineas = [];
  lineas.push(`Reserva #${bloque.reservaId}`);
  lineas.push(`Titular: ${bloque.titular ?? ''}`);
  if (habitacion && habitacion.numero != null) {
    lineas.push(`Habitaci\u00f3n: ${habitacion.numero}`);
  }
  lineas.push(`Check-in: ${formatearDMA(bloque.checkIn)}`);
  lineas.push(`Check-out: ${formatearDMA(bloque.checkOut)}`);
  lineas.push(`Noches: ${diasEntre(bloque.checkIn, bloque.checkOut)}`);
  let personas = `Personas: ${bloque.personas || 0}`;
  if (bloque.bebes > 0) personas += `${SEPARADOR_DATO}Beb\u00e9s: ${bloque.bebes}`;
  lineas.push(personas);
  if (bloque.subtotal > 0) {
    lineas.push(`Subtotal: ${formatearMonto(bloque.subtotal, bloque.moneda)}`);
  }
  if (bloque.estado != null) lineas.push(`Estado: ${bloque.estado}`);
  if (bloque.lateCheckout) lineas.push('Late check-out: s\u00ed');
  return lineas;
}

function parseDMA(txt) {
  const m = /^(\d{1,2})\/(\d{1,2})\/(\d{4})$/.exec(String(txt).trim());
  if (!m) return null;
  const d = Number(m[1]);
  const mo = Number(m[2]);
  const y = Number(m[3]);
  const fecha = new Date(y, mo - 1, d);
  if (fecha.getFullYear() !== y || fecha.getMonth() !== mo - 1 || fecha.getDate() !== d) {
    return null;
  }
  return fechaISO(fecha);
}

// ----------------------------------------------------------- GeometriaBarra

const INSET_CHECKIN = 0.25;
const INSET_CHECKOUT_NORMAL = 0.25;
const INSET_CHECKOUT_LATE = 0.75;
const SEPARACION_PX = 2;
const ANCHO_HABITACION = 110;
const ANCHO_NOCHE_MIN = 56;
const NOCHES_POR_DEFECTO = 14;
const MARGEN_VERTICAL = 4;
const UMBRAL_ARRASTRE_PX = 5;
const ANCHO_FLECHA = 9;
const ANCHO_PLACA = 2 * ANCHO_FLECHA;

function segmento(bloque, dia) {
  if (!bloque || !dia || !bloque.checkIn || !bloque.checkOut) return null;
  if (dia === bloque.checkIn) return { x0: INSET_CHECKIN, x1: 1 };
  if (dia === bloque.checkOut) {
    return { x0: 0, x1: bloque.lateCheckout ? INSET_CHECKOUT_LATE : INSET_CHECKOUT_NORMAL };
  }
  if (dia > bloque.checkIn && dia < bloque.checkOut) return { x0: 0, x1: 1 };
  return null;
}

function pixeles(seg, ancho) {
  if (!seg || ancho <= 0) return [0, 0];
  let x0 = Math.round(seg.x0 * ancho);
  let x1 = Math.round(seg.x1 * ancho);
  if (seg.x0 > 0) x0 += SEPARACION_PX;
  if (seg.x1 < 1) x1 -= SEPARACION_PX;
  x0 = Math.max(0, Math.min(ancho, x0));
  x1 = Math.max(0, Math.min(ancho, x1));
  if (x1 < x0) x1 = x0;
  return [x0, x1];
}

function nochesVisibles(ancho) {
  if (ancho <= ANCHO_HABITACION || ancho <= 0) return NOCHES_POR_DEFECTO;
  return Math.max(1, Math.floor((ancho - ANCHO_HABITACION) / ANCHO_NOCHE_MIN));
}

function anchoNoche(ancho, noches) {
  if (noches <= 0 || ancho <= ANCHO_HABITACION) return ANCHO_NOCHE_MIN;
  return Math.max(ANCHO_NOCHE_MIN, Math.floor((ancho - ANCHO_HABITACION) / noches));
}

// ------------------------------------------------- etiqueta de la reserva
// Mismo formato que EtiquetaReserva en la app Swing: "Titular — 2👤 + 👶".

const PADDING_ETIQUETA = 4;
const ANCHO_MINIMO_ETIQUETA = 14;
const SEPARADOR_ETIQUETA = ' \u2014 ';
const PUNTOS_ETIQUETA = '\u2026';
const ICONO_PERSONA = '\u{1F464}';
const ICONO_BEBE = '\u{1F476}';
// Debe coincidir con la fuente de .barra-etiqueta en App.css para que el
// recorte del nombre mida igual que lo que se pinta.
const FUENTE_ETIQUETA = "600 11px system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif";

let contextoTexto = null;

function anchoTexto(texto) {
  const cadena = texto == null ? '' : String(texto);
  if (cadena === '') return 0;
  if (!contextoTexto && typeof document !== 'undefined') {
    contextoTexto = document.createElement('canvas').getContext('2d');
    if (contextoTexto) contextoTexto.font = FUENTE_ETIQUETA;
  }
  return contextoTexto ? contextoTexto.measureText(cadena).width : 0;
}

function conteoReserva(personas, bebes) {
  const totalPersonas = Math.max(0, personas || 0);
  const totalBebes = Math.max(0, bebes || 0);
  let texto = '';
  if (totalPersonas > 0) {
    texto += totalPersonas === 1 ? ICONO_PERSONA : `${totalPersonas}${ICONO_PERSONA}`;
  }
  if (totalBebes > 0) {
    if (texto !== '') texto += ' + ';
    texto += totalBebes === 1 ? ICONO_BEBE : `${totalBebes}${ICONO_BEBE}`;
  }
  return texto;
}

/** Recorta conservando el principio y agregando "…"; "" si ni los puntos entran. */
function truncarTexto(texto, maxAncho) {
  const original = texto == null ? '' : String(texto);
  if (original === '' || maxAncho <= 0) return '';
  if (anchoTexto(original) <= maxAncho) return original;
  if (anchoTexto(PUNTOS_ETIQUETA) > maxAncho) return '';
  const caracteres = Array.from(original);
  let corte = caracteres.length;
  while (corte > 0 && anchoTexto(caracteres.slice(0, corte).join('').trimEnd() + PUNTOS_ETIQUETA) > maxAncho) {
    corte--;
  }
  const limpio = caracteres.slice(0, corte).join('').trimEnd();
  return limpio === '' ? '' : limpio + PUNTOS_ETIQUETA;
}

// ------------------------------------------------------- estado de arrastre

function nuevoEstado() {
  return {
    tipo: 'ninguno',
    filaOrigen: -1,
    colIni: -1,
    colFin: -1,
    ultimaColumna: -1,
    rangoExtendido: false,
    filaHover: -1,
    filaDestino: -1,
    bloqueOrigen: null,
    reservaOrigenId: null,
    compatiblePorFila: [],
    haySeleccion: false,
    filaSeleccionada: -1,
    reservaSeleccionadaId: null,
    bloqueSeleccionado: null,
    bordeRedimension: null,
    direccionRedimension: null,
    inicioRedimension: null,
    finRedimension: null,
    resizeBordeX: 0,
    resizeAnchoCelda: 0,
    presionPendiente: false,
    filaPresion: -1,
    columnaPresion: -1,
    puntoPresion: null,
    bloquePresion: null,
  };
}

function seleccionIni(est) {
  if (est.tipo !== 'rango') return -1;
  if (est.colFin < est.colIni) return est.colFin;
  if (est.colFin === est.colIni && est.colIni >= est.ultimaColumna) {
    return Math.max(1, est.ultimaColumna - 1);
  }
  return est.colIni;
}

function seleccionFin(est) {
  if (est.tipo !== 'rango') return -1;
  if (est.colFin < est.colIni) return est.colIni;
  if (est.colFin === est.colIni) return Math.min(est.colIni + 1, est.ultimaColumna);
  return est.colFin;
}

function esCeldaSeleccionada(est, fila, col) {
  if (est.tipo !== 'rango' || fila !== est.filaOrigen || col < 1) return false;
  if (!est.rangoExtendido) return col === est.colIni;
  return col >= seleccionIni(est) && col <= seleccionFin(est);
}

function ocultarBloqueOrigen(est) {
  return est.tipo === 'movimiento' && est.filaDestino >= 0 && est.filaDestino !== est.filaOrigen;
}

function esElBloqueSeleccionado(est, bloque, fila) {
  if (!bloque || !est.haySeleccion || fila !== est.filaSeleccionada) return false;
  const sel = est.bloqueSeleccionado;
  return (
    !!sel &&
    sel.checkIn === bloque.checkIn &&
    sel.checkOut === bloque.checkOut &&
    est.reservaSeleccionadaId != null &&
    est.reservaSeleccionadaId === bloque.reservaId
  );
}

function resolverDestino(filaHover, filaOrigen, filas, compatiblePorFila, filaValida) {
  if (filaHover < 0 || filaHover >= filas) return -1;
  if (filaHover === filaOrigen) return filaHover;
  if (filaValida && !filaValida(filaHover)) return -1;
  if (!compatiblePorFila[filaHover]) return -1;
  return filaHover;
}

function diasRedimension(deltaX, anchoCelda, borde) {
  const signo = borde === 'inicio' ? -1 : 1;
  const avance = deltaX * signo;
  const ancho = Math.max(1, anchoCelda);
  const umbral = Math.max(1, Math.floor(ancho / 4));
  if (Math.abs(avance) <= umbral) return 0;
  const dias = 1 + Math.floor((Math.abs(avance) - umbral) / ancho);
  return avance > 0 ? dias : -dias;
}

function contieneBarra(seg, ancho, x) {
  if (!seg) return false;
  const [x0, x1] = pixeles(seg, ancho);
  return x >= x0 && x <= x1;
}

// --------------------------------------------------------------- modelo

function construirModelo(ocupacion, habitaciones, desde, noches) {
  const fechas = [];
  for (let i = 0; i < noches; i++) fechas.push(sumarDias(desde, i));

  const habitacionPorId = new Map((habitaciones || []).map((h) => [h.id, h]));
  const filas = (ocupacion || []).map((o) => ({
    ...o,
    habitacion: habitacionPorId.get(o.habitacionId) || null,
  }));

  const ocupadasPorHab = new Map();
  for (const fila of filas) {
    const set = new Set();
    for (const b of fila.bloques || []) {
      for (const f of fechas) {
        if (f >= b.checkIn && f < b.checkOut) set.add(f);
      }
    }
    ocupadasPorHab.set(fila.habitacionId, set);
  }

  const fechaDeColumna = (col) => (col >= 1 && col <= noches ? fechas[col - 1] : null);

  return {
    filas,
    fechas,
    noches,
    desde,
    fechaDeColumna,
    columnaDeFecha(f) {
      const i = fechas.indexOf(f);
      return i === -1 ? -1 : i + 1;
    },
    estaOcupada(fila, col) {
      const f = fechaDeColumna(col);
      if (!f) return false;
      const habId = filas[fila]?.habitacionId;
      return ocupadasPorHab.get(habId)?.has(f) ?? false;
    },
    bloqueEn(fila, col) {
      const f = fechaDeColumna(col);
      if (!f) return null;
      return (filas[fila]?.bloques || []).find((b) => b.checkIn <= f && f < b.checkOut) || null;
    },
    bloqueSalidaEn(fila, col) {
      const f = fechaDeColumna(col);
      if (!f) return null;
      return (filas[fila]?.bloques || []).find((b) => b.checkOut === f) || null;
    },
    habitacionDeFila(fila) {
      return filas[fila]?.habitacion || null;
    },
    bloquesDeFila(fila) {
      return filas[fila]?.bloques || [];
    },
  };
}

function esCompatibleFila(m, fila, bloque) {
  const hab = m.habitacionDeFila(fila);
  if (!hab || hab.id == null) return true;
  const bloques = m.bloquesDeFila(fila);
  if (
    bloques.some(
      (b) => b.reservaId !== bloque.reservaId && b.checkIn < bloque.checkOut && bloque.checkIn < b.checkOut,
    )
  ) {
    return false;
  }
  if (bloque.personas > hab.capacidad + hab.colchones) return false;
  if (bloque.lateCheckout && bloques.some((b) => b.checkIn === bloque.checkOut)) return false;
  if (!hab.limpia && bloques.some((b) => b.lateCheckout && b.checkOut === bloque.checkIn)) return false;
  return true;
}

function bloqueEnPunto(m, fila, col, e, td) {
  if (fila < 0 || col < 1 || col > m.noches || !td) return null;
  const rect = td.getBoundingClientRect();
  const x = e.clientX - rect.left;
  const y = e.clientY - rect.top;
  if (y < MARGEN_VERTICAL || y > rect.height - MARGEN_VERTICAL) return null;
  const f = m.fechaDeColumna(col);
  const ancho = rect.width;
  const head = m.bloqueEn(fila, col);
  if (head && contieneBarra(segmento(head, f), ancho, x)) return head;
  const tail = m.bloqueSalidaEn(fila, col);
  if (tail && contieneBarra(segmento(tail, f), ancho, x)) return tail;
  return null;
}

function rangoDeColumnas(m, cIni, cFin) {
  const primera = Math.max(1, Math.min(cIni, cFin));
  const ultima = Math.min(m.noches, Math.max(cIni, cFin));
  if (primera >= ultima) return null;
  return { checkIn: m.fechaDeColumna(primera), checkOut: m.fechaDeColumna(ultima) };
}

function esRangoLibre(m, fila, cIni, cFin) {
  if (!rangoDeColumnas(m, cIni, cFin)) return false;
  const primera = Math.max(1, Math.min(cIni, cFin));
  const ultima = Math.min(m.noches, Math.max(cIni, cFin));
  for (let col = primera; col < ultima; col++) {
    if (m.estaOcupada(fila, col)) return false;
  }
  return true;
}

function filaDeEvento(e) {
  const td = e.target && e.target.closest ? e.target.closest('td[data-col]') : null;
  return td ? Number(td.dataset.fila) : -1;
}

function columnaDeEvento(e) {
  const td = e.target && e.target.closest ? e.target.closest('td[data-col]') : null;
  return td ? Number(td.dataset.col) : -1;
}

function limpiarInteraccion(est) {
  const sel = {
    haySeleccion: est.haySeleccion,
    filaSeleccionada: est.filaSeleccionada,
    reservaSeleccionadaId: est.reservaSeleccionadaId,
    bloqueSeleccionado: est.bloqueSeleccionado,
  };
  Object.assign(est, nuevoEstado());
  Object.assign(est, sel);
}

function limpiarSeleccion(est) {
  est.haySeleccion = false;
  est.filaSeleccionada = -1;
  est.reservaSeleccionadaId = null;
  est.bloqueSeleccionado = null;
  if (est.tipo === 'redimension') limpiarInteraccion(est);
}

function iniciarRango(est, fila, columna, ultimaColumna) {
  est.tipo = 'rango';
  est.filaOrigen = fila;
  est.colIni = columna;
  est.colFin = columna;
  est.ultimaColumna = ultimaColumna;
  est.filaHover = -1;
  est.bloqueOrigen = null;
  est.reservaOrigenId = null;
  est.rangoExtendido = false;
}

function iniciarMovimiento(est, m, fila, columna, bloque) {
  if (!bloque || bloque.reservaId == null) {
    limpiarInteraccion(est);
    return;
  }
  const hab = m.habitacionDeFila(fila);
  if (!hab || hab.id == null || !bloque.checkIn) {
    limpiarInteraccion(est);
    return;
  }
  limpiarSeleccion(est);
  est.tipo = 'movimiento';
  est.filaOrigen = fila;
  est.colIni = columna;
  est.colFin = columna;
  est.filaHover = fila;
  est.filaDestino = -1;
  est.bloqueOrigen = bloque;
  est.reservaOrigenId = bloque.reservaId;
  est.compatiblePorFila = m.filas.map((_, idx) => esCompatibleFila(m, idx, bloque));
}

function seleccionarReserva(est, fila, bloque) {
  if (!bloque || !bloque.checkIn || !bloque.checkOut) {
    limpiarSeleccion(est);
    return;
  }
  est.haySeleccion = true;
  est.filaSeleccionada = fila;
  est.reservaSeleccionadaId = bloque.reservaId;
  est.bloqueSeleccionado = bloque;
}

/**
 * Re-selects the reservation after a reload so the resize arrows survive a
 * successful move/resize (graceful continuation instead of silent capability loss).
 */
function reSeleccionarPorReserva(est, m, reservaId) {
  const encontrados = [];
  for (let fila = 0; fila < m.filas.length; fila++) {
    for (const bloque of m.bloquesDeFila(fila)) {
      if (bloque.reservaId === reservaId) encontrados.push({ fila, bloque });
    }
  }
  if (encontrados.length === 1) {
    seleccionarReserva(est, encontrados[0].fila, encontrados[0].bloque);
  }
}

function celdaInfo(m, est, fila, col, fecha) {
  const enMovimiento = est.tipo === 'movimiento';
  const enRedimension = est.tipo === 'redimension';
  const esOrigen = enMovimiento && fila === est.filaOrigen;
  const gris = enMovimiento && !esOrigen && !est.compatiblePorFila[fila];
  const seleccionRango = esCeldaSeleccionada(est, fila, col);

  let clase = 'cal-celda-limpia';
  if (seleccionRango) {
    clase = 'cal-celda-seleccion';
  } else if (gris) {
    clase = 'cal-celda-gris';
  } else {
    const hab = m.habitacionDeFila(fila);
    clase = hab && hab.limpia === false ? 'cal-celda-sucia' : 'cal-celda-limpia';
  }

  let ignorar = null;
  if (enMovimiento && fila === est.filaOrigen && ocultarBloqueOrigen(est)) {
    ignorar = est.reservaOrigenId;
  } else if (enRedimension && fila === est.filaSeleccionada) {
    ignorar = est.reservaSeleccionadaId;
  }

  const segments = [];
  const bloques = m.bloquesDeFila(fila);
  const tail = bloques.find((b) => b.reservaId !== ignorar && b.checkOut === fecha);
  if (tail) {
    const seg = segmento(tail, fecha);
    if (seg) segments.push({ ...seg, clase: gris ? 'barra barra-gris' : 'barra', bloque: tail });
  }
  const head = bloques.find((b) => b.reservaId !== ignorar && b.checkIn <= fecha && fecha < b.checkOut);
  if (head) {
    const seg = segmento(head, fecha);
    if (seg) segments.push({ ...seg, clase: gris ? 'barra barra-gris' : 'barra', bloque: head });
  }

  if (enMovimiento && fila === est.filaDestino && est.filaDestino !== est.filaOrigen && est.bloqueOrigen) {
    const seg = segmento(est.bloqueOrigen, fecha);
    if (seg) segments.push({ ...seg, clase: 'barra barra-preview', bloque: est.bloqueOrigen });
  }
  if (enRedimension && fila === est.filaSeleccionada && est.inicioRedimension && est.finRedimension && est.bloqueSeleccionado) {
    const rango = {
      checkIn: est.inicioRedimension,
      checkOut: est.finRedimension,
      lateCheckout: est.bloqueSeleccionado.lateCheckout,
    };
    const seg = segmento(rango, fecha);
    if (seg) segments.push({ ...seg, clase: 'barra barra-preview', bloque: { ...est.bloqueSeleccionado, ...rango } });
  }
  if (est.tipo === 'rango' && est.rangoExtendido && esCeldaSeleccionada(est, fila, col)) {
    const sIni = seleccionIni(est);
    const sFin = seleccionFin(est);
    const fIni = m.fechaDeColumna(sIni);
    const fFin = m.fechaDeColumna(sFin);
    if (fIni && fFin) {
      const seg = segmento({ checkIn: fIni, checkOut: fFin, lateCheckout: false }, fecha);
      if (seg) segments.push({ ...seg, clase: 'barra barra-fantasma' });
    }
  }

  return { clase, segments };
}

export default function Calendario({ habitaciones, onNuevaPrecargada, onEditarReserva, onCambio, refreshSignal = 0 }) {
  const contRef = useRef(null);
  const [containerWidth, setContainerWidth] = useState(0);
  const [fechaInicio, setFechaInicio] = useState(() => fechaISO(new Date()));
  const [fechaInput, setFechaInput] = useState(() => formatearDMA(fechaISO(new Date())));
  const [modelo, setModelo] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [errorCarga, setErrorCarga] = useState('');
  const [aviso, setAviso] = useState(null);
  const [, setTick] = useState(0);
  const estadoRef = useRef(nuevoEstado());
  const modeloRef = useRef(null);
  const enVueloRef = useRef(false);
  // Tooltip de reserva: el contenido cambia solo al pasar a otra barra, asi que
  // el estado guarda solo eso; la posicion se actualiza por DOM para no
  // re-renderizar la grilla en cada movimiento del mouse.
  const [tooltip, setTooltip] = useState(null);
  const tooltipRef = useRef(null);
  const tooltipClaveRef = useRef(null);
  // Menu contextual (click derecho), igual que CalendarioPanel.mostrarMenu.
  const [menu, setMenu] = useState(null);
  const render = useCallback(() => setTick((t) => t + 1), []);

  const noches = containerWidth > 0 ? nochesVisibles(containerWidth) : NOCHES_POR_DEFECTO;

  const recargar = useCallback(async () => {
    const desde = fechaInicio;
    const hasta = sumarDias(fechaInicio, noches);
    setCargando(true);
    setErrorCarga('');
    const selPrevia = estadoRef.current;
    const reservaSelId = selPrevia.haySeleccion ? selPrevia.reservaSeleccionadaId : null;
    try {
      const data = await api.calendario(desde, hasta);
      const m = construirModelo(data, habitaciones, desde, noches);
      modeloRef.current = m;
      setModelo(m);
      estadoRef.current = nuevoEstado();
      if (reservaSelId != null) reSeleccionarPorReserva(estadoRef.current, m, reservaSelId);
      render();
    } catch (err) {
      setErrorCarga(err.message);
    } finally {
      setCargando(false);
    }
  }, [fechaInicio, noches, habitaciones, render]);

  useEffect(() => {
    recargar();
  }, [recargar, refreshSignal]);

  const recargarRef = useRef(recargar);
  useEffect(() => {
    recargarRef.current = recargar;
  }, [recargar]);

  const onNuevaRef = useRef(onNuevaPrecargada);
  useEffect(() => {
    onNuevaRef.current = onNuevaPrecargada;
  }, [onNuevaPrecargada]);

  const onCambioRef = useRef(onCambio);
  useEffect(() => {
    onCambioRef.current = onCambio;
  }, [onCambio]);

  const onEditarRef = useRef(onEditarReserva);
  useEffect(() => {
    onEditarRef.current = onEditarReserva;
  }, [onEditarReserva]);

  // El menu contextual se cierra con Escape, al hacer click afuera o al scrollear.
  useEffect(() => {
    if (!menu) return undefined;
    const cerrar = () => setMenu(null);
    const cerrarFuera = (e) => {
      if (e.target && e.target.closest && e.target.closest('.cal-menu')) return;
      cerrar();
    };
    const tecla = (e) => {
      if (e.key === 'Escape') cerrar();
    };
    window.addEventListener('pointerdown', cerrarFuera);
    window.addEventListener('keydown', tecla);
    window.addEventListener('scroll', cerrar, true);
    return () => {
      window.removeEventListener('pointerdown', cerrarFuera);
      window.removeEventListener('keydown', tecla);
      window.removeEventListener('scroll', cerrar, true);
    };
  }, [menu]);

  useEffect(() => {
    const el = contRef.current;
    if (!el) return undefined;
    const ro = new ResizeObserver((entries) => {
      setContainerWidth(entries[0]?.contentRect?.width ?? 0);
    });
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  // ------------------------------------------- navegacion con Shift + rueda
  // Replica CalendarioPanel (Swing): rueda arriba = dia anterior, rueda abajo =
  // dia siguiente. Sin Shift la rueda conserva el scroll normal del calendario.
  const fechaInicioRef = useRef(fechaInicio);
  useEffect(() => {
    fechaInicioRef.current = fechaInicio;
  }, [fechaInicio]);

  /** Mueve la fecha de inicio y recarga, igual que aplicarFecha. */
  function moverDias(paso) {
    const iso = sumarDias(fechaInicioRef.current, paso);
    // La ref se actualiza al instante, asi dos giros seguidos antes de que
    // React vuelva a renderizar no se pisan.
    fechaInicioRef.current = iso;
    setFechaInput(formatearDMA(iso));
    setFechaInicio(iso);
  }

  function manejarRueda(e) {
    if (!e.shiftKey) return;
    // Con Shift los navegadores suelen mandar el giro en deltaX.
    const giro = e.deltaY !== 0 ? e.deltaY : e.deltaX;
    if (giro === 0) return;
    e.preventDefault();
    moverDias(giro < 0 ? -1 : 1);
  }

  const ruedaRef = useRef(null);
  useEffect(() => {
    ruedaRef.current = manejarRueda;
  });
  // React registra `wheel` como listener pasivo en la raiz, asi que
  // preventDefault dentro de onWheel no frena el scroll: se engancha a mano
  // sobre el contenedor con passive: false.
  useEffect(() => {
    const el = contRef.current;
    if (!el) return undefined;
    const rueda = (e) => ruedaRef.current(e);
    el.addEventListener('wheel', rueda, { passive: false });
    return () => el.removeEventListener('wheel', rueda);
  }, []);

  function iniciarRedimension(handle, e, est) {
    const borde = handle.dataset.borde;
    const direccion = handle.dataset.direccion;
    if (!est.haySeleccion || !est.bloqueSeleccionado) return;
    const placa = handle.closest('[data-placa]');
    const td = handle.closest('td[data-col]');
    const rect = placa ? placa.getBoundingClientRect() : null;
    const tdRect = td ? td.getBoundingClientRect() : null;
    let bordeX = e.clientX;
    if (rect) bordeX = borde === 'inicio' ? rect.left : rect.right;
    est.tipo = 'redimension';
    est.bordeRedimension = borde;
    est.direccionRedimension = direccion;
    est.inicioRedimension = est.bloqueSeleccionado.checkIn;
    est.finRedimension = est.bloqueSeleccionado.checkOut;
    est.resizeBordeX = bordeX;
    est.resizeAnchoCelda = tdRect ? tdRect.width : ANCHO_NOCHE_MIN;
    est.presionPendiente = false;
    est.bloquePresion = null;
    render();
  }

  function actualizarRedimension(e, m, est) {
    if (est.tipo !== 'redimension' || !est.bloqueSeleccionado) return;
    const fila = est.filaSeleccionada;
    const ultima = m.noches;
    const deltaX = e.clientX - est.resizeBordeX;
    const dias = diasRedimension(deltaX, est.resizeAnchoCelda, est.bordeRedimension);
    let ini = est.bloqueSeleccionado.checkIn;
    let fin = est.bloqueSeleccionado.checkOut;
    const nochesEstadia = diasEntre(ini, fin);

    if (est.bordeRedimension === 'inicio') {
      if (dias > 0) {
        let max = 0;
        while (max < dias) {
          const noche = sumarDias(ini, -(max + 1));
          const col = m.columnaDeFecha(noche);
          if (col < 1 || col > ultima || m.estaOcupada(fila, col)) break;
          max++;
        }
        ini = sumarDias(ini, -max);
      } else if (dias < 0) {
        const max = Math.max(0, nochesEstadia - 1);
        ini = sumarDias(ini, Math.min(-dias, max));
      }
    } else if (dias > 0) {
      let max = 0;
      while (max < dias) {
        const noche = sumarDias(fin, max);
        const col = m.columnaDeFecha(noche);
        if (col < 1 || col > ultima || m.estaOcupada(fila, col)) break;
        max++;
      }
      fin = sumarDias(fin, max);
    } else if (dias < 0) {
      const max = Math.max(0, nochesEstadia - 1);
      fin = sumarDias(fin, -Math.min(-dias, max));
    }

    if (ini < fin) {
      est.inicioRedimension = ini;
      est.finRedimension = fin;
      render();
    }
  }

  function extenderRango(e, m, est) {
    const fila = filaDeEvento(e);
    if (fila !== est.filaOrigen) return;
    const columna = Math.max(1, Math.min(m.noches, columnaDeEvento(e)));
    if (columna !== est.colFin) {
      est.colFin = columna;
      est.rangoExtendido = columna !== est.colIni;
      render();
    }
  }

  async function finalizarRango(m, est) {
    if (!est.rangoExtendido) {
      limpiarInteraccion(est);
      render();
      return;
    }
    const fila = est.filaOrigen;
    const cIni = seleccionIni(est);
    const cFin = seleccionFin(est);
    if (fila < 0 || cIni < 0 || cFin < 0) {
      limpiarInteraccion(est);
      render();
      return;
    }
    const rango = rangoDeColumnas(m, cIni, cFin);
    if (!rango) {
      limpiarInteraccion(est);
      render();
      return;
    }
    if (!esRangoLibre(m, fila, cIni, cFin)) {
      limpiarInteraccion(est);
      setAviso({ tipo: 'error', titulo: 'Selección no válida', mensaje: 'El rango cruza una noche ocupada. Elegí noches libres.' });
      render();
      return;
    }
    const hab = m.habitacionDeFila(fila);
    if (!hab || hab.id == null) {
      limpiarInteraccion(est);
      render();
      return;
    }
    try {
      const resp = await api.lateCheckout(hab.id, rango.checkIn);
      if (resp && resp.hayLateCheckout) {
        setAviso({
          tipo: 'info',
          mensaje: 'La habitación tiene un late check-out el día del check-in. Coordiná la limpieza antes del ingreso.',
        });
      }
    } catch {
      /* aviso best-effort */
    }
    limpiarInteraccion(est);
    render();
    if (onNuevaRef.current) {
      onNuevaRef.current({ habitacionId: hab.id, checkIn: rango.checkIn, checkOut: rango.checkOut });
    }
  }

  async function finalizarMovimiento(m, est) {
    if (enVueloRef.current) return;
    const filaOrigen = est.filaOrigen;
    const filaDestino = est.filaDestino;
    const reservaId = est.reservaOrigenId;
    const origen = m.habitacionDeFila(filaOrigen)?.id;
    const noche = est.bloqueOrigen?.checkIn;
    limpiarInteraccion(est);
    render();
    if (reservaId == null || origen == null || noche == null) return;
    if (filaDestino < 0 || filaDestino === filaOrigen) return;
    const destino = m.habitacionDeFila(filaDestino);
    if (!destino || destino.id == null) return;
    enVueloRef.current = true;
    try {
      await api.moverReserva(reservaId, {
        habitacionOrigenId: origen,
        noche,
        habitacionDestinoId: destino.id,
      });
      await recargarRef.current();
      if (onCambioRef.current) onCambioRef.current();
    } catch (err) {
      setAviso({
        tipo: 'error',
        titulo: 'No se pudo mover',
        mensaje: err.mensaje || err.message,
        motivo: err.motivo,
        detalle: err.detalle,
      });
      await recargarRef.current();
    } finally {
      enVueloRef.current = false;
    }
  }

  async function finalizarRedimension(m, est) {
    if (enVueloRef.current) return;
    const original = est.bloqueSeleccionado;
    const nuevoInicio = est.inicioRedimension;
    const nuevoFin = est.finRedimension;
    const fila = est.filaSeleccionada;
    const reservaId = est.reservaSeleccionadaId;
    const hab = fila >= 0 ? m.habitacionDeFila(fila) : null;
    const noche = original?.checkIn;

    est.tipo = 'ninguno';
    est.bordeRedimension = null;
    est.direccionRedimension = null;
    est.inicioRedimension = null;
    est.finRedimension = null;
    est.resizeBordeX = 0;
    est.resizeAnchoCelda = 0;
    render();

    const cambio =
      original &&
      nuevoInicio &&
      nuevoFin &&
      (nuevoInicio !== original.checkIn || nuevoFin !== original.checkOut);
    if (!cambio || reservaId == null || !hab || hab.id == null || !noche) return;

    enVueloRef.current = true;
    try {
      await api.redimensionarReserva(reservaId, {
        habitacionId: hab.id,
        noche,
        nuevoCheckIn: nuevoInicio,
        nuevoCheckOut: nuevoFin,
      });
      await recargarRef.current();
      if (onCambioRef.current) onCambioRef.current();
    } catch (err) {
      setAviso({ tipo: 'error', titulo: 'Error', mensaje: err.message });
      await recargarRef.current();
    } finally {
      enVueloRef.current = false;
    }
  }

  function ocultarTooltip() {
    if (tooltipClaveRef.current === null) return;
    tooltipClaveRef.current = null;
    setTooltip(null);
  }

  /** Sigue al mouse y se da vuelta antes de salirse de la ventana. */
  function posicionarTooltip(x, y) {
    const el = tooltipRef.current;
    if (!el) return;
    const separacion = 14;
    const margen = 8;
    const ancho = el.offsetWidth || 0;
    const alto = el.offsetHeight || 0;
    let left = x + separacion;
    let top = y + separacion;
    if (left + ancho > window.innerWidth - margen) left = Math.max(margen, x - ancho - separacion);
    if (top + alto > window.innerHeight - margen) top = Math.max(margen, y - alto - separacion);
    el.style.left = `${left}px`;
    el.style.top = `${top}px`;
  }

  useEffect(() => {
    if (tooltip) posicionarTooltip(tooltip.x, tooltip.y);
  }, [tooltip]);

  /**
   * Tooltip de hover: solo sobre la barra de una reserva (mismo criterio que
   * TablaCalendario.textoHover) y nunca mientras se crea, mueve o redimensiona.
   */
  function manejarHover(e) {
    const m = modeloRef.current;
    const est = estadoRef.current;
    if (!m || est.tipo !== 'ninguno' || est.presionPendiente) {
      ocultarTooltip();
      return;
    }
    // Sobre las flechas de redimension el tooltip taparia el handle.
    if (e.target && e.target.closest && e.target.closest('[data-handle]')) {
      ocultarTooltip();
      return;
    }
    const td = e.target && e.target.closest ? e.target.closest('td[data-col]') : null;
    const fila = td ? Number(td.dataset.fila) : -1;
    const col = td ? Number(td.dataset.col) : -1;
    const bloque = fila >= 0 && col >= 1 ? bloqueEnPunto(m, fila, col, e, td) : null;
    if (!bloque) {
      ocultarTooltip();
      return;
    }
    const clave = `${bloque.reservaId}|${bloque.checkIn}|${bloque.checkOut}`;
    if (clave !== tooltipClaveRef.current) {
      tooltipClaveRef.current = clave;
      setTooltip({
        lineas: lineasTooltipReserva(bloque, m.habitacionDeFila(fila)),
        x: e.clientX,
        y: e.clientY,
      });
      return;
    }
    posicionarTooltip(e.clientX, e.clientY);
  }

  // ------------------------------------------- menu contextual (click derecho)

  /**
   * Mismo criterio que CalendarioPanel.mostrarMenu + SeleccionCalendario.accionDe:
   * una noche ocupada ofrece editar/eliminar; una libre, el alta en esa noche.
   */
  function manejarContextMenu(e) {
    const m = modeloRef.current;
    if (!m) return;
    const td = e.target && e.target.closest ? e.target.closest('td[data-col]') : null;
    if (!td) return;
    const fila = Number(td.dataset.fila);
    const col = Number(td.dataset.col);
    if (fila < 0 || col < 1 || col > m.noches) return;
    const hab = m.habitacionDeFila(fila);
    const noche = m.fechaDeColumna(col);
    if (!hab || hab.id == null || !noche) return;

    e.preventDefault();
    ocultarTooltip();
    const bloque = m.estaOcupada(fila, col) ? m.bloqueEn(fila, col) : null;
    setMenu({
      tipo: bloque ? 'reserva' : 'nueva',
      reservaId: bloque ? bloque.reservaId : null,
      habitacionId: hab.id,
      noche,
      x: Math.max(0, Math.min(e.clientX, window.innerWidth - 200)),
      y: Math.max(0, Math.min(e.clientY, window.innerHeight - 110)),
    });
  }

  async function editarDesdeMenu() {
    const item = menu;
    if (!item || item.tipo !== 'reserva') return;
    setMenu(null);
    try {
      const ed = await api.edicionReserva(item.reservaId, item.habitacionId, item.noche);
      if (onEditarRef.current) {
        onEditarRef.current({
          id: ed.reservaId,
          titularId: ed.titularId,
          habitacionId: ed.habitacionId,
          checkIn: ed.checkIn,
          checkOut: ed.checkOut,
          camasMatrimoniales: ed.camasMatrimoniales,
          camasIndividuales: ed.camasIndividuales,
          cunas: ed.cunas,
          estado: ed.estado,
          lateCheckout: ed.lateCheckout,
        });
      }
    } catch (err) {
      setAviso({ tipo: 'error', titulo: 'No se pudo abrir la edición', mensaje: err.message });
    }
  }

  async function eliminarDesdeMenu() {
    const item = menu;
    if (!item || item.tipo !== 'reserva') return;
    setMenu(null);
    if (!window.confirm(`¿Eliminar la reserva #${item.reservaId}?`)) return;
    enVueloRef.current = true;
    try {
      await api.eliminarReserva(item.reservaId);
      await recargarRef.current();
      if (onCambioRef.current) onCambioRef.current();
    } catch (err) {
      setAviso({ tipo: 'error', titulo: 'No se pudo eliminar', mensaje: err.message });
    } finally {
      enVueloRef.current = false;
    }
  }

  function nuevaDesdeMenu() {
    const item = menu;
    if (!item || item.tipo !== 'nueva') return;
    setMenu(null);
    if (onNuevaRef.current) {
      onNuevaRef.current({
        habitacionId: item.habitacionId,
        checkIn: item.noche,
        checkOut: sumarDias(item.noche, 1),
      });
    }
  }

  function manejarPointerDown(e) {
    if (e.button !== 0) return;
    const m = modeloRef.current;
    if (!m) return;
    const est = estadoRef.current;
    // El tooltip no debe tapar ni frenar el crear/mover/redimensionar.
    ocultarTooltip();

    const handle = e.target && e.target.closest ? e.target.closest('[data-handle]') : null;
    if (handle && est.haySeleccion && est.tipo !== 'redimension') {
      iniciarRedimension(handle, e, est);
      e.preventDefault();
      return;
    }

    const td = e.target && e.target.closest ? e.target.closest('td[data-col]') : null;
    const fila = td ? Number(td.dataset.fila) : -1;
    const columna = td ? Number(td.dataset.col) : -1;

    if (fila < 0 || columna <= 0) {
      limpiarSeleccion(est);
      render();
      return;
    }

    const bloque = bloqueEnPunto(m, fila, columna, e, td);

    if (est.haySeleccion) {
      if (esElBloqueSeleccionado(est, bloque, fila)) {
        est.presionPendiente = true;
        est.filaPresion = fila;
        est.columnaPresion = columna;
        est.puntoPresion = { x: e.clientX, y: e.clientY };
        est.bloquePresion = bloque;
        render();
        return;
      }
      limpiarSeleccion(est);
      render();
      return;
    }

    if (bloque) {
      est.presionPendiente = true;
      est.filaPresion = fila;
      est.columnaPresion = columna;
      est.puntoPresion = { x: e.clientX, y: e.clientY };
      est.bloquePresion = bloque;
      render();
      return;
    }

    if (m.estaOcupada(fila, columna)) {
      limpiarInteraccion(est);
      render();
      return;
    }

    limpiarInteraccion(est);
    iniciarRango(est, fila, columna, m.noches);
    render();
    e.preventDefault();
  }

  function manejarMove(e) {
    const m = modeloRef.current;
    if (!m) return;
    const est = estadoRef.current;

    if (est.tipo === 'redimension') {
      actualizarRedimension(e, m, est);
      return;
    }
    if (est.tipo === 'movimiento') {
      est.filaHover = filaDeEvento(e);
      est.filaDestino = resolverDestino(est.filaHover, est.filaOrigen, m.filas.length, est.compatiblePorFila, (fila) => !!m.habitacionDeFila(fila));
      render();
      return;
    }
    if (est.presionPendiente) {
      if (!est.puntoPresion) return;
      const dist = Math.hypot(e.clientX - est.puntoPresion.x, e.clientY - est.puntoPresion.y);
      if (dist < UMBRAL_ARRASTRE_PX) return;
      est.presionPendiente = false;
      const fila = est.filaPresion;
      const columna = est.columnaPresion;
      const bloque = est.bloquePresion;
      est.bloquePresion = null;
      iniciarMovimiento(est, m, fila, columna, bloque);
      est.filaHover = filaDeEvento(e);
      est.filaDestino = resolverDestino(est.filaHover, est.filaOrigen, m.filas.length, est.compatiblePorFila, (fila) => !!m.habitacionDeFila(fila));
      render();
      return;
    }
    if (est.tipo === 'rango') {
      extenderRango(e, m, est);
    }
  }

  function manejarUp() {
    const m = modeloRef.current;
    if (!m) return;
    const est = estadoRef.current;

    if (est.tipo === 'redimension') {
      finalizarRedimension(m, est);
      return;
    }
    if (est.presionPendiente) {
      est.presionPendiente = false;
      const bloque = est.bloquePresion;
      const fila = est.filaPresion;
      est.bloquePresion = null;
      if (bloque && !est.haySeleccion) {
        seleccionarReserva(est, fila, bloque);
      }
      render();
      return;
    }
    if (est.tipo === 'ninguno') return;
    if (est.tipo === 'movimiento') {
      finalizarMovimiento(m, est);
      return;
    }
    finalizarRango(m, est);
  }

  useEffect(() => {
    const move = (e) => manejarMove(e);
    const up = () => manejarUp();
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', up);
    return () => {
      window.removeEventListener('pointermove', move);
      window.removeEventListener('pointerup', up);
    };
  }, []);

  function aplicarFecha() {
    const iso = parseDMA(fechaInput);
    if (!iso) {
      setAviso({ tipo: 'error', mensaje: 'Fecha inválida. Usá dd/MM/yyyy.' });
      return;
    }
    setAviso(null);
    setFechaInput(formatearDMA(iso));
    setFechaInicio(iso);
  }

  const est = estadoRef.current;
  const m = modelo;

  const anchoTotal = containerWidth > 0 ? containerWidth : ANCHO_HABITACION + noches * ANCHO_NOCHE_MIN;
  const anchoNocheCalc = anchoNoche(anchoTotal, noches);
  const resto = Math.max(0, anchoTotal - ANCHO_HABITACION - noches * anchoNocheCalc);
  const anchoCol = (col) => (col === 0 ? ANCHO_HABITACION : anchoNocheCalc + (col - 1 < resto ? 1 : 0));
  const totalWidth = m
    ? ANCHO_HABITACION + m.fechas.reduce((acc, _f, i) => acc + anchoCol(i + 1), 0)
    : ANCHO_HABITACION + noches * ANCHO_NOCHE_MIN;

  // --------------------------------------------------- etiqueta de reserva
  // Desplazamiento horizontal de cada columna dentro de la fila, para ubicar la
  // etiqueta desde el check-in real hasta el check-out real aunque cruce celdas.
  const totalColumnas = m ? m.fechas.length : noches;
  const izquierdaDeCol = [];
  let acumulado = ANCHO_HABITACION;
  for (let col = 1; col <= totalColumnas; col++) {
    izquierdaDeCol[col] = acumulado;
    acumulado += anchoCol(col);
  }

  /**
   * Disposicion absoluta (dentro de la fila) de la etiqueta de un bloque: mismo
   * criterio que TablaCalendario.pintarEtiqueta en Swing. Devuelve null cuando
   * el rango visible mide menos de 14 px o no hay nada que pintar.
   */
  function disposicionEtiqueta(bloque) {
    if (!m || !bloque || !bloque.checkIn || !bloque.checkOut) return null;
    const colIni = Math.max(1, m.columnaDeFecha(bloque.checkIn));
    const colFinBruta = m.columnaDeFecha(bloque.checkOut);
    const colFin = colFinBruta >= 1 ? Math.min(colFinBruta, m.noches) : m.noches;
    if (colFin < colIni || izquierdaDeCol[colIni] == null) return null;
    const segIni = segmento(bloque, m.fechaDeColumna(colIni));
    const segFin = segmento(bloque, m.fechaDeColumna(colFin));
    if (!segIni || !segFin) return null;

    const [x0Seg] = pixeles(segIni, anchoCol(colIni));
    const [, x1Seg] = pixeles(segFin, anchoCol(colFin));
    const inicio = izquierdaDeCol[colIni] + x0Seg + PADDING_ETIQUETA;
    const fin = izquierdaDeCol[colFin] + x1Seg - PADDING_ETIQUETA;
    const ancho = fin - inicio;
    if (ancho < ANCHO_MINIMO_ETIQUETA) return null;

    // El conteo se reserva primero: el nombre se recorta al espacio restante.
    const conteo = conteoReserva(bloque.personas, bloque.bebes);
    const anchoConteo = anchoTexto(conteo);
    const anchoGuion = anchoTexto(SEPARADOR_ETIQUETA);
    const anchoNombreMinimo = anchoTexto(`M${PUNTOS_ETIQUETA}`);
    const usarGuion = conteo !== '' && ancho - anchoConteo - anchoGuion >= anchoNombreMinimo;
    const sufijo = conteo === '' ? '' : usarGuion ? SEPARADOR_ETIQUETA + conteo : ` ${conteo}`;
    const nombre = truncarTexto(bloque.titular, Math.max(0, ancho - anchoTexto(sufijo)));
    const texto = nombre + sufijo;
    if (texto === '') return null;
    return { inicio, fin, texto };
  }

  const cacheEtiquetas = new Map();
  function etiquetaDeSegmento(s) {
    if (!s || !s.bloque) return null;
    const b = s.bloque;
    const clave = [s.clase, b.reservaId, b.checkIn, b.checkOut, b.lateCheckout, b.personas, b.bebes, b.titular].join('|');
    if (!cacheEtiquetas.has(clave)) cacheEtiquetas.set(clave, disposicionEtiqueta(b));
    return cacheEtiquetas.get(clave);
  }

  function renderHandle(borde, pos) {
    const izqDir = borde === 'inicio' ? 'alargar' : 'acortar';
    const derDir = borde === 'inicio' ? 'acortar' : 'alargar';
    return (
      <span className="handle-placa" data-placa data-borde={borde} style={{ left: pos.left, width: pos.width }}>
        <span className="handle-flecha hacia-izq" data-handle data-borde={borde} data-direccion={izqDir} />
        <span className="handle-flecha hacia-der" data-handle data-borde={borde} data-direccion={derDir} />
      </span>
    );
  }

  function renderCelda(fila, col, fecha) {
    const info = celdaInfo(m, est, fila, col, fecha);
    const ancho = anchoCol(col);
    let handleInicio = null;
    let handleFin = null;

    if (est.haySeleccion && est.tipo !== 'redimension' && fila === est.filaSeleccionada && est.bloqueSeleccionado) {
      const sel = est.bloqueSeleccionado;
      const colIni = m.columnaDeFecha(sel.checkIn);
      const colFin = m.columnaDeFecha(sel.checkOut);
      if (col === colIni && colIni >= 1 && colIni <= m.noches) {
        const seg = segmento(sel, fecha);
        const [x0] = pixeles(seg, ancho);
        handleInicio = { left: x0, width: Math.min(ANCHO_PLACA, ancho - x0) };
      }
      if (col === colFin && colFin >= 1 && colFin <= m.noches) {
        const seg = segmento(sel, fecha);
        const [, x1] = pixeles(seg, ancho);
        const w = Math.min(ANCHO_PLACA, x1);
        handleFin = { left: x1 - w, width: w };
      }
    }

    const etiquetas = [];
    const celdas = info.segments.map((s, i) => {
      const [x0, x1] = pixeles(s, ancho);
      const disp = etiquetaDeSegmento(s);
      if (disp) {
        // Cada celda pinta su trozo de etiqueta: el texto se corre hacia la
        // izquierda para que se lea continuo y el recorte lo hace la celda.
        const celdaIzq = izquierdaDeCol[col] ?? 0;
        const desde = Math.max(disp.inicio, celdaIzq);
        const hasta = Math.min(disp.fin, celdaIzq + ancho);
        if (hasta > desde) {
          etiquetas.push(
            <span key={`e${i}`} className="barra-etiqueta" style={{ left: desde - celdaIzq, width: hasta - desde }}>
              <span className="barra-etiqueta-texto" style={{ left: -(desde - disp.inicio) }}>
                {disp.texto}
              </span>
            </span>,
          );
        }
      }
      return <span key={i} className={s.clase} style={{ left: x0, width: Math.max(0, x1 - x0) }} />;
    });

    return (
      <td key={fecha} className={`cal-celda ${info.clase}`} data-col={col} data-fila={fila} style={{ width: ancho }}>
        {celdas}
        {etiquetas}
        {handleInicio && renderHandle('inicio', handleInicio)}
        {handleFin && renderHandle('fin', handleFin)}
      </td>
    );
  }

  return (
    <div className="calendario-contenedor">
      <div className="calendario-toolbar">
        <label className="cal-inicio-label" htmlFor="cal-inicio">
          Inicio:
        </label>
        <input
          id="cal-inicio"
          className="cal-fecha"
          value={fechaInput}
          onChange={(e) => setFechaInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') aplicarFecha();
          }}
          placeholder="dd/MM/yyyy"
        />
        <button type="button" className="cal-refrescar" onClick={aplicarFecha} title="Refrescar">
          ↻
        </button>
        <div className="cal-leyenda">
          <span className="chip cal-libre-chip">Libre</span>
          <span className="chip cal-sucia-chip">Sucia</span>
          <span className="chip cal-barra-chip">Ocupada</span>
        </div>
      </div>

      {aviso && (
        <div className={`cal-aviso cal-aviso-${aviso.tipo}`}>
          <div className="cal-aviso-texto">
            {aviso.titulo && <strong>{aviso.titulo}: </strong>}
            {aviso.mensaje}
            {aviso.motivo && <span className="cal-aviso-motivo"> ({aviso.motivo})</span>}
            {aviso.detalle && <div className="cal-aviso-detalle">{aviso.detalle}</div>}
          </div>
          <button type="button" className="cal-aviso-cerrar" onClick={() => setAviso(null)} aria-label="Cerrar">
            ×
          </button>
        </div>
      )}

      {errorCarga && <div className="error banner">{errorCarga}</div>}
      {cargando && !m && <div className="cargando">Cargando calendario…</div>}

      <div
        className="calendario-scroll"
        ref={contRef}
        onPointerDown={manejarPointerDown}
        onPointerMove={manejarHover}
        onPointerLeave={ocultarTooltip}
        onContextMenu={manejarContextMenu}
      >
        {m && (
          <table className="calendario" style={{ width: totalWidth }}>
            <colgroup>
              <col style={{ width: ANCHO_HABITACION }} />
              {m.fechas.map((f, i) => (
                <col key={f} style={{ width: anchoCol(i + 1) }} />
              ))}
            </colgroup>
            <thead>
              <tr>
                <th className="cal-sticky cal-sticky-head">Hab.</th>
                {m.fechas.map((f) => (
                  <th key={f}>{formatearDia(f)}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {m.filas.map((fila, filaIdx) => {
                const grisFila =
                  est.tipo === 'movimiento' && filaIdx !== est.filaOrigen && !est.compatiblePorFila[filaIdx];
                return (
                  <tr key={fila.habitacionId}>
                    <td
                      className={`cal-sticky cal-habitacion${grisFila ? ' cal-celda-gris' : ''}`}
                      data-col="0"
                      data-fila={filaIdx}
                    >
                      {fila.numero}
                    </td>
                    {m.fechas.map((f, colIdx) => renderCelda(filaIdx, colIdx + 1, f))}
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {tooltip && (
        <div
          className="cal-tooltip"
          role="tooltip"
          ref={tooltipRef}
          style={{ left: tooltip.x + 14, top: tooltip.y + 14 }}
        >
          {tooltip.lineas.map((linea, i) => (
            <span key={i} className={i === 0 ? 'cal-tooltip-titulo' : 'cal-tooltip-linea'}>
              {linea}
            </span>
          ))}
        </div>
      )}

      {menu && (
        <div className="cal-menu" role="menu" style={{ left: menu.x, top: menu.y }}>
          {menu.tipo === 'reserva' ? (
            <>
              <button type="button" role="menuitem" className="cal-menu-item" onClick={editarDesdeMenu}>
                Editar reserva
              </button>
              <button type="button" role="menuitem" className="cal-menu-item" onClick={eliminarDesdeMenu}>
                Eliminar reserva
              </button>
            </>
          ) : (
            <button type="button" role="menuitem" className="cal-menu-item" onClick={nuevaDesdeMenu}>
              Nueva reserva aquí
            </button>
          )}
        </div>
      )}
    </div>
  );
}
