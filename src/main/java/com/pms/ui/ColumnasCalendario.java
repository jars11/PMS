package com.pms.ui;

/**
 * Calculo puro de cuantas noches entran en el ancho disponible del calendario.
 * La columna de habitacion ocupa un ancho fijo y cada noche otro; antes del
 * primer layout (ancho 0) se usa el default del modelo.
 */
final class ColumnasCalendario {

    private ColumnasCalendario() {
    }

    static int nochesVisibles(int ancho, int anchoHabitacion, int anchoNoche, int porDefecto) {
        if (ancho <= anchoHabitacion || anchoNoche <= 0) {
            return porDefecto;
        }
        return Math.max(1, (ancho - anchoHabitacion) / anchoNoche);
    }

    /**
     * Ancho de cada noche para llenar el espacio disponible sin dejar una tira
     * vacia a la derecha: el resto de la division se reparte entre las columnas.
     */
    static int anchoNoche(int ancho, int noches, int anchoHabitacion, int minimo) {
        if (noches <= 0 || ancho <= anchoHabitacion) {
            return minimo;
        }
        return Math.max(minimo, (ancho - anchoHabitacion) / noches);
    }
}
