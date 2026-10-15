package com.pms;

import com.pms.utils.Fechas;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FechasTest {

    @Test
    void diasEntre10EneroY15EneroSon5() {
        assertEquals(5, Fechas.diasEntre(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15)));
    }

    @Test
    void diasEntreMismaFechaEs0() {
        assertEquals(0, Fechas.diasEntre(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 10)));
    }

    @Test
    void diasEntreRangoInvertidoEs0() {
        assertEquals(0, Fechas.diasEntre(LocalDate.of(2027, 1, 15), LocalDate.of(2027, 1, 10)));
    }

    @Test
    void aniosEntreJulio2010YEnero2027Son16() {
        assertEquals(16, Fechas.aniosEntre(LocalDate.of(2010, 7, 5), LocalDate.of(2027, 1, 1)));
    }

    @Test
    void aniosEntreRangoInvertidoEs0() {
        assertEquals(0, Fechas.aniosEntre(LocalDate.of(2027, 1, 1), LocalDate.of(2010, 7, 5)));
    }
}