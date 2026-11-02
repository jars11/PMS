package com.pms;

import com.pms.api.HabitacionVistaDto;
import com.pms.controllers.ControladorHabitaciones;
import com.pms.entities.Habitacion;
import com.pms.persistence.JpaUtil;
import com.pms.utils.AtributoHabitacion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prioridad de habitaciones (S3): el alta agrega la habitacion al final de la
 * lista (prioridad = max + 1), el reordenamiento asigna prioridad 1..N y el
 * listado por prioridad respeta ese orden. {@code getHabitaciones()} sigue
 * devolviendo las habitaciones ordenadas por numero.
 */
class HabitacionPrioridadTest {

    private final ControladorHabitaciones ch = ControladorHabitaciones.getInstance();

    @BeforeAll
    static void antesDeTodo() {
        JpaUtil.setUnidadPorDefecto("pms-test");
    }

    @BeforeEach
    void limpiar() {
        TestUtil.limpiarBase();
    }

    private long idDe(int numero) {
        return ch.getHabitaciones().stream()
                .filter(h -> h.getNumero() == numero)
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private List<Integer> numerosPorPrioridad() {
        return ch.getHabitacionesPorPrioridad().stream().map(Habitacion::getNumero).toList();
    }

    @Test
    void laPrimeraHabitacionArrancaEnPrioridadUno() {
        ch.altaHabitacion(101, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0, 1500.0f);

        assertEquals(1, ch.getHabitacion(idDe(101)).getPrioridad());
    }

    @Test
    void elAltaAgregaAlFinalConPrioridadMaxMasUno() {
        ch.altaHabitacion(101, Set.of(), 1, 1, 0, 0, 1500.0f);
        ch.altaHabitacion(102, Set.of(), 1, 1, 0, 0, 1500.0f);
        ch.altaHabitacion(103, Set.of(), 1, 1, 0, 0, 1500.0f);

        assertEquals(1, ch.getHabitacion(idDe(101)).getPrioridad());
        assertEquals(2, ch.getHabitacion(idDe(102)).getPrioridad());
        assertEquals(3, ch.getHabitacion(idDe(103)).getPrioridad());
        assertEquals(List.of(101, 102, 103), numerosPorPrioridad());
    }

    @Test
    void reordenarAsignaPrioridadUnoANYSirveElOrdenAscendente() {
        ch.altaHabitacion(101, Set.of(), 1, 1, 0, 0, 1500.0f);
        ch.altaHabitacion(102, Set.of(), 1, 1, 0, 0, 1500.0f);
        ch.altaHabitacion(103, Set.of(), 1, 1, 0, 0, 1500.0f);

        List<Habitacion> ordenadas = ch.reordenar(List.of(idDe(103), idDe(101), idDe(102)));

        assertEquals(List.of(103, 101, 102), ordenadas.stream().map(Habitacion::getNumero).toList());
        assertEquals(List.of(1, 2, 3), ordenadas.stream().map(Habitacion::getPrioridad).toList());
        // persistido, no solo en memoria
        assertEquals(1, ch.getHabitacion(idDe(103)).getPrioridad());
        assertEquals(2, ch.getHabitacion(idDe(101)).getPrioridad());
        assertEquals(3, ch.getHabitacion(idDe(102)).getPrioridad());
        assertEquals(List.of(103, 101, 102), numerosPorPrioridad());
    }

    @Test
    void reordenarConIdInexistenteLanzaIllegalArgument() {
        ch.altaHabitacion(101, Set.of(), 1, 1, 0, 0, 1500.0f);

        assertThrows(IllegalArgumentException.class,
                () -> ch.reordenar(List.of(idDe(101), 999999L)));
    }

    @Test
    void getHabitacionesSigueOrdenadaPorNumeroTrasReordenar() {
        ch.altaHabitacion(101, Set.of(), 1, 1, 0, 0, 1500.0f);
        ch.altaHabitacion(102, Set.of(), 1, 1, 0, 0, 1500.0f);
        ch.altaHabitacion(103, Set.of(), 1, 1, 0, 0, 1500.0f);

        ch.reordenar(List.of(idDe(103), idDe(102), idDe(101)));

        assertEquals(List.of(101, 102, 103),
                ch.getHabitaciones().stream().map(Habitacion::getNumero).toList());
    }

    @Test
    void laVistaExponeLaPrioridad() {
        ch.altaHabitacion(101, Set.of(), 1, 1, 0, 0, 1500.0f);
        ch.altaHabitacion(102, Set.of(), 1, 1, 0, 0, 1500.0f);
        ch.reordenar(List.of(idDe(102), idDe(101)));

        HabitacionVistaDto vista = HabitacionVistaDto.de(ch.getHabitacion(idDe(102)));

        assertEquals(102, vista.numero());
        assertEquals(1, vista.prioridad());
        assertTrue(vista.prioridad() < HabitacionVistaDto.de(ch.getHabitacion(idDe(101))).prioridad());
    }
}
