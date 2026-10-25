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
};
