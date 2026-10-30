# Calendario: resaltado, líneas, botón Hoy, mover relajado y animación admin

Cinco cambios de UI/UX del calendario y de la lista de habitaciones admin, más el fix de la regla de mover.

## Specs

- S1. `Al poner el cursor sobre una reserva de uno o más bloques resalta todos los bloques visibles de esa reserva como con un glow blanco`
- S2. `Añade lineas horizontales en la columna de habitaciones para dividir las celdas de habitaciones`
- S3. `Añade un botón de de Hoy al lado de la fecha de inicio`
- S4. `Arregla que al mover un rango de habitación no deja volver a la fila original, como si exigiera más atributos, cuando en realidad en lo que de tiene que fijar es en lo que se pidió en Solicitud y qué es lo que aceptó relajar el usuario que se supone wue está guardado en el contrato. O sea que todas las habitaciones que cumplan lo que acepta el usuario deberían estar habilitadas para mover el rango`
- S5. `Al mover las habitaciones en la lista de admin, haz una animación de que las habitaciones se van moviendo hacia arriba o hacia abajo según vaya moviendo la habitación seleccionada`

## Tasks

- T1 [S4] Backend: `moverReserva`/`compatibilidadMovimiento` deben fijarse en los atributos pedidos en la `Solicitud` (RHSolicitada) y en el perfil de relajación del `ContratoReserva`, no en los atributos de la habitación asignada que guarda la RHPropuesta. Tests.
- T2 [S1] Frontend: glow blanco sobre todos los bloques visibles de la reserva al pasar el cursor.
- T3 [S2] Frontend: líneas horizontales que dividen las celdas de la columna de habitaciones.
- T4 [S3] Frontend: botón "Hoy" al lado del campo de fecha de inicio.
- T5 [S5] Frontend: animación de las filas al reordenar en la lista admin de habitaciones.
- T6 [S1,S2,S3,S4,S5] Verificación end-to-end: `mvn test` verde, `npm run build`/`lint`, pasada en navegador.

## Log

- L1. `Al poner el cursor sobre una reserva de uno o más bloques resalta todos los bloques visibles de esa reserva como con un glow blanco. Añade lineas horizontales en la columna de habitaciones para dividir las celdas de habitaciones. Añade un botón de de Hoy al lado de la fecha de inicio. Arregla que al mover un rango de habitación no deja volver a la fila original, como si exigiera más atributos, cuando en realidad en lo que de tiene que fijar es en lo que se pidió en Solicitud y qué es lo que aceptó relajar el usuario que se supone wue está guardado en el contrato. O sea que todas las habitaciones que cumplan lo que acepta el usuario deberían estar habilitadas para mover el rango. Al mover las habitaciones en la lista de admin, haz una animación de que las habitaciones se van moviendo hacia arriba o hacia abajo según vaya moviendo la habitación seleccionada`
- L2. Decisión: la rama se basó en `main`, pero T5/S5 depende de `Habitaciones.jsx` que vive en `feat/prioridad-habitaciones`. Se mergeó `feat/prioridad-habitaciones` en esta rama (commit de merge 81ccaef) para poder implementar T5.
- L3. T1 backend en 489cc1e (`fix(backend): move compatibility uses solicitud + contrato relaxation`): `evaluarMovimiento` ya no exige los atributos de la habitación asignada; usa `atributosExigidosParaMover` (Solicitud/RHSolicitada por fechas + `contrato.isAtributosRelajados()`). 3 tests nuevos.
- L4. T2-T4 frontend en 541fc08 (`feat(frontend): calendar hover glow, room column lines, Hoy button`): glow blanco (`reservaHover` → `.barra-resaltada`), líneas horizontales (`.cal-habitacion` border-bottom) y botón Hoy (`aplicarFechaISO`).
- L5. T5 frontend en 93fa149 (`feat(frontend): animate admin room list reorder`): preview de desplazamiento durante el drag + FLIP al soltar.
- L6. T6 verificación: `mvn test` = 378 (362 base + 13 prioridad + 3 T1), 0 fallos; `npm run build` ok; `npm run lint` sin errores.
- L7. T6 pasada en navegador (ego-lite, admin): botón Hoy navega a hoy ✓; glow blanco aplica `barra-resaltada` a los 4 segmentos visibles de la reserva al hover ✓; líneas horizontales en `.cal-habitacion` (border-bottom) ✓; reordenar en admin sigue funcionando con la animación (101 → posición 5) ✓. El fix de mover (S4) queda cubierto por los 3 tests unitarios (RED→GREEN).
