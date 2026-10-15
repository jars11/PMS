package com.pms.utils;

/**
 * Capacity-relaxation level of a match. DECLARATION ORDER IS THE MAPPING
 * CONTRACT: the constructor argument is the {@code relajacionDistribucion}
 * code the search algorithm uses (0 = original bed distribution, 1 = relaxed
 * per-room capacity, 2 = relaxed total capacity). Tests freeze the order so a
 * future reorder cannot silently change the mapping.
 */
public enum NivelCapacidad {

    DISTRIBUCION_CAMAS(0),
    CAPACIDAD_POR_HABITACION(1),
    CAPACIDAD_TOTAL(2);

    private final int relajacionDistribucion;

    NivelCapacidad(int relajacionDistribucion) {
        this.relajacionDistribucion = relajacionDistribucion;
    }

    /** Code the search algorithm uses for this relaxation level. */
    public int getRelajacionDistribucion() {
        return relajacionDistribucion;
    }

    /**
     * Level matching the given {@code relajacionDistribucion} code.
     *
     * @throws IllegalArgumentException when no level maps to that code.
     */
    public static NivelCapacidad deRelajacion(int relajacionDistribucion) {
        for (NivelCapacidad nivel : values()) {
            if (nivel.relajacionDistribucion == relajacionDistribucion) {
                return nivel;
            }
        }
        throw new IllegalArgumentException("relajacionDistribucion invalida: " + relajacionDistribucion);
    }
}
