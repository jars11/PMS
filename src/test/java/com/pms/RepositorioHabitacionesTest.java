package com.pms;

import com.pms.controllers.ControladorReservas;
import com.pms.entities.Habitacion;
import com.pms.persistence.JpaUtil;
import com.pms.persistence.RepositorioHabitaciones;
import com.pms.utils.AtributoHabitacion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Repositorio de habitaciones contra la unidad en memoria pms-test:
 * actualizar / eliminar / findByNumero / existeNumero / estaEnUso.
 */
class RepositorioHabitacionesTest {

    private final RepositorioHabitaciones repo = new RepositorioHabitaciones();

    @BeforeAll
    static void antesDeTodo() {
        JpaUtil.setUnidadPorDefecto("pms-test");
    }

    @BeforeEach
    void limpiar() {
        TestUtil.limpiarBase();
    }

    private Habitacion sembrar(int numero) {
        Habitacion h = new Habitacion(numero, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0);
        h.setTarifaPorNoche(1500.0f);
        return repo.guardar(h);
    }

    @Test
    void actualizarPersisteCambios() {
        Habitacion h = sembrar(101);

        h.actualizar(101, Set.of(AtributoHabitacion.VistaAlMar), 2, 1, 0, 0, 1900.0f);
        repo.actualizar(h);

        Habitacion leida = repo.findById(h.getId());
        assertEquals(1900.0f, leida.getTarifaPorNoche(), 0.001f);
        assertEquals(2, leida.getCantidadMatrimoniales());
        assertEquals(Set.of(AtributoHabitacion.VistaAlMar), leida.getAtributos());
        assertEquals(5, leida.capacidad());
    }

    @Test
    void eliminarBorraYDevuelveFalseSiNoExiste() {
        Habitacion h = sembrar(106);

        assertTrue(repo.eliminar(h.getId()));
        assertNull(repo.findById(h.getId()));
        assertFalse(repo.eliminar(h.getId()));
    }

    @Test
    void findByNumeroDevuelveHabitacionO_Null() {
        sembrar(101);

        Habitacion encontrada = repo.findByNumero(101);
        assertNotNull(encontrada);
        assertEquals(101, encontrada.getNumero());

        assertNull(repo.findByNumero(999));
    }

    @Test
    void existeNumeroReflejaPresentesYAusentes() {
        sembrar(101);

        assertTrue(repo.existeNumero(101));
        assertFalse(repo.existeNumero(999));
    }

    @Test
    void estaEnUsoCuentaPropuestasDeLaHabitacion() {
        TestUtil.sembrarHabitaciones();
        Habitacion h101 = repo.findByNumero(101);
        Habitacion h104 = repo.findByNumero(104);

        assertFalse(repo.estaEnUso(h101.getId()));

        ControladorReservas cr = ControladorReservas.getInstance();
        cr.agregarBloqueReserva(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15), 1, 1, 0, 0,
                Set.of(AtributoHabitacion.Balcon));
        cr.crearReserva(TestUtil.demoTitular());

        assertTrue(repo.estaEnUso(h101.getId()));
        assertFalse(repo.estaEnUso(h104.getId()));
    }
}
