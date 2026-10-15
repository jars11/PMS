package com.pms.dtos;

import com.pms.utils.NivelCapacidad;

/**
 * Relaxation profile under which a reserva satisfied a solicitud. Value
 * record: {@code POR_DEFECTO} is the strict profile (no relaxation on any
 * axis, original bed distribution).
 */
public record PerfilRelajacion(boolean fechasRelajadas,
                               boolean atributosRelajados,
                               boolean colchonesExtra,
                               NivelCapacidad nivelCapacidad) {

    /** Strict profile: nothing relaxed, original bed distribution. */
    public static final PerfilRelajacion POR_DEFECTO =
            new PerfilRelajacion(false, false, false, NivelCapacidad.DISTRIBUCION_CAMAS);
}
