package com.pms.dtos;

import com.pms.utils.AtributoHabitacion;
import com.pms.utils.TipoMoneda;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * Proposed room-stay: dates, room attributes, subtotal, currency and bed
 * distribution. Comparison order mirrors the C++ operator&lt; of DTRHPropuesta:
 * base key, subtotal, moneda, camasMatrimoniales, camasIndividuales, cunas.
 */
public class DTRHPropuesta extends DTReservaHabitacion<DTRHPropuesta> {

    private final float subtotal;
    private final TipoMoneda moneda;
    private final int camasMatrimoniales;
    private final int camasIndividuales;
    private final int cunas;

    public DTRHPropuesta(LocalDate fechaInicio, LocalDate fechaFin,
                         Set<AtributoHabitacion> atributosHabitacion,
                         float subtotal, TipoMoneda moneda,
                         int camasMatrimoniales, int camasIndividuales, int cunas) {
        super(fechaInicio, fechaFin, atributosHabitacion);
        this.subtotal = subtotal;
        this.moneda = moneda;
        this.camasMatrimoniales = camasMatrimoniales;
        this.camasIndividuales = camasIndividuales;
        this.cunas = cunas;
    }

    public float getSubtotal() {
        return subtotal;
    }

    public TipoMoneda getMoneda() {
        return moneda;
    }

    public int getCamasMatrimoniales() {
        return camasMatrimoniales;
    }

    public int getCamasIndividuales() {
        return camasIndividuales;
    }

    public int getCunas() {
        return cunas;
    }

    @Override
    public int compareTo(DTRHPropuesta other) {
        int c = super.compareTo(other);
        if (c != 0) {
            return c;
        }
        c = Float.compare(subtotal, other.subtotal);
        if (c != 0) {
            return c;
        }
        c = moneda.compareTo(other.moneda);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(camasMatrimoniales, other.camasMatrimoniales);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(camasIndividuales, other.camasIndividuales);
        if (c != 0) {
            return c;
        }
        return Integer.compare(cunas, other.cunas);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DTRHPropuesta)) {
            return false;
        }
        DTRHPropuesta that = (DTRHPropuesta) o;
        return Float.compare(subtotal, that.subtotal) == 0
                && camasMatrimoniales == that.camasMatrimoniales
                && camasIndividuales == that.camasIndividuales
                && cunas == that.cunas
                && moneda == that.moneda
                && super.equals(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subtotal, moneda, camasMatrimoniales, camasIndividuales, cunas);
    }
}