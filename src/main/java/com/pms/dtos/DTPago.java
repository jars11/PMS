package com.pms.dtos;

import com.pms.utils.TipoMoneda;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Payment snapshot. Comparison order mirrors the C++ operator&lt; of DTPago:
 * monto, moneda, fechaPago.
 */
public class DTPago implements Comparable<DTPago> {

    private final float monto;
    private final TipoMoneda moneda;
    private final LocalDate fechaPago;

    public DTPago(float monto, TipoMoneda moneda, LocalDate fechaPago) {
        this.monto = monto;
        this.moneda = moneda;
        this.fechaPago = fechaPago;
    }

    public float getMonto() {
        return monto;
    }

    public TipoMoneda getMoneda() {
        return moneda;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    @Override
    public int compareTo(DTPago other) {
        int c = Float.compare(monto, other.monto);
        if (c != 0) {
            return c;
        }
        c = moneda.compareTo(other.moneda);
        if (c != 0) {
            return c;
        }
        return fechaPago.compareTo(other.fechaPago);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DTPago)) {
            return false;
        }
        DTPago that = (DTPago) o;
        return Float.compare(monto, that.monto) == 0
                && moneda == that.moneda
                && Objects.equals(fechaPago, that.fechaPago);
    }

    @Override
    public int hashCode() {
        return Objects.hash(monto, moneda, fechaPago);
    }
}