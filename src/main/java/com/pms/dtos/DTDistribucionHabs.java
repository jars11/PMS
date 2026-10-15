package com.pms.dtos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Passengers-per-room distribution for one night. Comparison order mirrors the
 * C++ operator&lt; of DTDistribucionHabs: noche, then lexicographic over the
 * passenger counts.
 */
public class DTDistribucionHabs implements Comparable<DTDistribucionHabs> {

    private final LocalDate noche;
    private final List<Integer> pasajerosPorHabitacion;

    public DTDistribucionHabs(LocalDate noche, List<Integer> pasajerosPorHabitacion) {
        this.noche = noche;
        this.pasajerosPorHabitacion = new ArrayList<>(pasajerosPorHabitacion != null ? pasajerosPorHabitacion : List.of());
    }

    public LocalDate getNoche() {
        return noche;
    }

    public List<Integer> getPasajerosPorHabitacion() {
        return pasajerosPorHabitacion;
    }

    @Override
    public int compareTo(DTDistribucionHabs other) {
        int c = noche.compareTo(other.noche);
        if (c != 0) {
            return c;
        }
        Iterator<Integer> ia = pasajerosPorHabitacion.iterator();
        Iterator<Integer> ib = other.pasajerosPorHabitacion.iterator();
        while (ia.hasNext() && ib.hasNext()) {
            c = Integer.compare(ia.next(), ib.next());
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
        if (!(o instanceof DTDistribucionHabs)) {
            return false;
        }
        DTDistribucionHabs that = (DTDistribucionHabs) o;
        return Objects.equals(noche, that.noche)
                && Objects.equals(pasajerosPorHabitacion, that.pasajerosPorHabitacion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noche, pasajerosPorHabitacion);
    }
}