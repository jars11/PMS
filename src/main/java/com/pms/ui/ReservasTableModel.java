package com.pms.ui;

import com.pms.controllers.ControladorReservas.ReservaVista;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Modelo de tabla de reservas para la pantalla de recepcion. Es un modelo
 * "tonto" (sin dependencias de Swing mas alla de {@link AbstractTableModel})
 * alimentado con las vistas del controlador, de modo que se mantenga separado
 * de la logica y sea verificable de forma aislada.
 *
 * <p>Columnas: Titular, Habitaciones, Check-in, Check-out y Estado. Las fechas
 * se muestran como dd/MM/yyyy y el estado agrega " (inactiva)" cuando la
 * reserva no esta activa.
 */
public final class ReservasTableModel extends AbstractTableModel {

    private static final String[] COLUMNAS = {"Titular", "Habitaciones", "Check-in", "Check-out", "Estado"};

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private List<ReservaVista> reservas = List.of();

    /** Reemplaza los datos y notifica a la tabla. */
    public void setReservas(List<ReservaVista> nuevas) {
        this.reservas = List.copyOf(nuevas);
        fireTableDataChanged();
    }

    /** Vista de la fila indicada (por si la pantalla necesita mas datos). */
    public ReservaVista reservaEn(int fila) {
        return reservas.get(fila);
    }

    @Override
    public int getRowCount() {
        return reservas.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNAS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ReservaVista v = reservas.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> v.titular();
            case 1 -> habitacionesTexto(v.numerosHabitacion());
            case 2 -> v.checkIn() == null ? "" : FORMATO_FECHA.format(v.checkIn());
            case 3 -> v.checkOut() == null ? "" : FORMATO_FECHA.format(v.checkOut());
            case 4 -> estadoTexto(v);
            default -> "";
        };
    }

    private static String habitacionesTexto(Set<Integer> numeros) {
        return numeros.stream().sorted().map(String::valueOf).collect(Collectors.joining(", "));
    }

    private static String estadoTexto(ReservaVista v) {
        return v.estado().name() + (v.activa() ? "" : " (inactiva)");
    }
}
