package com.pms.ui;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteraccionConsolaTest {

    private InteraccionConsola conEntrada(String entrada) {
        return new InteraccionConsola(new Scanner(entrada), new PrintStream(new ByteArrayOutputStream()));
    }

    @Test
    void aceptaConS() {
        assertTrue(conEntrada("s\n").confirmar("¿?")); 
    }

    @Test
    void aceptaConSi() {
        assertTrue(conEntrada("Si\n").confirmar("¿?"));
    }

    @Test
    void rechazaConN() {
        assertFalse(conEntrada("n\n").confirmar("¿?"));
    }

    @Test
    void rechazaOtraCosa() {
        assertFalse(conEntrada("quizas\n").confirmar("¿?"));
    }

    @Test
    void sinLineaRechaza() {
        assertFalse(conEntrada("").confirmar("¿?"));
    }
}
