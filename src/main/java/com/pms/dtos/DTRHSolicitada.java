package com.pms.dtos;

import com.pms.utils.AtributoHabitacion;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * Requested room-stay block: dates, wanted attributes and passenger counts.
 * Comparison order mirrors the C++ operator&lt; of DTRHSolicitada: base key,
 * then cantMatrimonios, cantIndividuales, cantBebes, cantCunas.
 */
public class DTRHSolicitada extends DTReservaHabitacion<DTRHSolicitada> {

    private final int cantMatrimonios;
    private final int cantIndividuales;
    private final int cantBebes;
    private final int cantCunas;

    public DTRHSolicitada(LocalDate fechaInicio, LocalDate fechaFin,
                          Set<AtributoHabitacion> atributosHabitacion,
                          int cantMatrimonios, int cantIndividuales, int cantBebes, int cantCunas) {
        super(fechaInicio, fechaFin, atributosHabitacion);
        this.cantMatrimonios = cantMatrimonios;
        this.cantIndividuales = cantIndividuales;
        this.cantBebes = cantBebes;
        this.cantCunas = cantCunas;
    }

    public int getCantMatrimonios() {
        return cantMatrimonios;
    }

    public int getCantIndividuales() {
        return cantIndividuales;
    }

    public int getCantBebes() {
        return cantBebes;
    }

    public int getCantCunas() {
        return cantCunas;
    }

    @Override
    public int compareTo(DTRHSolicitada other) {
        int c = super.compareTo(other);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(cantMatrimonios, other.cantMatrimonios);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(cantIndividuales, other.cantIndividuales);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(cantBebes, other.cantBebes);
        if (c != 0) {
            return c;
        }
        return Integer.compare(cantCunas, other.cantCunas);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DTRHSolicitada)) {
            return false;
        }
        DTRHSolicitada that = (DTRHSolicitada) o;
        return cantMatrimonios == that.cantMatrimonios
                && cantIndividuales == that.cantIndividuales
                && cantBebes == that.cantBebes
                && cantCunas == that.cantCunas
                && super.equals(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), cantMatrimonios, cantIndividuales, cantBebes, cantCunas);
    }
}