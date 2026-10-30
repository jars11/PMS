// Busquedas por fecha de la seccion Reservas.
//
// Funciones puras sobre la lista plana de reservas (ReservaVistaDto). Las
// fechas llegan y salen en ISO (YYYY-MM-DD) -igual que checkIn/checkOut del
// backend-, asi que comparar como texto es comparar por fecha y no hace falta
// construir objetos Date.

/**
 * «ingresa en la fecha X»: la reserva tiene check-in exactamente ese dia.
 * Sin fecha no filtra.
 */
export function reservasQueIngresanEn(reservas, fecha) {
  const lista = reservas || [];
  if (!fecha) return lista;
  return lista.filter((r) => r.checkIn === fecha);
}

/**
 * «se retira en la fecha Y»: la reserva tiene check-out exactamente ese dia.
 * Sin fecha no filtra.
 */
export function reservasQueSeRetiranEn(reservas, fecha) {
  const lista = reservas || [];
  if (!fecha) return lista;
  return lista.filter((r) => r.checkOut === fecha);
}

/**
 * «estadia intersecta [desde, hasta]» con la misma semantica half-open que el
 * backend: las noches son [checkIn, checkOut), asi que la estadia toca el
 * rango cuando `checkIn < hasta && desde < checkOut`. Hace falta el rango
 * completo; si falta un extremo no filtra.
 */
export function reservasQueIntersectan(reservas, desde, hasta) {
  const lista = reservas || [];
  if (!desde || !hasta) return lista;
  return lista.filter((r) => r.checkIn < hasta && desde < r.checkOut);
}
