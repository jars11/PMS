package com.pms.handlers;

import com.pms.handlers.ManejadorAtributosHabs;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.MotivoRechazo;

import java.time.LocalDate;
import java.util.Set;

/**
 * Pure predicate deciding whether a block can move onto a destination room. ONE
 * predicate serves both row-greying during the drag (rule 4) and drop validation
 * (rule 3), so the two can never drift apart.
 *
 * <p>{@link #evaluar} returns {@code null} when the room is compatible, or the
 * FIRST applicable {@link MotivoRechazo} walking the enum in its declaration
 * order (the precedence contract).
 */
public final class CompatibilidadMovimiento {

    private CompatibilidadMovimiento() {
    }

    /** The block being moved: its demands plus its (fixed) dates and late flag. */
    public record BloqueMovido(Set<AtributoHabitacion> atributos, int camasMatrimoniales,
                               int camasIndividuales, int cunas, LocalDate fechaInicio,
                               LocalDate fechaFin, boolean lateCheckout) {
    }

    /** Candidate destination room with all facts the predicate needs resolved. */
    public record HabitacionDestino(long id, Set<AtributoHabitacion> atributos, int camasMatrimoniales,
                                    int camasIndividuales, int cunas, int capacidad, boolean limpia,
                                    boolean libreEnRango, boolean checkInEnFechaFin,
                                    boolean lateCheckoutTerminaEnFechaInicio) {
    }

    /**
     * {@code null} when compatible; otherwise the first applicable reason in
     * {@link MotivoRechazo} declaration order.
     */
    public static MotivoRechazo evaluar(BloqueMovido movido, HabitacionDestino destino) {
        if (!ManejadorAtributosHabs.tieneAtributos(destino.atributos(), movido.atributos())) {
            return MotivoRechazo.ATRIBUTOS_FALTANTES;
        }
        if (destino.camasMatrimoniales() < movido.camasMatrimoniales()
                || destino.camasIndividuales() < movido.camasIndividuales()
                || destino.cunas() < movido.cunas()) {
            return MotivoRechazo.DISTRIBUCION_CAMAS;
        }
        if (destino.capacidad() < 2 * movido.camasMatrimoniales() + movido.camasIndividuales()) {
            return MotivoRechazo.CAPACIDAD_INSUFICIENTE;
        }
        if (!destino.libreEnRango()) {
            return MotivoRechazo.NO_DISPONIBLE;
        }
        if (movido.lateCheckout() && destino.checkInEnFechaFin()) {
            return MotivoRechazo.SOLAPE_LATE_CHECKOUT;
        }
        if (destino.lateCheckoutTerminaEnFechaInicio() && !destino.limpia()) {
            return MotivoRechazo.LIMPIEZA_LATE_CHECKOUT;
        }
        return null;
    }
}
