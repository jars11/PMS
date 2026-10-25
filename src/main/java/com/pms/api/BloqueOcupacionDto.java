package com.pms.api;

import com.pms.controllers.ControladorReservas.OcupacionHabitacion.BloqueNoche;
import com.pms.utils.EstadoReserva;
import com.pms.utils.TipoMoneda;

import java.time.LocalDate;

/**
 * Serializable read view of one occupancy bar for the web calendar. Mirrors the
 * existing {@link BloqueNoche} business record, decoupled from the JPA graph.
 * {@code checkIn} is the proposal's start day and {@code checkOut} its end day
 * (the departure day, painted as the bar's tail; the actual occupied nights are
 * half-open {@code [checkIn, checkOut)}). {@code subtotal} and {@code moneda}
 * feed the reservation hover tooltip, exactly like the Swing card.
 */
public record BloqueOcupacionDto(
        long reservaId,
        LocalDate checkIn,
        LocalDate checkOut,
        boolean lateCheckout,
        String titular,
        int personas,
        int bebes,
        float subtotal,
        TipoMoneda moneda,
        EstadoReserva estado) {

    public static BloqueOcupacionDto de(BloqueNoche b) {
        return new BloqueOcupacionDto(
                b.reservaId(), b.fechaInicio(), b.fechaFin(), b.lateCheckout(),
                b.titular(), b.personas(), b.bebes(), b.subtotal(), b.moneda(), b.estado());
    }
}
