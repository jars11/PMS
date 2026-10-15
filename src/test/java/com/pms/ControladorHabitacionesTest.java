package com.pms;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorReservas;
import com.pms.entities.Habitacion;
import com.pms.persistence.JpaUtil;
import com.pms.utils.AtributoHabitacion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guardas del controlador de habitaciones: create valido/invalido, numero
 * duplicado en alta y update, y borrado protegido por referencias.
 */
class ControladorHabitacionesTest {

    private final ControladorHabitaciones ch = ControladorHabitaciones.getInstance();

    @BeforeAll
    static void antesDeTodo() {
        JpaUtil.setUnidadPorDefecto("pms-test");
    }

    @BeforeEach
    void limpiar() {
        TestUtil.limpiarBase();
    }

    private Habitacion porNumero(int numero) {
        return ch.getHabitaciones().stream()
                .filter(h -> h.getNumero() == numero)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void altaValidaPersisteYApareceEnLista() {
        ch.altaHabitacion(105, Set.of(AtributoHabitacion.Balcon), 1, 2, 0, 0, 1800.0f);

        assertEquals(1, ch.getHabitaciones().size());
        Habitacion h = porNumero(105);
        assertEquals(1800.0f, h.getTarifaPorNoche(), 0.001f);
        assertNotNull(ch.getHabitacion(h.getId()));
    }

    @Test
    void altaInvalidaLanzaYNoPersiste() {
        assertThrows(IllegalArgumentException.class,
                () -> ch.altaHabitacion(0, Set.of(), 1, 1, 0, 0, 1500.0f));
        assertThrows(IllegalArgumentException.class,
                () -> ch.altaHabitacion(101, Set.of(), -1, 1, 0, 0, 1500.0f));

        assertTrue(ch.getHabitaciones().isEmpty());
    }

    @Test
    void altaConNumeroDuplicadoLanzaYPersisteUnaSola() {
        ch.altaHabitacion(101, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0, 1500.0f);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> ch.altaHabitacion(101, Set.of(), 1, 1, 0, 0, 1500.0f));

        assertTrue(ex.getMessage().contains("duplicado"), ex.getMessage());
        assertEquals(1, ch.getHabitaciones().size());
    }

    @Test
    void actualizarValidoCambiaCamposYCapacidad() {
        ch.altaHabitacion(101, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0, 1500.0f);
        long id = porNumero(101).getId();

        ch.actualizarHabitacion(id, 101, Set.of(AtributoHabitacion.VistaAlMar), 1, 3, 0, 0, 1900.0f);

        Habitacion h = ch.getHabitacion(id);
        assertEquals(1900.0f, h.getTarifaPorNoche(), 0.001f);
        assertEquals(3, h.getCantidadIndividuales());
        assertEquals(Set.of(AtributoHabitacion.VistaAlMar), h.getAtributos());
        assertEquals(5, h.capacidad());
    }

    @Test
    void actualizarRechazaNumeroDuplicadoYConservaNumeros() {
        ch.altaHabitacion(101, Set.of(), 1, 1, 0, 0, 1500.0f);
        ch.altaHabitacion(102, Set.of(), 1, 1, 0, 0, 1500.0f);
        long id101 = porNumero(101).getId();

        assertThrows(IllegalStateException.class,
                () -> ch.actualizarHabitacion(id101, 102, Set.of(), 1, 1, 0, 0, 1500.0f));

        assertEquals(101, ch.getHabitacion(id101).getNumero());
        assertEquals(102, porNumero(102).getNumero());
    }

    @Test
    void getHabitacionInexistenteLanza() {
        assertThrows(IllegalArgumentException.class, () -> ch.getHabitacion(9999L));
    }

    @Test
    void eliminarRechazadoCuandoLaHabitacionEstaEnUso() {
        TestUtil.sembrarHabitaciones();
        ControladorReservas cr = ControladorReservas.getInstance();
        cr.agregarBloqueReserva(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15), 1, 1, 0, 0,
                Set.of(AtributoHabitacion.Balcon));
        cr.crearReserva(TestUtil.demoTitular());

        long id101 = porNumero(101).getId();
        assertThrows(IllegalStateException.class, () -> ch.eliminarHabitacion(id101));
        assertNotNull(ch.getHabitacion(id101));
    }

    @Test
    void eliminarBorraCuandoNoEstaEnUso() {
        ch.altaHabitacion(106, Set.of(), 1, 0, 0, 0, 1000.0f);
        long id = porNumero(106).getId();

        assertTrue(ch.eliminarHabitacion(id));
        assertTrue(ch.getHabitaciones().isEmpty());
    }

    @Test
    void marcarLimpiaToggleaYPersiste() {
        ch.altaHabitacion(105, Set.of(), 1, 1, 0, 0, 1000.0f);
        long id = porNumero(105).getId();

        assertTrue(porNumero(105).isLimpia(), "habitacion nueva limpia por defecto");

        ch.marcarLimpia(id, false);
        assertFalse(porNumero(105).isLimpia(), "sucia persistida");

        ch.marcarLimpia(id, true);
        assertTrue(porNumero(105).isLimpia(), "limpia persistida");
    }
}
