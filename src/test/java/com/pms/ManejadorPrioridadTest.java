package com.pms;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.dtos.DTRHSolicitada;
import com.pms.entities.Habitacion;
import com.pms.handlers.ManejadorAtributosHabs;
import com.pms.persistence.JpaUtil;
import com.pms.persistence.RepositorioHabitaciones;
import com.pms.utils.AtributoHabitacion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Desempate de asignacion por prioridad (S3): cuando dos habitaciones entran
 * igual de bien en la propuesta, gana la de menor prioridad (la de mas arriba en
 * la lista) y, si empatan en prioridad, la de menor numero.
 */
class ManejadorPrioridadTest {

    private static final LocalDate INI = LocalDate.of(2027, 1, 10);
    private static final LocalDate FIN = LocalDate.of(2027, 1, 15);

    @BeforeAll
    static void antesDeTodo() {
        JpaUtil.setUnidadPorDefecto("pms-test");
    }

    @BeforeEach
    void limpiar() {
        TestUtil.limpiarBase();
    }

    private long idDe(int numero) {
        return ControladorHabitaciones.getInstance().getHabitaciones().stream()
                .filter(h -> h.getNumero() == numero)
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private static void fijarPrioridad(long id, int prioridad) {
        ControladorHabitaciones ch = ControladorHabitaciones.getInstance();
        Habitacion h = ch.getHabitacion(id);
        h.setPrioridad(prioridad);
        new RepositorioHabitaciones().actualizar(h);
    }

    @Test
    void laPrioridadMandaSobreElNumeroCuandoLosAtributosEmpatan() {
        // Dos exactas identicas: 101 es la de menor numero pero 102 tiene prioridad 1.
        ControladorHabitaciones ch = ControladorHabitaciones.getInstance();
        ch.altaHabitacion(101, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0, 1500.0f);
        ch.altaHabitacion(102, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0, 1500.0f);
        ch.reordenar(List.of(idDe(102), idDe(101)));

        DTRHSolicitada s = new DTRHSolicitada(INI, FIN, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0);

        assertEquals(102, new ManejadorAtributosHabs().brindarHabitacionesDisponibles(s).getNumero());
    }

    @Test
    void igualPrioridadDesempataPorMenorNumero() {
        ControladorHabitaciones ch = ControladorHabitaciones.getInstance();
        ch.altaHabitacion(101, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0, 1500.0f);
        ch.altaHabitacion(102, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0, 1500.0f);
        fijarPrioridad(idDe(101), 1);
        fijarPrioridad(idDe(102), 1);

        DTRHSolicitada s = new DTRHSolicitada(INI, FIN, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0);

        assertEquals(101, new ManejadorAtributosHabs().brindarHabitacionesDisponibles(s).getNumero());
    }

    @Test
    void laPrioridadTambienDesempataCuandoNoHayCoincidenciaExacta() {
        // Ninguna tiene {Twin}: ambas empatan en 0 coincidencias y 1 atributo.
        ControladorHabitaciones ch = ControladorHabitaciones.getInstance();
        ch.altaHabitacion(101, Set.of(AtributoHabitacion.Balcon), 2, 0, 0, 0, 1500.0f);
        ch.altaHabitacion(102, Set.of(AtributoHabitacion.VistaAlMar), 2, 0, 0, 0, 1500.0f);
        ch.reordenar(List.of(idDe(102), idDe(101)));

        DTRHSolicitada s = new DTRHSolicitada(INI, FIN, Set.of(AtributoHabitacion.Twin), 2, 0, 0, 0);

        assertEquals(102, new ManejadorAtributosHabs().brindarHabitacionesDisponibles(s).getNumero());
    }
}
