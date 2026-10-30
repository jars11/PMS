# Prioridad de habitaciones (admin) + sección Habitaciones

Sección "Habitaciones" en el sector admin: lista reordenable de habitaciones (número, distribución de camas, atributos) cuyo orden ascendente define la prioridad de asignación, más un formulario de alta.

## Specs

- S1. `En el sector de admin, además de tener el calendario y las reservas, pon la sección habitaciones` — pestaña "Habitaciones" visible solo para el rol ADMIN (además de Calendario y Reservas).
- S2. `añade una lista de habitaciones con filas móviles que diga número de habitación, distribución de camas y atributos` — lista reordenable por drag & drop; cada fila muestra número, camas (matrimoniales/individuales/cunas/colchones) y atributos.
- S3. `el orden ascendente de filas implique la prioridad que tiene una habitación sobre otra si ambas pueden entrar como misma propuesta de reserva (antes se seleccionaba la que tenía menor número de habitación)` — el desempate de asignación pasa de "menor número de habitación" a "mayor prioridad" (orden ascendente de filas = prioridad 1 es la de arriba).
- S4. `añade una parte para agregar habitaciones` — formulario de alta (número, camas, atributos, tarifa por noche).

## Tasks

- T1 [S3] Backend: campo `prioridad` en `Habitacion` + `HabitacionVistaDto` + orden por prioridad en `ControladorHabitaciones` + alta asigna prioridad al final. Tests de entidad/controlador.
- T2 [S3] Backend: desempate por prioridad (en vez de menor número) en `ManejadorAtributosHabs.brindarHabitacionesDisponibles` y `ManejadorBusquedaHuecos.Alternativa.claveHabitaciones`. Tests.
- T3 [S1,S4] Backend REST: `POST /api/habitaciones` (alta) y `PUT /api/habitaciones/orden` (reordenar por lista de ids). Tests REST.
- T4 [S1,S2,S4] Frontend: pestaña "Habitaciones" (solo admin) con lista reordenable (número/camas/atributos) + formulario de alta.
- T5 [S1,S3] Verificación end-to-end: `mvn test` verde, `npm run build`/`lint`, pasada en navegador.

## Log

- L1. `En el sector de admin, añade una lista de habitaciones son filas móviles que diga número de habitación, distribución de camas y atributos en donde el orden ascendente de filas implique la prioridad que tiene una habitación sobre otra si ambas pueden entrar como misma propuesta de reserva (antes se seleccionaba la que tenía menor número de habitación). En el sector de admin, además de tener el calendario y las reservas, pon la sección habitaciones en donde van la lista de habitaciones que te mencioné, y añade una parte para agregar habitaciones`
