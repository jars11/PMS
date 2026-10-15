package com.pms.ui;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless drag state machine: free cells start a range, occupied cells start a
 * move, and the same object answers every question the renderer and the panel
 * need. No Swing dependency.
 */
class EstadoArrastreTest {

    private static final LocalDate D1 = LocalDate.of(2026, 11, 1);

    @Test
    void arrancaInactivo() {
        EstadoArrastre e = new EstadoArrastre();
        assertEquals(EstadoArrastre.Tipo.NINGUNO, e.tipo());
        assertFalse(e.activo());
        assertFalse(e.esMovimiento());
    }

    @Test
    void rangoMarcaColumnasSeleccionadas() {
        EstadoArrastre e = new EstadoArrastre();
        e.iniciarRango(2, 3);
        assertTrue(e.activo());
        assertEquals(EstadoArrastre.Tipo.RANGO, e.tipo());
        assertFalse(e.esMovimiento());
        assertTrue(e.esCeldaSeleccionada(2, 3));

        e.extenderRango(5);
        assertTrue(e.esCeldaSeleccionada(2, 4));
        assertTrue(e.esCeldaSeleccionada(2, 5));
        assertFalse(e.esCeldaSeleccionada(2, 6), "fuera del rango");
        assertFalse(e.esCeldaSeleccionada(1, 4), "otra fila");
        assertFalse(e.esCeldaSeleccionada(2, 0), "columna 0");
    }

    @Test
    void extenderRangoNormalizaHaciaAtras() {
        EstadoArrastre e = new EstadoArrastre();
        e.iniciarRango(1, 5);
        e.extenderRango(2);
        assertTrue(e.esCeldaSeleccionada(1, 3));
        assertTrue(e.esCeldaSeleccionada(1, 5));
        assertFalse(e.esCeldaSeleccionada(1, 1));
    }

    @Test
    void movimientoExponeFilasValidas() {
        EstadoArrastre e = new EstadoArrastre();
        GeometriaBarra.Bloque b = new GeometriaBarra.Bloque(D1, D1.plusDays(3), false);
        e.iniciarMovimiento(1, 2, b);

        assertTrue(e.esMovimiento());
        assertEquals(1, e.filaOrigen());
        assertEquals(2, e.colIni());
        assertEquals(b, e.bloqueOrigen());
        assertFalse(e.esFilaValida(1), "el origen no es destino valido");
        assertTrue(e.esFilaValida(0));
        assertTrue(e.esFilaValida(2));
        assertFalse(e.esFilaValida(-1));
    }

    @Test
    void rangoNoValidaFilasDeMovimiento() {
        EstadoArrastre r = new EstadoArrastre();
        r.iniciarRango(1, 2);
        assertFalse(r.esFilaValida(0));
    }

    @Test
    void previewSegmentoSoloEnMovimientoConHoverCompatible() {
        EstadoArrastre e = new EstadoArrastre();
        GeometriaBarra.Bloque b = new GeometriaBarra.Bloque(D1, D1.plusDays(3), false);

        assertNull(e.previewSegmento(D1, true), "sin drag activo");

        e.iniciarMovimiento(2, 1, b);
        assertNull(e.previewSegmento(D1, true), "el origen nunca expone preview");
        e.hover(3);
        GeometriaBarra.Segmento seg = e.previewSegmento(D1, true);
        assertNotNull(seg);
        assertEquals(0.25, seg.x0(), 1e-9);
        assertEquals(1.0, seg.x1(), 1e-9);

        // El preview abarca las mismas fechas que el bloque origen.
        GeometriaBarra.Segmento interior = e.previewSegmento(D1.plusDays(1), true);
        assertNotNull(interior);
        assertEquals(0.0, interior.x0(), 1e-9);
        assertEquals(1.0, interior.x1(), 1e-9);

        e.hover(-1);
        assertNull(e.previewSegmento(D1, true), "hover invalido no expone preview");
    }

    /**
     * R10 negative: un hover sobre una fila INCOMPATIBLE no expone ningun
     * preview, incluso con el movimiento activo y la fila apuntada.
     */
    @Test
    void previewSegmentoNuloEnFilaIncompatible() {
        EstadoArrastre e = new EstadoArrastre();
        GeometriaBarra.Bloque b = new GeometriaBarra.Bloque(D1, D1.plusDays(3), false);
        e.iniciarMovimiento(2, 1, b);
        e.hover(3);

        assertNotNull(e.previewSegmento(D1, true), "fila compatible expone preview");
        assertNull(e.previewSegmento(D1, false), "fila incompatible NO expone preview");
    }

    /**
     * Preview de la CREACION: solo con un rango activo y celda dentro de el;
     * sin drag, en movimiento o fuera del rango devuelve null.
     */
    @Test
    void previewCreacionSoloEnRangoDentroDeLaSeleccion() {
        EstadoArrastre e = new EstadoArrastre();
        LocalDate ini = D1;
        LocalDate fin = D1.plusDays(4);

        // Sin drag activo.
        assertNull(e.previewCreacion(2, 3, D1, ini, fin), "sin drag activo");

        // Un movimiento nunca expone preview de creacion.
        e.iniciarMovimiento(2, 1, new GeometriaBarra.Bloque(D1, D1.plusDays(3), false));
        assertNull(e.previewCreacion(2, 3, D1, ini, fin), "un movimiento no crea preview");

        // Rango activo pero celda fuera de la seleccion.
        e.limpiar();
        e.iniciarRango(2, 3);
        e.extenderRango(5);
        assertNull(e.previewCreacion(2, 6, D1, ini, fin), "columna fuera del rango");
        assertNull(e.previewCreacion(1, 4, D1, ini, fin), "otra fila");

        // Dia de check-in (primero del bloque [ini, fin)).
        GeometriaBarra.Segmento checkin = e.previewCreacion(2, 3, ini, ini, fin);
        assertNotNull(checkin);
        assertEquals(0.25, checkin.x0(), 1e-9);
        assertEquals(1.0, checkin.x1(), 1e-9);

        // Dia interior.
        GeometriaBarra.Segmento interior = e.previewCreacion(2, 4, ini.plusDays(1), ini, fin);
        assertNotNull(interior);
        assertEquals(0.0, interior.x0(), 1e-9);
        assertEquals(1.0, interior.x1(), 1e-9);
    }

    @Test
    void hoverNoAfectaUnRango() {
        EstadoArrastre e = new EstadoArrastre();
        e.iniciarRango(1, 2);
        e.hover(3);
        assertEquals(-1, e.filaHover());
        assertNull(e.previewSegmento(D1, true));
    }

    @Test
    void limpiarReseteaTodo() {
        EstadoArrastre e = new EstadoArrastre();
        e.iniciarMovimiento(1, 2, new GeometriaBarra.Bloque(D1, D1.plusDays(1), false));
        e.hover(4);
        e.limpiar();

        assertEquals(EstadoArrastre.Tipo.NINGUNO, e.tipo());
        assertFalse(e.activo());
        assertFalse(e.esMovimiento());
        assertEquals(-1, e.filaOrigen());
        assertEquals(-1, e.colIni());
        assertEquals(-1, e.colFin());
        assertEquals(-1, e.filaHover());
        assertNull(e.bloqueOrigen());
    }

    @Test
    void columnasDeRangoExpuestas() {
        EstadoArrastre e = new EstadoArrastre();
        e.iniciarRango(4, 6);
        assertEquals(4, e.filaOrigen());
        assertEquals(6, e.colIni());
        assertEquals(6, e.colFin());
        assertNull(e.bloqueOrigen());
    }
}
