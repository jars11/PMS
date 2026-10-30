package com.pms;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.entities.RHPropuesta;
import com.pms.persistence.JpaUtil;
import com.pms.ui.CalendarioPanel;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.TipoMoneda;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Smoke test de integracion del panel del calendario: construirlo y refrescarlo
 * contra la base H2 no debe lanzar, y el preview de propuestas tampoco.
 */
class CalendarioPanelSmokeTest {

    @BeforeAll
    static void antesDeTodo() {
        JpaUtil.setUnidadPorDefecto("pms-test");
    }

    @BeforeEach
    void limpiar() {
        TestUtil.limpiarBase();
        TestUtil.sembrarHabitaciones();
    }

    @Test
    void construyeRefrescaYPrevisualizaPropuestas() {
        assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> {
            CalendarioPanel panel = new CalendarioPanel();
            panel.refrescar();

            RHPropuesta propuesta = new RHPropuesta(LocalDate.now(), LocalDate.now().plusDays(2),
                    Set.of(AtributoHabitacion.Balcon), 1000.0f, TipoMoneda.USD, 1, 1, 0);
            propuesta.setHabitacion(
                    ControladorHabitaciones.getInstance().getHabitaciones().get(0));
            panel.previsualizarPropuestas(Set.of(propuesta));
            panel.previsualizarPropuestas(Set.of());
        }));
    }
}
