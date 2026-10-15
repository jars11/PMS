package com.pms.ui;

import com.pms.handlers.InteraccionUsuario;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Adaptador Swing de {@link InteraccionUsuario}: reconecta el algoritmo
 * (que corre en un hilo de fondo) con el EDT.
 *
 * <p>{@link #confirmar(String)} pregunta con un dialogo SI/NO. Si ya estamos en
 * el EDT pregunta directo; si no, hace marshalling con {@code invokeAndWait} y
 * devuelve la respuesta mediante un {@link AtomicBoolean}. {@link #notificar(String)}
 * encola el mensaje con {@code invokeLater} para no bloquear el hilo del algoritmo.
 */
public final class InteraccionSwing implements InteraccionUsuario {

    private final Component padre;

    public InteraccionSwing(Component padre) {
        this.padre = padre;
    }

    @Override
    public boolean confirmar(String pregunta) {
        if (SwingUtilities.isEventDispatchThread()) {
            return preguntar(pregunta);
        }
        AtomicBoolean respuesta = new AtomicBoolean(false);
        try {
            SwingUtilities.invokeAndWait(() -> respuesta.set(preguntar(pregunta)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (InvocationTargetException e) {
            return false;
        }
        return respuesta.get();
    }

    @Override
    public void notificar(String mensaje) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                padre, mensaje, "Información", JOptionPane.INFORMATION_MESSAGE));
    }

    private boolean preguntar(String pregunta) {
        int opcion = JOptionPane.showConfirmDialog(padre, pregunta, "Confirmación",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return opcion == JOptionPane.YES_OPTION;
    }
}
