// Thin fetch wrapper for the PMS REST API. The Vite dev server proxies /api
// to the Spring Boot backend on port 8080.

const BASE = '/api';

async function request(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });

  if (res.status === 401) {
    const err = new Error('Credenciales inválidas');
    err.status = 401;
    throw err;
  }
  if (!res.ok) {
    let msg = `Error ${res.status}`;
    try {
      const body = await res.json();
      if (body && body.mensaje) msg = body.mensaje;
    } catch {
      /* ignore non-json body */
    }
    const err = new Error(msg);
    err.status = res.status;
    throw err;
  }
  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  login: (usuario, password) =>
    request('/auth/login', { method: 'POST', body: JSON.stringify({ usuario, password }) }),

  habitaciones: () => request('/habitaciones'),
  titulares: () => request('/titulares'),
  reservas: () => request('/reservas'),

  /**
   * Alta de titular: `{nombres:[], apellidos:[], numeroDocumento, tipoDocumento,
   * celular, telefonoFijo, email}` → `TitularVistaDto` con su `id` y documento.
   */
  crearTitular: (datos) =>
    request('/titulares', { method: 'POST', body: JSON.stringify(datos) }),

  /**
   * Titular registrado con ese documento, o `null` cuando todavía no existe
   * (el backend responde 404, que es la señal de "dale de alta los datos").
   */
  buscarTitularPorDocumento: async (documento) => {
    try {
      return await request(`/titulares/buscar?documento=${encodeURIComponent(documento)}`);
    } catch (err) {
      if (err.status === 404) return null;
      throw err;
    }
  },

  crearReserva: (payload) =>
    request('/reservas', { method: 'POST', body: JSON.stringify(payload) }),

  editarReserva: (id, payload) =>
    request(`/reservas/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),

  /** Prefill of ONE proposal, for the calendar context menu edit flow. */
  edicionReserva: (id, habitacionId, noche) =>
    request(
      `/reservas/${id}/edicion?habitacionId=${encodeURIComponent(habitacionId)}` +
        `&noche=${encodeURIComponent(noche)}`,
    ),

  /**
   * Compatibilidad de cada habitacion para mover el bloque que cubre
   * (habitacionOrigenId, noche): `[{habitacionId, motivo}]`, con `motivo` null
   * cuando la habitacion es compatible. Es la MISMA regla que valida el soltar
   * (409), asi que el calendario grisea con esto las filas destino.
   */
  compatibilidadReserva: (id, habitacionOrigenId, noche) =>
    request(
      `/reservas/${id}/compatibilidad?habitacionOrigenId=${encodeURIComponent(habitacionOrigenId)}` +
        `&noche=${encodeURIComponent(noche)}`,
    ),

  /** Payment-guarded delete: 204 on success, 409 with {mensaje} otherwise. */
  eliminarReserva: (id) => request(`/reservas/${id}`, { method: 'DELETE' }),

  /**
   * Toggle del flag late check-out de la reserva. El backend rechaza el marcado
   * con 409 cuando otra reserva hace check-in el mismo dia en la habitacion.
   */
  marcarLateCheckout: (id, lateCheckout) =>
    request(`/reservas/${id}/late-checkout`, {
      method: 'POST',
      body: JSON.stringify({ lateCheckout }),
    }),

  calendario: (desde, hasta) =>
    request(`/calendario?desde=${encodeURIComponent(desde)}&hasta=${encodeURIComponent(hasta)}`),

  lateCheckout: (habitacionId, dia) =>
    request(`/calendario/late-checkout?habitacionId=${habitacionId}&dia=${encodeURIComponent(dia)}`),

  redimensionarReserva: (id, payload) =>
    request(`/reservas/${id}/redimensionar`, { method: 'POST', body: JSON.stringify(payload) }),

  /**
   * Busca alternativas de habitaciones para los bloques pedidos (seccion
   * "Propuesta de estadia"). Sin capacidad suficiente devuelve `{propuestas: []}`.
   */
  buscarPropuestas: (bloques) =>
    request('/propuestas/buscar', { method: 'POST', body: JSON.stringify({ bloques }) }),

  /** Persiste la propuesta elegida como reserva real y devuelve `ReservaVistaDto`. */
  persistirPropuestas: (titularId, bloques) =>
    request('/propuestas/persistir', {
      method: 'POST',
      body: JSON.stringify({ titularId, bloques }),
    }),

  moverReserva: async (id, payload) => {
    const res = await fetch(`${BASE}/reservas/${id}/mover`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    if (res.ok) return res.json();
    let body = {};
    try {
      body = await res.json();
    } catch {
      /* ignore non-json body */
    }
    const err = new Error(body.mensaje || `Error ${res.status}`);
    err.status = res.status;
    err.mensaje = body.mensaje || err.message;
    err.motivo = body.motivo || '';
    err.detalle = body.detalle || '';
    throw err;
  },
};
