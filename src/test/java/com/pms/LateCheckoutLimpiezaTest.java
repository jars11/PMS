package com.pms;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.entities.Habitacion;
import com.pms.entities.Reserva;
import com.pms.entities.TitularReserva;
import com.pms.persistence.JpaUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Persistence round-trips for the two new defaulted booleans and the creation
 * guard they enable. Runs against the in-memory {@code pms-test} unit.
 */
class LateCheckoutLimpiezaTest {

    private static final LocalDate INI = LocalDate.of(2026, 11, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 11, 4);

    private final ControladorReservas cr = ControladorReservas.getInstance();
    private final ControladorHabitaciones ch = ControladorHabitaciones.getInstance();

    @BeforeAll
    static void antesDeTodo() {
        JpaUtil.setUnidadPorDefecto("pms-test");
    }

    @BeforeEach
    void limpiar() {
        TestUtil.limpiarBase();
        TestUtil.sembrarHabitaciones();
    }

    private static long idHabitacion(int numero) {
        return ControladorHabitaciones.getInstance().getHabitaciones().stream()
                .filter(h -> h.getNumero() == numero)
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private static TitularReserva altaTitular() {
        return ControladorPersonas.getInstance().altaTitular(
                List.of("Agustin"), List.of("Rivero"), "091000000", "22000000", "agustin@example.com");
    }

    private static Reserva cargarReserva(long id) {
        return JpaUtil.enTransaccion(em -> em.find(Reserva.class, id));
    }

    private static Habitacion cargarHabitacion(long id) {
        return JpaUtil.enTransaccion(em -> em.find(Habitacion.class, id));
    }

    // ---------------------------------------------------------- WU1: persistence

    @Test
    void lateCheckoutPorDefectoFalseYRoundTrip() {
        Reserva r = cr.crearReservaPara(altaTitular(), idHabitacion(103), INI, FIN, 1, 1, 0);

        assertFalse(r.isLateCheckout(), "por defecto false al crear");
        assertFalse(cargarReserva(r.getId()).isLateCheckout(), "false sobrevive el reload");
    }

    @Test
    void lateCheckoutTrueSobreviveRoundTrip() {
        Reserva r = cr.crearReservaPara(altaTitular(), idHabitacion(103), INI, FIN, 1, 1, 0);

        JpaUtil.enTransaccion(em -> {
            em.find(Reserva.class, r.getId()).setLateCheckout(true);
            return null;
        });

        assertTrue(cargarReserva(r.getId()).isLateCheckout(), "true sobrevive el reload");
    }

    @Test
    void limpiaPorDefectoTrueYRoundTrip() {
        ch.altaHabitacion(105, Set.of(), 1, 1, 0, 0, 1000.0f);
        Habitacion h = ch.getHabitaciones().stream()
                .filter(x -> x.getNumero() == 105).findFirst().orElseThrow();

        assertTrue(h.isLimpia(), "habitacion nueva esta limpia por defecto");
        assertTrue(cargarHabitacion(h.getId()).isLimpia(), "true sobrevive el reload");
    }

    @Test
    void limpiaFalseSobreviveRoundTrip() {
        ch.altaHabitacion(105, Set.of(), 1, 1, 0, 0, 1000.0f);
        long id = ch.getHabitaciones().stream()
                .filter(x -> x.getNumero() == 105).findFirst().orElseThrow().getId();

        JpaUtil.enTransaccion(em -> {
            em.find(Habitacion.class, id).setLimpia(false);
            return null;
        });

        assertFalse(cargarHabitacion(id).isLimpia(), "false sobrevive el reload");
    }

    // ------------------------------------------- WU3: creation guard (rule 2)

    private static void marcarLateCheckout(long reservaId) {
        JpaUtil.enTransaccion(em -> {
            em.find(Reserva.class, reservaId).setLateCheckout(true);
            return null;
        });
    }

    private static void marcarLimpia(int numero, boolean limpia) {
        JpaUtil.enTransaccion(em -> {
            em.find(Habitacion.class, idHabitacion(numero)).setLimpia(limpia);
            return null;
        });
    }

    @Test
    void crearReservaParaBloqueaCuandoLateCheckoutTerminaCheckInYNoEstaLimpia() {
        long id103 = idHabitacion(103);
        Reserva previa = cr.crearReservaPara(altaTitular(), id103, INI, FIN, 1, 1, 0);
        marcarLateCheckout(previa.getId());
        marcarLimpia(103, false);

        assertThrows(IllegalStateException.class,
                () -> cr.crearReservaPara(altaTitular(), id103, FIN, FIN.plusDays(2), 1, 1, 0),
                "debe rechazar por limpieza (late checkout)");

        assertEquals(1, cr.getReservas().size(), "nada nuevo se persiste");
    }

    @Test
    void crearReservaParaPermiteCuandoLaHabitacionEstaLimpia() {
        long id103 = idHabitacion(103);
        Reserva previa = cr.crearReservaPara(altaTitular(), id103, INI, FIN, 1, 1, 0);
        marcarLateCheckout(previa.getId()); // limpia queda true por defecto

        Reserva nueva = cr.crearReservaPara(altaTitular(), id103, FIN, FIN.plusDays(2), 1, 1, 0);

        assertNotNull(nueva.getId());
        assertEquals(2, cr.getReservas().size());
    }

    @Test
    void crearReservaParaNoBloqueaSinLateCheckout() {
        long id103 = idHabitacion(103);
        cr.crearReservaPara(altaTitular(), id103, INI, FIN, 1, 1, 0);
        marcarLimpia(103, false); // sucia, pero la reserva previa NO es late check-out

        Reserva nueva = cr.crearReservaPara(altaTitular(), id103, FIN, FIN.plusDays(2), 1, 1, 0);

        assertNotNull(nueva.getId());
        assertEquals(2, cr.getReservas().size());
    }

    @Test
    void crearReservaAutomaticaBloqueaCuandoLateCheckoutTerminaCheckInYNoEstaLimpia() {
        TitularReserva titular = altaTitular();
        long id103 = idHabitacion(103);
        Reserva previa = cr.crearReservaPara(titular, id103, INI, FIN, 1, 1, 0);
        marcarLateCheckout(previa.getId());
        marcarLimpia(103, false);

        // Ocupar todas las demas habitaciones en [FIN, FIN+2) para forzar la 103.
        for (int numero : new int[]{101, 102, 104, 201, 202}) {
            cr.crearReservaPara(titular, idHabitacion(numero), FIN, FIN.plusDays(2), 1, 0, 0);
        }

        assertThrows(IllegalStateException.class,
                () -> cr.crearReservaAutomatica(titular, FIN, FIN.plusDays(2), 1, 0, 0, 0, Set.of()));

        assertEquals(6, cr.getReservas().size(), "nada nuevo se persiste");
    }

    @Test
    void crearReservaAutomaticaPermiteCuandoLaHabitacionEstaLimpia() {
        TitularReserva titular = altaTitular();
        long id103 = idHabitacion(103);
        Reserva previa = cr.crearReservaPara(titular, id103, INI, FIN, 1, 1, 0);
        marcarLateCheckout(previa.getId()); // limpia true

        for (int numero : new int[]{101, 102, 104, 201, 202}) {
            cr.crearReservaPara(titular, idHabitacion(numero), FIN, FIN.plusDays(2), 1, 0, 0);
        }

        Reserva nueva = cr.crearReservaAutomatica(titular, FIN, FIN.plusDays(2), 1, 0, 0, 0, Set.of());

        assertNotNull(nueva.getId());
        assertEquals(7, cr.getReservas().size());
    }

    // ------------------------------- WU4: late checkout day read + same-day guard

    @Test
    void hayLateCheckoutElDiaSoloEnLaFechaDeCheckout() {
        long id103 = idHabitacion(103);
        Reserva previa = cr.crearReservaPara(altaTitular(), id103, INI, FIN, 1, 1, 0);
        marcarLateCheckout(previa.getId());

        assertTrue(cr.hayLateCheckoutElDia(id103, FIN), "true en el dia de salida");
        assertFalse(cr.hayLateCheckoutElDia(id103, FIN.minusDays(1)), "false el dia previo");
        assertFalse(cr.hayLateCheckoutElDia(id103, FIN.plusDays(1)), "false el dia siguiente");
        assertFalse(cr.hayLateCheckoutElDia(idHabitacion(101), FIN), "false en otra habitacion");
    }

    @Test
    void marcarLateCheckoutRechazaCuandoOtraReservaHaceCheckInElMismoDia() {
        long id103 = idHabitacion(103);
        TitularReserva titular = altaTitular();
        Reserva previa = cr.crearReservaPara(titular, id103, INI, FIN, 1, 1, 0);
        cr.crearReservaPara(titular, id103, FIN, FIN.plusDays(2), 1, 0, 0);

        assertThrows(IllegalStateException.class,
                () -> cr.marcarLateCheckout(previa.getId(), true),
                "no se puede marcar late check-out con un check-in el mismo dia");

        assertFalse(cargarReserva(previa.getId()).isLateCheckout(), "el flag no cambia");
    }

    @Test
    void marcarLateCheckoutPermiteCuandoNoHayCheckInElMismoDia() {
        long id103 = idHabitacion(103);
        Reserva previa = cr.crearReservaPara(altaTitular(), id103, INI, FIN, 1, 1, 0);

        cr.marcarLateCheckout(previa.getId(), true);

        assertTrue(cargarReserva(previa.getId()).isLateCheckout(), "el flag se persiste");
    }

    @Test
    void marcarLateCheckoutFalseSiempreSePermite() {
        long id103 = idHabitacion(103);
        TitularReserva titular = altaTitular();
        Reserva previa = cr.crearReservaPara(titular, id103, INI, FIN, 1, 1, 0);
        marcarLateCheckout(previa.getId()); // true directo, sin pasar por la validacion
        cr.crearReservaPara(titular, id103, FIN, FIN.plusDays(2), 1, 0, 0); // check-in el mismo dia

        cr.marcarLateCheckout(previa.getId(), false);

        assertFalse(cargarReserva(previa.getId()).isLateCheckout(), "destildar siempre se permite");
    }
}
