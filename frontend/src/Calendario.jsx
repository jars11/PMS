import { useMemo } from 'react';

function formatear(fechaISO) {
  const d = new Date(`${fechaISO}T00:00:00`);
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function fechaISO(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

// Builds a room x nights grid, marking each night occupied by an active reservation.
export default function Calendario({ habitaciones, reservas, dias = 14 }) {
  const { fechas, ocupacion } = useMemo(() => {
    const hoy = new Date();
    const fechasArr = [];
    for (let i = 0; i < dias; i++) {
      const d = new Date(hoy);
      d.setDate(d.getDate() + i);
      fechasArr.push(fechaISO(d));
    }

    const porHab = new Map();
    for (const r of reservas) {
      if (!r.activa) continue;
      const ini = r.checkIn;
      const fin = r.checkOut;
      for (const num of r.numerosHabitacion) {
        for (const f of fechasArr) {
          if (f >= ini && f < fin) {
            if (!porHab.has(num)) porHab.set(num, new Set());
            porHab.get(num).add(f);
          }
        }
      }
    }
    return { fechas: fechasArr, ocupacion: porHab };
  }, [habitaciones, reservas, dias]);

  return (
    <div className="calendario-scroll">
      <table className="calendario">
        <thead>
          <tr>
            <th className="sticky-col">Hab.</th>
            {fechas.map((f) => (
              <th key={f}>{formatear(f)}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {habitaciones.map((h) => (
            <tr key={h.id}>
              <td className="sticky-col">{h.numero}</td>
              {fechas.map((f) => {
                const ocupada = ocupacion.get(h.numero)?.has(f);
                return <td key={f} className={ocupada ? 'ocupada' : 'libre'} />;
              })}
            </tr>
          ))}
        </tbody>
      </table>
      <div className="leyenda">
        <span className="chip libre-chip">Libre</span>
        <span className="chip ocupada-chip">Ocupada</span>
      </div>
    </div>
  );
}
