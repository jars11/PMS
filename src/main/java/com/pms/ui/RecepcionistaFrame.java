package com.pms.ui;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorReservas;
import com.pms.controllers.ControladorReservas.ReservaVista;
import com.pms.entities.Habitacion;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Pantalla de recepcion (rol RECEPCIONISTA). Pestaña de reservas con una
 * {@link JTable} sobre {@link ReservasTableModel} alimentada por
 * {@code listarReservas()}, una accion "Nueva reserva" que abre
 * {@link DialogoReserva}, una accion "Colocar reserva automática" que abre el
 * dialogo en modo automatico (la habitacion la resuelve el sistema), la pestana
 * del calendario nocturno ({@link CalendarioPanel}) con el asistente de propuesta
 * de estadía ({@link PanelPropuestaEstadia}) debajo, en un split vertical
 * redimensionable.
 *
 * <p>El click derecho sobre una fila de la lista abre {@link DialogoReserva} en
 * modo edicion cuando la reserva tiene UNA sola propuesta (D1); si abarca varias
 * habitaciones se avisa y se sugiere editar desde el calendario. Cualquier
 * mutacion refresca la lista y el calendario.
 */
public final class RecepcionistaFrame extends JFrame {

    private final ControladorReservas controlador = ControladorReservas.getInstance();
    private final ControladorHabitaciones controladorHabitaciones = ControladorHabitaciones.getInstance();
    private final ReservasTableModel modelo = new ReservasTableModel();
    private final JTable tabla = new JTable(modelo);
    private final CalendarioPanel calendario = new CalendarioPanel();

    public RecepcionistaFrame(SesionUsuario sesion) {
        super("PMS – Recepción (" + sesion.getRol() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JTabbedPane pestanas = new JTabbedPane();
        // El calendario es la pestana principal de recepcion: queda primero y
        // seleccionada al abrir la pantalla.
        pestanas.addTab("Calendario", construirPanelCalendario());
        pestanas.addTab("Reservas", construirPanelReservas());
        add(pestanas, BorderLayout.CENTER);

        setSize(1120, 780);
        setLocationRelativeTo(null);
        refrescar();
    }

    /**
     * Calendario y asistente de propuesta comparten pestana: el asistente queda
     * debajo, en un split vertical que el usuario puede agrandar o achicar con
     * la barra limite. Las propuestas elegidas se previsualizan en gris sobre el
     * calendario antes de confirmarlas.
     */
    private JSplitPane construirPanelCalendario() {
        PanelPropuestaEstadia propuesta =
                new PanelPropuestaEstadia(this::refrescar, calendario::previsualizarPropuestas);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, calendario, propuesta);
        split.setResizeWeight(0.55);
        split.setContinuousLayout(true);
        split.setOneTouchExpandable(true);
        split.setBorder(null);
        split.setDividerLocation(400);
        calendario.setMinimumSize(new Dimension(0, 180));
        propuesta.setMinimumSize(new Dimension(0, 220));
        return split;
    }

    private JPanel construirPanelReservas() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setRowHeight(22);
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    menuReserva(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    menuReserva(e);
                }
            }
        });
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton nueva = new JButton("Nueva reserva");
        nueva.addActionListener(e -> nuevaReserva());
        JButton automatica = new JButton("Colocar reserva automática");
        automatica.addActionListener(e -> colocarReservaAutomatica());
        JButton refrescar = new JButton("Refrescar");
        refrescar.addActionListener(e -> refrescar());
        acciones.add(nueva);
        acciones.add(automatica);
        acciones.add(refrescar);
        panel.add(acciones, BorderLayout.SOUTH);
        return panel;
    }

    private void menuReserva(MouseEvent e) {
        int fila = tabla.rowAtPoint(e.getPoint());
        if (fila < 0 || fila >= tabla.getRowCount()) {
            return;
        }
        tabla.setRowSelectionInterval(fila, fila);
        ReservaVista vista = modelo.reservaEn(fila);

        JPopupMenu menu = new JPopupMenu();
        JMenuItem editar = new JMenuItem("Editar reserva");
        editar.addActionListener(a -> editarReserva(vista));
        menu.add(editar);
        menu.show(tabla, e.getX(), e.getY());
    }

    private void editarReserva(ReservaVista vista) {
        if (vista == null || vista.checkIn() == null
                || vista.numerosHabitacion() == null || vista.numerosHabitacion().size() != 1) {
            aviso("Esta reserva abarca varias habitaciones. Editá cada noche desde el calendario.");
            return;
        }
        Habitacion habitacion = habitacionPorNumero(vista.numerosHabitacion().iterator().next());
        if (habitacion == null || habitacion.getId() == null) {
            aviso("No se encontró la habitación de la reserva.");
            return;
        }
        try {
            ControladorReservas.EdicionReserva edicion =
                    controlador.edicionDe(vista.id(), habitacion.getId(), vista.checkIn());
            DialogoReserva dialogo = DialogoReserva.paraEdicion(this, edicion);
            if (dialogo.esAceptado()) {
                refrescar();
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            aviso(ex.getMessage());
        }
    }

    private Habitacion habitacionPorNumero(int numero) {
        for (Habitacion h : controladorHabitaciones.getHabitaciones()) {
            if (h.getNumero().equals(numero)) {
                return h;
            }
        }
        return null;
    }

    private void nuevaReserva() {
        DialogoReserva dialogo = DialogoReserva.paraAlta(this);
        if (dialogo.esAceptado()) {
            refrescar();
        }
    }

    /**
     * Abre el alta automatica: la recepcionista carga titular, fechas y camas y
     * el sistema resuelve la habitacion. No hay contexto de calendario ni celda.
     */
    private void colocarReservaAutomatica() {
        DialogoReserva dialogo = DialogoReserva.paraAltaAutomatica(this);
        if (dialogo.esAceptado()) {
            refrescar();
        }
    }

    private void aviso(String mensaje) {
        DialogoError.mostrar(this, "Aviso", mensaje);
    }

    private void refrescar() {
        modelo.setReservas(controlador.listarReservas());
        calendario.refrescar();
    }
}
