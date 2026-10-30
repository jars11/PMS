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
- L2. Decisiones: prioridad = campo `prioridad` int en Habitacion (ascendente = mayor prioridad; 1 = arriba). El orden de `getHabitaciones()` (por número) y del calendario NO cambia; el desempate de asignación pasa a comparar prioridad (y número como segundo criterio). Prioridad inicial 0 → hasta reordenar, el desempate cae a número (comportamiento previo). Alta de habitación agrega al final (prioridad = max+1).
- L3. T1-T3 backend en 259179b (`feat(backend): room priority and habitaciones REST (alta + reordenar)`): prioridad + getHabitacionesPorPrioridad + reordenar, desempate por prioridad en ManejadorAtributosHabs y ManejadorBusquedaHuecos, REST POST /api/habitaciones y PUT /api/habitaciones/orden. `mvn test` = 375, 0 fallos (baseline 362 + 13 nuevos; el 373/386 previos incluían un report stale de CompatibilidadMovimientoTest).
- L4. T4 frontend en 110c572 (`feat(frontend): admin Habitaciones section (reorderable list + add form)`): pestaña Habitaciones (solo ADMIN) con lista drag & drop + formulario de alta. `npm run build` ok, `npm run lint` sin errores.
- L5. T5 verificación end-to-end: navegador (pestaña solo para ADMIN, lista con número/camas/atributos, alta de 301 al final, drag de 101 a posición 5 persiste con prioridad en el backend). Endpoint reordenar verificado vía API. Estado restaurado a orden por número (prioridad 1..9). Quedó una habitación de prueba 301 en la BD de desarrollo (sin endpoint de borrado todavía).
