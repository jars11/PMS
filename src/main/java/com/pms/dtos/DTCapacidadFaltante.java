package com.pms.dtos;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Missing capacity for one night. Comparison order mirrors the C++ operator&lt;
 * of DTCapacidadFaltante: noche, capacidad, cantPasajeros.
 */
public class DTCapacidadFaltante implements Comparable<DTCapacidadFaltante> {

    private final LocalDate noche;
    private final int capacidad;
    private final int cantPasajeros;

    public DTCapacidadFaltante(LocalDate noche, int capacidad, int cantPasajeros) {
        this.noche = noche;
        this.capacidad = capacidad;
        this.cantPasajeros = cantPasajeros;
    }

    public LocalDate getNoche() {
        return noche;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public int getCantPasajeros() {
        return cantPasajeros;
    }

    @Override
    public int compareTo(DTCapacidadFaltante other) {
        int c = noche.compareTo(other.noche);
        if (c != 0) {
            return c;
        }
        c = Integer.compare(capacidad, other.capacidad);
        if (c != 0) {
            return c;
        }
        return Integer.compare(cantPasajeros, other.cantPasajeros);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DTCapacidadFaltante)) {
            return false;
        }
        DTCapacidadFaltante that = (DTCapacidadFaltante) o;
        return capacidad == that.capacidad
                && cantPasajeros == that.cantPasajeros
                && Objects.equals(noche, that.noche);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noche, capacidad, cantPasajeros);
    }
}