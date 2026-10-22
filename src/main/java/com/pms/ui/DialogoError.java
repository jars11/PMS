package com.pms.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Dialogo de error con un boton "Copiar al portapapeles" en lugar del clasico
 * "Aceptar". El texto completo queda seleccionable y se copia tal cual para
 * poder pegarlo en un reporte.
 */
public final class DialogoError {

    private DialogoError() {
    }

    /** Muestra el error. {@code mensaje} puede ser un String o un Object[]. */
    public static void mostrar(Component padre, String titulo, Object mensaje) {
        String texto = formatear(mensaje);

        JTextArea area = new JTextArea(texto);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setCaretPosition(0);
        area.setColumns(60);
        area.setRows(Math.min(14, Math.max(3, texto.split("\\R", -1).length + 1)));
        area.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JButton copiar = new JButton("Copiar al portapapeles");
        JButton cerrar = new JButton("Cerrar");

        Window owner = SwingUtilities.getWindowAncestor(padre);
        JDialog dialogo = new JDialog(owner, titulo == null ? "Error" : titulo,
                Dialog.ModalityType.APPLICATION_MODAL);
        dialogo.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialogo.setLayout(new BorderLayout(8, 8));

        copiar.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(texto), null);
            copiar.setText("Copiado");
        });
        cerrar.addActionListener(e -> dialogo.dispose());

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(copiar);
        botones.add(cerrar);
        dialogo.add(new JScrollPane(area), BorderLayout.CENTER);
        dialogo.add(botones, BorderLayout.SOUTH);
        dialogo.pack();
        dialogo.setLocationRelativeTo(padre);
        dialogo.setResizable(true);
        dialogo.setVisible(true);
    }

    private static String formatear(Object mensaje) {
        if (mensaje == null) {
            return "";
        }
        if (mensaje instanceof Object[] partes) {
            return Arrays.stream(partes)
                    .map(String::valueOf)
                    .collect(Collectors.joining("\n"));
        }
        return String.valueOf(mensaje);
    }
}
