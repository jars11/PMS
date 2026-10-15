package com.pms.ui;

import com.pms.persistence.JpaUtil;
import com.pms.utils.Rol;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Entrada de la GUI. Siembra datos demo sólo si la base está vacía, resuelve el
 * rol en un selector modal y abre la pantalla correspondiente. Al cerrar la
 * ventana cierra todas las factorías JPA (flush del archivo H2).
 *
 * <p>El selector es el único punto que conoce el rol: las pantallas lo reciben
 * como {@link SesionUsuario}, de modo que un futuro login reemplaza sólo
 * {@link #resolverSesion()} sin tocar las pantallas restringidas.
 */
public final class PmsApp {

    private PmsApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PmsApp::arrancar);
    }

    /** Arranca la GUI en el EDT. */
    static void arrancar() {
        SemillaDemo.sembrarSiVacio();
        SemillaDemo.sembrarReservasDemo();

        SesionUsuario sesion = resolverSesion();
        if (sesion == null) {
            JpaUtil.cerrarTodas();
            return;
        }

        JFrame frame = sesion.esAdmin() ? new AdminFrame(sesion) : new RecepcionistaFrame(sesion);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JpaUtil.cerrarTodas();
            }
        });
        frame.setVisible(true);
    }

    /**
     * Selector modal de rol sobre {@link Rol#values()}. Cancelar o cerrar el
     * diálogo devuelve {@code null} (sin sesión, sin pantalla restringida).
     */
    private static SesionUsuario resolverSesion() {
        Object eleccion = JOptionPane.showInputDialog(null,
                "Seleccione el rol para esta sesión:", "PMS – Selección de rol",
                JOptionPane.QUESTION_MESSAGE, null, Rol.values(), Rol.ADMIN);
        if (eleccion == null) {
            return null;
        }
        return SesionUsuario.de((Rol) eleccion);
    }
}
