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

    /** La ventana de prueba tiene 10 noches (columnas 1..10). */
    private static final int ULTIMA = 10;

    @Test
    void pressSinArrastreMarcaSoloLaCasillaPresionada() {
        EstadoArrastre e = new EstadoArrastre();
        e.iniciarRango(2, 3, ULTIMA);
        assertTrue(e.activo());
        assertEquals(EstadoArrastre.Tipo.RANGO, e.tipo());
        assertFalse(e.esMovimiento());
        // El rango eventual sigue siendo [3,4], pero sin cambio de casillero
        // solo se resalta la celda presionada.
        assertEquals(3, e.seleccionIni());
        assertEquals(4, e.seleccionFin());
        assertFalse(e.rangoExtendido());
        assertTrue(e.esCeldaSeleccionada(2, 3));
        assertFalse(e.esCeldaSeleccionada(2, 4), "todavia no se pinto el rango");
        assertFalse(e.esCeldaSeleccionada(2, 5), "fuera del rango");
        assertFalse(e.esCeldaSeleccionada(1, 4), "otra fila");
        assertFalse(e.esCeldaSeleccionada(2, 0), "columna 0");
        assertNull(e.previewCreacion(2, 3, D1, D1, D1.plusDays(1)),
                "sin cambio de casillero no hay barra fantasma");
    }

    @Test
    void arrastrarUnaColumnaAMasNoAgregaNoche() {
        EstadoArrastre e = new EstadoArrastre();
        e.iniciarRango(2, 3, ULTIMA);

        e.extenderRango(4);
        assertEquals(3, e.seleccionIni());
        assertEquals(4, e.seleccionFin());
        assertTrue(e.esCeldaSeleccionada(2, 3));
        assertTrue(e.esCeldaSeleccionada(2, 4));
        assertFalse(e.esCeldaSeleccionada(2, 5), "la columna 5 queda fuera");
    }

    @Test
    void arrastrarDosColumnasAgregaUnaNoche() {
        EstadoArrastre e = new EstadoArrastre();
        e.iniciarRango(2, 3, ULTIMA);

        e.extenderRango(5);
        assertEquals(3, e.seleccionIni());
        assertEquals(5, e.seleccionFin());
        assertTrue(e.esCeldaSeleccionada(2, 3));
        assertTrue(e.esCeldaSeleccionada(2, 4));
        assertTrue(e.esCeldaSeleccionada(2, 5));
        assertFalse(e.esCeldaSeleccionada(2, 6), "fuera del rango");
    }

    @Test
    void arrastrarHaciaIzquierdaUsaLaCeldaPresionadaComoCheckOut() {
        EstadoArrastre e = new EstadoArrastre();
        e.iniciarRango(1, 5, ULTIMA);

        e.extenderRango(4);
        assertEquals(4, e.seleccionIni());
        assertEquals(5, e.seleccionFin());
        assertTrue(e.esCeldaSeleccionada(1, 4));
        assertTrue(e.esCeldaSeleccionada(1, 5));
        assertFalse(e.esCeldaSeleccionada(1, 3), "todavia no entra la columna 3");

        e.extenderRango(3);
        assertEquals(3, e.seleccionIni());
        assertEquals(5, e.seleccionFin());
        assertTrue(e.esCeldaSeleccionada(1, 3));
        assertTrue(e.esCeldaSeleccionada(1, 4));
        assertTrue(e.esCeldaSeleccionada(1, 5));
        assertFalse(e.esCeldaSeleccionada(1, 2), "fuera del rango");
    }

    @Test
    void pressEnLaUltimaColumnaSoloResaltaEsaCasillaHastaQueSeMueve() {
        EstadoArrastre e = new EstadoArrastre();
        e.iniciarRango(2, ULTIMA, ULTIMA);
        assertEquals(ULTIMA - 1, e.seleccionIni());
        assertEquals(ULTIMA, e.seleccionFin());
        assertTrue(e.esCeldaSeleccionada(2, ULTIMA));
        assertFalse(e.esCeldaSeleccionada(2, ULTIMA - 1),
                "sin cambio de casillero no se pinta la noche anterior");
    }

    @Test
    void volverAlOrigenTrasArrastrarDerechaCancelaElPreview() {
        EstadoArrastre e = new EstadoArrastre();
        e.iniciarRango(1, 3, ULTIMA);
        e.extenderRango(5);
        assertTrue(e.rangoExtendido());
        assertEquals(3, e.seleccionIni());
        assertEquals(5, e.seleccionFin());

        e.extenderRango(3);
        assertFalse(e.rangoExtendido(), "volver al origen cancela el rango");
        assertTrue(e.esCeldaSeleccionada(1, 3));
        assertFalse(e.esCeldaSeleccionada(1, 4), "sin rango no se pinta la noche siguiente");
        assertFalse(e.esCeldaSeleccionada(1, 5), "la columna 5 ya no esta en la seleccion");
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
        r.iniciarRango(1, 2, ULTIMA);
        assertFalse(r.esFilaValida(0));
    }

    @Test
    void previewSegmentoSoloConDestinoResuelto() {
        EstadoArrastre e = new EstadoArrastre();
        GeometriaBarra.Bloque b = new GeometriaBarra.Bloque(D1, D1.plusDays(3), false);

        assertNull(e.previewSegmento(D1), "sin drag activo");

        e.iniciarMovimiento(2, 1, b);
        assertNull(e.previewSegmento(D1), "sin destino resuelto no hay preview");
        e.hover(3);
        assertNull(e.previewSegmento(D1), "el hover solo no fija el destino");
        e.destino(3);
        GeometriaBarra.Segmento seg = e.previewSegmento(D1);
        assertNotNull(seg);
        assertEquals(0.25, seg.x0(), 1e-9);
        assertEquals(1.0, seg.x1(), 1e-9);

        // El preview abarca las mismas fechas que el bloque origen.
        GeometriaBarra.Segmento interior = e.previewSegmento(D1.plusDays(1));
        assertNotNull(interior);
        assertEquals(0.0, interior.x0(), 1e-9);
        assertEquals(1.0, interior.x1(), 1e-9);

        e.destino(-1);
        assertNull(e.previewSegmento(D1), "sin destino no hay preview");
    }

    /**
     * El hover por una fila gris no alcanza: la reserva solo se mueve de su
     * fila cuando el panel resuelve un destino permitido.
     */
    @Test
    void hoverSoloNoFijaDestino() {
        EstadoArrastre e = new EstadoArrastre();
        GeometriaBarra.Bloque b = new GeometriaBarra.Bloque(D1, D1.plusDays(3), false);
        e.iniciarMovimiento(2, 1, b);
        e.hover(3);

        assertEquals(-1, e.filaDestino());
        assertNull(e.previewSegmento(D1));

        e.destino(4);
        assertEquals(4, e.filaDestino());
        assertNotNull(e.previewSegmento(D1));
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

        // Rango activo sin cambio de casillero: solo celeste, sin barra.
        e.limpiar();
        e.iniciarRango(2, 3, ULTIMA);
        assertEquals(3, e.seleccionIni());
        assertEquals(4, e.seleccionFin());
        assertNull(e.previewCreacion(2, 3, D1, ini, fin), "sin cambio de casillero no hay barra");

        // Al cambiar de casillero se pinta el rango.
        e.extenderRango(5);
        assertTrue(e.rangoExtendido());
        assertNull(e.previewCreacion(2, 6, D1, ini, fin), "columna fuera del rango");
        assertNull(e.previewCreacion(1, 4, D1, ini, fin), "otra fila");

        // La celda de check-in (columna 3) es la cabeza del bloque [ini, fin].
        GeometriaBarra.Segmento checkin = e.previewCreacion(2, 3, ini, ini, fin);
        assertNotNull(checkin);
        assertEquals(0.25, checkin.x0(), 1e-9);
        assertEquals(1.0, checkin.x1(), 1e-9);

        // La celda de check-out (columna 5) es la cola del bloque, inclusiva.
        GeometriaBarra.Segmento checkout = e.previewCreacion(2, 5, fin, ini, fin);
        assertNotNull(checkout);
        assertEquals(0.0, checkout.x0(), 1e-9);
        assertEquals(0.25, checkout.x1(), 1e-9);
    }

    @Test
    void hoverNoAfectaUnRango() {
        EstadoArrastre e = new EstadoArrastre();
        e.iniciarRango(1, 2, ULTIMA);
        e.hover(3);
        assertEquals(-1, e.filaHover());
        assertNull(e.previewSegmento(D1));
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
        assertEquals(-1, e.filaDestino());
        assertEquals(-1, e.seleccionIni());
        assertEquals(-1, e.seleccionFin());
        assertNull(e.bloqueOrigen());
    }

    @Test
    void columnasDeRangoExpuestas() {
        EstadoArrastre e = new EstadoArrastre();
        e.iniciarRango(4, 6, ULTIMA);
        assertEquals(4, e.filaOrigen());
        assertEquals(6, e.colIni());
        assertEquals(6, e.colFin());
        assertEquals(6, e.seleccionIni());
        assertEquals(7, e.seleccionFin());
        assertNull(e.bloqueOrigen());
    }

    @Test
    void ocultarBloqueOrigenSoloConDestinoPermitido() {
        EstadoArrastre e = new EstadoArrastre();
        e.iniciarMovimiento(2, 1, new GeometriaBarra.Bloque(D1, D1.plusDays(3), false), 9L);

        assertFalse(e.ocultarBloqueOrigen(), "sin destino la reserva sigue en su fila");

        e.hover(3);
        assertFalse(e.ocultarBloqueOrigen(), "pasar por una fila gris no alcanza");

        e.destino(4);
        assertTrue(e.ocultarBloqueOrigen(), "con fila permitida la reserva se levanta del origen");

        e.destino(-1);
        assertFalse(e.ocultarBloqueOrigen(), "si se pierde el destino vuelve a su fila");

        e.limpiar();
        assertFalse(e.ocultarBloqueOrigen(), "sin movimiento activo no hay nada oculto");
    }

    @Test
    void reservaOrigenIdSeExponeYSeLimpia() {
        EstadoArrastre e = new EstadoArrastre();
        assertNull(e.reservaOrigenId());

        e.iniciarMovimiento(1, 2, new GeometriaBarra.Bloque(D1, D1.plusDays(1), false), 77L);
        assertEquals(Long.valueOf(77L), e.reservaOrigenId());
        e.destino(3);
        assertEquals(3, e.filaDestino());

        e.limpiar();
        assertNull(e.reservaOrigenId());
        assertEquals(-1, e.filaDestino());
    }

    @Test
    void siElDestinoEsElOrigenNoHayPreviewNiSeOcultaLaBarra() {
        EstadoArrastre e = new EstadoArrastre();
        e.iniciarMovimiento(2, 1, new GeometriaBarra.Bloque(D1, D1.plusDays(3), false), 9L);

        e.destino(2);

        assertEquals(2, e.filaDestino());
        assertFalse(e.ocultarBloqueOrigen());
        assertNull(e.previewSegmento(D1), "la reserva se queda pintada donde estaba");
    }

    @Test
    void seleccionarUnaReservaExponeSusFlechas() {
        EstadoArrastre e = new EstadoArrastre();
        GeometriaBarra.Bloque bloque = new GeometriaBarra.Bloque(D1, D1.plusDays(3), false);
        HandlesEstadia.Flechas flechas =
                new HandlesEstadia.Flechas(true, true, true, false);

        e.seleccionar(2, 9L, bloque, flechas);

        assertTrue(e.haySeleccion());
        assertEquals(2, e.filaSeleccionada());
        assertEquals(Long.valueOf(9L), e.reservaSeleccionadaId());
        assertEquals(bloque, e.bloqueSeleccionado());
        assertEquals(flechas, e.flechasSeleccion());

        e.limpiarSeleccionReserva();
        assertFalse(e.haySeleccion());
        assertNull(e.bloqueSeleccionado());
    }

    @Test
    void laRedimensionPrevisualizaElRangoYConservaLaSeleccionAlTerminar() {
        EstadoArrastre e = new EstadoArrastre();
        GeometriaBarra.Bloque bloque = new GeometriaBarra.Bloque(D1, D1.plusDays(2), false);
        e.seleccionar(1, 5L, bloque,
                new HandlesEstadia.Flechas(false, true, true, false));

        e.iniciarRedimension(HandlesEstadia.Borde.FIN, HandlesEstadia.Direccion.ACORTAR);
        assertTrue(e.esRedimension());
        e.previewRedimension(D1, D1.plusDays(1));

        GeometriaBarra.Segmento fin = e.previewRedimensionSegmento(D1.plusDays(1));
        assertNotNull(fin);
        assertEquals(0.25, fin.x1(), 1e-9);

        e.terminarRedimension();
        assertFalse(e.esRedimension());
        assertTrue(e.haySeleccion(), "la seleccion sigue para volver a mostrar las flechas");
        assertNull(e.previewRedimensionSegmento(D1));
    }
}
