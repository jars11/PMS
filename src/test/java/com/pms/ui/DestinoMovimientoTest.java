package com.pms.ui;

import org.junit.jupiter.api.Test;

import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El destino del movimiento es la fila bajo el mouse solo si es compatible. Si
 * la fila es gris no hay destino (la reserva queda en su origen); no se salta a
 * otra fila. La fila de origen siempre se permite para representar "quedarse".
 */
class DestinoMovimientoTest {

    private static final int FILAS = 5;

    private static int resolver(int hover, int origen, IntPredicate compatible) {
        return DestinoMovimiento.resolver(hover, origen, FILAS, compatible);
    }

    @Test
    void devuelveLaFilaBajoElMouseSiEsCompatible() {
        assertEquals(0, resolver(0, 4, fila -> true));
        assertEquals(2, resolver(2, 0, fila -> true));
        assertEquals(4, resolver(4, 0, fila -> true));
    }

    @Test
    void unaFilaGrisNoEsDestino() {
        assertEquals(-1, resolver(2, 0, fila -> false));
        assertEquals(-1, resolver(3, 1, fila -> fila != 3));
    }

    @Test
    void laFilaOriginalSiempreSePermiteParaQuedarse() {
        assertEquals(3, resolver(3, 3, fila -> false));
    }

    @Test
    void fueraDeLaTablaNoHayDestino() {
        assertEquals(-1, resolver(-1, 0, fila -> true));
        assertEquals(-1, resolver(FILAS, 0, fila -> true));
        assertEquals(-1, resolver(99, 0, fila -> true));
    }
}
