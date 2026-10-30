package com.pms.ui;

import com.pms.controllers.ControladorReservas.OcupacionHabitacion;
import com.pms.entities.Habitacion;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Modelo de tabla del calendario nocturno. Una fila por habitacion y una
 * columna por noche (la columna 0 es la etiqueta de la habitacion). Es un
 * modelo "tonto", sin dependencias de Swing mas alla de
 * {@link AbstractTableModel}, alimentado con la matriz de ocupacion del
 * controlador; por eso se puede probar headless.
 *
 * <p>La ventana por defecto son 14 noches. Las fechas de columna se calculan a
 * partir de {@code inicio}: la columna 1 es {@code inicio}, la columna
 * {@code 1+noches} sera {@code inicio.plusDays(noches)}.
 */
public final class CalendarioOcupacionModel extends AbstractTableModel {

    /** Cantidad de noches de la ventana por defecto. */
    public static final int NOCHES_POR_DEFECTO = 14;

    private static final String COLUMNA_HABITACION = "Habitación";

    private static final DateTimeFormatter FORMATO_DIA = DateTimeFormatter.ofPattern("dd/MM");

    private final LocalDate inicio;
    private final int noches;
    private final List<OcupacionHabitacion> ocupacion;

    /** Constructor de conveniencia: ventana de {@value #NOCHES_POR_DEFECTO} noches. */
    public CalendarioOcupacionModel(LocalDate inicio, List<OcupacionHabitacion> ocupacion) {
        this(inicio, NOCHES_POR_DEFECTO, ocupacion);
    }

    public CalendarioOcupacionModel(LocalDate inicio, int noches, List<OcupacionHabitacion> ocupacion) {
        this.inicio = Objects.requireNonNull(inicio, "inicio");
        this.noches = noches;
        this.ocupacion = List.copyOf(ocupacion == null ? List.of() : ocupacion);
    }

    @Override
    public int getRowCount() {
        return ocupacion.size();
    }

    @Override
    public int getColumnCount() {
        return 1 + noches;
    }

    @Override
    public String getColumnName(int column) {
        return column == 0 ? COLUMNA_HABITACION : FORMATO_DIA.format(inicio.plusDays(column - 1));
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return habitacionDeFila(rowIndex).getNumero();
        }
        // Sin texto por noche: la ocupacion se representa con la barra contigua
        // (RendererOcupacion), nunca con una celda "Ocupada" aislada. Para la
        // semantica de ocupacion use estaOcupada(...)/reservaEn(...).
        return "";
    }

    /** Habitacion de la fila indicada. */
    public Habitacion habitacionDeFila(int fila) {
        return ocupacion.get(fila).habitacion();
    }

    /** Fecha de la columna indicada; {@code null} para la columna 0 o fuera de rango. */
    public LocalDate fechaDeColumna(int columna) {
        if (columna <= 0 || columna > noches) {
            return null;
        }
        return inicio.plusDays(columna - 1);
    }

    /**
     * Columna correspondiente a una fecha (1-based); devuelve {@code -1} cuando
     * la fecha es nula o cae fuera de la ventana [inicio, inicio+noches).
     */
    public int columnaDeFecha(LocalDate fecha) {
        if (fecha == null) {
            return -1;
        }
        long dias = ChronoUnit.DAYS.between(inicio, fecha);
        if (dias < 0 || dias >= noches) {
            return -1;
        }
        return (int) dias + 1;
    }

    /** True cuando la celda (fila, columna) esta ocupada por una reserva activa. */
    public boolean estaOcupada(int fila, int columna) {
        LocalDate fecha = fechaDeColumna(columna);
        if (fecha == null) {
            return false;
        }
        Set<LocalDate> noches = ocupacion.get(fila).nochesOcupadas();
        return noches != null && noches.contains(fecha);
    }

    /**
     * Id de la reserva que ocupa la celda (fila, columna); {@code null} para la
     * columna 0, una celda libre o fuera de la ventana.
     */
    public Long reservaEn(int fila, int columna) {
        LocalDate fecha = fechaDeColumna(columna);
        if (fecha == null) {
            return null;
        }
        Map<LocalDate, Long> porNoche = ocupacion.get(fila).reservaPorNoche();
        return porNoche == null ? null : porNoche.get(fecha);
    }

    /** True cuando la habitacion de la fila esta limpia (housekeeping). */
    public boolean limpiaDeFila(int fila) {
        return habitacionDeFila(fila).isLimpia();
    }

    /**
     * Bloque activo que cubre la noche (fila, columna) con cobertura half-open
     * {@code [inicio,fin)}; {@code null} para la columna 0, una noche libre o un
     * dia de salida (no cubierto).
     */
    public OcupacionHabitacion.BloqueNoche bloqueEn(int fila, int columna) {
        return bloqueEnIgnorando(fila, columna, null);
    }

    /**
     * Igual que {@link #bloqueEn(int, int)} pero descarta el bloque de la reserva
     * indicada. El calendario lo usa para "levantar" la reserva que se esta
     * moviendo de su fila original.
     */
    public OcupacionHabitacion.BloqueNoche bloqueEnIgnorando(int fila, int columna, Long reservaIgnorada) {
        LocalDate fecha = fechaDeColumna(columna);
        if (fecha == null) {
            return null;
        }
        for (OcupacionHabitacion.BloqueNoche bloque : ocupacion.get(fila).bloques()) {
            if (reservaIgnorada != null && reservaIgnorada == bloque.reservaId()) {
                continue;
            }
            if (!fecha.isBefore(bloque.fechaInicio()) && fecha.isBefore(bloque.fechaFin())) {
                return bloque;
            }
        }
        return null;
    }

    /** Bloques de la fila (reservas activas), en el orden que los dio el controlador. */
    public List<OcupacionHabitacion.BloqueNoche> bloquesDeFila(int fila) {
        List<OcupacionHabitacion.BloqueNoche> bloques = ocupacion.get(fila).bloques();
        return bloques == null ? List.of() : List.copyOf(bloques);
    }

    /** Bloque cuya fecha de salida es exactamente la fecha de (fila, columna); {@code null} si no hay. */
    public OcupacionHabitacion.BloqueNoche bloqueSalidaEn(int fila, int columna) {
        return bloqueSalidaEnIgnorando(fila, columna, null);
    }

    /** Igual que {@link #bloqueSalidaEn(int, int)} pero descarta la reserva indicada. */
    public OcupacionHabitacion.BloqueNoche bloqueSalidaEnIgnorando(int fila, int columna,
                                                                   Long reservaIgnorada) {
        LocalDate fecha = fechaDeColumna(columna);
        if (fecha == null) {
            return null;
        }
        for (OcupacionHabitacion.BloqueNoche bloque : ocupacion.get(fila).bloques()) {
            if (reservaIgnorada != null && reservaIgnorada == bloque.reservaId()) {
                continue;
            }
            if (fecha.equals(bloque.fechaFin())) {
                return bloque;
            }
        }
        return null;
    }

    /** Segmento de la barra que cubre la noche (fila, columna); {@code null} si libre o columna 0. */
    public GeometriaBarra.Segmento segmentoEn(int fila, int columna) {
        return segmentoEnIgnorando(fila, columna, null);
    }

    /** Igual que {@link #segmentoEn(int, int)} pero descarta la reserva indicada. */
    public GeometriaBarra.Segmento segmentoEnIgnorando(int fila, int columna, Long reservaIgnorada) {
        OcupacionHabitacion.BloqueNoche bloque = bloqueEnIgnorando(fila, columna, reservaIgnorada);
        return bloque == null ? null : GeometriaBarra.segmento(aBloque(bloque), fechaDeColumna(columna));
    }

    /** Cola del dia de salida: segmento del bloque que termina en la fecha de (fila, columna). */
    public GeometriaBarra.Segmento segmentoSalidaEn(int fila, int columna) {
        return segmentoSalidaEnIgnorando(fila, columna, null);
    }

    /** Igual que {@link #segmentoSalidaEn(int, int)} pero descarta la reserva indicada. */
    public GeometriaBarra.Segmento segmentoSalidaEnIgnorando(int fila, int columna, Long reservaIgnorada) {
        OcupacionHabitacion.BloqueNoche bloque = bloqueSalidaEnIgnorando(fila, columna, reservaIgnorada);
        return bloque == null ? null : GeometriaBarra.segmento(aBloque(bloque), fechaDeColumna(columna));
    }

    private static GeometriaBarra.Bloque aBloque(OcupacionHabitacion.BloqueNoche bloque) {
        return new GeometriaBarra.Bloque(bloque.fechaInicio(), bloque.fechaFin(), bloque.lateCheckout());
    }
}
