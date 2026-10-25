package com.pms.api;

import com.pms.controllers.ControladorReservas.OcupacionHabitacion;

import java.util.List;

/**
 * Serializable read view of one calendar row: a room plus the contiguous
 * occupancy bars drawn over the requested window. Only active reservations are
 * included (see {@link com.pms.controllers.ControladorReservas#matrizOcupacion}).
 */
public record OcupacionHabitacionDto(
        long habitacionId,
        Integer numero,
        List<BloqueOcupacionDto> bloques) {

    public static OcupacionHabitacionDto de(OcupacionHabitacion o) {
        return new OcupacionHabitacionDto(
                o.habitacion().getId(),
                o.habitacion().getNumero(),
                o.bloques().stream().map(BloqueOcupacionDto::de).toList());
    }
}
