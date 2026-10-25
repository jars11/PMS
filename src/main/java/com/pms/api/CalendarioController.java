package com.pms.api;

import com.pms.controllers.ControladorReservas;
import com.pms.entities.Reserva;
import com.pms.ui.MensajesMovimiento;
import com.pms.utils.MotivoRechazo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Calendar REST endpoints for the web PoC. Each operation delegates to the
 * existing {@link ControladorReservas} business logic unchanged: occupancy
 * reuses {@code matrizOcupacion}, move reuses {@code moverReserva}, resize
 * reuses {@code redimensionarReserva}, late-checkout reuses
 * {@code hayLateCheckoutElDia}.
 */
@RestController
@RequestMapping("/api")
public class CalendarioController {

    private final ControladorReservas reservas = ControladorReservas.getInstance();

    @GetMapping("/calendario")
    public List<OcupacionHabitacionDto> calendario(@RequestParam String desde, @RequestParam String hasta) {
        return reservas.matrizOcupacion(LocalDate.parse(desde), LocalDate.parse(hasta)).stream()
                .map(OcupacionHabitacionDto::de)
                .toList();
    }

    @GetMapping("/calendario/late-checkout")
    public ResponseEntity<Map<String, Boolean>> lateCheckout(@RequestParam long habitacionId,
                                                             @RequestParam String dia) {
        boolean hay = reservas.hayLateCheckoutElDia(habitacionId, LocalDate.parse(dia));
        return ResponseEntity.ok(Map.of("hayLateCheckout", hay));
    }

    /** Move request shape, mapped to {@code moverReserva}. */
    public record MoverRequest(long habitacionOrigenId, LocalDate noche, long habitacionDestinoId) {
    }

    /** Resize request shape, mapped to {@code redimensionarReserva}. */
    public record RedimensionarRequest(long habitacionId, LocalDate noche,
                                       LocalDate nuevoCheckIn, LocalDate nuevoCheckOut) {
    }

    /** Rejected-move error shape: fixed headline, reason code and detail line. */
    public record MoverError(String mensaje, String motivo, String detalle) {
    }

    @PostMapping("/reservas/{id}/mover")
    public ResponseEntity<?> mover(@PathVariable long id, @RequestBody MoverRequest req) {
        ControladorReservas.ResultadoMovimiento r =
                reservas.moverReserva(id, req.habitacionOrigenId(), req.noche(), req.habitacionDestinoId());
        if (!r.movida()) {
            MotivoRechazo motivo = r.motivo();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new MoverError(
                    MensajesMovimiento.RECHAZO_MOVIMIENTO,
                    motivo == null ? "" : motivo.name(),
                    MensajesMovimiento.detalle(motivo)));
        }
        return ResponseEntity.ok(Map.of("movida", true));
    }

    @PostMapping("/reservas/{id}/redimensionar")
    public ResponseEntity<ReservaVistaDto> redimensionar(@PathVariable long id,
                                                         @RequestBody RedimensionarRequest req) {
        Reserva r = reservas.redimensionarReserva(id, req.habitacionId(), req.noche(),
                req.nuevoCheckIn(), req.nuevoCheckOut());
        return ResponseEntity.ok(aVista(r));
    }

    private ReservaVistaDto aVista(Reserva r) {
        return reservas.listarReservas().stream()
                .filter(v -> v.id() == r.getId())
                .map(ReservaVistaDto::de)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("reserva no reflejada en la vista"));
    }
}
