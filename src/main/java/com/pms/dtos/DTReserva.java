package com.pms.dtos;

import com.pms.utils.EstadoReserva;
import com.pms.utils.TipoMoneda;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reservation snapshot. Comparison order mirrors the C++ operator&lt; of
 * DTReserva: activa, montoTotal, montoSenia, moneda, estado, then
 * lexicographic over the ordered set of proposals.
 */
public class DTReserva implements Comparable<DTReserva> {

    private final boolean activa;
    private final float montoTotal;
    private final float montoSenia;
    private final TipoMoneda moneda;
    private final EstadoReserva estado;
    private final Set<DTRHPropuesta> dtrhpropuestas;

    public DTReserva(boolean activa, float montoTotal, float montoSenia,
                     TipoMoneda moneda, EstadoReserva estado,
                     Set<DTRHPropuesta> dtrhpropuestas) {
        this.activa = activa;
        this.montoTotal = montoTotal;
        this.montoSenia = montoSenia;
        this.moneda = moneda;
        this.estado = estado;
        this.dtrhpropuestas = new TreeSet<>(dtrhpropuestas != null ? dtrhpropuestas : Set.of());
    }

    public boolean isActiva() {
        return activa;
    }

    public float getMontoTotal() {
        return montoTotal;
    }

    public float getMontoSenia() {
        return montoSenia;
    }

    public TipoMoneda getMoneda() {
        return moneda;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public Set<DTRHPropuesta> getDTRHPropuestas() {
        return dtrhpropuestas;
    }

    @Override
    public int compareTo(DTReserva other) {
        int c = Boolean.compare(activa, other.activa);
        if (c != 0) {
            return c;
        }
        c = Float.compare(montoTotal, other.montoTotal);
        if (c != 0) {
            return c;
        }
        c = Float.compare(montoSenia, other.montoSenia);
        if (c != 0) {
            return c;
        }
        c = moneda.compareTo(other.moneda);
        if (c != 0) {
            return c;
        }
        c = estado.compareTo(other.estado);
        if (c != 0) {
            return c;
        }
        Iterator<DTRHPropuesta> ia = dtrhpropuestas.iterator();
        Iterator<DTRHPropuesta> ib = other.dtrhpropuestas.iterator();
        while (ia.hasNext() && ib.hasNext()) {
            c = ia.next().compareTo(ib.next());
            if (c != 0) {
                return c;
            }
        }
        return Boolean.compare(ia.hasNext(), ib.hasNext());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DTReserva)) {
            return false;
        }
        DTReserva that = (DTReserva) o;
        return activa == that.activa
                && Float.compare(montoTotal, that.montoTotal) == 0
                && Float.compare(montoSenia, that.montoSenia) == 0
                && moneda == that.moneda
                && estado == that.estado
                && Objects.equals(dtrhpropuestas, that.dtrhpropuestas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(activa, montoTotal, montoSenia, moneda, estado, dtrhpropuestas);
    }
}