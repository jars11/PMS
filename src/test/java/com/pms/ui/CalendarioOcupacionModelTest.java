package com.pms.ui;

import com.pms.controllers.ControladorReservas.OcupacionHabitacion;
import com.pms.entities.Habitacion;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas headless del modelo de tabla del calendario. No se instancia ningun
 * frame ni se necesita display: solo se ejercita la API pura del modelo.
 */
class CalendarioOcupacionModelTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);

    private static List<OcupacionHabitacion> ocupacionDemo() {
        Habitacion h103 = new Habitacion(103, Set.of(), 1, 2, 0, 0);
        Habitacion h104 = new Habitacion(104, Set.of(), 1, 2, 0, 0);
        return List.of(
                new OcupacionHabitacion(h103, Set.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3))),
                new OcupacionHabitacion(h104, Set.of()));
    }

    @Test
    void getColumnCountEsUnoMasNoches() {
        CalendarioOcupacionModel modelo = new CalendarioOcupacionModel(INICIO, 14, ocupacionDemo());
        assertEquals(15, modelo.getColumnCount());

        // Constructor de conveniencia: noches por defecto = 14.
        CalendarioOcupacionModel porDefecto = new CalendarioOcupacionModel(INICIO, ocupacionDemo());
        assertEquals(15, porDefecto.getColumnCount());
    }

    @Test
    void getRowCountEsCantidadDeHabitaciones() {
        CalendarioOcupacionModel modelo = new CalendarioOcupacionModel(INICIO, 14, ocupacionDemo());
        assertEquals(2, modelo.getRowCount());
    }

    @Test
    void getColumnNameColumnaCeroEsHabitacionYRestoDdMm() {
        CalendarioOcupacionModel modelo = new CalendarioOcupacionModel(INICIO, 14, ocupacionDemo());
        assertEquals("Habitación", modelo.getColumnName(0));
        assertEquals("01/11", modelo.getColumnName(1));
        assertEquals("14/11", modelo.getColumnName(14));
    }

    @Test
    void fechaDeColumnaYColumnaDeFechaHacenRoundTrip() {
        CalendarioOcupacionModel modelo = new CalendarioOcupacionModel(INICIO, 14, ocupacionDemo());

        assertNull(modelo.fechaDeColumna(0));
        assertEquals(INICIO, modelo.fechaDeColumna(1));
        assertEquals(INICIO.plusDays(13), modelo.fechaDeColumna(14));

        assertEquals(1, modelo.columnaDeFecha(INICIO));
        assertEquals(2, modelo.columnaDeFecha(INICIO.plusDays(1)));
        assertEquals(14, modelo.columnaDeFecha(INICIO.plusDays(13)));
        assertEquals(-1, modelo.columnaDeFecha(INICIO.plusDays(14)));
        assertEquals(-1, modelo.columnaDeFecha(INICIO.minusDays(1)));
        assertEquals(-1, modelo.columnaDeFecha(null));
    }

    @Test
    void estaOcupadaReflejaLaMatrizYLaColumnaCeroEsSiempreLibre() {
        CalendarioOcupacionModel modelo = new CalendarioOcupacionModel(INICIO, 14, ocupacionDemo());

        assertFalse(modelo.estaOcupada(0, 0));
        assertTrue(modelo.estaOcupada(0, 1));   // 2026-11-01 ocupada
        assertFalse(modelo.estaOcupada(0, 2));  // 2026-11-02 libre
        assertTrue(modelo.estaOcupada(0, 3));   // 2026-11-03 ocupada
        assertFalse(modelo.estaOcupada(1, 1));  // hab 104 sin reservas
    }

    @Test
    void habitacionDeFilaYValorDeEtiqueta() {
        CalendarioOcupacionModel modelo = new CalendarioOcupacionModel(INICIO, 14, ocupacionDemo());

        assertEquals(103, modelo.habitacionDeFila(0).getNumero().intValue());
        assertEquals(104, modelo.habitacionDeFila(1).getNumero().intValue());
        assertEquals(Integer.valueOf(103), modelo.getValueAt(0, 0));
    }

    /**
     * Test 11: el modelo expone el id de la reserva que ocupa cada noche; las
     * noches libres y la columna 0 devuelven null.
     */
    @Test
    void reservaEnDevuelveIdYNullEnLibre() {
        Habitacion h103 = new Habitacion(103, Set.of(), 1, 2, 0, 0);
        LocalDate nocheOcupada = LocalDate.of(2026, 11, 1);
        OcupacionHabitacion ocupacion = new OcupacionHabitacion(
                h103, Set.of(nocheOcupada), Map.of(nocheOcupada, 77L));
        CalendarioOcupacionModel modelo = new CalendarioOcupacionModel(INICIO, 14, List.of(ocupacion));

        assertEquals(Long.valueOf(77L), modelo.reservaEn(0, 1));   // columna 1 = 2026-11-01
        assertNull(modelo.reservaEn(0, 2));                        // 2026-11-02 libre
        assertNull(modelo.reservaEn(0, 0));                        // columna 0 = etiqueta
    }

    /**
     * Test 12: el constructor de 2 argumentos de {@link OcupacionHabitacion}
     * sigue compilando (back-compat) y expone noches ocupadas sin ids.
     */
    @Test
    void constructorDeDosArgumentosSigueCompilando() {
        Habitacion h105 = new Habitacion(105, Set.of(), 1, 2, 0, 0);
        OcupacionHabitacion sinIds = new OcupacionHabitacion(h105, Set.of(LocalDate.of(2026, 11, 2)));
        CalendarioOcupacionModel modelo = new CalendarioOcupacionModel(INICIO, 14, List.of(sinIds));

        assertTrue(modelo.estaOcupada(0, 2));
        assertNull(modelo.reservaEn(0, 2));
    }

    /**
     * R16: la barra contigua reemplaza las celdas "Ocupada" por noche, por lo
     * que una celda ocupada NO debe emitir texto aislado. La semantica de
     * ocupacion ({@code estaOcupada}/{@code reservaEn}) sigue intacta.
     */
    @Test
    void celdaOcupadaNoExponeTextoPorNochePeroConservaLaOcupacion() {
        Habitacion h103 = new Habitacion(103, Set.of(), 1, 2, 0, 0);
        LocalDate nocheOcupada = LocalDate.of(2026, 11, 1);
        OcupacionHabitacion ocupacion = new OcupacionHabitacion(
                h103, Set.of(nocheOcupada), Map.of(nocheOcupada, 42L));
        CalendarioOcupacionModel modelo = new CalendarioOcupacionModel(INICIO, 14, List.of(ocupacion));

        assertTrue(modelo.estaOcupada(0, 1), "precondicion: la noche esta ocupada");
        assertEquals("", modelo.getValueAt(0, 1), "celda ocupada sin texto 'Ocupada' por noche");
        assertEquals("", modelo.getValueAt(0, 2), "celda libre sin texto por noche");
        assertEquals(Long.valueOf(42L), modelo.reservaEn(0, 1), "reservaEn intacta");
    }

    // ---------------------------------- Slice 6: bar geometry + clean status

    private static final LocalDate FIN = INICIO.plusDays(3); // 2026-11-04

    private static CalendarioOcupacionModel modeloConBloque(boolean late) {
        Habitacion h103 = new Habitacion(103, Set.of(), 1, 2, 0, 0);
        OcupacionHabitacion.BloqueNoche bloque =
                new OcupacionHabitacion.BloqueNoche(7L, INICIO, FIN, late);
        OcupacionHabitacion ocupacion = new OcupacionHabitacion(h103,
                Set.of(INICIO, INICIO.plusDays(1), INICIO.plusDays(2)),
                Map.of(INICIO, 7L), List.of(bloque));
        return new CalendarioOcupacionModel(INICIO, 14, List.of(ocupacion));
    }

    @Test
    void limpiaDeFilaReflejaLaHabitacion() {
        assertTrue(modeloConBloque(false).limpiaDeFila(0), "nueva = limpia");

        Habitacion sucia = new Habitacion(103, Set.of(), 1, 2, 0, 0);
        sucia.setLimpia(false);
        OcupacionHabitacion ocupacion = new OcupacionHabitacion(sucia, Set.of());
        CalendarioOcupacionModel modelo = new CalendarioOcupacionModel(INICIO, 14, List.of(ocupacion));

        assertFalse(modelo.limpiaDeFila(0));
    }

    @Test
    void bloqueEnDevuelveElBloqueQueCubreLaNoche() {
        CalendarioOcupacionModel modelo = modeloConBloque(false);

        assertNotNull(modelo.bloqueEn(0, 1)); // 2026-11-01 = inicio
        assertEquals(INICIO, modelo.bloqueEn(0, 1).fechaInicio());
        assertEquals(7L, modelo.bloqueEn(0, 2).reservaId());
        assertNull(modelo.bloqueEn(0, 4), "el dia de salida no esta cubierto [inicio,fin)");
        assertNull(modelo.bloqueEn(0, 0), "columna 0");
    }

    @Test
    void bloqueSalidaEnDevuelveElBloqueQueSaleEseDia() {
        CalendarioOcupacionModel modelo = modeloConBloque(false);

        assertNotNull(modelo.bloqueSalidaEn(0, 4)); // 2026-11-04 = fin
        assertEquals(FIN, modelo.bloqueSalidaEn(0, 4).fechaFin());
        assertNull(modelo.bloqueSalidaEn(0, 3));
        assertNull(modelo.bloqueSalidaEn(0, 0));
    }

    @Test
    void segmentoEnAplicaLaGeometriaBloqueada() {
        CalendarioOcupacionModel modelo = modeloConBloque(false);

        GeometriaBarra.Segmento checkIn = modelo.segmentoEn(0, 1);
        assertEquals(0.25, checkIn.x0(), 1e-9);
        assertEquals(1.0, checkIn.x1(), 1e-9);

        GeometriaBarra.Segmento interior = modelo.segmentoEn(0, 2);
        assertEquals(0.0, interior.x0(), 1e-9);
        assertEquals(1.0, interior.x1(), 1e-9);

        assertNull(modelo.segmentoEn(0, 4), "salida no cubierta");
        assertNull(modelo.segmentoEn(0, 0));
    }

    @Test
    void segmentoSalidaEnUsaLaColaDelDiaDeSalida() {
        assertEquals(0.25, modeloConBloque(false).segmentoSalidaEn(0, 4).x1(), 1e-9);
        assertEquals(0.75, modeloConBloque(true).segmentoSalidaEn(0, 4).x1(), 1e-9);
        assertNull(modeloConBloque(false).segmentoSalidaEn(0, 3));
    }

    @Test
    void celdaDeTraspasoExponeHeadYTail() {
        Habitacion h103 = new Habitacion(103, Set.of(), 1, 2, 0, 0);
        OcupacionHabitacion.BloqueNoche saliente =
                new OcupacionHabitacion.BloqueNoche(7L, INICIO, FIN, false);
        OcupacionHabitacion.BloqueNoche entrante =
                new OcupacionHabitacion.BloqueNoche(8L, FIN, FIN.plusDays(2), false);
        OcupacionHabitacion ocupacion = new OcupacionHabitacion(h103, Set.of(FIN), Map.of(FIN, 8L),
                List.of(saliente, entrante));
        CalendarioOcupacionModel modelo = new CalendarioOcupacionModel(INICIO, 14, List.of(ocupacion));

        int colTurnover = modelo.columnaDeFecha(FIN);
        assertNotNull(modelo.segmentoEn(0, colTurnover), "head entrante");
        assertNotNull(modelo.segmentoSalidaEn(0, colTurnover), "tail saliente");
    }
}
