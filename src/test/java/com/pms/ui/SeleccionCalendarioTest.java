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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas headless de los helpers puros del calendario ({@link SeleccionCalendario}).
 * No se instancia ningun componente Swing: solo se ejercita la traduccion de
 * columnas de la matriz a rangos de estadia y acciones de celda.
 */
class SeleccionCalendarioTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);
    private static final int NOCHES = 5;

    /** Fila unica (hab 101) sin ninguna noche ocupada. */
    private static CalendarioOcupacionModel modeloLibre() {
        Habitacion h101 = new Habitacion(101, Set.of(), 1, 2, 0, 0);
        return new CalendarioOcupacionModel(INICIO, NOCHES,
                List.of(new OcupacionHabitacion(h101, Set.of())));
    }

    /** Fila unica con la noche de la columna 3 ocupada por la reserva 55. */
    private static CalendarioOcupacionModel modeloConOcupada() {
        Habitacion h101 = new Habitacion(101, Set.of(), 1, 2, 0, 0);
        LocalDate noche = INICIO.plusDays(2); // columna 3
        return new CalendarioOcupacionModel(INICIO, NOCHES,
                List.of(new OcupacionHabitacion(h101, Set.of(noche), Map.of(noche, 55L))));
    }

    /** Fila unica con una noche ocupada SIN id de reserva (back-compat 2-arg). */
    private static CalendarioOcupacionModel modeloOcupadaSinId() {
        Habitacion h101 = new Habitacion(101, Set.of(), 1, 2, 0, 0);
        LocalDate noche = INICIO.plusDays(2); // columna 3
        return new CalendarioOcupacionModel(INICIO, NOCHES,
                List.of(new OcupacionHabitacion(h101, Set.of(noche))));
    }

    /**
     * Fila unica con la celda de check-out de la columna 4 ocupada (simula el
     * check-in del proximo huesped); las columnas 2 y 3 estan libres.
     */
    private static CalendarioOcupacionModel modeloConCheckOutOcupado() {
        Habitacion h101 = new Habitacion(101, Set.of(), 1, 2, 0, 0);
        LocalDate noche = INICIO.plusDays(3); // columna 4 (dia de check-out de la estadia 2..4)
        return new CalendarioOcupacionModel(INICIO, NOCHES,
                List.of(new OcupacionHabitacion(h101, Set.of(noche), Map.of(noche, 77L))));
    }

    @Test
    void rangoDeUnaNocheUsaDosColumnasInclusivas() {
        CalendarioOcupacionModel m = modeloLibre();

        // Columna 2 = check-in, columna 3 = check-out (una noche).
        SeleccionCalendario.Rango r = SeleccionCalendario.rango(m, 2, 3);

        assertEquals(new SeleccionCalendario.Rango(INICIO.plusDays(1), INICIO.plusDays(2)), r);
    }

    @Test
    void unaSolaColumnaNoEsUnRangoValido() {
        CalendarioOcupacionModel m = modeloLibre();

        assertNull(SeleccionCalendario.rango(m, 2, 2), "hace falta check-in y check-out distintos");
    }

    @Test
    void rangoDeDragMultiNoche() {
        CalendarioOcupacionModel m = modeloLibre();

        // Columnas 1..3: check-in el dia 1, check-out el dia 3 (dos noches).
        SeleccionCalendario.Rango r = SeleccionCalendario.rango(m, 1, 3);

        assertEquals(new SeleccionCalendario.Rango(INICIO, INICIO.plusDays(2)), r);
    }

    @Test
    void rangoNormalizaColumnasInvertidas() {
        CalendarioOcupacionModel m = modeloLibre();

        assertEquals(SeleccionCalendario.rango(m, 1, 3), SeleccionCalendario.rango(m, 3, 1));
    }

    @Test
    void rangoFueraDeVentanaEsNull() {
        CalendarioOcupacionModel m = modeloLibre();

        assertNull(SeleccionCalendario.rango(m, 0, 0), "solo columna 0");
        assertNull(SeleccionCalendario.rango(m, NOCHES + 1, NOCHES + 2), "todo fuera de ventana");

        // Clamp parcial: un extremo fuera de ventana se recorta a la ultima columna.
        assertEquals(new SeleccionCalendario.Rango(INICIO.plusDays(3), INICIO.plusDays(NOCHES - 1)),
                SeleccionCalendario.rango(m, 4, 99));
    }

    @Test
    void esRangoLibreConTodasLibresEsTrue() {
        CalendarioOcupacionModel m = modeloConOcupada();

        assertTrue(SeleccionCalendario.esRangoLibre(m, 0, 1, 2));
        assertTrue(SeleccionCalendario.esRangoLibre(m, 0, 4, 5));
    }

    @Test
    void esRangoLibreQueCruzaOcupadaEsFalse() {
        CalendarioOcupacionModel m = modeloConOcupada();

        assertFalse(SeleccionCalendario.esRangoLibre(m, 0, 2, 4), "cruza la noche ocupada");
        assertFalse(SeleccionCalendario.esRangoLibre(m, 0, 3, 4), "la noche 3 esta ocupada");
    }

    @Test
    void esRangoLibreIgnoraLaCeldaDeCheckOutOcupada() {
        CalendarioOcupacionModel m = modeloConCheckOutOcupado();

        assertTrue(m.estaOcupada(0, 4), "precondicion: el check-out lo ocupa el proximo huesped");
        assertTrue(SeleccionCalendario.esRangoLibre(m, 0, 2, 4),
                "las noches 2 y 3 estan libres; la celda de check-out no bloquea");
    }

    @Test
    void esRangoLibreFalseCuandoUnaNocheInteriorEstaOcupada() {
        CalendarioOcupacionModel m = modeloConOcupada();

        assertTrue(m.estaOcupada(0, 3), "precondicion: la noche 3 esta ocupada");
        assertFalse(SeleccionCalendario.esRangoLibre(m, 0, 2, 4),
                "la noche 3 cae dentro del rango y lo bloquea");
    }

    @Test
    void accionDeCeldaLibreEsNueva() {
        CalendarioOcupacionModel m = modeloConOcupada();

        assertEquals(SeleccionCalendario.Tipo.NUEVA, SeleccionCalendario.accionDe(m, 0, 1));
    }

    @Test
    void accionDeCeldaOcupadaEsEditarConId() {
        CalendarioOcupacionModel m = modeloConOcupada();

        assertEquals(SeleccionCalendario.Tipo.EDITAR, SeleccionCalendario.accionDe(m, 0, 3));
        assertEquals(Long.valueOf(55L), m.reservaEn(0, 3), "EDITAR implica que hay id de reserva");
    }

    @Test
    void accionDeEncabezadoOColumnaCeroEsNada() {
        CalendarioOcupacionModel m = modeloConOcupada();

        assertEquals(SeleccionCalendario.Tipo.NADA, SeleccionCalendario.accionDe(m, 0, 0));
        assertEquals(SeleccionCalendario.Tipo.NADA, SeleccionCalendario.accionDe(m, 0, NOCHES + 1));
    }

    @Test
    void accionDeOcupadaSinIdEsNada() {
        CalendarioOcupacionModel m = modeloOcupadaSinId();

        assertTrue(m.estaOcupada(0, 3), "precondicion: la noche esta ocupada");
        assertEquals(SeleccionCalendario.Tipo.NADA, SeleccionCalendario.accionDe(m, 0, 3));
    }
}
