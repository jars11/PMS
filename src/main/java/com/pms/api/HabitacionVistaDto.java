package com.pms.api;

import com.pms.entities.Habitacion;
import com.pms.utils.AtributoHabitacion;

import java.util.Set;

/**
 * Serializable read view of a room. Exists so the REST layer never serializes
 * the JPA entity graph (which would risk lazy-loading or cycles).
 */
public record HabitacionVistaDto(
        long id,
        Integer numero,
        Set<AtributoHabitacion> atributos,
        int camasMatrimoniales,
        int camasIndividuales,
        int cunas,
        int colchones,
        int capacidad,
        float tarifaPorNoche,
        boolean limpia) {

    public static HabitacionVistaDto de(Habitacion h) {
        return new HabitacionVistaDto(
                h.getId(),
                h.getNumero(),
                h.getAtributos(),
                h.getCantidadMatrimoniales(),
                h.getCantidadIndividuales(),
                h.getCantidadCunas(),
                h.getColchones(),
                h.capacidad(),
                h.getTarifaPorNoche(),
                h.isLimpia());
    }
}
