package com.pms.ui;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pure geometry of the occupancy bar: bar spans check-in day through check-out
 * day inclusive; each covered day exposes a fraction [x0,x1] of its own cell.
 */
class GeometriaBarraTest {

    private static final LocalDate D1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate D2 = D1.plusDays(1);
    private static final LocalDate D3 = D1.plusDays(2);
    private static final LocalDate D4 = D1.plusDays(3);

    private static GeometriaBarra.Bloque bloque(LocalDate ini, LocalDate fin, boolean late) {
        return new GeometriaBarra.Bloque(ini, fin, late);
    }

    @Test
    void insetConstantesBloqueadas() {
        assertEquals(0.25, GeometriaBarra.INSET_CHECKIN, 1e-9);
        assertEquals(0.25, GeometriaBarra.INSET_CHECKOUT_NORMAL, 1e-9);
        assertEquals(0.75, GeometriaBarra.INSET_CHECKOUT_LATE, 1e-9);
    }

    @Test
    void checkInEsInset025AUno() {
        GeometriaBarra.Segmento seg = GeometriaBarra.segmento(bloque(D1, D4, false), D1);
        assertEquals(0.25, seg.x0(), 1e-9);
        assertEquals(1.0, seg.x1(), 1e-9);
    }

    @Test
    void interiorEsCeroAUno() {
        GeometriaBarra.Segmento seg = GeometriaBarra.segmento(bloque(D1, D4, false), D2);
        assertEquals(0.0, seg.x0(), 1e-9);
        assertEquals(1.0, seg.x1(), 1e-9);
    }

    @Test
    void checkOutNormalTerminaEn025() {
        GeometriaBarra.Segmento seg = GeometriaBarra.segmento(bloque(D1, D4, false), D4);
        assertEquals(0.0, seg.x0(), 1e-9);
        assertEquals(0.25, seg.x1(), 1e-9);
    }

    @Test
    void checkOutLateTerminaEn075() {
        GeometriaBarra.Segmento seg = GeometriaBarra.segmento(bloque(D1, D4, true), D4);
        assertEquals(0.0, seg.x0(), 1e-9);
        assertEquals(0.75, seg.x1(), 1e-9);
    }

    @Test
    void unaNocheDaCheckInYColaEnDiasDistintos() {
        GeometriaBarra.Segmento checkIn = GeometriaBarra.segmento(bloque(D1, D2, false), D1);
        GeometriaBarra.Segmento checkOut = GeometriaBarra.segmento(bloque(D1, D2, false), D2);
        assertEquals(0.25, checkIn.x0(), 1e-9);
        assertEquals(1.0, checkIn.x1(), 1e-9);
        assertEquals(0.0, checkOut.x0(), 1e-9);
        assertEquals(0.25, checkOut.x1(), 1e-9);
    }

    @Test
    void diaFueraDelRangoEsNull() {
        assertNull(GeometriaBarra.segmento(bloque(D2, D4, false), D1), "antes del check-in");
        assertNull(GeometriaBarra.segmento(bloque(D1, D3, false), D4), "despues del check-out");
        assertNull(GeometriaBarra.segmento(null, D1), "bloque nulo");
        assertNull(GeometriaBarra.segmento(bloque(D1, D3, false), null), "dia nulo");
    }
}
