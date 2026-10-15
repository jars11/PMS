package com.pms.ui;

import com.pms.TestUtil;
import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.controllers.ControladorReservas.OcupacionHabitacion;
import com.pms.controllers.ControladorReservas.ReservaVista;
import com.pms.entities.Habitacion;
import com.pms.entities.TitularReserva;
import com.pms.persistence.JpaUtil;
import com.pms.persistence.RepositorioPersonas;
import com.pms.utils.AtributoHabitacion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del sembrado no destructivo de la GUI. Verifica idempotencia y el
 * perfil de ocupacion demo (~80%) bajo H2 (in-memory) sin instanciar Swing, por
 * lo que corre headless.
 */
class SemillaDemoTest {

    /** Noches de la ventana demo: [hoy, hoy+14). */
    private static final int VENTANA_NOCHES = 14;

    private final ControladorHabitaciones ch = ControladorHabitaciones.getInstance();
    private final RepositorioPersonas repositorioPersonas = new RepositorioPersonas();

    @BeforeAll
    static void antesDeTodo() {
        JpaUtil.setUnidadPorDefecto("pms-test");
    }

    @BeforeEach
    void limpiar() {
        TestUtil.limpiarBase();
    }

    /** Matriz de ocupacion de la ventana demo anclada a hoy. */
    private static List<OcupacionHabitacion> matrizVentana() {
        LocalDate desde = LocalDate.now();
        return ControladorReservas.getInstance().matrizOcupacion(desde, desde.plusDays(VENTANA_NOCHES));
    }

    /** Suma de noches-habitacion ocupadas en la matriz. */
    private static int nochesOcupadasTotales(List<OcupacionHabitacion> matriz) {
        int total = 0;
        for (OcupacionHabitacion oh : matriz) {
            total += oh.nochesOcupadas().size();
        }
        return total;
    }

    /**
     * Mayor racha de noches libres contiguas de una habitacion dentro de la
     * ventana [desde, desde+noches).
     */
    private static int mayorRachaLibre(Set<LocalDate> ocupadas, LocalDate desde, int noches) {
        int mejor = 0;
        int actual = 0;
        for (int i = 0; i < noches; i++) {
            if (ocupadas.contains(desde.plusDays(i))) {
                actual = 0;
            } else {
                actual++;
                if (actual > mejor) {
                    mejor = actual;
                }
            }
        }
        return mejor;
    }

    @Test
    void sembrarSiVacioSiembraCuandoLaBaseEstaVacia() {
        SemillaDemo.sembrarSiVacio();

        assertEquals(6, ch.getHabitaciones().size());
        assertEquals(1, repositorioPersonas.findAll().size());
    }

    @Test
    void sembrarSiVacioEsIdempotenteAlLlamarloDosVeces() {
        SemillaDemo.sembrarSiVacio();
        SemillaDemo.sembrarSiVacio();

        assertEquals(6, ch.getHabitaciones().size());
        assertEquals(1, repositorioPersonas.findAll().size());
    }

    @Test
    void sembrarSiVacioNoDuplicaCuandoYaHayDatos() {
        ch.altaHabitacion(999, Set.of(AtributoHabitacion.Balcon), 1, 0, 0, 0, 1000.0f);

        SemillaDemo.sembrarSiVacio();

        assertEquals(1, ch.getHabitaciones().size());
        assertEquals(999, ch.getHabitaciones().get(0).getNumero());
        assertEquals(0, repositorioPersonas.findAll().size());
    }

    @Test
    void sembrarReservasDemoSeedsRoughlyEightyPercentOccupancy() {
        TestUtil.sembrarHabitaciones();

        SemillaDemo.sembrarReservasDemo();

        List<OcupacionHabitacion> matriz = matrizVentana();
        int habitaciones = matriz.size();
        assertTrue(habitaciones > 0, "debe haber habitaciones sembradas");

        double ratio = (double) nochesOcupadasTotales(matriz) / (habitaciones * VENTANA_NOCHES);
        assertTrue(ratio >= 0.75 && ratio <= 0.85,
                "ocupacion esperada 75%-85%, fue " + ratio);
    }

    @Test
    void sembrarReservasDemoLeavesAtLeastTwoContiguousFreeNights() {
        TestUtil.sembrarHabitaciones();

        SemillaDemo.sembrarReservasDemo();

        LocalDate desde = LocalDate.now();
        boolean hayHueco = false;
        for (OcupacionHabitacion oh : matrizVentana()) {
            if (mayorRachaLibre(oh.nochesOcupadas(), desde, VENTANA_NOCHES) >= 2) {
                hayHueco = true;
                break;
            }
        }
        assertTrue(hayHueco, "al menos una habitacion debe dejar >=2 noches libres contiguas");
    }

    @Test
    void sembrarReservasDemoCreatesPartialReservationsWithoutPayments() {
        TestUtil.sembrarHabitaciones();

        SemillaDemo.sembrarReservasDemo();

        List<ReservaVista> reservas = ControladorReservas.getInstance().listarReservas();
        assertTrue(reservas.size() >= 2, "debe sembrar varias reservas parciales");
        for (ReservaVista r : reservas) {
            assertEquals(1, r.numerosHabitacion().size(), "una sola habitacion por reserva");
            assertTrue(ControladorReservas.getInstance().mostrarPagos(r.id()).isEmpty(),
                    "reserva parcial sin pagos");
        }
    }

    @Test
    void sembrarReservasDemoKeepsEveryStayInsideWindow() {
        TestUtil.sembrarHabitaciones();

        SemillaDemo.sembrarReservasDemo();

        LocalDate desde = LocalDate.now();
        LocalDate hasta = desde.plusDays(VENTANA_NOCHES);
        for (ReservaVista r : ControladorReservas.getInstance().listarReservas()) {
            assertTrue(r.checkIn() != null && !r.checkIn().isBefore(desde), "check-in dentro de la ventana");
            assertTrue(r.checkOut() != null && !r.checkOut().isAfter(hasta), "check-out dentro de la ventana");
            assertTrue(r.checkIn().isBefore(r.checkOut()), "check-in antes de check-out");
        }
    }

    @Test
    void sembrarReservasDemoEsIdempotente() {
        TestUtil.sembrarHabitaciones();

        SemillaDemo.sembrarReservasDemo();
        int primeraCorrida = ControladorReservas.getInstance().listarReservas().size();
        assertTrue(primeraCorrida > 0, "la primera corrida siembra reservas");

        SemillaDemo.sembrarReservasDemo();
        int segundaCorrida = ControladorReservas.getInstance().listarReservas().size();

        // la segunda corrida no crea nada nuevo (ni reservas ni titulares)
        assertEquals(primeraCorrida, segundaCorrida);
        assertEquals(1, repositorioPersonas.findAllTitulares().size());
    }

    @Test
    void sembrarReservasDemoNoBorraExistentes() {
        TestUtil.sembrarHabitaciones();
        TitularReserva existente = ControladorPersonas.getInstance().altaTitular(
                List.of("Existente"), List.of("Previo"), "099111111", "24001111", "previo@example.com");
        Habitacion hab = ch.getHabitaciones().get(0);
        LocalDate checkIn = LocalDate.now().plusDays(10);
        ControladorReservas.getInstance().crearReservaPara(existente, hab.getId(),
                checkIn, checkIn.plusDays(2), 1, 0, 0);

        SemillaDemo.sembrarReservasDemo();

        List<ReservaVista> reservas = ControladorReservas.getInstance().listarReservas();
        // no-op: la reserva existente sigue intacta y NO se sembro ninguna demo
        assertEquals(1, reservas.size());
        assertEquals(checkIn, reservas.get(0).checkIn());
        assertEquals(Set.of(hab.getNumero()), reservas.get(0).numerosHabitacion());
    }
}
