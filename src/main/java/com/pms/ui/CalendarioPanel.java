package com.pms.ui;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorReservas;
import com.pms.controllers.ControladorReservas.OcupacionHabitacion;
import com.pms.controllers.ControladorReservas.ResultadoCompatibilidad;
import com.pms.controllers.ControladorReservas.ResultadoMovimiento;
import com.pms.entities.Habitacion;
import com.pms.utils.MotivoRechazo;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pestana del calendario nocturno. Muestra una {@link JTable} con el
 * {@link CalendarioOcupacionModel} y permite navegar la ventana de 14 noches
 * con los botones anterior/siguiente (mas un selector de fecha de inicio).
 *
 * <p>La interaccion con el raton la gobierna {@link EstadoArrastre}: un press
 * sobre una noche libre arranca una seleccion de rango; un press sobre una noche
 * ocupada arranca un MOVIMIENTO del bloque clickeado. Durante un movimiento el
 * renderer pinta en gris las filas incompatibles (compatibilidad cacheada por
 * habitacion en el arranque) y un preview gris en la fila apuntada. Al soltar en
 * una habitacion valida se llama a {@code moverReserva}; un rechazo muestra el
 * mensaje exacto mas el detalle del motivo, y un drop fuera de filas cancela en
 * silencio. El click derecho abre el menu contextual de siempre.
 */
public final class CalendarioPanel extends JPanel {

    private static final int NOCHES = CalendarioOcupacionModel.NOCHES_POR_DEFECTO;
    private static final int ANCHO_HABITACION = 110;
    private static final int ANCHO_NOCHE = 56;

    private final ControladorReservas controlador = ControladorReservas.getInstance();
    private final JTable tabla = new JTable();
    private final JSpinner spinnerInicio = new JSpinner(
            new SpinnerDateModel(aFecha(LocalDate.now()), null, null, Calendar.DAY_OF_MONTH));

    /** Estado del arrastre en curso (rango o movimiento). */
    private final EstadoArrastre estado = new EstadoArrastre();

    /** Compatibilidad cacheada por habitacion para el movimiento en curso. */
    private final Map<Long, MotivoRechazo> motivoPorHabitacion = new HashMap<>();

    /** Contexto del movimiento en curso; null cuando no hay uno. */
    private Long reservaOrigenId;
    private Long habitacionOrigenId;
    private LocalDate nocheOrigen;

    private final MouseAdapter raton = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                mostrarMenu(e);
                return;
            }
            if (SwingUtilities.isLeftMouseButton(e)) {
                iniciarInteraccion(e);
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (estado.esMovimiento()) {
                estado.hover(filaEn(e));
                tabla.repaint();
            } else if (estado.tipo() == EstadoArrastre.Tipo.RANGO) {
                extenderRango(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                mostrarMenu(e);
                return;
            }
            if (!estado.activo()) {
                return;
            }
            if (estado.esMovimiento()) {
                finalizarMovimiento(e);
            } else {
                finalizarRango(e);
            }
        }
    };

    public CalendarioPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        tabla.setRowHeight(26);
        tabla.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tabla.setIntercellSpacing(new Dimension(0, 0));
        tabla.setShowGrid(false);
        tabla.setRowMargin(0);
        tabla.setOpaque(true);
        tabla.addMouseListener(raton);
        tabla.addMouseMotionListener(raton);
        // Shift + rueda navega dia a dia (rueda arriba = dia anterior).
        tabla.addMouseWheelListener(e -> {
            if (e.isShiftDown()) {
                mover(e.getWheelRotation() < 0 ? -1 : 1);
                e.consume();
            }
        });

        add(construirBarraNavegacion(), BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        refrescar();
    }

    private JPanel construirBarraNavegacion() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

        JButton anterior = new JButton("< Día anterior");
        anterior.addActionListener(e -> mover(-1));
        JButton siguiente = new JButton("Día siguiente >");
        siguiente.addActionListener(e -> mover(1));

        spinnerInicio.setEditor(new JSpinner.DateEditor(spinnerInicio, "dd/MM/yyyy"));
        spinnerInicio.addChangeListener(e -> refrescar());

        JButton refrescar = new JButton("Refrescar");
        refrescar.addActionListener(e -> refrescar());
        JButton agregarHabitacion = new JButton("Agregar habitación");
        agregarHabitacion.addActionListener(e -> agregarHabitacion());

        barra.add(anterior);
        barra.add(new JLabel("Inicio:"));
        barra.add(spinnerInicio);
        barra.add(siguiente);
        barra.add(refrescar);
        barra.add(agregarHabitacion);
        barra.add(new JLabel("(arrastrá noches libres para reservar; arrastrá una reserva para moverla)"));
        return barra;
    }

    private void mover(int dias) {
        spinnerInicio.setValue(aFecha(inicioActual().plusDays(dias)));
    }

    // ------------------------------------------------------- estado del raton

    private int filaEn(MouseEvent e) {
        int fila = tabla.rowAtPoint(e.getPoint());
        return (fila < 0 || fila >= tabla.getRowCount()) ? -1 : fila;
    }

    private void iniciarInteraccion(MouseEvent e) {
        int fila = filaEn(e);
        int columna = tabla.columnAtPoint(e.getPoint());
        if (fila < 0 || columna <= 0 || !(tabla.getModel() instanceof CalendarioOcupacionModel modelo)) {
            limpiarSeleccion();
            return;
        }
        if (modelo.estaOcupada(fila, columna)) {
            iniciarMovimiento(modelo, fila, columna);
        } else {
            estado.iniciarRango(fila, columna);
            tabla.repaint();
        }
    }

    private void iniciarMovimiento(CalendarioOcupacionModel modelo, int fila, int columna) {
        OcupacionHabitacion.BloqueNoche bloque = modelo.bloqueEn(fila, columna);
        Long reservaId = modelo.reservaEn(fila, columna);
        LocalDate fecha = modelo.fechaDeColumna(columna);
        Habitacion habitacion = modelo.habitacionDeFila(fila);
        if (bloque == null || reservaId == null || fecha == null
                || habitacion == null || habitacion.getId() == null) {
            limpiarSeleccion();
            return;
        }

        reservaOrigenId = reservaId;
        habitacionOrigenId = habitacion.getId();
        nocheOrigen = fecha;
        motivoPorHabitacion.clear();
        try {
            for (ResultadoCompatibilidad rc
                    : controlador.compatibilidadMovimiento(reservaId, habitacion.getId(), fecha)) {
                motivoPorHabitacion.put(rc.habitacionId(), rc.motivo());
            }
        } catch (RuntimeException ex) {
            limpiarSeleccion();
            mostrarError(ex.getMessage());
            return;
        }

        estado.iniciarMovimiento(fila, columna, new GeometriaBarra.Bloque(
                bloque.fechaInicio(), bloque.fechaFin(), bloque.lateCheckout()));
        tabla.repaint();
    }

    private void extenderRango(MouseEvent e) {
        if (filaEn(e) != estado.filaOrigen()) {
            return; // solo se extiende dentro de la misma habitacion
        }
        int noches = tabla.getColumnCount() - 1;
        int columna = Math.max(1, Math.min(noches, tabla.columnAtPoint(e.getPoint())));
        if (columna != estado.colFin()) {
            estado.extenderRango(columna);
            tabla.repaint();
        }
    }

    private void finalizarRango(MouseEvent e) {
        int fila = estado.filaOrigen();
        int cIni = estado.colIni();
        int cFin = estado.colFin();
        limpiarSeleccion();

        if (fila < 0 || !(tabla.getModel() instanceof CalendarioOcupacionModel modelo)) {
            return;
        }
        SeleccionCalendario.Rango rango = SeleccionCalendario.rango(modelo, cIni, cFin);
        if (rango == null) {
            return;
        }
        if (!SeleccionCalendario.esRangoLibre(modelo, fila, cIni, cFin)) {
            JOptionPane.showMessageDialog(this,
                    "El rango cruza una noche ocupada. Elegí noches libres.",
                    "Selección no válida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Habitacion habitacion = modelo.habitacionDeFila(fila);
        if (habitacion == null) {
            return;
        }
        DialogoReserva dialogo = DialogoReserva.paraAlta(ventana(), habitacion, rango.checkIn(), rango.checkOut());
        if (dialogo.esAceptado()) {
            refrescar();
        }
    }

    private void finalizarMovimiento(MouseEvent e) {
        int filaOrigen = estado.filaOrigen();
        int filaDestino = filaEn(e);
        Long reservaId = reservaOrigenId;
        Long origen = habitacionOrigenId;
        LocalDate noche = nocheOrigen;
        limpiarSeleccion();

        if (reservaId == null || origen == null || noche == null) {
            return;
        }
        // Drop fuera de filas validas o sobre el propio origen: cancelar en silencio.
        if (filaDestino < 0 || filaDestino == filaOrigen) {
            return;
        }
        if (!(tabla.getModel() instanceof CalendarioOcupacionModel modelo)) {
            return;
        }
        Habitacion destino = modelo.habitacionDeFila(filaDestino);
        if (destino == null || destino.getId() == null) {
            return;
        }

        ResultadoMovimiento resultado =
                controlador.moverReserva(reservaId, origen, noche, destino.getId());
        if (resultado.movida()) {
            refrescar();
        } else {
            JOptionPane.showMessageDialog(this,
                    new Object[]{MensajesMovimiento.RECHAZO_MOVIMIENTO,
                            MensajesMovimiento.detalle(resultado.motivo())},
                    "No se pudo mover", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** True cuando la habitacion de la fila no tiene motivo de rechazo en el movimiento actual. */
    private boolean esCompatibleFila(int fila) {
        if (!(tabla.getModel() instanceof CalendarioOcupacionModel modelo)) {
            return true;
        }
        Habitacion habitacion = modelo.habitacionDeFila(fila);
        if (habitacion == null || habitacion.getId() == null) {
            return true;
        }
        return motivoPorHabitacion.get(habitacion.getId()) == null;
    }

    private void limpiarSeleccion() {
        estado.limpiar();
        motivoPorHabitacion.clear();
        reservaOrigenId = null;
        habitacionOrigenId = null;
        nocheOrigen = null;
        tabla.repaint();
    }

    // ---------------------------------------------------------- menu contextual

    private void mostrarMenu(MouseEvent e) {
        int fila = tabla.rowAtPoint(e.getPoint());
        int columna = tabla.columnAtPoint(e.getPoint());
        if (fila < 0 || fila >= tabla.getRowCount()
                || !(tabla.getModel() instanceof CalendarioOcupacionModel modelo)) {
            return;
        }
        SeleccionCalendario.Tipo tipo = SeleccionCalendario.accionDe(modelo, fila, columna);
        if (tipo == SeleccionCalendario.Tipo.NADA) {
            return;
        }

        JPopupMenu menu = new JPopupMenu();
        if (tipo == SeleccionCalendario.Tipo.EDITAR) {
            menu.add(menuItem("Editar reserva", () -> editarReserva(modelo, fila, columna)));
            menu.add(menuItem("Eliminar reserva", () -> eliminarReserva(modelo, fila, columna)));
        } else {
            menu.add(menuItem("Nueva reserva aquí", () -> nuevaReservaAqui(modelo, fila, columna)));
        }
        menu.show(tabla, e.getX(), e.getY());
    }

    private void editarReserva(CalendarioOcupacionModel modelo, int fila, int columna) {
        Long reservaId = modelo.reservaEn(fila, columna);
        LocalDate noche = modelo.fechaDeColumna(columna);
        Habitacion habitacion = modelo.habitacionDeFila(fila);
        if (reservaId == null || noche == null || habitacion == null || habitacion.getId() == null) {
            return;
        }
        try {
            ControladorReservas.EdicionReserva edicion =
                    controlador.edicionDe(reservaId, habitacion.getId(), noche);
            DialogoReserva dialogo = DialogoReserva.paraEdicion(ventana(), edicion);
            if (dialogo.esAceptado()) {
                refrescar();
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void eliminarReserva(CalendarioOcupacionModel modelo, int fila, int columna) {
        Long reservaId = modelo.reservaEn(fila, columna);
        if (reservaId == null) {
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this, "¿Eliminar la reserva #" + reservaId + "?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            boolean eliminada = controlador.eliminarReserva(reservaId);
            if (!eliminada) {
                JOptionPane.showMessageDialog(this,
                        "No se puede eliminar: la reserva tiene pagos asociados.",
                        "No se pudo eliminar", JOptionPane.WARNING_MESSAGE);
            }
        } catch (RuntimeException ex) {
            mostrarError(ex.getMessage());
        }
        refrescar();
    }

    private void nuevaReservaAqui(CalendarioOcupacionModel modelo, int fila, int columna) {
        Habitacion habitacion = modelo.habitacionDeFila(fila);
        LocalDate noche = modelo.fechaDeColumna(columna);
        if (habitacion == null || noche == null) {
            return;
        }
        DialogoReserva dialogo = DialogoReserva.paraAlta(ventana(), habitacion, noche);
        if (dialogo.esAceptado()) {
            refrescar();
        }
    }

    // ------------------------------------------------------- gestion de habitacion

    private void agregarHabitacion() {
        DialogoHabitacion dialogo = DialogoHabitacion.paraAlta(ventana());
        if (!dialogo.esAceptado()) {
            return;
        }
        try {
            ControladorHabitaciones instance = ControladorHabitaciones.getInstance();
            instance.altaHabitacion(dialogo.getNumero(), dialogo.getAtributos(),
                    dialogo.getMatrimoniales(), dialogo.getIndividuales(),
                    dialogo.getCunas(), dialogo.getColchones(), dialogo.getTarifa());
            if (!dialogo.getLimpia()) {
                instance.getHabitaciones().stream()
                        .filter(h -> h.getNumero() == dialogo.getNumero())
                        .findFirst()
                        .ifPresent(h -> instance.marcarLimpia(h.getId(), false));
            }
        } catch (IllegalStateException | IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        }
        refrescar();
    }

    /** Reconstruye la ventana [inicio, inicio+NOCHES) y reemplaza el modelo. */
    public void refrescar() {
        LocalDate desde = inicioActual();
        LocalDate hasta = desde.plusDays(NOCHES);
        List<OcupacionHabitacion> ocupacion = controlador.matrizOcupacion(desde, hasta);
        tabla.setModel(new CalendarioOcupacionModel(desde, NOCHES, ocupacion));
        tabla.setDefaultRenderer(Object.class, new RendererOcupacion(estado, this::esCompatibleFila));
        aplicarAnchoColumnas();
        limpiarSeleccion();
    }

    /** Columnas de ancho fijo para que ambos scrollbars sean utiles. */
    private void aplicarAnchoColumnas() {
        if (tabla.getColumnModel().getColumnCount() == 0) {
            return;
        }
        tabla.getColumnModel().getColumn(0).setPreferredWidth(ANCHO_HABITACION);
        for (int columna = 1; columna < tabla.getColumnModel().getColumnCount(); columna++) {
            tabla.getColumnModel().getColumn(columna).setPreferredWidth(ANCHO_NOCHE);
        }
    }

    private LocalDate inicioActual() {
        return ((Date) spinnerInicio.getValue()).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static JMenuItem menuItem(String texto, Runnable accion) {
        JMenuItem item = new JMenuItem(texto);
        item.addActionListener(e -> accion.run());
        return item;
    }

    private java.awt.Window ventana() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static Date aFecha(LocalDate fecha) {
        return Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
