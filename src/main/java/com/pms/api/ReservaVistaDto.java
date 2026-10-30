package com.pms.api;

import com.pms.controllers.ControladorReservas.ReservaVista;
import com.pms.utils.EstadoReserva;

import java.time.LocalDate;
import java.util.Set;

/**
 * Serializable read view of a reservation for the list. Mirrors the existing
 * {@link ReservaVista} business record, decoupled from the JPA graph.
 */
public record ReservaVistaDto(
        long id,
        String titular,
        Set<Integer> numerosHabitacion,
        LocalDate checkIn,
        LocalDate checkOut,
        EstadoReserva estado,
        boolean activa,
        boolean lateCheckout) {

    public static ReservaVistaDto de(ReservaVista v, boolean lateCheckout) {
        return new ReservaVistaDto(v.id(), v.titular(), v.numerosHabitacion(),
                v.checkIn(), v.checkOut(), v.estado(), v.activa(), lateCheckout);
    }
}
