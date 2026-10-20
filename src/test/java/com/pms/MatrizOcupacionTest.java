package com.pms;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.controllers.ControladorReservas.OcupacionHabitacion;
import com.pms.dtos.PerfilRelajacion;
import com.pms.entities.ContratoReserva;
import com.pms.entities.Habitacion;
import com.pms.entities.RHPropuesta;
import com.pms.entities.Reserva;
import com.pms.entities.Solicitud;
import com.pms.entities.TitularReserva;
import com.pms.persistence.JpaUtil;
import com.pms.persistence.RepositorioRHPropuestas;
import com.pms.utils.EstadoReserva;
import com.pms.utils.TipoMoneda;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Slice 4: per-night occupancy matrix over H2. Verifica que solo las reservas
 * ACTIVAS ocupan noches, la semantica [inicio, fin) y los bordes de ventana.
 */
class MatrizOcupacionTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);

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

    private static OcupacionHabitacion porNumero(List<OcupacionHabitacion> matriz, int numero) {
        return matriz.stream()
                .filter(o -> o.habitacion().getNumero() == numero)
                .findFirst()
                .orElseThrow();
    }

    /**
     * Persiste una reserva NO activa directamente: {@code crearReservaPara}
     * siempre activa, asi que este helper arma la Reserva a mano para probar el
     * filtro {@code reserva.activa}.
     */
    private static void sembrarReservaInactiva(long habitacionId, LocalDate inicio, LocalDate fin) {
        TitularReserva titular = altaTitular();
        JpaUtil.enTransaccion(em -> {
            Habitacion hab = em.find(Habitacion.class, habitacionId);
            TitularReserva gestionado = em.merge(titular);
            Reserva r = new Reserva(gestionado);
            RHPropuesta p = new RHPropuesta(inicio, fin, hab.getAtributos(), 1000.0f,
                    TipoMoneda.USD, 1, 0, 0);
            p.setHabitacion(hab);
            r.addRHPropuesta(p);
            r.setActiva(false);
            r.setMontoTotal(1000.0f);
            r.setMontoSenia(0.0f);
            r.setMoneda(TipoMoneda.USD);
            r.setEstado(EstadoReserva.PendienteDePago);
            // Toda reserva persistida queda vinculada por un contrato (cardinalidad 1:1).
            Solicitud s = new Solicitud(gestionado);
            s.addContrato(new ContratoReserva(s, r, PerfilRelajacion.POR_DEFECTO));
            em.persist(r);
            em.persist(s);
            return null;
        });
    }

    @Test
    void reservaActivaOcupaNochesInicioInclusiveFinExclusive() {
        ControladorReservas cr = ControladorReservas.getInstance();
        cr.crearReservaPara(altaTitular(), idHabitacion(103),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0);

        OcupacionHabitacion hab103 = porNumero(cr.matrizOcupacion(INICIO, INICIO.plusDays(14)), 103);

        assertEquals(Set.of(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3)),
                hab103.nochesOcupadas());
    }

    @Test
    void reservaInactivaNoOcupa() {
        ControladorReservas cr = ControladorReservas.getInstance();
        sembrarReservaInactiva(idHabitacion(103), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4));

        OcupacionHabitacion hab103 = porNumero(cr.matrizOcupacion(INICIO, INICIO.plusDays(14)), 103);

        assertTrue(hab103.nochesOcupadas().isEmpty());
    }

    @Test
    void bordesDeVentanaNocheCheckInOcupadaYCheckOutLibre() {
        ControladorReservas cr = ControladorReservas.getInstance();
        cr.crearReservaPara(altaTitular(), idHabitacion(103),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0);

        Set<LocalDate> noches = porNumero(cr.matrizOcupacion(INICIO, INICIO.plusDays(14)), 103)
                .nochesOcupadas();

        assertTrue(noches.contains(LocalDate.of(2026, 11, 1)), "check-in ocupa");
        assertTrue(noches.contains(LocalDate.of(2026, 11, 3)), "noche previa al check-out ocupa");
        assertFalse(noches.contains(LocalDate.of(2026, 11, 4)), "check-out no ocupa");
    }

    @Test
    void habitacionesSinReservasTienenConjuntoVacio() {
        ControladorReservas cr = ControladorReservas.getInstance();
        cr.crearReservaPara(altaTitular(), idHabitacion(103),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0);

        List<OcupacionHabitacion> matriz = cr.matrizOcupacion(INICIO, INICIO.plusDays(14));

        assertEquals(6, matriz.size());
        for (OcupacionHabitacion o : matriz) {
            if (o.habitacion().getNumero() != 103) {
                assertTrue(o.nochesOcupadas().isEmpty(), "hab " + o.habitacion().getNumero());
            }
        }
    }

    @Test
    void findActivasEnRangoSoloTraePropuestasDeReservasActivas() {
        sembrarReservaInactiva(idHabitacion(103), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4));
        ControladorReservas.getInstance().crearReservaPara(altaTitular(), idHabitacion(101),
                LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 5), 1, 0, 0);

        RepositorioRHPropuestas repo = new RepositorioRHPropuestas();

        // findEnRango (sin filtro) ve ambas; findActivasEnRango solo la activa.
        assertEquals(2, repo.findEnRango(INICIO, INICIO.plusDays(14)).size());
        assertEquals(1, repo.findActivasEnRango(INICIO, INICIO.plusDays(14)).size());
    }

    /**
     * Test 23: la matriz de ocupacion expone, por noche, el id de la reserva que
     * la ocupa; las noches libres o fuera de rango devuelven null.
     */
    @Test
    void matrizOcupacionExponeReservaPorNoche() {
        ControladorReservas cr = ControladorReservas.getInstance();
        Reserva creada = cr.crearReservaPara(altaTitular(), idHabitacion(103),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0);

        OcupacionHabitacion hab103 = porNumero(cr.matrizOcupacion(INICIO, INICIO.plusDays(14)), 103);

        assertEquals(creada.getId(), hab103.reservaEn(LocalDate.of(2026, 11, 1)));
        assertEquals(creada.getId(), hab103.reservaEn(LocalDate.of(2026, 11, 3)));
        assertNull(hab103.reservaEn(LocalDate.of(2026, 11, 4)), "check-out no ocupa");
        assertNull(hab103.reservaEn(LocalDate.of(2026, 11, 5)), "fuera de la reserva");
    }

    @Test
    void matrizOcupacionRechazaRangoInvalido() {
        ControladorReservas cr = ControladorReservas.getInstance();

        assertThrows(IllegalArgumentException.class, () -> cr.matrizOcupacion(INICIO, INICIO));
        assertThrows(IllegalArgumentException.class, () -> cr.matrizOcupacion(INICIO, INICIO.minusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> cr.matrizOcupacion(null, INICIO));
    }

    // --------------------------------------- Slice 6: blocks for the bar

    @Test
    void matrizOcupacionExponeBloquesParaLaBarra() {
        ControladorReservas cr = ControladorReservas.getInstance();
        Reserva creada = cr.crearReservaPara(altaTitular(), idHabitacion(103),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0);

        OcupacionHabitacion hab103 = porNumero(cr.matrizOcupacion(INICIO, INICIO.plusDays(14)), 103);

        assertEquals(1, hab103.bloques().size());
        OcupacionHabitacion.BloqueNoche b = hab103.bloques().get(0);
        assertEquals(creada.getId(), b.reservaId());
        assertEquals(LocalDate.of(2026, 11, 1), b.fechaInicio());
        assertEquals(LocalDate.of(2026, 11, 4), b.fechaFin());
        assertFalse(b.lateCheckout());
    }

    @Test
    void matrizOcupacionBloqueReflejaLateCheckout() {
        ControladorReservas cr = ControladorReservas.getInstance();
        Reserva creada = cr.crearReservaPara(altaTitular(), idHabitacion(103),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0);
        JpaUtil.enTransaccion(em -> {
            em.find(Reserva.class, creada.getId()).setLateCheckout(true);
            return null;
        });

        OcupacionHabitacion hab103 = porNumero(cr.matrizOcupacion(INICIO, INICIO.plusDays(14)), 103);

        assertEquals(1, hab103.bloques().size());
        assertTrue(hab103.bloques().get(0).lateCheckout());
    }
}
