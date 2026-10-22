package com.pms.ui;

import com.pms.handlers.InteraccionUsuario;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * Adaptador Swing de {@link InteraccionUsuario}: reconecta el algoritmo
 * (que corre en un hilo de fondo) con el EDT.
 *
 * <p>{@link #confirmar(String)} pregunta con un dialogo SI/NO. Si ya estamos en
 * el EDT pregunta directo; si no, hace marshalling con {@code invokeAndWait} y
 * devuelve la respuesta mediante un {@link AtomicBoolean}. {@link #notificar(String)}
 * encola el mensaje con {@code invokeLater} para no bloquear el hilo del algoritmo.
 *
 * <p>El constructor con {@code cancelado} permite abortar una busqueda: las
 * confirmaciones devuelven {@code false} y las notificaciones se descartan.
 */
public final class InteraccionSwing implements InteraccionUsuario {

    private final Component padre;
    private final BooleanSupplier cancelado;

    public InteraccionSwing(Component padre) {
        this(padre, () -> false);
    }

    public InteraccionSwing(Component padre, BooleanSupplier cancelado) {
        this.padre = padre;
        this.cancelado = cancelado == null ? () -> false : cancelado;
    }

    @Override
    public boolean confirmar(String pregunta) {
        return confirmar("Confirmación", pregunta);
    }

    @Override
    public boolean confirmar(String titulo, String pregunta) {
        if (cancelado.getAsBoolean()) {
            return false;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            return preguntar(titulo, pregunta);
        }
        AtomicBoolean respuesta = new AtomicBoolean(false);
        try {
            SwingUtilities.invokeAndWait(() -> respuesta.set(preguntar(titulo, pregunta)));
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
        if (cancelado.getAsBoolean()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (!cancelado.getAsBoolean()) {
                JOptionPane.showMessageDialog(
                        padre, mensaje, "Información", JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }

    private boolean preguntar(String titulo, String pregunta) {
        if (cancelado.getAsBoolean()) {
            return false;
        }
        int opcion = JOptionPane.showConfirmDialog(padre, pregunta,
                titulo == null ? "Confirmación" : titulo,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return opcion == JOptionPane.YES_OPTION;
    }
}
