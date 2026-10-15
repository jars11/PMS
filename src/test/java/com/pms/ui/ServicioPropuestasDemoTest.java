package com.pms.ui;

import com.pms.dtos.DTRHPropuesta;
import com.pms.ui.ServicioPropuestasDemo.PropuestaAsignada;
import com.pms.ui.ServicioPropuestasDemo.ResultadoPropuesta;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.TipoMoneda;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la heurística DEMO de propuestas (sin base de datos ni JPA).
 * Verifica el resultado determinístico de {@link ServicioPropuestasDemo}.
 */
class ServicioPropuestasDemoTest {

    private static final LocalDate CHECK_IN = LocalDate.of(2027, 1, 10);
    private static final LocalDate CHECK_OUT = LocalDate.of(2027, 1, 12);

    private final ServicioPropuestasDemo servicio = new ServicioPropuestasDemo();

    @Test
    void coincidenciaExactaDevuelveLaHabitacionCorrectaConSubtotal() {
        // 1 matrimonio + 1 adulto + {Balcon}, 2 noches: 101 (1M+1I + Balcon) cubre 1M+1I exacto.
        ResultadoPropuesta resultado = servicio.generarPropuesta(
                CHECK_IN, CHECK_OUT, 1, 1, 0, Set.of(AtributoHabitacion.Balcon));

        assertTrue(resultado.esCompleta());
        assertEquals(0, resultado.getCamasMatrimonialesFaltantes());
        assertEquals(0, resultado.getCamasIndividualesFaltantes());

        assertEquals(1, resultado.getAsignaciones().size());
        PropuestaAsignada asignada = resultado.getAsignaciones().iterator().next();
        assertEquals(101, asignada.habitacion().getNumero());
        assertEquals(1, asignada.camasMatrimoniales());
        assertEquals(1, asignada.camasIndividuales());
        assertEquals(0, asignada.cunas());

        DTRHPropuesta propuesta = asignada.propuesta();
        assertEquals(3000.0f, propuesta.getSubtotal(), 0.001f); // 2 noches x 1500
        assertEquals(TipoMoneda.USD, propuesta.getMoneda());
        assertEquals(CHECK_IN, propuesta.getFechaInicio());
        assertEquals(CHECK_OUT, propuesta.getFechaFin());
        assertEquals(0, propuesta.getCunas());
        assertEquals(Set.of(AtributoHabitacion.Balcon),
                propuesta.getAtributosHabitacion());
        assertEquals(3000.0f, resultado.getTotal(), 0.001f);
        assertEquals(1, resultado.getPropuestas().size());
    }

    @Test
    void subconjuntoMasGrandeCuandoNoHayCoincidenciaExacta() {
        // {VistaAlMar, Jacuzzi, Balcon}: ninguna habitacion tiene los tres; 102
        // cumple el subconjunto más grande (2) y cubre 1M+1I.
        ResultadoPropuesta resultado = servicio.generarPropuesta(
                CHECK_IN, LocalDate.of(2027, 1, 13), 1, 1, 0,
                Set.of(AtributoHabitacion.VistaAlMar, AtributoHabitacion.Jacuzzi, AtributoHabitacion.Balcon));

        assertTrue(resultado.esCompleta());
        assertEquals(1, resultado.getAsignaciones().size());
        PropuestaAsignada asignada = resultado.getAsignaciones().iterator().next();
        assertEquals(102, asignada.habitacion().getNumero());
        assertEquals(6600.0f, asignada.propuesta().getSubtotal(), 0.001f); // 3 noches x 2200
        Set<AtributoHabitacion> atributos = asignada.propuesta().getAtributosHabitacion();
        assertTrue(atributos.contains(AtributoHabitacion.VistaAlMar));
        assertTrue(atributos.contains(AtributoHabitacion.Jacuzzi));
    }

    @Test
    void necesidadesQueAbarcanVariasHabitaciones() {
        // 1 matrimonio + 3 adultos + {Balcon}, 3 noches: 101 aporta 1M+1I y 103 aporta las 2I restantes.
        ResultadoPropuesta resultado = servicio.generarPropuesta(
                CHECK_IN, LocalDate.of(2027, 1, 13), 1, 3, 0, Set.of(AtributoHabitacion.Balcon));

        assertTrue(resultado.esCompleta());
        assertEquals(2, resultado.getAsignaciones().size());
        List<Integer> numeros = resultado.getAsignaciones().stream()
                .map(a -> a.habitacion().getNumero())
                .toList();
        assertEquals(List.of(101, 103), numeros);
        // 101: 1500x3 = 4500; 103: 1800x3 = 5400.
        assertEquals(9900.0f, resultado.getTotal(), 0.001f);
        assertEquals(2, resultado.getPropuestas().size());
    }

    @Test
    void capacidadInsuficienteReportaFaltantes() {
        // 9 matrimonios + 1 adulto: la capacidad total de camas matrimoniales es 8.
        ResultadoPropuesta resultado = servicio.generarPropuesta(
                CHECK_IN, CHECK_OUT, 9, 1, 0, Set.of());

        assertFalse(resultado.esCompleta());
        assertEquals(1, resultado.getCamasMatrimonialesFaltantes());
        assertEquals(0, resultado.getCamasIndividualesFaltantes());
        assertEquals(6, resultado.getAsignaciones().size()); // todas las habitaciones aportan
        assertEquals(25200.0f, resultado.getTotal(), 0.001f);
        String resumen = resultado.resumen();
        assertTrue(resumen.contains("faltan 1 cama matrimonial"), resumen);
        assertEquals(resumen, resultado.toString());
    }

    @Test
    void ninosOcupanCamasIndividuales() {
        // 1 matrimonio + 1 adulto + 1 niño -> 1M + 2I. Sin atributos, el orden
        // por tarifa da 101 (1M+1I) y luego 103 (1I restante).
        ResultadoPropuesta resultado = servicio.generarPropuesta(
                CHECK_IN, CHECK_OUT, 1, 1, 1, Set.of());

        assertTrue(resultado.esCompleta());
        assertEquals(2, resultado.getAsignaciones().size());
        List<Integer> numeros = resultado.getAsignaciones().stream()
                .map(a -> a.habitacion().getNumero())
                .toList();
        assertEquals(List.of(101, 103), numeros);
        int totalMatrimoniales = resultado.getAsignaciones().stream()
                .mapToInt(PropuestaAsignada::camasMatrimoniales).sum();
        int totalIndividuales = resultado.getAsignaciones().stream()
                .mapToInt(PropuestaAsignada::camasIndividuales).sum();
        assertEquals(1, totalMatrimoniales);
        assertEquals(2, totalIndividuales);
        assertEquals(6600.0f, resultado.getTotal(), 0.001f); // 3000 + 3600
    }

    @Test
    void entradaInvalidaLanzaExcepcion() {
        LocalDate despues = LocalDate.of(2027, 1, 15);

        // Fechas: check-out no posterior al check-in.
        assertThrows(IllegalArgumentException.class,
                () -> servicio.generarPropuesta(despues, CHECK_IN, 1, 1, 0, Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.generarPropuesta(CHECK_IN, CHECK_IN, 1, 1, 0, Set.of()));
        // Fechas nulas.
        assertThrows(IllegalArgumentException.class,
                () -> servicio.generarPropuesta(null, CHECK_OUT, 1, 1, 0, Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.generarPropuesta(CHECK_IN, null, 1, 1, 0, Set.of()));
        // Cantidades negativas.
        assertThrows(IllegalArgumentException.class,
                () -> servicio.generarPropuesta(CHECK_IN, CHECK_OUT, -1, 1, 0, Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.generarPropuesta(CHECK_IN, CHECK_OUT, 1, -1, 0, Set.of()));
        assertThrows(IllegalArgumentException.class,
                () -> servicio.generarPropuesta(CHECK_IN, CHECK_OUT, 1, 1, -1, Set.of()));
        // El helper estático valida igual.
        assertThrows(IllegalArgumentException.class,
                () -> ServicioPropuestasDemo.validar(despues, CHECK_IN, 1, 1, 0));
    }

    @Test
    void atributosNulosSeTratanComoVacios() {
        ResultadoPropuesta resultado = servicio.generarPropuesta(CHECK_IN, CHECK_OUT, 1, 1, 0, null);

        assertTrue(resultado.esCompleta());
        assertEquals(1, resultado.getAsignaciones().size());
        PropuestaAsignada asignada = resultado.getAsignaciones().iterator().next();
        assertEquals(101, asignada.habitacion().getNumero());
    }
}