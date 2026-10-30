package com.pms.api;

import com.pms.controllers.ControladorReservas.EdicionReserva;
import com.pms.utils.EstadoReserva;

import java.time.LocalDate;

/**
 * Serializable edit view of ONE reservation proposal for the web calendar
 * context menu. Mirrors the existing {@link EdicionReserva} business record so
 * the edit form can prefill titular and beds without touching the JPA graph.
 */
public record ReservaEdicionDto(
        long reservaId,
        long habitacionId,
        LocalDate noche,
        String titular,
        Long titularId,
        LocalDate checkIn,
        LocalDate checkOut,
        int camasMatrimoniales,
        int camasIndividuales,
        int cunas,
        EstadoReserva estado,
        boolean lateCheckout) {

    public static ReservaEdicionDto de(EdicionReserva e) {
        return new ReservaEdicionDto(e.reservaId(), e.habitacionId(), e.nocheObjetivo(),
                e.titular(), e.titularId(), e.checkIn(), e.checkOut(),
                e.camasMatrimoniales(), e.camasIndividuales(), e.cunas(),
                e.estado(), e.lateCheckout());
    }
}
