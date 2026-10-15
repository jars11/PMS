package com.pms;

import com.pms.controllers.ControladorReservas;
import com.pms.dtos.DTRHSolicitada;
import com.pms.entities.Habitacion;
import com.pms.handlers.ManejadorAtributosHabs;
import com.pms.persistence.JpaUtil;
import com.pms.utils.AtributoHabitacion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManejadorAtributosHabsTest {

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
    void capacidadDerivadaDeCamas() {
        // capacidad = 2*matrimoniales + individuales (cunas y colchones excluidos)
        assertEquals(2, ManejadorAtributosHabs.capacidadDe(new Habitacion(1, Set.of(), 1, 0, 0, 0)));
        assertEquals(3, ManejadorAtributosHabs.capacidadDe(new Habitacion(2, Set.of(), 1, 1, 0, 0)));
        assertEquals(4, ManejadorAtributosHabs.capacidadDe(new Habitacion(3, Set.of(), 1, 2, 0, 0)));
        assertEquals(6, ManejadorAtributosHabs.capacidadDe(new Habitacion(4, Set.of(), 2, 2, 0, 0)));
        // cunas y colchones no suman a la capacidad
        assertEquals(2, ManejadorAtributosHabs.capacidadDe(new Habitacion(5, Set.of(), 1, 0, 3, 2)));
    }

    @Test
    void seleccionConVistaAlMarPrefiereMenosAtributosExtra() {
        // {VistaAlMar} con 3 pasajeros: 102 (VistaAlMar+Jacuzzi, 1 extra) vs 104 (VistaAlMar, 0 extra) -> 104.
        DTRHSolicitada s = new DTRHSolicitada(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15),
                Set.of(AtributoHabitacion.VistaAlMar), 1, 1, 0, 0);
        Habitacion mejor = new ManejadorAtributosHabs().brindarHabitacionesDisponibles(s);
        assertEquals(104, mejor.getNumero());
    }

    @Test
    void seleccionExactaPorMenorNumeroCuandoLosAtributosExtraEmpatan() {
        // 2 matrimonios (4 pasajeros) con {Balcon}: 103 y 201 cumplen, ambos sin extras -> menor numero 103.
        DTRHSolicitada s = new DTRHSolicitada(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15),
                Set.of(AtributoHabitacion.Balcon), 2, 0, 0, 0);
        Habitacion mejor = new ManejadorAtributosHabs().brindarHabitacionesDisponibles(s);
        assertEquals(103, mejor.getNumero());
    }

    @Test
    void seleccionPorCoincidenciasCuandoNoHayExacta() {
        // {Twin} no lo tiene ninguna con capacidad para 4; todas empatan en 0 coincidencias y 1 atributo -> 103.
        DTRHSolicitada s = new DTRHSolicitada(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15),
                Set.of(AtributoHabitacion.Twin), 2, 0, 0, 0);
        Habitacion mejor = new ManejadorAtributosHabs().brindarHabitacionesDisponibles(s);
        assertEquals(103, mejor.getNumero());
    }

    @Test
    void noSeleccionaHabitacionOcupadaEnElBloque() {
        // Reservo el bloque que cae en 101 y verifico que ya no vuelve a salir.
        ControladorReservas cr = ControladorReservas.getInstance();
        cr.agregarBloqueReserva(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15),
                1, 1, 0, 0, Set.of(AtributoHabitacion.Balcon));
        cr.crearReserva(TestUtil.demoTitular());

        DTRHSolicitada s = new DTRHSolicitada(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15),
                Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0);
        Habitacion mejor = new ManejadorAtributosHabs().brindarHabitacionesDisponibles(s);
        assertNotEquals(101, mejor.getNumero());
        // Exacta restante con Balcon y capacidad: 103.
        assertEquals(103, mejor.getNumero());
    }

    @Test
    void sinCandidatasDevuelveNull() {
        DTRHSolicitada s = new DTRHSolicitada(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15),
                Set.of(), 4, 0, 0, 0); // 8 pasajeros: no hay habitacion con capacidad 8
        assertNull(new ManejadorAtributosHabs().brindarHabitacionesDisponibles(s));
    }
}