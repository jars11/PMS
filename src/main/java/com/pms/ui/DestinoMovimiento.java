package com.pms.ui;

import java.util.function.IntPredicate;

/**
 * Resolucion pura de la fila destino de un movimiento: la fila exacta bajo el
 * mouse, pero SOLO si es compatible. Si el cursor esta sobre una fila gris no
 * hay destino: la reserva se queda pintada en su fila original y soltar ahi
 * cancela el movimiento. No se salta a una fila compatible mas lejana.
 */
final class DestinoMovimiento {

    private DestinoMovimiento() {
    }

    /**
     * @param filaHover      fila bajo el cursor ({@code -1} fuera de la tabla)
     * @param filaOrigen     fila de la reserva movida (siempre se permite:
     *                       representa quedarse en el origen)
     * @param filas          cantidad total de filas
     * @param filaCompatible true cuando la reserva puede moverse a esa fila
     * @return la fila bajo el cursor, el origen si el cursor esta sobre el, o
     *         {@code -1} si la fila es gris o no existe
     */
    static int resolver(int filaHover, int filaOrigen, int filas,
                        IntPredicate filaCompatible) {
        if (filaHover < 0 || filaHover >= filas) {
            return -1;
        }
        if (filaHover == filaOrigen) {
            return filaHover;
        }
        if (filaCompatible != null && !filaCompatible.test(filaHover)) {
            return -1;
        }
        return filaHover;
    }
}
