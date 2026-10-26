# Calendario web: drag & drop completo (paridad con Swing)

Replicar en el frontend React el calendario nocturno del Swing con drag & drop completo: crear reservas arrastrando sobre noches libres, mover reservas entre habitaciones y redimensionar con flechas, con navegación por fecha de inicio y barras de ocupación contiguas. Reutilizar la lógica de negocio existente (ControladorReservas) sin reescribirla, exponiéndola por REST.

## Specs

- S1. `el drag & drop para crear reservas no funciona. Haz que el calendario se vea bien y funcione correctamente como estaba antes en java swing`
- S2. Alcance elegido por el usuario: calendario completo = crear por drag + mover reservas + redimensionar con flechas + navegación por fecha de inicio.
- S3. Crear por drag: press sobre una noche libre arranca una selección de rango (misma habitación). Un click sin arrastre no crea nada. Arrastrar a otra columna pinta un preview fantasma. Al soltar, si el rango cruza una noche ocupada se muestra "El rango cruza una noche ocupada. Elegí noches libres." y no se crea nada; si está libre se abre el formulario de alta precargado con habitación, checkIn y checkOut. El rango es half-open [checkIn, checkOut): las columnas seleccionadas son check-in y check-out INCLUSIVAS (la columna de check-out es el día de salida, no una noche más).
- S4. Mover por drag: press sobre una noche ocupada de una reserva arranca, al superar el umbral de arrastre, un movimiento del bloque clickeado. Las filas incompatibles se pintan en gris y no cuentan como destino. Soltar sobre una fila compatible llama a moverReserva; un rechazo muestra el mensaje exacto y el motivo; soltar sin destino cancela en silencio. Click simple (sin arrastre) selecciona la reserva.
- S5. Redimensionar: al seleccionar una reserva se muestran flechas en cada extremo visible (check-in: alargar/atrás y acortar/adelante; check-out: acortar/atrás y alargar/adelante). Arrastrar una flecha extiende o acorta la estadía, clampeada contra la primera noche ocupada y contra el borde de la ventana; acortar deja al menos una noche. Al soltar llama a redimensionarReserva.
- S6. Navegación: selector de fecha de inicio (dd/MM/yyyy) + botón refrescar. La cantidad de noches visibles se adapta al ancho disponible (default 14).
- S7. Pintado de barras contiguas: la barra cubre del día de check-in al día de check-out INCLUSIVE. Check-in con inset 25% a la izquierda; cola de check-out al 25% (o 75% con late check-out) en el día de salida; segmentos interiores [0,1]; separación de 2px en bordes compartidos. La ocupación real es half-open [checkIn, checkOut).
- S8. Backend REST reutiliza ControladorReservas sin reimplementar: ocupación (matrizOcupacion), mover (moverReserva), redimensionar (redimensionarReserva), late-checkout (hayLateCheckoutElDia). No se toca Swing ni se rompen los tests existentes.
- S9. `Quiero todo igual a como estaba en java swing` — paridad visual/funcional restante del calendario Swing (tooltip de hover, menú contextual, toggle de late check-out, navegación Shift+rueda). El formato de etiqueta de barras (titular + personas/bebés) ya se replicó en L9. Nota: Swing no tiene doble click para editar; no se agrega.

## Contrato REST (fijo)

### GET /api/calendario?desde=YYYY-MM-DD&hasta=YYYY-MM-DD
200 → lista de habitaciones ordenadas por número:
```json
[
  {
    "habitacionId": 1,
    "numero": 101,
    "bloques": [
      {
        "reservaId": 6,
        "checkIn": "2027-01-10",
        "checkOut": "2027-01-15",
        "lateCheckout": false,
        "titular": "Agustin Rivero",
        "personas": 2,
        "bebes": 0,
        "estado": "PendienteDePago"
      }
    ]
  }
]
```
`checkIn` = propuesta.fechaInicio; `checkOut` = propuesta.fechaFin (día de salida, para pintar la cola). Noches ocupadas = [checkIn, checkOut). Solo reservas activas.

### POST /api/reservas/{id}/mover
Body: `{"habitacionOrigenId": 1, "noche": "2027-01-12", "habitacionDestinoId": 3}`
200 → `{"movida": true}`. Rechazo (sin mutar): 409 → `{"mensaje": "...", "motivo": "CAPACIDAD_INSUFICIENTE"}`.

### POST /api/reservas/{id}/redimensionar
Body: `{"habitacionId": 1, "noche": "2027-01-12", "nuevoCheckIn": "2027-01-10", "nuevoCheckOut": "2027-01-18"}`
200 → `ReservaVistaDto`. Errores: 400 (fechas inválidas) / 409 (habitación ocupada) → `{"mensaje": ...}`.

### GET /api/calendario/late-checkout?habitacionId=1&dia=YYYY-MM-DD
200 → `{"hayLateCheckout": true}`.

## Tasks

- T1 [S8] Backend REST: DTOs de ocupación/mover/redimensionar + los 4 endpoints reutilizando ControladorReservas. Tests REST (ocupación con bloques, mover ok/rechazo, redimensionar ok/rechazo, late-checkout). Ruta: inline. Commit: feat(api): calendar occupancy, move and resize endpoints.
- T2 [S1,S3,S6,S7] Frontend calendario: grilla habitaciones × noches, barras contiguas (GeometriaBarra), selector de fecha de inicio + refrescar, drag para crear con preview y validación de rango libre. Ruta: writer. Commit: a8e9874 (fusionado con T3).
- T3 [S4,S5] Frontend: selección de reserva con flechas, drag para mover (filas incompatibles en gris), drag para redimensionar. Ruta: writer. Commit: a8e9874 (fusionado con T2).
- T4 [S1] Verificación end-to-end: mvn test verde, build frontend, pasada manual en ego-lite (crear, seleccionar, barras). Ruta: inline. Commit: (ninguno; cierra la feature).
- T5 [S9] Tooltip al pasar el mouse sobre una reserva: titular, fechas, estado, personas/bebés, subtotal (paridad con TablaCalendario). Ruta: writer. Commit: 4d10f9d.
- T6 [S9] Menú contextual con click derecho: editar reserva y alta en una noche libre (paridad con CalendarioPanel.mostrarMenu). Ruta: writer. Commit: 96547ec.
- T7 [S9] Toggle de late check-out por reserva. Ruta: writer. Commit: 5b7769c.
- T8 [S9] Navegación Shift+rueda día a día (Swing no tiene doble click). Ruta: writer. Commit: b9f1063.

## Log

- L1. `el drag & drop para crear reservas no funciona. Haz que el calendario se vea bien y funcione correctamente como estaba antes en java swing`
- L2. Decisión de alcance (usuario eligió): calendario completo — crear + mover + redimensionar + navegación.
- L3. Fix previo no relacionado: App.css no se importaba (interfaz en blanco y negro), corregido en App.jsx.
- L4. T1 backend completado: endpoints de ocupación/mover/redimensionar/late-checkout + DTOs + tests REST (83f20d2). `mvn test` = 328 tests, 0 fallos.
- L5. T2+T3 frontend implementados por writer en un solo commit cohesivo (a8e9874): Calendario.jsx reescrito (máquina de estados RANGO/MOVIMIENTO/REDIMENSION), barras contiguas, navegación, drag crear/mover/redimensionar. `npm run build` y `npm run lint` verdes.
- L6. T4 verificación end-to-end en ego-lite: grilla + barras pintadas (101..203), drag para crear abre el formulario precargado (habitación 201, check-in/check-out correctos), click selecciona reserva y muestra las flechas de redimensionar. Backend mover/redimensionar cubiertos por tests REST.
- L7. Revisión RDD: slice frontend (`a8e9874`) aprobado (lineage `review-854747b5e3343d68`). Slice backend (rango desde `e8d2f43`, lineage `review-d4b9bdd9af4b021c`): 3 reviewers + refuter corrieron; el refuter marcó **R4-1 CRITICAL** (`recargar()` perdía la selección tras move/resize, sin guard in-flight, fila destino sin re-validar). Corrección aplicada en `frontend/src/Calendario.jsx` (36 líneas de diff): preservar selección tras recargar, `enVueloRef`, validación de fila destino. Build verde. Bloqueo: el *correction plan* capture se rechaza como `stale` (inconsistencia `authority.revision` 2ac2542a… vs `expected-revision` 454c6a4e… del correction request); el review del backend queda en `correction_required`.
- L8. Bug reportado: al abrir el diálogo de nueva reserva, las barras del calendario se pintaban por arriba. Causa: `.modal-backdrop` sin `z-index` (auto) vs barras `z-index: 1` y sticky `z-index: 5`. Fix: `z-index: 100` en `.modal-backdrop` (App.css). Verificado en navegador: modal z-index 100 > barras 1 / sticky 5, el form-card recibe el click.
- L9. Etiquetas de barras (pedido del usuario): se replica el formato de Swing (`EtiquetaReserva`): `Titular — 2👤 + 👶`, conteo reservado primero, nombre truncado con `…`, pintado sobre el rango (cada celda pinta su trozo con el texto desplazado y clip por celda), color `#331a1a`, `pointer-events: none`. Verificado en navegador: `Agustin Rivero — 3👤`, `Ag… 2👤`. Build/lint verdes (fc9abbc).
- L10. Pedido del usuario: `Quiero todo igual a como estaba en java swing`. La etiqueta de barras ya quedó (L9). Quedan pendientes T5-T8: tooltip de hover, menú contextual (click derecho), toggle de late check-out, y Shift+rueda / doble click para editar. Al retomar: leer este documento hasta `## Log`, reconciliar y continuar por T5.
- L11. T5 completado (4d10f9d): tooltip de reserva con el contenido de Swing (`Reserva #id`, Titular, Habitación, Check-in/out `dd/MM/yyyy`, Noches, Personas · Bebés, Subtotal `USD` es-UY, Estado, Late check-out: sí). Backend: `BloqueOcupacionDto` expone `subtotal`/`moneda` (+ test `ocupacionIncluyeSubtotalYMonedaDelBloque`). Verificado en navegador: `Subtotal: USD 6.000,00`. `mvn -Dtest=CalendarioControllerTest` 7 verdes; build/lint verdes. Backend reiniciado (job-5) para servir el DTO nuevo. Siguiente: T6 (menú contextual).
- L12. T6 completado (96547ec): menú contextual (click derecho) con 3 acciones. Backend: `EdicionReserva` + `titularId`; `GET /api/reservas/{id}/edicion` (prefill) y `DELETE /api/reservas/{id}` (204 / 409 con `No se puede eliminar: la reserva tiene pagos asociados.`); test `ReservasEdicionEliminarTest` (5). Frontend: `api.edicionReserva`/`eliminarReserva`, menú (Editar/Eliminar sobre reserva; Nueva reserva aquí sobre libre), `ReservaForm` precarga titularId+camas. Verificado en navegador: `[Editar reserva, Eliminar reserva]` y `[Nueva reserva aquí]`. `mvn test` 334 verdes; build/lint verdes. Siguiente: T7 (toggle late check-out).
- L13. T7 completado (5b7769c): toggle `Late check-out` en el form de reserva (checkbox), precargado en edición; al guardar aplica la regla de Swing (alta: solo si marcado; edición: siempre); 409 → avisa y destilda sin revertir la reserva. Backend: `POST /api/reservas/{id}/late-checkout` + `ReservaVistaDto.lateCheckout` (aditivo); tests. Verificado en navegador: checkbox presente y marcado en edición. `mvn test` 337 verdes; build/lint verdes. Siguiente: T8 (Shift+rueda / doble click).
- L14. T8 completado (b9f1063): Shift+rueda navega la fecha de inicio día a día (arriba=anterior, abajo=siguiente), con listener nativo no-passive (React registra `wheel` como passive). Swing no tiene doble click, así que no se agregó. Verificado en navegador: `20/10 → 21/10 → 20/10`. build/lint verdes. **Cierra la paridad S9 (T5–T8).**
- L15. Ajuste pedido: las placas de las flechas de redimensionar (check-in y check-out) tienen el mismo ancho fijo (ANCHO_PLACA = 18px); la del check-out entra en la celda anterior (última noche de la misma reserva), como Swing. Verificado en navegador: inicio 18px, fin 18px (antes el fin ~15px). build/lint verdes.
