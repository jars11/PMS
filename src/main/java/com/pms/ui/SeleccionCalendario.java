package com.pms.ui;

import java.time.LocalDate;

/**
 * Helpers PUROS (sin dependencias de Swing) que traducen gestos del calendario
 * de ocupacion a rangos de estadia y acciones sobre una celda. Se mantienen
 * fuera del panel para poder probarlos headless.
 *
 * <p>El rango de estadia es half-open: {@code [checkIn, checkOut)}. La columna 0
 * (etiqueta de habitacion) y las columnas fuera de la ventana visible no son
 * noches validas.
 */
public final class SeleccionCalendario {

    private SeleccionCalendario() {
    }

    /** Rango de estadia half-open: {@code [checkIn, checkOut)}. */
    public record Rango(LocalDate checkIn, LocalDate checkOut) {
    }

    /** Accion inferida para una celda clickeada del calendario. */
    public enum Tipo {
        NUEVA,
        EDITAR,
        NADA
    }

    /**
     * Traduce el par de columnas [cIni, cFin] a un rango de estadia. Las
     * columnas se normalizan (min/max) y se recortan a [1, noches].
     *
     * @return el rango, o {@code null} si no hay ninguna noche valida.
     */
    public static Rango rango(CalendarioOcupacionModel modelo, int cIni, int cFin) {
        if (modelo == null) {
            return null;
        }
        int noches = modelo.getColumnCount() - 1;
        int primera = Math.max(1, Math.min(cIni, cFin));
        int ultima = Math.min(noches, Math.max(cIni, cFin));
        if (primera > ultima) {
            return null;
        }
        LocalDate checkIn = modelo.fechaDeColumna(primera);
        LocalDate checkOut = modelo.fechaDeColumna(ultima).plusDays(1);
        return new Rango(checkIn, checkOut);
    }

    /**
     * True cuando TODAS las noches del rango [cIni, cFin] estan libres para la
     * fila indicada. Un rango sin noches validas no es libre ({@code false}).
     */
    public static boolean esRangoLibre(CalendarioOcupacionModel modelo, int fila, int cIni, int cFin) {
        if (rango(modelo, cIni, cFin) == null) {
            return false;
        }
        int noches = modelo.getColumnCount() - 1;
        int primera = Math.max(1, Math.min(cIni, cFin));
        int ultima = Math.min(noches, Math.max(cIni, cFin));
        for (int columna = primera; columna <= ultima; columna++) {
            if (modelo.estaOcupada(fila, columna)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Accion de la celda (fila, columna): {@code EDITAR} si esta ocupada por una
     * reserva identificable, {@code NUEVA} si esta libre, y {@code NADA} para la
     * columna 0, fuera de ventana o una noche ocupada sin id de reserva.
     */
    public static Tipo accionDe(CalendarioOcupacionModel modelo, int fila, int columna) {
        if (modelo == null || modelo.fechaDeColumna(columna) == null) {
            return Tipo.NADA;
        }
        if (!modelo.estaOcupada(fila, columna)) {
            return Tipo.NUEVA;
        }
        return modelo.reservaEn(fila, columna) != null ? Tipo.EDITAR : Tipo.NADA;
    }
}
