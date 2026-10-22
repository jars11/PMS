package com.pms.handlers;

import com.pms.entities.Habitacion;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asignacion de personas a recursos fisicos: parejas, personas individuales y
 * bebes contra camas matrimoniales, individuales, colchones y cunas.
 */
class AsignacionCamasTest {

    private static Habitacion habitacion(int matrimoniales, int individuales,
                                        int cunas, int colchones) {
        return new Habitacion(1, Set.of(), matrimoniales, individuales, cunas, colchones);
    }

    @Test
    void elCasoExactoUsaCadaCamaSinAvisos() {
        AsignacionCamas.Asignacion a = AsignacionCamas.calcular(
                habitacion(1, 2, 0, 0), 1, 2, 0, true);

        assertTrue(a.capacidadSuficiente());
        assertEquals(1, a.camasMatrimoniales());
        assertEquals(2, a.camasIndividuales());
        assertEquals(2, a.camasIndividualesReales());
        assertEquals(0, a.colchones());
        assertEquals("", a.avisoMatrimonios());
        assertEquals("", a.avisoPersonasIndividuales());
    }

    @Test
    void lasParejasSobrantesUsanCamasIndividuales() {
        AsignacionCamas.Asignacion a = AsignacionCamas.calcular(
                habitacion(1, 2, 0, 0), 2, 0, 0, true);

        assertTrue(a.capacidadSuficiente());
        assertEquals(1, a.camasMatrimoniales());
        assertEquals(2, a.camasIndividuales());
        assertEquals(1, a.parejasEnCamasIndividuales());
        assertTrue(a.avisoMatrimonios().contains("camas individuales"));
    }

    @Test
    void lasParejasSobrantesUsanColchones() {
        AsignacionCamas.Asignacion a = AsignacionCamas.calcular(
                habitacion(1, 0, 0, 2), 2, 0, 0, true);

        assertTrue(a.capacidadSuficiente());
        assertEquals(1, a.camasMatrimoniales());
        assertEquals(2, a.camasIndividuales());
        assertEquals(2, a.colchones());
        assertEquals(1, a.parejasEnColchones());
        assertTrue(a.avisoMatrimonios().contains("colchones"));
    }

    @Test
    void unaParejaPuedeUsarUnaCamaIndividualYUnColchon() {
        AsignacionCamas.Asignacion a = AsignacionCamas.calcular(
                habitacion(1, 1, 0, 1), 2, 0, 0, true);

        assertTrue(a.capacidadSuficiente());
        assertEquals(1, a.camasMatrimoniales());
        assertEquals(1, a.camasIndividualesReales());
        assertEquals(1, a.colchones());
        assertEquals(1, a.parejasEnMixto());
        assertTrue(a.avisoMatrimonios().contains("cama individual + colchón"));
    }

    @Test
    void lasPersonasIndividualesSobrantesUsanMatrimonialesDosPorCama() {
        AsignacionCamas.Asignacion a = AsignacionCamas.calcular(
                habitacion(2, 0, 0, 0), 1, 2, 0, true);

        assertTrue(a.capacidadSuficiente());
        assertEquals(2, a.camasMatrimoniales());
        assertEquals(2, a.individualesEnCamasMatrimoniales());
        assertTrue(a.avisoPersonasIndividuales().contains("camas matrimoniales"));
    }

    @Test
    void lasPersonasIndividualesSobrantesUsanMatrimonialesYColchones() {
        AsignacionCamas.Asignacion a = AsignacionCamas.calcular(
                habitacion(1, 0, 0, 2), 0, 4, 0, true);

        assertTrue(a.capacidadSuficiente());
        assertEquals(1, a.camasMatrimoniales());
        assertEquals(2, a.individualesEnCamasMatrimoniales());
        assertEquals(2, a.individualesEnColchones());
        assertTrue(a.avisoPersonasIndividuales().contains("camas matrimoniales"));
        assertTrue(a.avisoPersonasIndividuales().contains("colchones"));
    }

    @Test
    void losBebesNoConsumenCamasYElAvisoEsSoloPorFaltaDeCunas() {
        AsignacionCamas.Asignacion a = AsignacionCamas.calcular(
                habitacion(0, 0, 2, 0), 0, 0, 3, true);

        assertTrue(a.capacidadSuficiente());
        assertEquals(2, a.cunas());
        assertEquals(1, a.bebesSinCuna());
        assertTrue(a.avisoBebes().contains("cunas"));
    }

    @Test
    void sinCapacidadSuficienteAvisaYNoInventaCamas() {
        AsignacionCamas.Asignacion a = AsignacionCamas.calcular(
                habitacion(1, 0, 0, 0), 2, 0, 0, true);

        assertFalse(a.capacidadSuficiente());
        assertTrue(a.camasMatrimoniales() <= 1);
        assertFalse(a.avisoCapacidad().isBlank());
    }
}
