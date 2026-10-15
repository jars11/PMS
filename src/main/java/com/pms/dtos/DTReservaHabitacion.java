package com.pms.dtos;

import com.pms.utils.AtributoHabitacion;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Base for room-stay DTOs: dates plus requested/proposed attributes.
 * Comparison order mirrors the C++ operator&lt; of DTReservaHabitacion:
 * fechaInicio, fechaFin, then lexicographic over the attribute set
 * (natural enum order).
 *
 * <p>Self-type generic so subclasses can override compareTo with their
 * subtype (the C++ operator&lt; chains the base key with their own fields).
 */
public abstract class DTReservaHabitacion<T extends DTReservaHabitacion<T>> implements Comparable<T> {

    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;
    private final Set<AtributoHabitacion> atributosHabitacion;

    public DTReservaHabitacion(LocalDate fechaInicio, LocalDate fechaFin,
                               Set<AtributoHabitacion> atributosHabitacion) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.atributosHabitacion = new TreeSet<>(atributosHabitacion != null ? atributosHabitacion : Set.of());
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public Set<AtributoHabitacion> getAtributosHabitacion() {
        return atributosHabitacion;
    }

    /** Lexicographic comparison of two sorted sets (shortest-prefix first), like std::lexicographical_compare. */
    protected static <T extends Comparable<T>> int compararConjuntos(Set<T> a, Set<T> b) {
        Iterator<T> ia = new TreeSet<>(a).iterator();
        Iterator<T> ib = new TreeSet<>(b).iterator();
        while (ia.hasNext() && ib.hasNext()) {
            int c = ia.next().compareTo(ib.next());
            if (c != 0) {
                return c;
            }
        }
        return Boolean.compare(ia.hasNext(), ib.hasNext());
    }

    @Override
    public int compareTo(T other) {
        int c = fechaInicio.compareTo(other.getFechaInicio());
        if (c != 0) {
            return c;
        }
        c = fechaFin.compareTo(other.getFechaFin());
        if (c != 0) {
            return c;
        }
        return compararConjuntos(atributosHabitacion, other.getAtributosHabitacion());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DTReservaHabitacion)) {
            return false;
        }
        DTReservaHabitacion that = (DTReservaHabitacion) o;
        return Objects.equals(fechaInicio, that.fechaInicio)
                && Objects.equals(fechaFin, that.fechaFin)
                && Objects.equals(atributosHabitacion, that.atributosHabitacion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fechaInicio, fechaFin, atributosHabitacion);
    }
}