package com.pms.ui;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.entities.Habitacion;
import com.pms.entities.Reserva;
import com.pms.entities.TitularReserva;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.EstadoReserva;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Dialogo modal de alta de una reserva manual para una habitacion explicita.
 * El titular se elige de un combo (titulares existentes) o se crea en el
 * momento ("nuevo titular") via {@code altaTitular}. Al guardar delega en
 * {@code ControladorReservas.crearReservaPara(...)}; los errores de negocio
 * (fechas invalidas, habitacion ocupada, etc.) se muestran en un
 * {@link JOptionPane} de error y el dialogo permanece abierto.
 *
 * <p>Expone {@link #prellenar(Habitacion, LocalDate)} para que el calendario
 * (Slice 4) podea sembrar la habitacion y la noche de check-in.
 */
public final class DialogoReserva extends JDialog {

    private static final String NUEVO_TITULAR = "<nuevo titular>";
    private static final int MAX_CANTIDAD = 50;

    private final ControladorReservas controladorReservas = ControladorReservas.getInstance();

    private final JComboBox<Object> comboTitular = new JComboBox<>();
    private final JTextField campoNombres = new JTextField(16);
    private final JTextField campoApellidos = new JTextField(16);
    private final JTextField campoCelular = new JTextField(16);
    private final JTextField campoTelefono = new JTextField(16);
    private final JTextField campoEmail = new JTextField(16);

    private final JComboBox<Habitacion> comboHabitacion = new JComboBox<>();
    private final JComboBox<EstadoReserva> comboEstado = new JComboBox<>();
    private final JSpinner spinnerCheckIn = spinnerFecha(LocalDate.now());
    private final JSpinner spinnerCheckOut = spinnerFecha(LocalDate.now().plusDays(1));
    private final JSpinner spinnerMatrimoniales = spinnerCantidad();
    private final JSpinner spinnerIndividuales = spinnerCantidad();
    private final JSpinner spinnerCunas = spinnerCantidad();
    private final JCheckBox checkLateCheckout = new JCheckBox();

    /** Atributos solicitados: checkboxes reconstruidos al elegir la habitacion. */
    private final JPanel panelAtributos = new JPanel(new GridLayout(0, 2, 8, 4));
    private final Map<AtributoHabitacion, JCheckBox> checksAtributos = new LinkedHashMap<>();
    private final JPanel contenedorAtributos = new JPanel(new BorderLayout());

    /** Contexto de edicion; null en modo alta. */
    private ControladorReservas.EdicionReserva edicion;

    /** Modo alta automatica: la habitacion la elige el controlador (D5). */
    private boolean automatica;

    private boolean aceptado;
    private Reserva reservaCreada;

    private DialogoReserva(Window padre) {
        super(padre, "Nueva reserva", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        cargarCombos();
        add(construirFormulario(), BorderLayout.CENTER);
        add(construirBotones(), BorderLayout.SOUTH);
        actualizarCamposNuevoTitular();
        pack();
        setLocationRelativeTo(padre);
        aplicarHabitacion((Habitacion) comboHabitacion.getSelectedItem());
    }

    /** Abre el dialogo en modo alta. */
    public static DialogoReserva paraAlta(Window padre) {
        return mostrar(new DialogoReserva(padre));
    }

    /** Abre el dialogo prellenado con una habitacion y noche de check-in (Slice 4). */
    public static DialogoReserva paraAlta(Window padre, Habitacion habitacion, LocalDate checkIn) {
        DialogoReserva dialogo = new DialogoReserva(padre);
        dialogo.prellenar(habitacion, checkIn);
        return mostrar(dialogo);
    }

    /** Abre el dialogo en modo alta prellenado con habitacion y rango completo (Slice 4). */
    public static DialogoReserva paraAlta(Window padre, Habitacion habitacion,
                                          LocalDate checkIn, LocalDate checkOut) {
        DialogoReserva dialogo = new DialogoReserva(padre);
        dialogo.prellenar(habitacion, checkIn, checkOut);
        return mostrar(dialogo);
    }

    /**
     * Abre el dialogo en modo alta AUTOMATICA sin contexto de calendario: el
     * usuario carga titular, fechas, camas y cunas, y la habitacion la resuelve
     * el controlador (D5). El combo de habitacion queda deshabilitado porque no
     * aplica; el resto de los campos permanece editable.
     */
    public static DialogoReserva paraAltaAutomatica(Window padre) {
        DialogoReserva dialogo = new DialogoReserva(padre);
        dialogo.automatica = true;
        dialogo.setTitle("Colocar reserva automática");
        dialogo.comboHabitacion.setEnabled(false);
        dialogo.limpiarHabitacion();
        dialogo.prellenar(null, LocalDate.now(), LocalDate.now().plusDays(1));
        return mostrar(dialogo);
    }

    /** Abre el dialogo en modo edicion, prellenado con la propuesta clickeada. */
    public static DialogoReserva paraEdicion(Window padre, ControladorReservas.EdicionReserva edicion) {
        if (edicion == null) {
            throw new IllegalArgumentException("edicion nula");
        }
        DialogoReserva dialogo = new DialogoReserva(padre);
        dialogo.edicion = edicion;
        dialogo.setTitle("Editar reserva");
        dialogo.prellenarEdicion(edicion);
        return mostrar(dialogo);
    }

    private static DialogoReserva mostrar(DialogoReserva dialogo) {
        dialogo.setVisible(true);
        return dialogo;
    }

    /** True cuando el usuario guardo la reserva con exito. */
    public boolean esAceptado() {
        return aceptado;
    }

    /** Reserva creada; solo tiene sentido cuando {@link #esAceptado()} es true. */
    public Reserva getReservaCreada() {
        return reservaCreada;
    }

    /** Sembra la habitacion y la noche de check-in (check-out = check-in + 1). */
    public void prellenar(Habitacion habitacion, LocalDate checkIn) {
        prellenar(habitacion, checkIn, checkIn == null ? null : checkIn.plusDays(1));
    }

    /** Sembra la habitacion y el rango completo [checkIn, checkOut). */
    public void prellenar(Habitacion habitacion, LocalDate checkIn, LocalDate checkOut) {
        if (habitacion != null) {
            seleccionarHabitacion(habitacion);
        }
        if (checkIn != null) {
            spinnerCheckIn.setValue(aFecha(checkIn));
        }
        if (checkOut != null) {
            spinnerCheckOut.setValue(aFecha(checkOut));
        }
    }

    /** Prellena todos los campos con la propuesta a editar. */
    private void prellenarEdicion(ControladorReservas.EdicionReserva ed) {
        seleccionarTitular(ed.titular());
        seleccionarHabitacionPorId(ed.habitacionId());
        spinnerCheckIn.setValue(aFecha(ed.checkIn()));
        spinnerCheckOut.setValue(aFecha(ed.checkOut()));
        spinnerMatrimoniales.setValue(ed.camasMatrimoniales());
        spinnerIndividuales.setValue(ed.camasIndividuales());
        spinnerCunas.setValue(ed.cunas());
        comboEstado.setSelectedItem(ed.estado());
        checkLateCheckout.setSelected(ed.lateCheckout());
        marcarAtributos(ed.atributos());
        actualizarCamposNuevoTitular();
    }

    // --------------------------------------------------------------- internals

    private void cargarCombos() {
        List<TitularReserva> titulares = ControladorPersonas.getInstance().getTitulares();
        for (TitularReserva t : titulares) {
            comboTitular.addItem(t);
        }
        comboTitular.addItem(NUEVO_TITULAR);
        if (titulares.isEmpty()) {
            comboTitular.setSelectedItem(NUEVO_TITULAR);
        }
        comboTitular.addActionListener(e -> actualizarCamposNuevoTitular());
        comboTitular.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TitularReserva t) {
                    setText(nombreTitular(t));
                }
                return this;
            }
        });

        for (Habitacion h : ControladorHabitaciones.getInstance().getHabitaciones()) {
            comboHabitacion.addItem(h);
        }
        comboHabitacion.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Habitacion h) {
                    setText("N° " + h.getNumero() + "  ($" + h.getTarifaPorNoche() + "/noche)");
                }
                return this;
            }
        });
        comboHabitacion.addActionListener(e -> {
            if (!automatica) {
                aplicarHabitacion((Habitacion) comboHabitacion.getSelectedItem());
            }
        });

        for (EstadoReserva e : EstadoReserva.values()) {
            comboEstado.addItem(e);
        }
    }

    private void actualizarCamposNuevoTitular() {
        boolean nuevo = !(comboTitular.getSelectedItem() instanceof TitularReserva);
        for (JTextField campo : new JTextField[]{campoNombres, campoApellidos,
                campoCelular, campoTelefono, campoEmail}) {
            campo.setEnabled(nuevo);
        }
    }

    /** Aplica los topes/defaults de camas y la lista de atributos de la habitacion. */
    private void aplicarHabitacion(Habitacion h) {
        if (h == null) {
            return;
        }
        ajustarCantidad(spinnerMatrimoniales, h.getCantidadMatrimoniales());
        ajustarCantidad(spinnerIndividuales, h.getCantidadIndividuales());
        ajustarCantidad(spinnerCunas, h.getCantidadCunas());
        reconstruirAtributos(h.getAtributos());
    }

    /** Resetea camas y atributos cuando no hay habitacion explicita (alta automatica). */
    private void limpiarHabitacion() {
        reiniciarCantidad(spinnerMatrimoniales);
        reiniciarCantidad(spinnerIndividuales);
        reiniciarCantidad(spinnerCunas);
        reconstruirAtributos(Set.of());
    }

    /** Restaura tope MAX_CANTIDAD y valor 0: sin habitacion no hay tope real. */
    private static void reiniciarCantidad(JSpinner spinner) {
        SpinnerNumberModel modelo = (SpinnerNumberModel) spinner.getModel();
        modelo.setMaximum(MAX_CANTIDAD);
        modelo.setValue(0);
    }

    /** Ajusta tope y valor del spinner: tope = max(0, n), valor = tope. */
    private static void ajustarCantidad(JSpinner spinner, int maximo) {
        SpinnerNumberModel modelo = (SpinnerNumberModel) spinner.getModel();
        modelo.setMaximum(Math.max(0, maximo));
        modelo.setValue(modelo.getMaximum());
    }

    /** Reconstruye los checkboxes de atributos (todos destildados por defecto). */
    private void reconstruirAtributos(Set<AtributoHabitacion> atributos) {
        checksAtributos.clear();
        panelAtributos.removeAll();
        for (AtributoHabitacion a : new TreeSet<>(atributos == null ? Set.of() : atributos)) {
            JCheckBox check = new JCheckBox(DialogoHabitacion.etiquetaAtributo(a));
            checksAtributos.put(a, check);
            panelAtributos.add(check);
        }
        panelAtributos.revalidate();
        panelAtributos.repaint();
        contenedorAtributos.setVisible(!checksAtributos.isEmpty());
    }

    /** Tilda los checkboxes de los atributos indicados que existan en la lista. */
    private void marcarAtributos(Set<AtributoHabitacion> atributos) {
        if (atributos == null) {
            return;
        }
        for (AtributoHabitacion a : atributos) {
            JCheckBox check = checksAtributos.get(a);
            if (check != null) {
                check.setSelected(true);
            }
        }
    }

    /** Atributos tildados, ordenados por el orden natural del enum. */
    private Set<AtributoHabitacion> atributosSeleccionados() {
        Set<AtributoHabitacion> seleccionados = new TreeSet<>();
        for (Map.Entry<AtributoHabitacion, JCheckBox> entrada : checksAtributos.entrySet()) {
            if (entrada.getValue().isSelected()) {
                seleccionados.add(entrada.getKey());
            }
        }
        return seleccionados;
    }

    private JPanel construirFormulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        int fila = 0;
        agregarCampo(panel, fila++, "Titular:", comboTitular);
        agregarCampo(panel, fila++, "Nombres:", campoNombres);
        agregarCampo(panel, fila++, "Apellidos:", campoApellidos);
        agregarCampo(panel, fila++, "Celular:", campoCelular);
        agregarCampo(panel, fila++, "Teléfono fijo:", campoTelefono);
        agregarCampo(panel, fila++, "Email:", campoEmail);
        agregarCampo(panel, fila++, "Habitación:", comboHabitacion);
        agregarCampo(panel, fila++, "Check-in:", spinnerCheckIn);
        agregarCampo(panel, fila++, "Check-out:", spinnerCheckOut);
        agregarCampo(panel, fila++, "Camas matrimoniales:", spinnerMatrimoniales);
        agregarCampo(panel, fila++, "Camas individuales:", spinnerIndividuales);
        agregarCampo(panel, fila++, "Cunas:", spinnerCunas);
        agregarCampo(panel, fila++, "Estado:", comboEstado);
        agregarCampo(panel, fila++, "Late check-out:", checkLateCheckout);

        contenedorAtributos.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Atributos solicitados"));
        contenedorAtributos.add(panelAtributos, BorderLayout.CENTER);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(6, 6, 4, 6);
        panel.add(contenedorAtributos, gbc);
        return panel;
    }

    private JPanel construirBotones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton botonCancelar = new JButton("Cancelar");
        botonCancelar.addActionListener(e -> dispose());
        JButton botonAceptar = new JButton("Guardar");
        botonAceptar.addActionListener(e -> aceptar());
        panel.add(botonCancelar);
        panel.add(botonAceptar);
        getRootPane().setDefaultButton(botonAceptar);
        return panel;
    }

    private void aceptar() {
        try {
            TitularReserva titular = resolverTitular();
            int matrimoniales = cantidad(spinnerMatrimoniales);
            int individuales = cantidad(spinnerIndividuales);
            int cunas = cantidad(spinnerCunas);
            if (automatica) {
                reservaCreada = controladorReservas.crearReservaAutomatica(titular,
                        fecha(spinnerCheckIn), fecha(spinnerCheckOut),
                        matrimoniales, individuales, 0, cunas, atributosSeleccionados());
                aplicarLateCheckout(reservaCreada, false);
            } else {
                Habitacion habitacion = (Habitacion) comboHabitacion.getSelectedItem();
                if (habitacion == null) {
                    mostrarError("Seleccioná una habitación.");
                    return;
                }
                if (edicion != null) {
                    reservaCreada = controladorReservas.actualizarReserva(edicion.reservaId(),
                            edicion.habitacionId(), edicion.nocheObjetivo(), titular, habitacion.getId(),
                            fecha(spinnerCheckIn), fecha(spinnerCheckOut),
                            matrimoniales, individuales, cunas,
                            (EstadoReserva) comboEstado.getSelectedItem(), atributosSeleccionados());
                    aplicarLateCheckout(reservaCreada, true);
                } else {
                    reservaCreada = controladorReservas.crearReservaPara(titular, habitacion.getId(),
                            fecha(spinnerCheckIn), fecha(spinnerCheckOut),
                            matrimoniales, individuales, cunas, atributosSeleccionados());
                    aplicarLateCheckout(reservaCreada, false);
                }
            }
            aceptado = true;
            dispose();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
        }
    }

    /**
     * Aplica el flag de late check-out a la reserva recien creada o editada.
     * En edicion se persiste siempre el valor del checkbox (permite destildar);
     * en alta solo cuando esta marcado (el default ya es false).
     */
    private void aplicarLateCheckout(Reserva reserva, boolean siempre) {
        if (reserva == null || reserva.getId() == null) {
            return;
        }
        boolean deseado = checkLateCheckout.isSelected();
        if (siempre || deseado) {
            controladorReservas.marcarLateCheckout(reserva.getId(), deseado);
        }
    }

    private TitularReserva resolverTitular() {
        if (comboTitular.getSelectedItem() instanceof TitularReserva t) {
            return t;
        }
        List<String> nombres = separar(campoNombres.getText());
        List<String> apellidos = separar(campoApellidos.getText());
        if (nombres.isEmpty()) {
            throw new IllegalArgumentException("El nombre del titular es obligatorio.");
        }
        if (apellidos.isEmpty()) {
            throw new IllegalArgumentException("El apellido del titular es obligatorio.");
        }
        return ControladorPersonas.getInstance().altaTitular(nombres, apellidos,
                textoONull(campoCelular.getText()), textoONull(campoTelefono.getText()),
                textoONull(campoEmail.getText()));
    }

    private void seleccionarHabitacion(Habitacion habitacion) {
        for (int i = 0; i < comboHabitacion.getItemCount(); i++) {
            Habitacion candidata = comboHabitacion.getItemAt(i);
            if (candidata.getNumero().equals(habitacion.getNumero())) {
                comboHabitacion.setSelectedIndex(i);
                return;
            }
        }
    }

    /** Selecciona la habitacion del combo por id (usado por el prefill de edicion). */
    private void seleccionarHabitacionPorId(long habitacionId) {
        for (int i = 0; i < comboHabitacion.getItemCount(); i++) {
            Habitacion candidata = comboHabitacion.getItemAt(i);
            if (candidata.getId() != null && candidata.getId() == habitacionId) {
                comboHabitacion.setSelectedIndex(i);
                return;
            }
        }
    }

    /** Selecciona el titular del combo cuyo nombre coincide (prefill de edicion). */
    private void seleccionarTitular(String nombre) {
        if (nombre == null) {
            return;
        }
        for (int i = 0; i < comboTitular.getItemCount(); i++) {
            if (comboTitular.getItemAt(i) instanceof TitularReserva t
                    && nombreTitular(t).equals(nombre)) {
                comboTitular.setSelectedIndex(i);
                return;
            }
        }
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "No se pudo guardar", JOptionPane.ERROR_MESSAGE);
    }

    private static String nombreTitular(TitularReserva t) {
        return (String.join(" ", t.getNombres()) + " " + String.join(" ", t.getApellidos())).trim();
    }

    private static List<String> separar(String texto) {
        if (texto == null) {
            return List.of();
        }
        return Arrays.stream(texto.split("[,\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static String textoONull(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private static JSpinner spinnerCantidad() {
        return new JSpinner(new SpinnerNumberModel(0, 0, MAX_CANTIDAD, 1));
    }

    private static JSpinner spinnerFecha(LocalDate inicial) {
        JSpinner spinner = new JSpinner(new SpinnerDateModel(aFecha(inicial), null, null, java.util.Calendar.DAY_OF_MONTH));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy"));
        return spinner;
    }

    private static Date aFecha(LocalDate fecha) {
        return Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate fecha(JSpinner spinner) {
        Date valor = (Date) spinner.getValue();
        return valor.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static int cantidad(JSpinner spinner) {
        return (Integer) spinner.getValue();
    }

    private static void agregarCampo(JPanel panel, int fila, String etiqueta, Component campo) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.gridx = 0;
        gbc.gridy = fila;
        panel.add(new JLabel(etiqueta), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(campo, gbc);
    }
}
