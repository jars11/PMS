package com.pms.ui;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    // --------------------------------------------- pixel insets between bars

    @Test
    void separacionDejaHuecoEntreColaYCheckInVecino() {
        int ancho = 56;
        // Same turnover day cell: previous checkout tail [0,0.25] and the
        // incoming stay's check-in head [0.25,1].
        GeometriaBarra.Segmento cola = GeometriaBarra.segmento(bloque(D1, D4, false), D4);
        GeometriaBarra.Segmento cabecera =
                GeometriaBarra.segmento(bloque(D4, D4.plusDays(2), false), D4);

        int[] pxCola = GeometriaBarra.pixeles(cola, ancho);
        int[] pxCabecera = GeometriaBarra.pixeles(cabecera, ancho);
        int turno = (int) Math.round(0.25 * ancho);

        assertEquals(turno - GeometriaBarra.SEPARACION_PX, pxCola[1], "la cola retrocede");
        assertEquals(turno + GeometriaBarra.SEPARACION_PX, pxCabecera[0], "la cabecera avanza");
        assertTrue(pxCola[1] + GeometriaBarra.SEPARACION_PX < pxCabecera[0],
                "debe quedar un hueco visible entre la cola y el check-in");
        assertTrue(pxCola[1] > pxCola[0], "la cola sigue teniendo ancho");
    }

    @Test
    void lateCheckoutRestaLaMismaSeparacion() {
        int ancho = 56;
        GeometriaBarra.Segmento cola = GeometriaBarra.segmento(bloque(D1, D4, true), D4);

        int[] px = GeometriaBarra.pixeles(cola, ancho);

        assertEquals((int) Math.round(0.75 * ancho) - GeometriaBarra.SEPARACION_PX, px[1],
                "late check-out aplica la misma separacion que el checkout normal");
    }

    @Test
    void barraDeUnaNocheConservaAnchoPositivo() {
        int ancho = 56;
        GeometriaBarra.Segmento checkIn = GeometriaBarra.segmento(bloque(D1, D2, false), D1);
        GeometriaBarra.Segmento cola = GeometriaBarra.segmento(bloque(D1, D2, false), D2);

        int[] pxCheckIn = GeometriaBarra.pixeles(checkIn, ancho);
        int[] pxCola = GeometriaBarra.pixeles(cola, ancho);

        assertTrue(pxCheckIn[1] > pxCheckIn[0], "la noche de check-in debe tener ancho positivo");
        assertTrue(pxCola[1] > pxCola[0], "la cola de una noche debe tener ancho positivo");
    }

    @Test
    void pixelesNoSeSalenDeLaCeldaEnAnchosChicos() {
        List<GeometriaBarra.Segmento> segmentos = List.of(
                new GeometriaBarra.Segmento(0.0, 1.0),
                new GeometriaBarra.Segmento(0.25, 1.0),
                new GeometriaBarra.Segmento(0.0, 0.25),
                new GeometriaBarra.Segmento(0.0, 0.75));

        for (int ancho = 1; ancho <= 5; ancho++) {
            for (GeometriaBarra.Segmento seg : segmentos) {
                int[] px = GeometriaBarra.pixeles(seg, ancho);
                assertTrue(px[0] >= 0, "x0 no puede ser negativo (ancho " + ancho + ")");
                assertTrue(px[1] >= px[0], "x1 no puede quedar antes de x0 (ancho " + ancho + ")");
                assertTrue(px[1] <= ancho, "x1 no puede salirse de la celda (ancho " + ancho + ")");
            }
        }
    }
}
