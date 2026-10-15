package com.pms.dtos;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Bed distribution for one night. Comparison order mirrors the C++ operator&lt;
 * of DTDistribucionCamas: noche, camasMatrimoniales, camasIndividuales, cunas.
 */
public class DTDistribucionCamas implements Comparable<DTDistribucionCamas> {

    private final LocalDate noche;
    private final int camasMatrimoniales;
    private final int camasIndividuales;
    private final int cunas;

    public DTDistribucionCamas(LocalDate noche, int camasMatrimoniales, int camasIndividuales, int cunas) {
        this.noche = noche;
        this.camasMatrimoniales = camasMatrimoniales;
        this.camasIndividuales = camasIndividuales;
        this.cunas = cunas;
    }

    public LocalDate getNoche() {
        return noche;
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
    public int compareTo(DTDistribucionCamas other) {
        int c = noche.compareTo(other.noche);
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
        if (!(o instanceof DTDistribucionCamas)) {
            return false;
        }
        DTDistribucionCamas that = (DTDistribucionCamas) o;
        return camasMatrimoniales == that.camasMatrimoniales
                && camasIndividuales == that.camasIndividuales
                && cunas == that.cunas
                && Objects.equals(noche, that.noche);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noche, camasMatrimoniales, camasIndividuales, cunas);
    }
}