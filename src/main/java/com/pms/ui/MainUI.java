package com.pms.ui;

import com.pms.ui.ServicioPropuestasDemo.PropuestaAsignada;
import com.pms.ui.ServicioPropuestasDemo.ResultadoPropuesta;
import com.pms.utils.AtributoHabitacion;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interfaz de usuario Swing "Propuesta de estadía" para el recepcionista.
 * Permite ingresar cantidades de pasajeros, fechas de check-in/check-out y
 * atributos deseados, y muestra la propuesta armada por
 * {@link ServicioPropuestasDemo}. No persiste ni consulta disponibilidad real.
 */
public final class MainUI {

    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_CANTIDAD = 20;

    /** Atributos de confort seleccionables, en el orden mostrado en el formulario. */
    private static final Map<AtributoHabitacion, String> ATRIBUTOS_COMFORT = new LinkedHashMap<>();

    static {
        ATRIBUTOS_COMFORT.put(AtributoHabitacion.Balcon, "Balcón");
        ATRIBUTOS_COMFORT.put(AtributoHabitacion.VistaAlMar, "Vista al mar");
        ATRIBUTOS_COMFORT.put(AtributoHabitacion.Jacuzzi, "Jacuzzi");
        ATRIBUTOS_COMFORT.put(AtributoHabitacion.Twin, "Camas separadas (Twin)");
        ATRIBUTOS_COMFORT.put(AtributoHabitacion.DosAmbientes, "Dos ambientes");
    }

    private final JSpinner spinnerMatrimonios;
    private final JSpinner spinnerAdultos;
    private final JSpinner spinnerNinos;
    private final JSpinner spinnerCheckIn;
    private final JSpinner spinnerCheckOut;
    private final Map<AtributoHabitacion, JCheckBox> checksAtributos = new LinkedHashMap<>();
    private final JTextArea areaResultado;
    private final ServicioPropuestasDemo servicio = new ServicioPropuestasDemo();

    private MainUI() {
        spinnerMatrimonios = crearSpinnerCantidad();
        spinnerAdultos = crearSpinnerCantidad();
        spinnerNinos = crearSpinnerCantidad();
        spinnerCheckIn = crearSpinnerFecha(LocalDate.now());
        spinnerCheckOut = crearSpinnerFecha(LocalDate.now().plusDays(1));
        areaResultado = crearAreaResultado();

        JFrame frame = new JFrame("PMS – Propuesta de estadía");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(8, 8));
        frame.add(construirPanelFormulario(), BorderLayout.NORTH);
        frame.add(construirPanelResultado(), BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainUI());
    }

    private static JSpinner crearSpinnerCantidad() {
        return new JSpinner(new SpinnerNumberModel(0, 0, MAX_CANTIDAD, 1));
    }

    private static JSpinner crearSpinnerFecha(LocalDate fecha) {
        Calendar calendario = Calendar.getInstance();
        calendario.clear();
        calendario.set(fecha.getYear(), fecha.getMonthValue() - 1, fecha.getDayOfMonth());
        JSpinner spinner = new JSpinner(
                new SpinnerDateModel(calendario.getTime(), null, null, Calendar.DAY_OF_MONTH));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "dd/MM/yyyy"));
        return spinner;
    }

    private static JTextArea crearAreaResultado() {
        JTextArea area = new JTextArea(16, 58);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return area;
    }

    private JPanel construirPanelFormulario() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        int fila = 0;
        agregarCampo(panel, fila++, "Cantidad de matrimonios:", spinnerMatrimonios);
        agregarCampo(panel, fila++, "Cantidad de adultos separados:", spinnerAdultos);
        agregarCampo(panel, fila++, "Cantidad de niños:", spinnerNinos);
        agregarCampo(panel, fila++, "Fecha de check-in:", spinnerCheckIn);
        agregarCampo(panel, fila++, "Fecha de check-out:", spinnerCheckOut);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = fila++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 6);
        panel.add(construirPanelAtributos(), gbc);

        gbc.gridy = fila;
        gbc.fill = GridBagConstraints.NONE;
        JButton botonGenerar = new JButton("Generar propuesta");
        botonGenerar.addActionListener(e -> generarPropuesta());
        panel.add(botonGenerar, gbc);

        return panel;
    }

    private static void agregarCampo(JPanel panel, int fila, String etiqueta, JSpinner spinner) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.gridx = 0;
        gbc.gridy = fila;
        panel.add(new JLabel(etiqueta), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(spinner, gbc);
    }

    private JPanel construirPanelAtributos() {
        JPanel grupo = new JPanel(new GridLayout(0, 2, 8, 4));
        for (Map.Entry<AtributoHabitacion, String> entrada : ATRIBUTOS_COMFORT.entrySet()) {
            JCheckBox check = new JCheckBox(entrada.getValue());
            checksAtributos.put(entrada.getKey(), check);
            grupo.add(check);
        }
        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.add(grupo, BorderLayout.CENTER);
        contenedor.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Atributos de habitación"));
        return contenedor;
    }

    private JPanel construirPanelResultado() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        panel.add(new JScrollPane(areaResultado), BorderLayout.CENTER);
        return panel;
    }

    private void generarPropuesta() {
        int matrimonios = (Integer) spinnerMatrimonios.getValue();
        int adultosSeparados = (Integer) spinnerAdultos.getValue();
        int ninos = (Integer) spinnerNinos.getValue();
        LocalDate checkIn = aLocalDate((Date) spinnerCheckIn.getValue());
        LocalDate checkOut = aLocalDate((Date) spinnerCheckOut.getValue());
        Set<AtributoHabitacion> solicitados = checksAtributos.entrySet().stream()
                .filter(entrada -> entrada.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        try {
            ServicioPropuestasDemo.validar(checkIn, checkOut, matrimonios, adultosSeparados, ninos);
            ResultadoPropuesta resultado = servicio.generarPropuesta(
                    checkIn, checkOut, matrimonios, adultosSeparados, ninos, solicitados);
            areaResultado.setText(formatearResultado(resultado));
        } catch (IllegalArgumentException excepcion) {
            JOptionPane.showMessageDialog(null, excepcion.getMessage(),
                    "Datos inválidos", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String formatearResultado(ResultadoPropuesta resultado) {
        StringBuilder salida = new StringBuilder(resultado.resumen());
        if (!resultado.getAsignaciones().isEmpty()) {
            salida.append(System.lineSeparator()).append(System.lineSeparator());
            for (PropuestaAsignada asignada : resultado.getAsignaciones()) {
                salida.append("Habitación ").append(asignada.habitacion().getNumero())
                        .append(" | ").append(FECHA_FORMATO.format(asignada.propuesta().getFechaInicio()))
                        .append(" -> ").append(FECHA_FORMATO.format(asignada.propuesta().getFechaFin()))
                        .append(" | Camas: ").append(camasTexto(asignada.camasMatrimoniales(), asignada.camasIndividuales()))
                        .append(" | Atributos: ").append(atributosTexto(asignada.habitacion().getAtributos()))
                        .append(" | Subtotal: ").append(ServicioPropuestasDemo.formatearMonto(asignada.propuesta().getSubtotal()))
                        .append(System.lineSeparator());
            }
            salida.append(System.lineSeparator())
                    .append("Total: ").append(ServicioPropuestasDemo.formatearMonto(resultado.getTotal()));
        }
        return salida.toString();
    }

    private static String camasTexto(int matrimoniales, int individuales) {
        List<String> partes = new ArrayList<>();
        if (matrimoniales > 0) {
            partes.add(matrimoniales + (matrimoniales == 1 ? " matrimonial" : " matrimoniales"));
        }
        if (individuales > 0) {
            partes.add(individuales + (individuales == 1 ? " individual" : " individuales"));
        }
        return partes.isEmpty() ? "ninguna" : String.join(", ", partes);
    }

    private static String atributosTexto(Set<AtributoHabitacion> atributos) {
        return atributos.stream()
                .map(AtributoHabitacion::toString)
                .collect(Collectors.joining(", "));
    }

    private static LocalDate aLocalDate(Date fecha) {
        return fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}