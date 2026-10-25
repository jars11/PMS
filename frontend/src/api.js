// Thin fetch wrapper for the PMS REST API. The Vite dev server proxies /api
// to the Spring Boot backend on port 8080.

const BASE = '/api';

async function request(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });

  if (res.status === 401) {
    throw new Error('Credenciales inválidas');
  }
  if (!res.ok) {
    let msg = `Error ${res.status}`;
    try {
      const body = await res.json();
      if (body && body.mensaje) msg = body.mensaje;
    } catch {
      /* ignore non-json body */
    }
    throw new Error(msg);
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
