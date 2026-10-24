package com.pms.api;

import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.entities.Reserva;
import com.pms.entities.TitularReserva;
import com.pms.persistence.RepositorioPersonas;
import com.pms.utils.EstadoReserva;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Reservations REST endpoints for the web PoC. Each operation delegates to the
 * existing {@link ControladorReservas} business logic (no reimplementation):
 * list reuses {@code listarReservas()}, create reuses {@code crearReservaPara},
 * edit reuses {@code actualizarReserva} by targeting the reservation's single
 * proposal.
 */
@RestController
@RequestMapping("/api/reservas")
public class ReservasController {

    private final ControladorReservas reservas = ControladorReservas.getInstance();
    private final RepositorioPersonas repositorioPersonas = new RepositorioPersonas();

    @GetMapping
    public List<ReservaVistaDto> listar() {
        return reservas.listarReservas().stream().map(ReservaVistaDto::de).toList();
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
                .map(ReservaVistaDto::de)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("reserva no reflejada en la vista"));
    }
}
