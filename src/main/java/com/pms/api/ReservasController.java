package com.pms.api;

import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.entities.Reserva;
import com.pms.entities.TitularReserva;
import com.pms.persistence.RepositorioPersonas;
import com.pms.utils.EstadoReserva;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reservations REST endpoints for the web PoC. Each operation delegates to the
 * existing {@link ControladorReservas} business logic (no reimplementation):
 * list reuses {@code listarReservas()}, create reuses {@code crearReservaPara},
 * edit reuses {@code actualizarReserva} by targeting the reservation's single
 * proposal, the calendar context menu reuses {@code edicionDe} (prefill) and
 * {@code eliminarReserva} (payment-guarded delete), and the drag of a block
 * reuses {@code compatibilidadMovimiento} to grey out the destination rooms
 * with the same rule that rejects the drop.
 */
@RestController
@RequestMapping("/api/reservas")
public class ReservasController {

    /** Same wording as the Swing deletion guard (CalendarioPanel.eliminarReserva). */
    static final String MENSAJE_ELIMINAR_CON_PAGOS =
            "No se puede eliminar: la reserva tiene pagos asociados.";

    private final ControladorReservas reservas = ControladorReservas.getInstance();
    private final RepositorioPersonas repositorioPersonas = new RepositorioPersonas();

    @GetMapping
    public List<ReservaVistaDto> listar() {
        // El flag lateCheckout no forma parte de la vista plana de negocio
        // (ReservaVista); se toma de la entidad para que el formulario pueda
        // precargar el checkbox tambien cuando se edita desde el listado.
        Map<Long, Boolean> lateCheckoutPorId = new HashMap<>();
        for (Reserva r : reservas.getReservas()) {
            if (r.getId() != null) {
                lateCheckoutPorId.put(r.getId(), r.isLateCheckout());
            }
        }
        return reservas.listarReservas().stream()
                .map(v -> ReservaVistaDto.de(v, lateCheckoutPorId.getOrDefault(v.id(), false)))
                .toList();
    }

    /** Create/update request shape, mapped to the existing crear/actualizar methods. */
    public record ReservaRequest(
            Long titularId,
            long habitacionId,
            LocalDate checkIn,
            LocalDate checkOut,
            int camasMatrimoniales,
            int camasIndividuales,
            int cunas) {
    }

    @PostMapping
    public ResponseEntity<ReservaVistaDto> crear(@RequestBody ReservaRequest req) {
        TitularReserva titular = titular(req.titularId());
        Reserva r = reservas.crearReservaPara(titular, req.habitacionId(),
                req.checkIn(), req.checkOut(), req.camasMatrimoniales(),
                req.camasIndividuales(), req.cunas());
        return ResponseEntity.status(HttpStatus.CREATED).body(aVista(r));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReservaVistaDto> editar(@PathVariable long id, @RequestBody ReservaRequest req) {
        TitularReserva titular = titular(req.titularId());

        // Target the reservation's single proposal: pass the current room and
        // the proposal's start night so actualizarReserva finds the block to
        // replace. PoC reservations have exactly one proposal.
        Reserva actual = reservas.getReservas().stream()
                .filter(r -> r.getId() != null && r.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("reserva no existe: " + id));

        var propuesta = actual.getRHPropuestas().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("reserva sin propuesta: " + id));
        long habitacionClickId = propuesta.getHabitacion().getId();
        LocalDate nocheClick = propuesta.getFechaInicio();

        Reserva r = reservas.actualizarReserva(id, habitacionClickId, nocheClick, titular,
                req.habitacionId(), req.checkIn(), req.checkOut(),
                req.camasMatrimoniales(), req.camasIndividuales(), req.cunas(),
                EstadoReserva.PendienteDePago);

        return ResponseEntity.ok(aVista(r));
    }

    /** Body of the late check-out toggle, mapped to {@code marcarLateCheckout}. */
    public record LateCheckoutRequest(boolean lateCheckout) {
    }

    /**
     * Toggle del flag {@code lateCheckout} de la reserva, paridad con el
     * checkbox "Late check-out" de {@code DialogoReserva} en Swing: se reutiliza
     * {@code marcarLateCheckout}, que rechaza el marcado (409) cuando otra
     * reserva hace check-in el mismo dia en la habitacion.
     */
    @PostMapping("/{id}/late-checkout")
    public ResponseEntity<ReservaVistaDto> lateCheckout(@PathVariable long id,
                                                        @RequestBody LateCheckoutRequest req) {
        Reserva r = reservas.marcarLateCheckout(id, req.lateCheckout());
        return ResponseEntity.ok(aVista(r));
    }

    @GetMapping("/{id}/edicion")
    public ReservaEdicionDto edicion(@PathVariable long id,
                                     @RequestParam long habitacionId,
                                     @RequestParam String noche) {
        return ReservaEdicionDto.de(reservas.edicionDe(id, habitacionId, LocalDate.parse(noche)));
    }

    /**
     * Compatibilidad de TODAS las habitaciones para mover el bloque que cubre
     * {@code (habitacionOrigenId, noche)}: el calendario grisea con esto las
     * filas destino al iniciar el arrastre. Reutiliza {@code
     * compatibilidadMovimiento}, la misma regla que valida el soltar (409).
     */
    @GetMapping("/{id}/compatibilidad")
    public List<CompatibilidadMovimientoDto> compatibilidad(@PathVariable long id,
                                                            @RequestParam long habitacionOrigenId,
                                                            @RequestParam String noche) {
        return reservas.compatibilidadMovimiento(id, habitacionOrigenId, LocalDate.parse(noche)).stream()
                .map(CompatibilidadMovimientoDto::de)
                .toList();
    }

    /** Payment-guarded delete: 204 when removed, 409 when it has payments. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable long id) {
        if (reservas.eliminarReserva(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("mensaje", MENSAJE_ELIMINAR_CON_PAGOS));
    }

    private TitularReserva titular(Long titularId) {
        if (titularId == null) {
            throw new IllegalArgumentException("titularId es obligatorio");
        }
        TitularReserva t = repositorioPersonas.findTitularById(titularId);
        if (t == null) {
            throw new IllegalArgumentException("titular no existe: " + titularId);
        }
        return t;
    }

    private ReservaVistaDto aVista(Reserva r) {
        return reservas.listarReservas().stream()
                .filter(v -> v.id() == r.getId())
                .map(v -> ReservaVistaDto.de(v, r.isLateCheckout()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("reserva no reflejada en la vista"));
    }
}
