package com.pms.ui;

import com.pms.dtos.DTRHSolicitada;
import com.pms.utils.AtributoHabitacion;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests headless de la costura de entrada del asistente de propuesta: la
 * validacion de bloques es un metodo puro y no debe requerir pantalla ni
 * instanciar el panel Swing.
 */
class PanelPropuestaEstadiaTest {

    private static DTRHSolicitada bloque(LocalDate ini, LocalDate fin, int m, int i, int bebes, int cunas) {
        return new DTRHSolicitada(ini, fin, Set.of(AtributoHabitacion.Balcon), m, i, bebes, cunas);
    }

    /** LinkedHashSet para tolerar fechas nulas (TreeSet no admite comparacion con null). */
    private static Set<DTRHSolicitada> conjunto(DTRHSolicitada... bloques) {
        Set<DTRHSolicitada> set = new LinkedHashSet<>();
        for (DTRHSolicitada b : bloques) {
            set.add(b);
        }
        return set;
    }

    @Test
    void aceptaUnBloqueValido() {
        Set<DTRHSolicitada> bloques = conjunto(
                bloque(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0, 0));
        assertDoesNotThrow(() -> PanelPropuestaEstadia.validarBloques(bloques));
    }

    @Test
    void aceptaVariosBloquesValidos() {
        Set<DTRHSolicitada> bloques = conjunto(
                bloque(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0, 0),
                bloque(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 6), 0, 2, 1, 1));
        assertDoesNotThrow(() -> PanelPropuestaEstadia.validarBloques(bloques));
    }

    @Test
    void rechazaConjuntoVacio() {
        assertThrows(IllegalArgumentException.class,
                () -> PanelPropuestaEstadia.validarBloques(Set.of()));
    }

    @Test
    void rechazaConjuntoNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> PanelPropuestaEstadia.validarBloques(null));
    }

    @Test
    void rechazaCheckoutNoPosteriorAlCheckin() {
        assertThrows(IllegalArgumentException.class, () -> PanelPropuestaEstadia.validarBloques(
                conjunto(bloque(LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 4), 1, 0, 0, 0))));
        assertThrows(IllegalArgumentException.class, () -> PanelPropuestaEstadia.validarBloques(
                conjunto(bloque(LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 4), 1, 0, 0, 0))));
    }

    @Test
    void rechazaFechasNulas() {
        assertThrows(IllegalArgumentException.class, () -> PanelPropuestaEstadia.validarBloques(
                conjunto(bloque(null, LocalDate.of(2026, 11, 4), 1, 0, 0, 0))));
        assertThrows(IllegalArgumentException.class, () -> PanelPropuestaEstadia.validarBloques(
                conjunto(bloque(LocalDate.of(2026, 11, 1), null, 1, 0, 0, 0))));
    }

    @Test
    void rechazaCantidadesNegativas() {
        assertThrows(IllegalArgumentException.class, () -> PanelPropuestaEstadia.validarBloques(
                conjunto(bloque(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), -1, 0, 0, 0))));
        assertThrows(IllegalArgumentException.class, () -> PanelPropuestaEstadia.validarBloques(
                conjunto(bloque(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 0, -2, 0, 0))));
        assertThrows(IllegalArgumentException.class, () -> PanelPropuestaEstadia.validarBloques(
                conjunto(bloque(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 0, 0, -1, 0))));
        assertThrows(IllegalArgumentException.class, () -> PanelPropuestaEstadia.validarBloques(
                conjunto(bloque(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 0, 0, 0, -3))));
    }
}
