package com.pms;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.controllers.ControladorReservas.ResultadoCompatibilidad;
import com.pms.controllers.ControladorReservas.ResultadoMovimiento;
import com.pms.entities.Habitacion;
import com.pms.entities.RHPropuesta;
import com.pms.entities.Reserva;
import com.pms.entities.TitularReserva;
import com.pms.persistence.JpaUtil;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.MotivoRechazo;
import com.pms.utils.TipoMoneda;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@code moverReserva}/{@code compatibilidadMovimiento}
 * over the in-memory {@code pms-test} unit. ONE pure predicate decides
 * compatibility, so every rejection surfaces as a {@link MotivoRechazo}; on any
 * rejection NOTHING may mutate.
 *
 * <p>CAPACIDAD_INSUFICIENTE is unreachable through real persisted rooms (the bed
 * distribution check dominates the derived capacity) and is covered by
 * {@code ui/CompatibilidadMovimientoTest}.
 */
class ControladorReservasMovimientoTest {

    private static final LocalDate D0 = LocalDate.of(2026, 11, 1);
    private static final LocalDate D1 = LocalDate.of(2026, 11, 2);
    private static final LocalDate D2 = LocalDate.of(2026, 11, 3);
    private static final LocalDate D3 = LocalDate.of(2026, 11, 4);
    private static final LocalDate D4 = LocalDate.of(2026, 11, 5);

    private final ControladorReservas cr = ControladorReservas.getInstance();

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
        return JpaUtil.enTransaccion(em -> {
            Reserva r = em.createQuery(
                            "select distinct r from Reserva r left join fetch r.rhPropuestas p " +
                                    "left join fetch p.habitacion where r.id = :id", Reserva.class)
                    .setParameter("id", id)
                    .getResultList().stream().findFirst().orElse(null);
            return r;
        });
    }

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

    private static MotivoRechazo motivoDe(List<ResultadoCompatibilidad> lista, long habitacionId) {
        return lista.stream()
                .filter(x -> x.habitacionId() == habitacionId)
                .findFirst()
                .orElseThrow()
                .motivo();
    }

    private static RHPropuesta propuestaDesacoplada(int numero, LocalDate ini, LocalDate fin,
                                                    float subtotal, int m, int i, int cunas) {
        Habitacion h = ControladorHabitaciones.getInstance().getHabitaciones().stream()
                .filter(x -> x.getNumero() == numero).findFirst().orElseThrow();
        RHPropuesta p = new RHPropuesta(ini, fin, h.getAtributos(), subtotal,
                TipoMoneda.USD, m, i, cunas);
        p.setHabitacion(h);
        return p;
    }

    // ------------------------------------------------------------- happy path

    @Test
    void moverReservaCambiaHabitacionMantieneFechasYRecalculaTotal() {
        long h101 = idHabitacion(101);
        long h103 = idHabitacion(103);
        Reserva original = cr.crearReservaPara(altaTitular(), h101, D1, D4, 1, 1, 0); // 3 x 1500

        ResultadoMovimiento res = cr.moverReserva(original.getId(), h101, D1, h103);

        assertTrue(res.movida());
        assertNull(res.motivo());

        Reserva recargada = cargarReserva(original.getId());
        assertEquals(1, recargada.getRHPropuestas().size());
        RHPropuesta p = recargada.getRHPropuestas().iterator().next();
        assertEquals(103, p.getHabitacion().getNumero().intValue());
        assertEquals(D1, p.getFechaInicio(), "las fechas son fijas");
        assertEquals(D4, p.getFechaFin(), "las fechas son fijas");
        assertEquals(5400.0f, p.getSubtotal(), 0.001f);       // 3 x 1800 (hab 103)
        assertEquals(5400.0f, recargada.getMontoTotal(), 0.001f);
        // El origen queda libre.
        assertFalse(cr.isHabitacionOcupada(h101, D1, D4));
    }

    @Test
    void moverReservaSoloTocaLaPropuestaDeLaNocheClickeada() {
        TitularReserva titular = altaTitular();
        Set<RHPropuesta> propuestas = new LinkedHashSet<>();
        propuestas.add(propuestaDesacoplada(101, D1, D2, 1500.0f, 1, 0, 0)); // 1 noche x 1500
        propuestas.add(propuestaDesacoplada(202, D1, D4, 4800.0f, 2, 2, 0)); // 2 noches x 2400
        Reserva original = cr.persistirPropuestas(titular, propuestas);
        long h101 = idHabitacion(101);
        long h103 = idHabitacion(103);

        ResultadoMovimiento res = cr.moverReserva(original.getId(), h101, D1, h103);

        assertTrue(res.movida());
        Reserva recargada = cargarReserva(original.getId());
        assertEquals(2, recargada.getRHPropuestas().size());
        RHPropuesta movida = recargada.getRHPropuestas().stream()
                .filter(x -> x.getHabitacion().getNumero() == 103).findFirst().orElseThrow();
        assertEquals(D1, movida.getFechaInicio());
        assertEquals(D2, movida.getFechaFin());
        assertEquals(1800.0f, movida.getSubtotal(), 0.001f); // 1 noche x 1800
        RHPropuesta intacta = recargada.getRHPropuestas().stream()
                .filter(x -> x.getHabitacion().getNumero() == 202).findFirst().orElseThrow();
        assertEquals(4800.0f, intacta.getSubtotal(), 0.001f);
        assertEquals(6600.0f, recargada.getMontoTotal(), 0.001f);
    }

    // -------------------------------------------------------------- rejections

    @Test
    void atributosFaltantes() {
        long h102 = idHabitacion(102);
        long h104 = idHabitacion(104);
        Reserva original = cr.crearReservaPara(altaTitular(), h102, D1, D4, 1, 1, 0);

        ResultadoMovimiento res = cr.moverReserva(original.getId(), h102, D1, h104);

        assertFalse(res.movida());
        assertEquals(MotivoRechazo.ATRIBUTOS_FALTANTES, res.motivo());
        assertSinMutacion(original.getId(), h102, 6600.0f); // 3 x 2200 (hab 102)
    }

    @Test
    void distribucionCamas() {
        long h201 = idHabitacion(201);
        long h103 = idHabitacion(103);
        Reserva original = cr.crearReservaPara(altaTitular(), h201, D1, D4, 2, 0, 0); // 2 matrimoniales

        ResultadoMovimiento res = cr.moverReserva(original.getId(), h201, D1, h103);

        assertFalse(res.movida());
        assertEquals(MotivoRechazo.DISTRIBUCION_CAMAS, res.motivo());
        assertSinMutacion(original.getId(), h201, 7800.0f); // 3 x 2600 (hab 201)
    }

    @Test
    void noDisponible() {
        long h101 = idHabitacion(101);
        long h103 = idHabitacion(103);
        TitularReserva titular = altaTitular();
        Reserva original = cr.crearReservaPara(titular, h101, D1, D4, 1, 1, 0);
        cr.crearReservaPara(titular, h103, D2, D3, 1, 0, 0); // ocupa la noche D2 en 103

        ResultadoMovimiento res = cr.moverReserva(original.getId(), h101, D1, h103);

        assertFalse(res.movida());
        assertEquals(MotivoRechazo.NO_DISPONIBLE, res.motivo());
        assertSinMutacion(original.getId(), h101, 4500.0f); // 3 x 1500 (hab 101)
    }

    @Test
    void solapeLateCheckout() {
        long h101 = idHabitacion(101);
        long h103 = idHabitacion(103);
        TitularReserva titular = altaTitular();
        Reserva original = cr.crearReservaPara(titular, h101, D1, D4, 1, 1, 0);
        marcarLateCheckout(original.getId());           // sale D4 con late check-out
        cr.crearReservaPara(titular, h103, D4, D4.plusDays(2), 1, 0, 0); // check-in en D4 en 103

        ResultadoMovimiento res = cr.moverReserva(original.getId(), h101, D1, h103);

        assertFalse(res.movida());
        assertEquals(MotivoRechazo.SOLAPE_LATE_CHECKOUT, res.motivo());
        assertSinMutacion(original.getId(), h101, 4500.0f); // 3 x 1500 (hab 101)
    }

    @Test
    void limpiezaLateCheckout() {
        long h101 = idHabitacion(101);
        long h103 = idHabitacion(103);
        TitularReserva titular = altaTitular();
        Reserva original = cr.crearReservaPara(titular, h101, D1, D4, 1, 1, 0);
        Reserva previa = cr.crearReservaPara(titular, h103, D0, D1, 1, 0, 0); // termina D1
        marcarLateCheckout(previa.getId());
        marcarLimpia(103, false);

        ResultadoMovimiento res = cr.moverReserva(original.getId(), h101, D1, h103);

        assertFalse(res.movida());
        assertEquals(MotivoRechazo.LIMPIEZA_LATE_CHECKOUT, res.motivo());
        assertSinMutacion(original.getId(), h101, 4500.0f); // 3 x 1500 (hab 101)
    }

    @Test
    void moverReservaRechazaDestinoInexistente() {
        long h101 = idHabitacion(101);
        Reserva original = cr.crearReservaPara(altaTitular(), h101, D1, D4, 1, 1, 0);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> cr.moverReserva(original.getId(), h101, D1, 9999L));
        assertSinMutacion(original.getId(), h101, 4500.0f); // 3 x 1500 (hab 101)
    }

    /** El rechazo no deja rastro: la reserva conserva origen, fechas y total. */
    private void assertSinMutacion(long reservaId, long habitacionOrigenId, float totalEsperado) {
        Reserva recargada = cargarReserva(reservaId);
        assertEquals(1, recargada.getRHPropuestas().size());
        RHPropuesta p = recargada.getRHPropuestas().iterator().next();
        assertEquals(habitacionOrigenId, p.getHabitacion().getId());
        assertEquals(D1, p.getFechaInicio());
        assertEquals(D4, p.getFechaFin());
        assertEquals(totalEsperado, recargada.getMontoTotal(), 0.001f);
    }

    // ---------------------------------------------------- compatibilidad (greying)

    @Test
    void compatibilidadMovimientoMarcaCadaHabitacion() {
        long h101 = idHabitacion(101);
        Reserva original = cr.crearReservaPara(altaTitular(), h101, D1, D4, 1, 1, 0);

        List<ResultadoCompatibilidad> estados =
                cr.compatibilidadMovimiento(original.getId(), h101, D1);

        assertEquals(6, estados.size(), "una entrada por habitacion");
        assertNull(motivoDe(estados, idHabitacion(103)), "103 tiene Balcon y capacidad");
        assertNull(motivoDe(estados, idHabitacion(201)), "201 tiene Balcon y capacidad");
        assertEquals(MotivoRechazo.ATRIBUTOS_FALTANTES, motivoDe(estados, idHabitacion(102)));
        assertEquals(MotivoRechazo.ATRIBUTOS_FALTANTES, motivoDe(estados, idHabitacion(104)));
        assertEquals(MotivoRechazo.ATRIBUTOS_FALTANTES, motivoDe(estados, idHabitacion(202)));
    }

    @Test
    void compatibilidadMovimientoDetectaNoDisponible() {
        long h101 = idHabitacion(101);
        long h103 = idHabitacion(103);
        TitularReserva titular = altaTitular();
        Reserva original = cr.crearReservaPara(titular, h101, D1, D4, 1, 1, 0);
        cr.crearReservaPara(titular, h103, D2, D3, 1, 0, 0);

        List<ResultadoCompatibilidad> estados =
                cr.compatibilidadMovimiento(original.getId(), h101, D1);

        assertEquals(MotivoRechazo.NO_DISPONIBLE, motivoDe(estados, h103));
    }
}
