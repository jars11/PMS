package com.pms.ui;

import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.dtos.DTRHSolicitada;
import com.pms.entities.RHPropuesta;
import com.pms.entities.Reserva;
import com.pms.entities.TitularReserva;
import com.pms.handlers.ManejadorBusquedaHuecos;
import com.pms.utils.AtributoHabitacion;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

/**
 * Pestana "Propuesta de estadía" (rol RECEPCIONISTA). Permite cargar uno o mas
 * bloques solicitados (fechas, cantidades y atributos), validarlos de forma
 * pura, buscar alternativas con {@link ManejadorBusquedaHuecos} en un hilo de
 * fondo y persistir la propuesta elegida como una {@link Reserva} real a traves
 * de {@link ControladorReservas#persistirPropuestas}.
 *
 * <p>La busqueda y el guardado corren en {@link SwingWorker}: el EDT nunca toca
 * la base de datos. Las preguntas/notificaciones del algoritmo se enrutan al EDT
 * con {@link InteraccionSwing}.
 */
public final class PanelPropuestaEstadia extends JPanel {

    private static final String NUEVO_TITULAR = "<nuevo titular>";
    private static final int MAX_CANTIDAD = 50;

    private final ControladorReservas controladorReservas = ControladorReservas.getInstance();
    private final ControladorPersonas controladorPersonas = ControladorPersonas.getInstance();
    private final Runnable alCrearReserva;

    private final JComboBox<Object> comboTitular = new JComboBox<>();
    private final JTextField campoNombres = new JTextField(14);
    private final JTextField campoApellidos = new JTextField(14);
    private final JTextField campoCelular = new JTextField(14);
    private final JTextField campoTelefono = new JTextField(14);
    private final JTextField campoEmail = new JTextField(14);

    private final JPanel panelFilas = new JPanel();
    private final List<FilaBloque> filas = new ArrayList<>();

    private final DefaultListModel<Set<RHPropuesta>> modeloPropuestas = new DefaultListModel<>();
    private final JList<Set<RHPropuesta>> listaPropuestas = new JList<>(modeloPropuestas);

    private final JButton botonBuscar = new JButton("Buscar propuestas");
    private final JButton botonAceptar = new JButton("Aceptar y crear reserva");
    private final JLabel etiquetaEstado = new JLabel(" ");

    public PanelPropuestaEstadia() {
        this(null);
    }

    public PanelPropuestaEstadia(Runnable alCrearReserva) {
        this.alCrearReserva = alCrearReserva;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        panelFilas.setLayout(new GridLayout(0, 1, 6, 6));
        listaPropuestas.setVisibleRowCount(8);
        listaPropuestas.setCellRenderer(rendererPropuestas());

        add(construirPanelSuperior(), BorderLayout.NORTH);
        add(construirPanelCentral(), BorderLayout.CENTER);
        add(construirPanelAcciones(), BorderLayout.SOUTH);

        cargarTitulares();
        agregarFila();
    }

    // ------------------------------------------------------------ validacion

    /**
     * Validacion PURA de la costura de entrada (sin Swing): rechaza un conjunto
     * vacio/nulo; por bloque rechaza fechas nulas, check-out no posterior al
     * check-in y cualquier cantidad negativa. Lanza {@link IllegalArgumentException}.
     */
    public static void validarBloques(Set<DTRHSolicitada> bloques) {
        if (bloques == null || bloques.isEmpty()) {
            throw new IllegalArgumentException("Agregá al menos un bloque de estadía.");
        }
        for (DTRHSolicitada b : bloques) {
            if (b == null) {
                throw new IllegalArgumentException("Hay un bloque vacío.");
            }
            if (b.getFechaInicio() == null || b.getFechaFin() == null) {
                throw new IllegalArgumentException("Las fechas de check-in y check-out son obligatorias.");
            }
            if (!b.getFechaFin().isAfter(b.getFechaInicio())) {
                throw new IllegalArgumentException("El check-out debe ser posterior al check-in.");
            }
            if (b.getCantMatrimonios() < 0 || b.getCantIndividuales() < 0
                    || b.getCantBebes() < 0 || b.getCantCunas() < 0) {
                throw new IllegalArgumentException("Las cantidades no pueden ser negativas.");
            }
        }
    }

    /** Construye un {@link DTRHSolicitada} por cada fila cargada. */
    Set<DTRHSolicitada> construirBloques() {
        Set<DTRHSolicitada> bloques = new LinkedHashSet<>();
        for (FilaBloque fila : filas) {
            bloques.add(fila.aDTO());
        }
        return bloques;
    }

    // --------------------------------------------------------------- acciones

    private void buscar() {
        Set<DTRHSolicitada> bloques;
        try {
            bloques = construirBloques();
            validarBloques(bloques);
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
            return; // NO se corre la busqueda
        }

        botonBuscar.setEnabled(false);
        etiquetaEstado.setText("Buscando propuestas...");
        new SwingWorker<Set<Set<RHPropuesta>>, Void>() {
            @Override
            protected Set<Set<RHPropuesta>> doInBackground() {
                return new ManejadorBusquedaHuecos().buscarHuecos(bloques, new InteraccionSwing(ventana()));
            }

            @Override
            protected void done() {
                botonBuscar.setEnabled(true);
                try {
                    mostrarPropuestas(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    mostrarError(mensajeCausa(e));
                }
            }
        }.execute();
    }

    private void aceptar() {
        Set<RHPropuesta> elegido = listaPropuestas.getSelectedValue();
        if (elegido == null) {
            mostrarError("Seleccioná una propuesta de la lista.");
            return;
        }
        final TitularReserva titular;
        try {
            titular = resolverTitular();
        } catch (IllegalArgumentException ex) {
            mostrarError(ex.getMessage());
            return;
        }

        botonAceptar.setEnabled(false);
        etiquetaEstado.setText("Guardando reserva...");
        new SwingWorker<Reserva, Void>() {
            @Override
            protected Reserva doInBackground() {
                return controladorReservas.persistirPropuestas(titular, elegido);
            }

            @Override
            protected void done() {
                try {
                    Reserva r = get();
                    etiquetaEstado.setText("Reserva #" + r.getId() + " creada.");
                    new InteraccionSwing(ventana()).notificar("Reserva creada: " + resumen(elegido));
                    modeloPropuestas.clear();
                    if (alCrearReserva != null) {
                        alCrearReserva.run();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    botonAceptar.setEnabled(true);
                    mostrarError(mensajeCausa(e));
                }
            }
        }.execute();
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
        return controladorPersonas.altaTitular(nombres, apellidos,
                textoONull(campoCelular.getText()), textoONull(campoTelefono.getText()),
                textoONull(campoEmail.getText()));
    }

    // --------------------------------------------------------------- armado UI

    private JPanel construirPanelSuperior() {
        JPanel contenedor = new JPanel(new BorderLayout(8, 8));
        contenedor.add(construirPanelTitular(), BorderLayout.CENTER);

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton agregar = new JButton("Agregar bloque");
        agregar.addActionListener(e -> agregarFila());
        barra.add(agregar);
        barra.add(new JLabel("(cada bloque se busca en conjunto)"));
        contenedor.add(barra, BorderLayout.SOUTH);
        return contenedor;
    }

    private JPanel construirPanelTitular() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Titular"));
        int fila = 0;
        agregarCampo(panel, fila++, "Titular:", comboTitular);
        agregarCampo(panel, fila++, "Nombres (nuevo):", campoNombres);
        agregarCampo(panel, fila++, "Apellidos (nuevo):", campoApellidos);
        agregarCampo(panel, fila++, "Celular:", campoCelular);
        agregarCampo(panel, fila++, "Teléfono fijo:", campoTelefono);
        agregarCampo(panel, fila, "Email:", campoEmail);
        return panel;
    }

    private JSplitPane construirPanelCentral() {
        JScrollPane scrollBloques = new JScrollPane(panelFilas);
        scrollBloques.setBorder(BorderFactory.createTitledBorder("Bloques solicitados"));

        JScrollPane scrollPropuestas = new JScrollPane(listaPropuestas);
        scrollPropuestas.setBorder(BorderFactory.createTitledBorder("Propuestas encontradas"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollBloques, scrollPropuestas);
        split.setResizeWeight(0.55);
        return split;
    }

    private JPanel construirPanelAcciones() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botonBuscar.addActionListener(e -> buscar());
        botonAceptar.addActionListener(e -> aceptar());
        botonAceptar.setEnabled(false);
        botones.add(botonBuscar);
        botones.add(botonAceptar);
        panel.add(etiquetaEstado, BorderLayout.WEST);
        panel.add(botones, BorderLayout.EAST);
        return panel;
    }

    private void agregarFila() {
        final FilaBloque[] holder = new FilaBloque[1];
        FilaBloque fila = new FilaBloque(() -> quitarFila(holder[0]));
        holder[0] = fila;
        filas.add(fila);
        panelFilas.add(fila.panel);
        panelFilas.revalidate();
        panelFilas.repaint();
    }

    private void quitarFila(FilaBloque fila) {
        if (fila == null || filas.size() <= 1) {
            return; // siempre queda al menos un bloque
        }
        filas.remove(fila);
        panelFilas.remove(fila.panel);
        panelFilas.revalidate();
        panelFilas.repaint();
    }

    private void cargarTitulares() {
        for (TitularReserva t : controladorPersonas.getTitulares()) {
            comboTitular.addItem(t);
        }
        comboTitular.addItem(NUEVO_TITULAR);
        if (controladorPersonas.getTitulares().isEmpty()) {
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
        actualizarCamposNuevoTitular();
    }

    private void actualizarCamposNuevoTitular() {
        boolean nuevo = !(comboTitular.getSelectedItem() instanceof TitularReserva);
        for (JTextField campo : new JTextField[]{campoNombres, campoApellidos,
                campoCelular, campoTelefono, campoEmail}) {
            campo.setEnabled(nuevo);
        }
    }

    private void mostrarPropuestas(Set<Set<RHPropuesta>> resultado) {
        modeloPropuestas.clear();
        for (Set<RHPropuesta> conjunto : resultado) {
            modeloPropuestas.addElement(conjunto);
        }
        botonAceptar.setEnabled(!resultado.isEmpty());
        if (resultado.isEmpty()) {
            etiquetaEstado.setText("Sin propuestas.");
            new InteraccionSwing(ventana()).notificar("No se encontraron propuestas para los bloques pedidos.");
        } else {
            etiquetaEstado.setText(resultado.size() + " propuesta(s) encontrada(s).");
        }
    }

    private DefaultListCellRenderer rendererPropuestas() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Set<?> conjunto) {
                    @SuppressWarnings("unchecked")
                    Set<RHPropuesta> propuestas = (Set<RHPropuesta>) conjunto;
                    setText(resumen(propuestas));
                }
                return this;
            }
        };
    }

    private Window ventana() {
        return SwingUtilities.getWindowAncestor(this);
    }

    private void mostrarError(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "No se pudo completar", JOptionPane.ERROR_MESSAGE);
    }

    private static String mensajeCausa(Throwable e) {
        Throwable causa = e.getCause() == null ? e : e.getCause();
        return causa.getMessage() == null ? causa.toString() : causa.getMessage();
    }

    /** Resumen legible de una alternativa de habitaciones. */
    private static String resumen(Set<RHPropuesta> conjunto) {
        List<String> partes = new ArrayList<>();
        float total = 0.0f;
        for (RHPropuesta p : conjunto) {
            String numero = p.getHabitacion() == null ? "?" : String.valueOf(p.getHabitacion().getNumero());
            partes.add("N° " + numero + " (" + ServicioPropuestasDemo.formatearMonto(p.getSubtotal()) + ")");
            total += p.getSubtotal();
        }
        String prefijo = conjunto.size() == 1 ? "1 habitación: " : conjunto.size() + " habitaciones: ";
        return prefijo + String.join(", ", partes)
                + " — total " + ServicioPropuestasDemo.formatearMonto(total);
    }

    private static String nombreTitular(TitularReserva t) {
        return (String.join(" ", t.getNombres()) + " " + String.join(" ", t.getApellidos())).trim();
    }

    private static List<String> separar(String texto) {
        if (texto == null) {
            return List.of();
        }
        List<String> partes = new ArrayList<>();
        for (String s : texto.split("[,\\s]+")) {
            String limpio = s.trim();
            if (!limpio.isEmpty()) {
                partes.add(limpio);
            }
        }
        return partes;
    }

    private static String textoONull(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        return limpio.isEmpty() ? null : limpio;
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

    private static JSpinner spinnerCantidad() {
        return new JSpinner(new SpinnerNumberModel(0, 0, MAX_CANTIDAD, 1));
    }

    private static JSpinner spinnerFecha(LocalDate inicial) {
        JSpinner spinner = new JSpinner(new SpinnerDateModel(aFecha(inicial), null, null, Calendar.DAY_OF_MONTH));
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

    /** Fila de edicion de un bloque solicitado. */
    private static final class FilaBloque {

        private final JPanel panel = new JPanel(new GridBagLayout());
        private final JSpinner checkIn = spinnerFecha(LocalDate.now());
        private final JSpinner checkOut = spinnerFecha(LocalDate.now().plusDays(1));
        private final JSpinner matrimonios = spinnerCantidad();
        private final JSpinner individuales = spinnerCantidad();
        private final JSpinner bebes = spinnerCantidad();
        private final JSpinner cunas = spinnerCantidad();
        private final Map<AtributoHabitacion, JCheckBox> checks = new LinkedHashMap<>();

        FilaBloque(Runnable alQuitar) {
            panel.setBorder(BorderFactory.createTitledBorder("Bloque"));
            int fila = 0;
            agregarCampo(panel, fila++, "Check-in:", checkIn);
            agregarCampo(panel, fila++, "Check-out:", checkOut);
            agregarCampo(panel, fila++, "Matrimonios:", matrimonios);
            agregarCampo(panel, fila++, "Individuales:", individuales);
            agregarCampo(panel, fila++, "Bebés:", bebes);
            agregarCampo(panel, fila++, "Cunas:", cunas);

            JPanel atributos = new JPanel(new GridLayout(0, 2, 6, 2));
            for (AtributoHabitacion a : AtributoHabitacion.values()) {
                JCheckBox check = new JCheckBox(a.toString());
                checks.put(a, check);
                atributos.add(check);
            }
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = fila++;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(4, 6, 4, 6);
            panel.add(atributos, gbc);

            JButton quitar = new JButton("Quitar bloque");
            quitar.addActionListener(e -> alQuitar.run());
            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = fila;
            gbc.gridwidth = 2;
            gbc.insets = new Insets(4, 6, 4, 6);
            panel.add(quitar, gbc);
        }

        DTRHSolicitada aDTO() {
            Set<AtributoHabitacion> atributos = new TreeSet<>();
            checks.forEach((atributo, check) -> {
                if (check.isSelected()) {
                    atributos.add(atributo);
                }
            });
            return new DTRHSolicitada(fecha(checkIn), fecha(checkOut), atributos,
                    cantidad(matrimonios), cantidad(individuales), cantidad(bebes), cantidad(cunas));
        }
    }
}
