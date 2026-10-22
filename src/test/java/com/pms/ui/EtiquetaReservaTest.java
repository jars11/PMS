package com.pms.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Formato puro de la etiqueta de reserva: nombre + conteo de personas/bebes y
 * recorte con puntos suspensivos conservando el principio del nombre.
 */
class EtiquetaReservaTest {

    @Test
    void unaSolaPersonaSoloMuestraElSimbolo() {
        assertEquals(EtiquetaReserva.ICONO_PERSONA, EtiquetaReserva.conteo(1, 0));
    }

    @Test
    void variasPersonasMuestranNumeroYSimbolo() {
        assertEquals("3" + EtiquetaReserva.ICONO_PERSONA, EtiquetaReserva.conteo(3, 0));
    }

    @Test
    void unSoloBebeSoloMuestraElSimboloConMas() {
        assertEquals(EtiquetaReserva.ICONO_PERSONA + " + " + EtiquetaReserva.ICONO_BEBE,
                EtiquetaReserva.conteo(1, 1));
    }

    @Test
    void variosBebesMuestranNumeroYSimbolo() {
        assertEquals("2" + EtiquetaReserva.ICONO_PERSONA + " + 3" + EtiquetaReserva.ICONO_BEBE,
                EtiquetaReserva.conteo(2, 3));
    }

    @Test
    void sinPersonasNiBebesElConteoEsVacio() {
        assertEquals("", EtiquetaReserva.conteo(0, 0));
    }

    @Test
    void completaUneNombreSeparadorYConteo() {
        assertEquals("Ana" + EtiquetaReserva.SEPARADOR + "2" + EtiquetaReserva.ICONO_PERSONA,
                EtiquetaReserva.completa("  Ana ", 2, 0));
    }

    @Test
    void completaOmitePartesVacias() {
        assertEquals(EtiquetaReserva.ICONO_PERSONA, EtiquetaReserva.completa("", 1, 0));
        assertEquals("Ana", EtiquetaReserva.completa("Ana", 0, 0));
    }

    @Test
    void truncarNoCambiaUnTextoQueEntra() {
        assertEquals("Ana", EtiquetaReserva.truncar("Ana", 10, String::length));
    }

    @Test
    void truncarConservaElPrincipioYPonePuntos() {
        assertEquals("Agustin" + EtiquetaReserva.PUNTOS,
                EtiquetaReserva.truncar("Agustina Rivero", 8, String::length));
    }

    @Test
    void truncarSinEspacioParaLosPuntosDevuelveVacio() {
        assertEquals("", EtiquetaReserva.truncar("Agustina", 0, String::length));
    }

    @Test
    void truncarNoParteEmojis() {
        String texto = EtiquetaReserva.ICONO_PERSONA + EtiquetaReserva.ICONO_BEBE;
        // El ancho lo medimos en unidades UTF-16: si se partiera un par sustituto
        // el recorte devolveria un caracter invalido en lugar de un emoji entero.
        assertEquals(EtiquetaReserva.ICONO_PERSONA + EtiquetaReserva.PUNTOS,
                EtiquetaReserva.truncar(texto, 3, String::length));
    }
}
