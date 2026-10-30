package com.pms.api;

import com.pms.controllers.ControladorReservas.ResultadoCompatibilidad;

/**
 * Serializable per-room compatibility entry for the web calendar drag: one
 * entry for every room, with {@code motivo == null} when the block can be moved
 * there. Mirrors the existing {@link ResultadoCompatibilidad} business record
 * (the same predicate that validates the drop) so the grey rows the calendar
 * paints and the 409 rejection can never disagree.
 */
public record CompatibilidadMovimientoDto(long habitacionId, String motivo) {

    public static CompatibilidadMovimientoDto de(ResultadoCompatibilidad r) {
        return new CompatibilidadMovimientoDto(r.habitacionId(),
                r.motivo() == null ? null : r.motivo().name());
    }
}
