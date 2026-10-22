package com.pms.ui;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.entities.Habitacion;
import com.pms.entities.Reserva;
import com.pms.entities.TitularReserva;
import com.pms.handlers.AsignacionCamas;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
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
 * {@code ControladorReservas.crearReservaParaPersonas(...)}; los errores de negocio
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
    private final JSpinner spinnerParejas = spinnerCantidad();
    private final JSpinner spinnerPersonasIndividuales = spinnerCantidad();
    private final JSpinner spinnerBebes = spinnerBebes();
    private final JLabel avisoParejas = avisoInline();
    private final JLabel avisoPersonasIndividuales = avisoInline();
    private final JLabel avisoBebes = avisoInline();
    private final JLabel avisoCapacidad = avisoInline();
    private final JCheckBox checkLateCheckout = new JCheckBox();

    /** Evita recursión al ajustar los topes de los spinners. */
    private boolean actualizandoLimites;

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
        for (JSpinner spinner : new JSpinner[]{spinnerParejas,
                spinnerPersonasIndividuales, spinnerBebes}) {
            spinner.addChangeListener(e -> {
                if (actualizandoLimites) {
                    return;
                }
                actualizarLimitesYavisos();
                if (isShowing()) {
                    pack();
                }
            });
        }
        actualizarCamposNuevoTitular();
        aplicarHabitacion((Habitacion) comboHabitacion.getSelectedItem());
        pack();
        setLocationRelativeTo(padre);
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
        actualizarLimitesYavisos();
        pack();
        setLocationRelativeTo(getOwner());
    }

    /** Prellena todos los campos con la propuesta a editar. */
    private void prellenarEdicion(ControladorReservas.EdicionReserva ed) {
        seleccionarTitular(ed.titular());
        seleccionarHabitacionPorId(ed.habitacionId());
        spinnerCheckIn.setValue(aFecha(ed.checkIn()));
        spinnerCheckOut.setValue(aFecha(ed.checkOut()));
        reiniciarCantidad(spinnerParejas, MAX_CANTIDAD);
        reiniciarCantidad(spinnerPersonasIndividuales, MAX_CANTIDAD);
        reiniciarCantidad(spinnerBebes, Integer.MAX_VALUE);
        spinnerParejas.setValue(ed.cantMatrimonios());
        spinnerPersonasIndividuales.setValue(ed.cantPersonasIndividuales());
        spinnerBebes.setValue(ed.cantBebes());
        comboEstado.setSelectedItem(ed.estado());
        checkLateCheckout.setSelected(ed.lateCheckout());
        marcarAtributos(ed.atributos());
        actualizarCamposNuevoTitular();
        actualizarLimitesYavisos();
        pack();
        setLocationRelativeTo(getOwner());
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

    /**
     * Aplica los defaults de personas de la habitacion (una pareja por cama
     * matrimonial, una persona por cama individual, un bebe por cuna) y
     * reconstruye sus atributos.
     */
    private void aplicarHabitacion(Habitacion h) {
        if (h == null) {
            return;
        }
        reiniciarCantidad(spinnerParejas, MAX_CANTIDAD);
        reiniciarCantidad(spinnerPersonasIndividuales, MAX_CANTIDAD);
        reiniciarCantidad(spinnerBebes, Integer.MAX_VALUE);
        spinnerParejas.setValue(h.getCantidadMatrimoniales());
        spinnerPersonasIndividuales.setValue(h.getCantidadIndividuales());
        spinnerBebes.setValue(h.getCantidadCunas());
        reconstruirAtributos(h.getAtributos());
        actualizarLimitesYavisos();
    }

    /** Resetea personas/atributos cuando no hay habitacion explicita (alta automatica). */
    private void limpiarHabitacion() {
        reiniciarCantidad(spinnerParejas, MAX_CANTIDAD);
        reiniciarCantidad(spinnerPersonasIndividuales, MAX_CANTIDAD);
        reiniciarCantidad(spinnerBebes, Integer.MAX_VALUE);
        reconstruirAtributos(Set.of());
        limpiarAvisos();
    }

    /** Restaura el tope indicado y valor 0: sin habitacion no hay tope real. */
    private static void reiniciarCantidad(JSpinner spinner, int maximo) {
        SpinnerNumberModel modelo = (SpinnerNumberModel) spinner.getModel();
        modelo.setMaximum(maximo);
        modelo.setValue(0);
    }

    /**
     * Mantiene los topes de parejas/individuales dentro de la capacidad fisica
     * de la habitacion (camas + colchones) y actualiza los avisos inline. Los
     * bebes NO se topean: solo avisan cuando faltan cunas.
     */
    private void actualizarLimitesYavisos() {
        if (actualizandoLimites) {
            return;
        }
        actualizandoLimites = true;
        try {
            Habitacion h = habitacionSeleccionada();
            if (h == null) {
                limpiarAvisos();
                return;
            }
            int capacidadPersonas = 2 * h.getCantidadMatrimoniales()
                    + h.getCantidadIndividuales() + h.getColchones();

            SpinnerNumberModel modeloParejas = (SpinnerNumberModel) spinnerParejas.getModel();
            SpinnerNumberModel modeloIndividuales =
                    (SpinnerNumberModel) spinnerPersonasIndividuales.getModel();

            int individuales = cantidad(spinnerPersonasIndividuales);
            int maxParejas = Math.max(0, (capacidadPersonas - individuales) / 2);
            modeloParejas.setMaximum(Math.min(MAX_CANTIDAD, maxParejas));

            int parejas = cantidad(spinnerParejas);
            int maxIndividuales = Math.max(0, capacidadPersonas - 2 * parejas);
            modeloIndividuales.setMaximum(Math.min(MAX_CANTIDAD, maxIndividuales));

            SpinnerNumberModel modeloBebes = (SpinnerNumberModel) spinnerBebes.getModel();
            modeloBebes.setMaximum(Integer.MAX_VALUE);

            AsignacionCamas.Asignacion asignacion = AsignacionCamas.calcular(h,
                    cantidad(spinnerParejas), cantidad(spinnerPersonasIndividuales),
                    cantidad(spinnerBebes), true);
            avisoParejas.setText(htmlAviso(asignacion.avisoMatrimonios()));
            avisoPersonasIndividuales.setText(htmlAviso(asignacion.avisoPersonasIndividuales()));
            avisoBebes.setText(htmlAviso(asignacion.avisoBebes()));
            avisoCapacidad.setText(htmlAviso(asignacion.avisoCapacidad()));
        } finally {
            actualizandoLimites = false;
        }
    }

    private Habitacion habitacionSeleccionada() {
        if (automatica) {
            return null; // la habitacion la resuelve el controlador
        }
        return comboHabitacion.getSelectedItem() instanceof Habitacion h ? h : null;
    }

    private static JLabel avisoInline() {
        JLabel aviso = new JLabel();
        aviso.setFont(aviso.getFont().deriveFont(Font.PLAIN, 10f));
        aviso.setForeground(new Color(0xB0, 0x5A, 0x00));
        return aviso;
    }

    private static String htmlAviso(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        return "<html><body style='width:270px'>" + texto + "</body></html>";
    }

    private void limpiarAvisos() {
        avisoParejas.setText("");
        avisoPersonasIndividuales.setText("");
        avisoBebes.setText("");
        avisoCapacidad.setText("");
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
        agregarCampo(panel, fila++, "Parejas / matrimonios:", spinnerParejas);
        fila = agregarAviso(panel, fila, avisoParejas);
        agregarCampo(panel, fila++, "Personas individuales:", spinnerPersonasIndividuales);
        fila = agregarAviso(panel, fila, avisoPersonasIndividuales);
        agregarCampo(panel, fila++, "Bebés:", spinnerBebes);
        fila = agregarAviso(panel, fila, avisoBebes);
        fila = agregarAviso(panel, fila, avisoCapacidad);
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
            int parejas = cantidad(spinnerParejas);
            int personasIndividuales = cantidad(spinnerPersonasIndividuales);
            int bebes = cantidad(spinnerBebes);
            if (automatica) {
                reservaCreada = controladorReservas.crearReservaAutomaticaPersonas(titular,
                        fecha(spinnerCheckIn), fecha(spinnerCheckOut),
                        parejas, personasIndividuales, bebes, atributosSeleccionados());
                aplicarLateCheckout(reservaCreada, false);
            } else {
                Habitacion habitacion = (Habitacion) comboHabitacion.getSelectedItem();
                if (habitacion == null) {
                    mostrarError("Seleccioná una habitación.");
                    return;
                }
                AsignacionCamas.Asignacion asignacion = AsignacionCamas.calcular(habitacion,
                        parejas, personasIndividuales, bebes, true);
                if (!asignacion.capacidadSuficiente()) {
                    avisoCapacidad.setText(htmlAviso(asignacion.avisoCapacidad()));
                    pack();
                    return; // aviso inline, sin popup
                }
                if (edicion != null) {
                    reservaCreada = controladorReservas.actualizarReservaPersonas(
                            edicion.reservaId(), edicion.habitacionId(), edicion.nocheObjetivo(),
                            titular, habitacion.getId(),
                            fecha(spinnerCheckIn), fecha(spinnerCheckOut),
                            parejas, personasIndividuales, bebes,
                            (EstadoReserva) comboEstado.getSelectedItem(), atributosSeleccionados());
                    aplicarLateCheckout(reservaCreada, true);
                } else {
                    reservaCreada = controladorReservas.crearReservaParaPersonas(
                            titular, habitacion.getId(),
                            fecha(spinnerCheckIn), fecha(spinnerCheckOut),
                            parejas, personasIndividuales, bebes, atributosSeleccionados());
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
     *
     * <p>Si el controlador rechaza el marcado (otra reserva hace check-in el
     * mismo dia en la habitacion), se avisa y se destilda el checkbox: la
     * reserva ya guardada NO se revierte, solo se corrige el flag.
     */
    private void aplicarLateCheckout(Reserva reserva, boolean siempre) {
        if (reserva == null || reserva.getId() == null) {
            return;
        }
        boolean deseado = checkLateCheckout.isSelected();
        if (siempre || deseado) {
            try {
                controladorReservas.marcarLateCheckout(reserva.getId(), deseado);
            } catch (IllegalStateException ex) {
                checkLateCheckout.setSelected(false);
                mostrarError(ex.getMessage());
            }
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
        DialogoError.mostrar(this, "No se pudo guardar", mensaje);
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

    private static JSpinner spinnerBebes() {
        return new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
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

    /** Agrega una fila de aviso inline debajo del campo correspondiente. */
    private static int agregarAviso(JPanel panel, int fila, JLabel aviso) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 6, 3, 6);
        panel.add(aviso, gbc);
        return fila + 1;
    }
}
