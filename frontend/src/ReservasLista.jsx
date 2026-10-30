import { useState } from 'react';
import {
  reservasQueIngresanEn,
  reservasQueIntersectan,
  reservasQueSeRetiranEn,
} from './busquedaReservas';

// Columnas ordenables: `valor` es el dato que se compara y ordena la tabla.
const COLUMNAS = [
  { clave: 'id', titulo: 'ID', valor: (r) => r.id },
  { clave: 'titular', titulo: 'Titular', valor: (r) => r.titular },
  { clave: 'habitacion', titulo: 'Habitación', valor: (r) => (r.numerosHabitacion || []).join(', ') },
  { clave: 'checkIn', titulo: 'Check-in', valor: (r) => r.checkIn },
  { clave: 'checkOut', titulo: 'Check-out', valor: (r) => r.checkOut },
  { clave: 'estado', titulo: 'Estado', valor: (r) => r.estado },
];

/** Compara dos valores de una columna: números como números, texto con locale. */
function compararValores(a, b) {
  if (typeof a === 'number' && typeof b === 'number') return a - b;
  return String(a ?? '').localeCompare(String(b ?? ''), 'es', { numeric: true });
}

export default function ReservasLista({ reservas }) {
  // Orden activo: la primera vez que se clickea un header ordena ascendente y
  // el segundo click invierte. Sin columna elegida se respeta el orden de la API.
  const [orden, setOrden] = useState({ clave: null, ascendente: true });
  const [filtros, setFiltros] = useState({ ingresa: '', retira: '', desde: '', hasta: '' });

  function ordenarPor(clave) {
    setOrden((previo) =>
      previo.clave === clave
        ? { clave, ascendente: !previo.ascendente }
        : { clave, ascendente: true },
    );
  }

  function cambiarFiltro(campo) {
    return (e) => setFiltros((previos) => ({ ...previos, [campo]: e.target.value }));
  }

  const conFiltros = reservasQueIntersectan(
    reservasQueSeRetiranEn(reservasQueIngresanEn(reservas, filtros.ingresa), filtros.retira),
    filtros.desde,
    filtros.hasta,
  );
  const columnaOrden = COLUMNAS.find((c) => c.clave === orden.clave) || null;
  const visibles = columnaOrden
    ? [...conFiltros].sort((a, b) => {
        const diferencia = compararValores(columnaOrden.valor(a), columnaOrden.valor(b));
        return orden.ascendente ? diferencia : -diferencia;
      })
    : conFiltros;
  const hayFiltros = Object.values(filtros).some((v) => v !== '');

  return (
    <div className="reservas-lista">
      <div className="reservas-busqueda">
        <label>
          Ingresa el
          <input type="date" value={filtros.ingresa} onChange={cambiarFiltro('ingresa')} />
        </label>
        <label>
          Se retira el
          <input type="date" value={filtros.retira} onChange={cambiarFiltro('retira')} />
        </label>
        <label>
          Estadía intersecta desde
          <input type="date" value={filtros.desde} onChange={cambiarFiltro('desde')} />
        </label>
        <label>
          hasta
          <input type="date" value={filtros.hasta} onChange={cambiarFiltro('hasta')} />
        </label>
      </div>

      <table className="tabla">
        <thead>
          <tr>
            {COLUMNAS.map((col) => {
              const activa = orden.clave === col.clave;
              const marca = activa ? (orden.ascendente ? '▲' : '▼') : '';
              return (
                <th
                  key={col.clave}
                  className="ordenable"
                  aria-sort={activa ? (orden.ascendente ? 'ascending' : 'descending') : 'none'}
                >
                  <button type="button" className={`tabla-orden${activa ? ' activo' : ''}`} onClick={() => ordenarPor(col.clave)}>
                    {col.titulo}
                    <span className="tabla-orden-marca" aria-hidden="true">
                      {marca}
                    </span>
                  </button>
                </th>
              );
            })}
          </tr>
        </thead>
        <tbody>
          {visibles.map((r) => (
            <tr key={r.id}>
              <td>{r.id}</td>
              <td>{r.titular}</td>
              <td>{r.numerosHabitacion.join(', ')}</td>
              <td>{r.checkIn}</td>
              <td>{r.checkOut}</td>
              <td>{r.estado}</td>
            </tr>
          ))}
          {visibles.length === 0 && (
            <tr>
              <td colSpan={COLUMNAS.length} className="vacio">
                {hayFiltros ? 'No hay reservas para esa búsqueda.' : 'No hay reservas.'}
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
