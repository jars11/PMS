package com.pms.ui;

import java.time.LocalDate;

/**
 * Mutable, headless drag state shared between {@link CalendarioPanel} (mouse
 * translation) and {@link RendererOcupacion}/{@link TablaCalendario} (painting).
 *
 * <p>Hay tres interacciones de arrastre:
 * <ul>
 *   <li><b>RANGO</b>: un press sobre una noche libre arranca una seleccion de
 *       rango, inclusiva de la columna de check-out. La estadia sigue siendo
 *       half-open {@code [checkIn, checkOut)}. Con un press en la columna
 *       {@code A} y la ultima columna de noche {@code L}: un click sin arrastre
 *       selecciona {@code [A, A+1]} (o {@code [L-1, L]} si {@code A == L});
 *       arrastrar a la derecha selecciona {@code [A, C]}; a la izquierda trata
 *       la celda presionada como check-out y selecciona {@code [C, A]}.</li>
 *   <li><b>MOVIMIENTO</b>: un drag sobre una reserva la mueve de fila.</li>
 *   <li><b>REDIMENSION</b>: un drag sobre una flecha del extremo extiende o
 *       acorta la estadia de la reserva seleccionada.</li>
 * </ul>
 *
 * <p>La seleccion de una reserva (para mostrar sus flechas) es independiente
 * del arrastre: sobrevive a un click simple y se limpia al refrescar o al
 * empezar otra interaccion.
 */
public final class EstadoArrastre {

    public enum Tipo {
        NINGUNO,
        RANGO,
        MOVIMIENTO,
        REDIMENSION
    }

    private Tipo tipo = Tipo.NINGUNO;
    private int filaOrigen = -1;
    private int colIni = -1;
    private int colFin = -1;
    private int ultimaColumna = -1;
    private int filaHover = -1;
    private int filaDestino = -1;
    private GeometriaBarra.Bloque bloqueOrigen;
    private Long reservaOrigenId;
    /** True recien cuando el mouse cambio de casillero durante un rango. */
    private boolean rangoExtendido;

    // ------------------------------------------------------- reserva elegida
    private boolean haySeleccion;
    private int filaSeleccionada = -1;
    private Long reservaSeleccionadaId;
    private GeometriaBarra.Bloque bloqueSeleccionado;
    private HandlesEstadia.Flechas flechas = HandlesEstadia.Flechas.NINGUNA;

    // ---------------------------------------------------- redimension en curso
    private HandlesEstadia.Borde bordeRedimension;
    private HandlesEstadia.Direccion direccionRedimension;
    private LocalDate inicioRedimension;
    private LocalDate finRedimension;

    // ------------------------------------------------------------ rango/movimiento

    public void iniciarRango(int fila, int columna, int ultimaColumna) {
        tipo = Tipo.RANGO;
        filaOrigen = fila;
        colIni = columna;
        colFin = columna;
        this.ultimaColumna = ultimaColumna;
        filaHover = -1;
        bloqueOrigen = null;
        reservaOrigenId = null;
        rangoExtendido = false;
    }

    public void iniciarMovimiento(int fila, int columna, GeometriaBarra.Bloque bloque) {
        iniciarMovimiento(fila, columna, bloque, null);
    }

    public void iniciarMovimiento(int fila, int columna, GeometriaBarra.Bloque bloque, Long reservaId) {
        tipo = Tipo.MOVIMIENTO;
        filaOrigen = fila;
        colIni = columna;
        colFin = columna;
        ultimaColumna = -1;
        filaHover = fila;
        filaDestino = -1;
        bloqueOrigen = bloque;
        reservaOrigenId = reservaId;
    }

    public void extenderRango(int columna) {
        if (tipo == Tipo.RANGO) {
            colFin = columna;
            // El rango existe solo mientras el mouse esta en otra columna:
            // volver al casillero presionado cancela el preview.
            rangoExtendido = columna != colIni;
        }
    }

    public void hover(int fila) {
        if (tipo == Tipo.MOVIMIENTO) {
            filaHover = fila;
        }
    }

    /** Limpia SOLO la interaccion en curso; conserva la reserva seleccionada. */
    public void limpiar() {
        tipo = Tipo.NINGUNO;
        filaOrigen = -1;
        colIni = -1;
        colFin = -1;
        ultimaColumna = -1;
        filaHover = -1;
        filaDestino = -1;
        bloqueOrigen = null;
        reservaOrigenId = null;
        rangoExtendido = false;
        bordeRedimension = null;
        direccionRedimension = null;
        inicioRedimension = null;
        finRedimension = null;
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

    /**
     * First highlighted column (inclusive). For a leftward drag the pressed cell
     * is the check-out, so the current column becomes the start. For a press
     * without drag this is the pressed column, or {@code L-1} on the last night
     * (the pressed cell is then the check-out). {@code -1} outside a range.
     */
    public int seleccionIni() {
        if (tipo != Tipo.RANGO) {
            return -1;
        }
        if (colFin < colIni) {
            return colFin;
        }
        if (colFin == colIni && colIni >= ultimaColumna) {
            return Math.max(1, ultimaColumna - 1);
        }
        return colIni;
    }

    /**
     * Last highlighted column (inclusive): the check-out cell. A press without
     * drag extends one column past the pressed cell (clamped to the last night);
     * a leftward drag ends at the pressed column. {@code -1} outside a range.
     */
    public int seleccionFin() {
        if (tipo != Tipo.RANGO) {
            return -1;
        }
        if (colFin < colIni) {
            return colIni;
        }
        if (colFin == colIni) {
            return Math.min(colIni + 1, ultimaColumna);
        }
        return colFin;
    }

    public int filaHover() {
        return filaHover;
    }

    public GeometriaBarra.Bloque bloqueOrigen() {
        return bloqueOrigen;
    }

    /** Reserva que se esta moviendo; {@code null} fuera de un movimiento. */
    public Long reservaOrigenId() {
        return reservaOrigenId;
    }

    /**
     * True cuando la reserva movida debe quedar OCULTA en su fila original: hay
     * un movimiento activo y existe una fila destino valida (compatible y
     * visible) distinta del origen. Si el destino resuelto es el origen (el
     * cursor esta mas cerca de el), la reserva sigue pintada donde estaba.
     */
    public boolean ocultarBloqueOrigen() {
        return tipo == Tipo.MOVIMIENTO && filaDestino >= 0 && filaDestino != filaOrigen;
    }

    /**
     * Fila destino resuelta por el panel: la permitida (compatible y visible)
     * mas cercana a la fila apuntada, o el propio origen cuando este queda mas
     * cerca. {@code -1} cuando no hay destino posible.
     */
    public void destino(int fila) {
        if (tipo == Tipo.MOVIMIENTO) {
            filaDestino = fila;
        }
    }

    public int filaDestino() {
        return filaDestino;
    }

    /** A valid move target: in a move-drag, a real row that is not the origin. */
    public boolean esFilaValida(int fila) {
        return tipo == Tipo.MOVIMIENTO && fila != filaOrigen && fila >= 0;
    }

    /** True when (fila, columna) falls inside the active range selection (inclusive check-out). */
    public boolean esCeldaSeleccionada(int fila, int columna) {
        if (tipo != Tipo.RANGO || fila != filaOrigen || columna < 1) {
            return false;
        }
        // Sin cambio de casillero solo se resalta la celda presionada: no hay
        // rango pintado hasta que el mouse se mueve a otra columna.
        if (!rangoExtendido) {
            return columna == colIni;
        }
        return columna >= seleccionIni() && columna <= seleccionFin();
    }

    /** True cuando el mouse ya cambio de casillero durante el rango. */
    public boolean rangoExtendido() {
        return tipo == Tipo.RANGO && rangoExtendido;
    }

    /**
     * Segmento de la reserva movida para {@code dia}, o {@code null} cuando no
     * hay un destino resuelto distinto del origen. La compatibilidad y la
     * visibilidad ya las filtro el panel al fijar {@link #destino(int)}; aca
     * solo se dibuja donde va a caer.
     */
    public GeometriaBarra.Segmento previewSegmento(LocalDate dia) {
        if (bloqueOrigen == null || tipo != Tipo.MOVIMIENTO
                || filaDestino < 0 || filaDestino == filaOrigen) {
            return null;
        }
        return GeometriaBarra.segmento(bloqueOrigen, dia);
    }

    /** Segmento de la barra fantasma de la CREACION: cuando hay un arrastre de
     *  rango activo y (fila, columna) cae dentro de el, devuelve el segmento de
     *  {@code dia} segun el bloque {@code [fechaIni, fechaFin)}; si no, null. */
    public GeometriaBarra.Segmento previewCreacion(int fila, int columna, LocalDate dia,
                                                   LocalDate fechaIni, LocalDate fechaFin) {
        if (tipo != Tipo.RANGO || !rangoExtendido || dia == null || fechaIni == null
                || fechaFin == null || !esCeldaSeleccionada(fila, columna)) {
            return null;
        }
        return GeometriaBarra.segmento(new GeometriaBarra.Bloque(fechaIni, fechaFin, false), dia);
    }

    // ------------------------------------------------------- seleccion de reserva

    /** Marca la reserva clickeada como seleccionada y guarda sus flechas disponibles. */
    public void seleccionar(int fila, Long reservaId, GeometriaBarra.Bloque bloque,
                            HandlesEstadia.Flechas flechas) {
        this.haySeleccion = bloque != null;
        this.filaSeleccionada = fila;
        this.reservaSeleccionadaId = reservaId;
        this.bloqueSeleccionado = bloque;
        this.flechas = flechas == null ? HandlesEstadia.Flechas.NINGUNA : flechas;
    }

    public void limpiarSeleccionReserva() {
        haySeleccion = false;
        filaSeleccionada = -1;
        reservaSeleccionadaId = null;
        bloqueSeleccionado = null;
        flechas = HandlesEstadia.Flechas.NINGUNA;
        if (tipo == Tipo.REDIMENSION) {
            limpiar();
        }
    }

    public boolean haySeleccion() {
        return haySeleccion && bloqueSeleccionado != null;
    }

    public int filaSeleccionada() {
        return filaSeleccionada;
    }

    public Long reservaSeleccionadaId() {
        return reservaSeleccionadaId;
    }

    public GeometriaBarra.Bloque bloqueSeleccionado() {
        return bloqueSeleccionado;
    }

    public HandlesEstadia.Flechas flechasSeleccion() {
        return flechas;
    }

    // ----------------------------------------------------------- redimension

    /** Arranca el arrastre de una flecha de la reserva seleccionada. */
    public void iniciarRedimension(HandlesEstadia.Borde borde, HandlesEstadia.Direccion direccion) {
        if (!haySeleccion() || bloqueSeleccionado == null) {
            return;
        }
        tipo = Tipo.REDIMENSION;
        filaDestino = -1;
        bordeRedimension = borde;
        direccionRedimension = direccion;
        inicioRedimension = bloqueSeleccionado.fechaInicio();
        finRedimension = bloqueSeleccionado.fechaFin();
    }

    public boolean esRedimension() {
        return tipo == Tipo.REDIMENSION;
    }

    public HandlesEstadia.Borde bordeRedimension() {
        return bordeRedimension;
    }

    public HandlesEstadia.Direccion direccionRedimension() {
        return direccionRedimension;
    }

    /** Fija el rango que se esta previsualizando mientras se arrastra una flecha. */
    public void previewRedimension(LocalDate inicio, LocalDate fin) {
        if (tipo == Tipo.REDIMENSION && inicio != null && fin != null && inicio.isBefore(fin)) {
            inicioRedimension = inicio;
            finRedimension = fin;
        }
    }

    public LocalDate inicioRedimension() {
        return inicioRedimension;
    }

    public LocalDate finRedimension() {
        return finRedimension;
    }

    /** Segmento del rango previsualizado en {@code dia}; {@code null} fuera de una redimension. */
    public GeometriaBarra.Segmento previewRedimensionSegmento(LocalDate dia) {
        if (tipo != Tipo.REDIMENSION || bloqueSeleccionado == null
                || inicioRedimension == null || finRedimension == null) {
            return null;
        }
        return GeometriaBarra.segmento(new GeometriaBarra.Bloque(inicioRedimension, finRedimension,
                bloqueSeleccionado.lateCheckout()), dia);
    }

    /** Termina el arrastre de la flecha conservando la seleccion. */
    public void terminarRedimension() {
        if (tipo == Tipo.REDIMENSION) {
            limpiar();
        }
    }
}
