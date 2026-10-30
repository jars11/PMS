package com.pms.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Cantidad de dias visibles segun el ancho de la ventana: la columna de
 * habitacion ocupa 110 px y cada noche 56 px.
 */
class ColumnasCalendarioTest {

    private static final int HABITACION = 110;
    private static final int NOCHE = 56;
    private static final int DEFAULT = 14;

    private static int noches(int ancho) {
        return ColumnasCalendario.nochesVisibles(ancho, HABITACION, NOCHE, DEFAULT);
    }

    @Test
    void antesDelPrimerLayoutUsaElDefault() {
        assertEquals(DEFAULT, noches(0));
        assertEquals(DEFAULT, noches(HABITACION));
    }

    @Test
    void calculaLasNochesQueEntran() {
        assertEquals(12, noches(HABITACION + 12 * NOCHE));
        assertEquals(12, noches(HABITACION + 12 * NOCHE + NOCHE - 1));
        assertEquals(13, noches(HABITACION + 13 * NOCHE));
    }

    @Test
    void siempreMuestraAlMenosUnaNoche() {
        assertEquals(1, noches(HABITACION + 1));
    }

    @Test
    void ventanaMasGrandeMuestraMasDias() {
        assertEquals(20, noches(HABITACION + 20 * NOCHE));
    }

    @Test
    void reparteElAnchoEntreLasNochesSinDejarTiraVacia() {
        assertEquals(59, ColumnasCalendario.anchoNoche(1000, 15, HABITACION, NOCHE));
        assertEquals(56, ColumnasCalendario.anchoNoche(900, 14, HABITACION, NOCHE));
        assertEquals(57, ColumnasCalendario.anchoNoche(2000, 33, HABITACION, NOCHE));
        assertEquals(NOCHE, ColumnasCalendario.anchoNoche(0, DEFAULT, HABITACION, NOCHE));
    }
}
