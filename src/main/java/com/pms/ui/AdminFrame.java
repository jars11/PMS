package com.pms.ui;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.entities.Habitacion;
import com.pms.utils.AtributoHabitacion;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pantalla de administración de habitaciones (rol ADMIN). Muestra el listado
 * real en una {@link JTable} alimentada por el controlador y permite alta,
 * edición y baja mediante {@link DialogoHabitacion}. Los errores de negocio
 * (numero duplicado, habitación en uso, datos inválidos) se muestran en un
 * diálogo de error y la tabla se refresca tras cada mutación.
 */
public final class AdminFrame extends JFrame {

    private final ControladorHabitaciones controlador = ControladorHabitaciones.getInstance();
    private final ModeloTablaHabitaciones modelo = new ModeloTablaHabitaciones();
    private final JTable tabla = new JTable(modelo);

    public AdminFrame(SesionUsuario sesion) {
        super("PMS – Administración de habitaciones (" + sesion.getRol() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(22);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(construirPanelAcciones(), BorderLayout.SOUTH);

        setSize(820, 420);
        setLocationRelativeTo(null);
        refrescar();
    }

    private JPanel construirPanelAcciones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton nueva = new JButton("Nueva");
        nueva.addActionListener(e -> nuevaHabitacion());
        JButton editar = new JButton("Editar");
        editar.addActionListener(e -> editarHabitacion());
        JButton eliminar = new JButton("Eliminar");
        eliminar.addActionListener(e -> eliminarHabitacion());
        panel.add(nueva);
        panel.add(editar);
        panel.add(eliminar);
        return panel;
    }

    private void nuevaHabitacion() {
        DialogoHabitacion dialogo = DialogoHabitacion.paraAlta(this);
        if (!dialogo.esAceptado()) {
            return;
        }
        try {
            controlador.altaHabitacion(dialogo.getNumero(), dialogo.getAtributos(),
                    dialogo.getMatrimoniales(), dialogo.getIndividuales(),
                    dialogo.getCunas(), dialogo.getColchones(), dialogo.getTarifa());
            if (!dialogo.getLimpia()) {
                controlador.getHabitaciones().stream()
                        .filter(h -> h.getNumero() == dialogo.getNumero())
                        .findFirst()
                        .ifPresent(h -> controlador.marcarLimpia(h.getId(), false));
            }
        } catch (IllegalStateException | IllegalArgumentException ex) {
            mostrarError(ex);
        }
        refrescar();
    }

    private void editarHabitacion() {
        Habitacion actual = habitacionSeleccionada("editar");
        if (actual == null) {
            return;
        }
        DialogoHabitacion dialogo = DialogoHabitacion.paraEdicion(this, actual);
        if (!dialogo.esAceptado()) {
            return;
        }
        try {
            controlador.actualizarHabitacion(actual.getId(), dialogo.getNumero(), dialogo.getAtributos(),
                    dialogo.getMatrimoniales(), dialogo.getIndividuales(),
                    dialogo.getCunas(), dialogo.getColchones(), dialogo.getTarifa());
            controlador.marcarLimpia(actual.getId(), dialogo.getLimpia());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            mostrarError(ex);
        }
        refrescar();
    }

    private void eliminarHabitacion() {
        Habitacion actual = habitacionSeleccionada("eliminar");
        if (actual == null) {
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar la habitación " + actual.getNumero() + "?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controlador.eliminarHabitacion(actual.getId());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            mostrarError(ex);
        }
        refrescar();
    }

    private Habitacion habitacionSeleccionada(String accion) {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this,
                    "Seleccioná una habitación para " + accion + ".",
                    "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return modelo.habitacionEn(fila);
    }

    private void refrescar() {
        modelo.setHabitaciones(controlador.getHabitaciones());
    }

    private void mostrarError(RuntimeException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    /** Modelo de tabla de habitaciones alimentado por el controlador. */
    private static final class ModeloTablaHabitaciones extends AbstractTableModel {

        private static final String[] COLUMNAS = {
                "Número", "Atributos", "Matrimoniales", "Individuales",
                "Cunas", "Colchones", "Tarifa", "Capacidad"
        };

        private List<Habitacion> habitaciones = List.of();

        void setHabitaciones(List<Habitacion> nuevas) {
            this.habitaciones = List.copyOf(nuevas);
            fireTableDataChanged();
        }

        Habitacion habitacionEn(int fila) {
            return habitaciones.get(fila);
        }

        @Override
        public int getRowCount() {
            return habitaciones.size();
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
            Habitacion h = habitaciones.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> h.getNumero();
                case 1 -> atributosTexto(h.getAtributos());
                case 2 -> h.getCantidadMatrimoniales();
                case 3 -> h.getCantidadIndividuales();
                case 4 -> h.getCantidadCunas();
                case 5 -> h.getColchones();
                case 6 -> h.getTarifaPorNoche();
                case 7 -> h.capacidad();
                default -> "";
            };
        }

        private static String atributosTexto(Set<AtributoHabitacion> atributos) {
            return atributos.stream().map(AtributoHabitacion::name).collect(Collectors.joining(", "));
        }
    }
}
