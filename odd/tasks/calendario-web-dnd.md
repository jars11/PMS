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
- T2 [S1,S3,S6,S7] Frontend calendario: grilla habitaciones × noches, barras contiguas (GeometriaBarra), selector de fecha de inicio + refrescar, drag para crear con preview y validación de rango libre. Ruta: writer. Commit: feat(frontend): interactive calendar with drag-to-create.
- T3 [S4,S5] Frontend: selección de reserva con flechas, drag para mover (filas incompatibles en gris), drag para redimensionar. Ruta: writer. Commit: feat(frontend): drag-to-move and resize in calendar.
- T4 [S1] Verificación end-to-end: mvn test verde, build frontend, pasada manual en ego-lite (crear, mover, redimensionar). Ruta: inline. Commit: (ninguno; cierra la feature).

## Log

- L1. `el drag & drop para crear reservas no funciona. Haz que el calendario se vea bien y funcione correctamente como estaba antes en java swing`
- L2. Decisión de alcance (usuario eligió): calendario completo — crear + mover + redimensionar + navegación.
- L3. Fix previo no relacionado: App.css no se importaba (interfaz en blanco y negro), corregido en App.jsx.
