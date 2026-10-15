package com.pms.ui;

import java.time.LocalDate;

/**
 * Mutable, headless drag state shared between {@link CalendarioPanel} (mouse
 * translation) and {@link RendererOcupacion} (painting). A left-press on a free
 * cell starts a range selection; a left-press on an occupied cell starts a move.
 */
public final class EstadoArrastre {

    public enum Tipo {
        NINGUNO,
        RANGO,
        MOVIMIENTO
    }

    private Tipo tipo = Tipo.NINGUNO;
    private int filaOrigen = -1;
    private int colIni = -1;
    private int colFin = -1;
    private int filaHover = -1;
    private GeometriaBarra.Bloque bloqueOrigen;

    public void iniciarRango(int fila, int columna) {
        tipo = Tipo.RANGO;
        filaOrigen = fila;
        colIni = columna;
        colFin = columna;
        filaHover = -1;
        bloqueOrigen = null;
    }

    public void iniciarMovimiento(int fila, int columna, GeometriaBarra.Bloque bloque) {
        tipo = Tipo.MOVIMIENTO;
        filaOrigen = fila;
        colIni = columna;
        colFin = columna;
        filaHover = fila;
        bloqueOrigen = bloque;
    }

    public void extenderRango(int columna) {
        if (tipo == Tipo.RANGO) {
            colFin = columna;
        }
    }

    public void hover(int fila) {
        if (tipo == Tipo.MOVIMIENTO) {
            filaHover = fila;
        }
    }

    public void limpiar() {
        tipo = Tipo.NINGUNO;
        filaOrigen = -1;
        colIni = -1;
        colFin = -1;
        filaHover = -1;
        bloqueOrigen = null;
    }

    public Tipo tipo() {
        return tipo;
    }

    public boolean activo() {
        return tipo != Tipo.NINGUNO;
    }

    public boolean esMovimiento() {
        return tipo == Tipo.MOVIMIENTO;
    }

    public int filaOrigen() {
        return filaOrigen;
    }

    public int colIni() {
        return colIni;
    }

    public int colFin() {
        return colFin;
    }

    public int filaHover() {
        return filaHover;
    }

    public GeometriaBarra.Bloque bloqueOrigen() {
        return bloqueOrigen;
    }

    /** A valid move target: in a move-drag, a real row that is not the origin. */
    public boolean esFilaValida(int fila) {
        return tipo == Tipo.MOVIMIENTO && fila != filaOrigen && fila >= 0;
    }

    /** True when (fila, columna) falls inside the active range selection. */
    public boolean esCeldaSeleccionada(int fila, int columna) {
        if (tipo != Tipo.RANGO || fila != filaOrigen || columna < 1) {
            return false;
        }
        int min = Math.min(colIni, colFin);
        int max = Math.max(colIni, colFin);
        return columna >= min && columna <= max;
    }

    /**
     * True when the hovered row can host the grey preview: a move-drag is
     * active, the hovered row is a real row that is not the origin, and the
     * caller reports that row as compatible. This is the single gate for the
     * "incompatible hovered row shows no preview" rule (R10).
     */
    public boolean esFilaHoverCompatible(boolean filaCompatible) {
        return tipo == Tipo.MOVIMIENTO && filaHover >= 0 && filaHover != filaOrigen && filaCompatible;
    }

    /**
     * Grey preview segment for the hovered move target, or {@code null} when the
     * hovered row cannot host a preview (no move-drag, origin row, or an
     * incompatible row). The compatibility of the hovered row is decided here, so
     * the renderer does not re-derive the rule.
     */
    public GeometriaBarra.Segmento previewSegmento(LocalDate dia, boolean filaCompatible) {
        if (bloqueOrigen == null || !esFilaHoverCompatible(filaCompatible)) {
            return null;
        }
        return GeometriaBarra.segmento(bloqueOrigen, dia);
    }

    /** Segmento de la barra fantasma de la CREACION: cuando hay un arrastre de
     *  rango activo y (fila, columna) cae dentro de el, devuelve el segmento de
     *  {@code dia} segun el bloque {@code [fechaIni, fechaFin)}; si no, null. */
    public GeometriaBarra.Segmento previewCreacion(int fila, int columna, LocalDate dia,
                                                   LocalDate fechaIni, LocalDate fechaFin) {
        if (tipo != Tipo.RANGO || dia == null || fechaIni == null || fechaFin == null
                || !esCeldaSeleccionada(fila, columna)) {
            return null;
        }
        return GeometriaBarra.segmento(new GeometriaBarra.Bloque(fechaIni, fechaFin, false), dia);
    }
}
