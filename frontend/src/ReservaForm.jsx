import { useEffect, useState } from 'react';

export default function ReservaForm({ titulares, habitaciones, reserva, precarga, onCancelar, onGuardado }) {
  const esEdicion = Boolean(reserva);
  const [forma, setForma] = useState({
    titularId: reserva?.titularId ?? '',
    habitacionId: reserva ? (reserva.habitacionId ?? '') : (precarga?.habitacionId ?? ''),
    checkIn: reserva?.checkIn ?? precarga?.checkIn ?? '',
    checkOut: reserva?.checkOut ?? precarga?.checkOut ?? '',
    camasMatrimoniales: reserva?.camasMatrimoniales ?? 1,
    camasIndividuales: reserva?.camasIndividuales ?? 0,
    cunas: reserva?.cunas ?? 0,
    lateCheckout: reserva?.lateCheckout ?? false,
  });
  const [error, setError] = useState('');
  const [cargando, setCargando] = useState(false);

  // Seed the titular/room when data loads (create) or when editing a reservation.
  useEffect(() => {
    setForma((f) => {
      const titularId = !f.titularId && titulares.length > 0 ? titulares[0].id : f.titularId;
      if (reserva) {
        // La edicion desde el calendario ya trae habitacionId; la de la lista,
        // solo el numero, asi que se resuelve por numero como fallback.
        const hab =
          reserva.habitacionId != null
            ? null
            : habitaciones.find((h) => h.numero === reserva.numerosHabitacion?.[0]);
        return {
          titularId,
          habitacionId: reserva.habitacionId ?? (hab ? hab.id : ''),
          checkIn: reserva.checkIn,
          checkOut: reserva.checkOut,
          camasMatrimoniales: reserva.camasMatrimoniales ?? 1,
          camasIndividuales: reserva.camasIndividuales ?? 0,
          cunas: reserva.cunas ?? 0,
          lateCheckout: reserva.lateCheckout ?? false,
        };
      }
      return { ...f, titularId };
    });
  }, [reserva, habitaciones, titulares]);

  function set(key, val) {
    setForma((f) => ({ ...f, [key]: val }));
  }

  return (
    <div className="modal-backdrop">
      <form
        className="form-card"
        onSubmit={async (e) => {
          e.preventDefault();
          setError('');
          setCargando(true);
          const payload = {
            titularId: Number(forma.titularId),
            habitacionId: Number(forma.habitacionId),
            checkIn: forma.checkIn,
            checkOut: forma.checkOut,
            camasMatrimoniales: Number(forma.camasMatrimoniales),
            camasIndividuales: Number(forma.camasIndividuales),
            cunas: Number(forma.cunas),
          };
          try {
            await onGuardado(reserva, payload, forma.lateCheckout);
          } catch (err) {
            setError(err.message);
            setCargando(false);
          }
        }}
      >
        <h2>{esEdicion ? 'Editar reserva' : 'Nueva reserva'}</h2>

        <label>
          Titular
          <select value={forma.titularId} onChange={(e) => set('titularId', e.target.value)}>
            {titulares.map((t) => (
              <option key={t.id} value={t.id}>
                {t.nombre}
              </option>
            ))}
          </select>
        </label>

        <label>
          Habitación
          <select value={forma.habitacionId} onChange={(e) => set('habitacionId', e.target.value)}>
            <option value="">Seleccionar…</option>
            {habitaciones.map((h) => (
              <option key={h.id} value={h.id}>
                {h.numero} (cap. {h.capacidad})
              </option>
            ))}
          </select>
        </label>

        <div className="fila">
          <label>
            Check-in
            <input
              type="date"
              value={forma.checkIn}
              onChange={(e) => set('checkIn', e.target.value)}
            />
          </label>
          <label>
            Check-out
            <input
              type="date"
              value={forma.checkOut}
              onChange={(e) => set('checkOut', e.target.value)}
            />
          </label>
        </div>

        <div className="fila">
          <label>
            Matrimoniales
            <input
              type="number"
              min="0"
              value={forma.camasMatrimoniales}
              onChange={(e) => set('camasMatrimoniales', e.target.value)}
            />
          </label>
          <label>
            Individuales
            <input
              type="number"
              min="0"
              value={forma.camasIndividuales}
              onChange={(e) => set('camasIndividuales', e.target.value)}
            />
          </label>
          <label>
            Cunas
            <input
              type="number"
              min="0"
              value={forma.cunas}
              onChange={(e) => set('cunas', e.target.value)}
            />
          </label>
        </div>

        <label className="check">
          <input
            type="checkbox"
            checked={forma.lateCheckout}
            onChange={(e) => set('lateCheckout', e.target.checked)}
          />
          Late check-out
        </label>

        {error && <div className="error">{error}</div>}

        <div className="acciones">
          <button type="button" className="secundario" onClick={onCancelar}>
            Cancelar
          </button>
          <button type="submit" disabled={cargando}>
            {cargando ? 'Guardando…' : 'Guardar'}
          </button>
        </div>
      </form>
    </div>
  );
}
