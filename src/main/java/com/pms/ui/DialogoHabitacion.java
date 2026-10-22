package com.pms.ui;

import com.pms.entities.Habitacion;
import com.pms.utils.AtributoHabitacion;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Diálogo modal de alta/edición de una habitación. Valida la entrada básica
 * (tarifa presente y numérica) y expone los valores ingresados; la validación
 * de negocio (numero/beds/tarifa) queda delegada al
 * {@code ControladorHabitaciones}, cuyos errores superficiea {@link AdminFrame}.
 */
public final class DialogoHabitacion extends JDialog {

    private static final int MAX_CANTIDAD = 50;

    /** Etiquetas amigables mostradas en los checkboxes de atributos. */
    private static final Map<AtributoHabitacion, String> ETIQUETAS = new LinkedHashMap<>();

    static {
        ETIQUETAS.put(AtributoHabitacion.Balcon, "Balcón");
        ETIQUETAS.put(AtributoHabitacion.VistaAlMar, "Vista al mar");
        ETIQUETAS.put(AtributoHabitacion.Jacuzzi, "Jacuzzi");
        ETIQUETAS.put(AtributoHabitacion.Twin, "Camas separadas (Twin)");
        ETIQUETAS.put(AtributoHabitacion.DosAmbientes, "Dos ambientes");
    }

    private final JSpinner spinnerNumero = new JSpinner(new SpinnerNumberModel(101, 1, 9999, 1));
    private final JSpinner spinnerMatrimoniales = spinnerCantidad();
    private final JSpinner spinnerIndividuales = spinnerCantidad();
    private final JSpinner spinnerCunas = spinnerCantidad();
    private final JSpinner spinnerColchones = spinnerCantidad();
    private final Map<AtributoHabitacion, JCheckBox> checksAtributos = new LinkedHashMap<>();
    private final JCheckBox checkLimpia = new JCheckBox("Limpia (lista para huésped)", true);
    private final JTextField campoTarifa = new JTextField("1500", 12);

    private boolean aceptado;
    private float tarifa;

    private DialogoHabitacion(Window padre, String titulo, Habitacion existente) {
        super(padre, titulo, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        add(construirFormulario(), BorderLayout.CENTER);
        add(construirBotones(), BorderLayout.SOUTH);
        if (existente != null) {
            prellenar(existente);
        }
        pack();
        setLocationRelativeTo(padre);
    }

    /** Abre el diálogo en modo alta y devuelve la instancia con los valores elegidos. */
    public static DialogoHabitacion paraAlta(Window padre) {
        return mostrar(new DialogoHabitacion(padre, "Nueva habitación", null));
    }

    /** Abre el diálogo en modo edición, prellenado con la habitación dada. */
    public static DialogoHabitacion paraEdicion(Window padre, Habitacion habitacion) {
        return mostrar(new DialogoHabitacion(padre, "Editar habitación", habitacion));
    }

    private static DialogoHabitacion mostrar(DialogoHabitacion dialogo) {
        dialogo.setVisible(true);
        return dialogo;
    }

    /** True cuando el usuario confirmó con datos válidos. */
    public boolean esAceptado() {
        return aceptado;
    }

    public int getNumero() {
        return (Integer) spinnerNumero.getValue();
    }

    public int getMatrimoniales() {
        return (Integer) spinnerMatrimoniales.getValue();
    }

    public int getIndividuales() {
        return (Integer) spinnerIndividuales.getValue();
    }

    public int getCunas() {
        return (Integer) spinnerCunas.getValue();
    }

    public int getColchones() {
        return (Integer) spinnerColchones.getValue();
    }

    /** Tarifa ingresada; sólo tiene sentido cuando {@link #esAceptado()} es true. */
    public float getTarifa() {
        return tarifa;
    }

    /** Estado de limpieza elegido (housekeeping); se aplica con {@code marcarLimpia}. */
    public boolean getLimpia() {
        return checkLimpia.isSelected();
    }

    /** Etiqueta amigable de un atributo; nombre del enum si no hay etiqueta. */
    static String etiquetaAtributo(AtributoHabitacion a) {
        return ETIQUETAS.getOrDefault(a, a.name());
    }

    /** Atributos seleccionados, ordenados por el orden natural del enum. */
    public Set<AtributoHabitacion> getAtributos() {
        Set<AtributoHabitacion> seleccionados = new TreeSet<>();
        for (Map.Entry<AtributoHabitacion, JCheckBox> entrada : checksAtributos.entrySet()) {
            if (entrada.getValue().isSelected()) {
                seleccionados.add(entrada.getKey());
            }
        }
        return seleccionados;
    }

    private static JSpinner spinnerCantidad() {
        return new JSpinner(new SpinnerNumberModel(0, 0, MAX_CANTIDAD, 1));
    }

    private void prellenar(Habitacion existente) {
        spinnerNumero.setValue(existente.getNumero());
        spinnerMatrimoniales.setValue(existente.getCantidadMatrimoniales());
        spinnerIndividuales.setValue(existente.getCantidadIndividuales());
        spinnerCunas.setValue(existente.getCantidadCunas());
        spinnerColchones.setValue(existente.getColchones());
        campoTarifa.setText(String.valueOf(existente.getTarifaPorNoche()));
        checkLimpia.setSelected(existente.isLimpia());
        for (AtributoHabitacion atributo : existente.getAtributos()) {
            JCheckBox check = checksAtributos.get(atributo);
            if (check != null) {
                check.setSelected(true);
            }
        }
    }

    private JPanel construirFormulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        int fila = 0;
        agregarCampo(panel, fila++, "Número:", spinnerNumero);
        agregarCampo(panel, fila++, "Camas matrimoniales:", spinnerMatrimoniales);
        agregarCampo(panel, fila++, "Camas individuales:", spinnerIndividuales);
        agregarCampo(panel, fila++, "Cunas:", spinnerCunas);
        agregarCampo(panel, fila++, "Colchones:", spinnerColchones);
        agregarCampo(panel, fila++, "Tarifa por noche:", campoTarifa);
        agregarCampo(panel, fila++, "Limpieza:", checkLimpia);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 4, 6);
        panel.add(construirPanelAtributos(), gbc);
        return panel;
    }

    private JPanel construirPanelAtributos() {
        JPanel grupo = new JPanel(new GridLayout(0, 2, 8, 4));
        for (AtributoHabitacion atributo : ETIQUETAS.keySet()) {
            JCheckBox check = new JCheckBox(etiquetaAtributo(atributo));
            checksAtributos.put(atributo, check);
            grupo.add(check);
        }
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.add(grupo, BorderLayout.CENTER);
        contenedor.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Atributos de habitación"));
        return contenedor;
    }

    private JPanel construirBotones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton botonCancelar = new JButton("Cancelar");
        botonCancelar.addActionListener(e -> dispose());
        JButton botonAceptar = new JButton("Aceptar");
        botonAceptar.addActionListener(e -> aceptar());
        panel.add(botonCancelar);
        panel.add(botonAceptar);
        getRootPane().setDefaultButton(botonAceptar);
        return panel;
    }

    private void aceptar() {
        if (validarEntrada()) {
            aceptado = true;
            dispose();
        }
    }

    /** Validación de forma: tarifa no vacía y numérica. */
    private boolean validarEntrada() {
        String texto = campoTarifa.getText().trim();
        if (texto.isEmpty()) {
            mostrarError("La tarifa por noche es obligatoria.");
            return false;
        }
        try {
            tarifa = Float.parseFloat(texto.replace(',', '.'));
        } catch (NumberFormatException ex) {
            mostrarError("La tarifa por noche debe ser un número.");
            return false;
        }
        return true;
    }

    private void mostrarError(String mensaje) {
        DialogoError.mostrar(this, "Datos inválidos", mensaje);
    }

    private static void agregarCampo(JPanel panel, int fila, String etiqueta, java.awt.Component campo) {
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
