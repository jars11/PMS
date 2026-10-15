package com.pms;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.controllers.ControladorReservas.ReservaVista;
import com.pms.dtos.DTRHPropuesta;
import com.pms.dtos.DTRHSolicitada;
import com.pms.dtos.DTReserva;
import com.pms.entities.Habitacion;
import com.pms.entities.RHPropuesta;
import com.pms.entities.Pasajero;
import com.pms.entities.Reserva;
import com.pms.entities.TitularReserva;
import com.pms.persistence.JpaUtil;
import com.pms.persistence.RepositorioHabitaciones;
import com.pms.persistence.RepositorioReservas;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.EstadoReserva;
import com.pms.utils.TipoMoneda;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The full Main-demo scenario against the in-memory pms-test unit, plus the
 * payment-guarded deletion rule of eliminarReserva.
 */
class ControladorReservasTest {

    @BeforeAll
    static void antesDeTodo() {
        JpaUtil.setUnidadPorDefecto("pms-test");
    }

    @BeforeEach
    void limpiar() {
        TestUtil.limpiarBase();
        TestUtil.sembrarHabitaciones();
    }

    private static Set<DTRHSolicitada> bloquesDemo() {
        Set<DTRHSolicitada> bloques = new TreeSet<>();
        bloques.add(new DTRHSolicitada(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15),
                Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0));
        bloques.add(new DTRHSolicitada(LocalDate.of(2027, 1, 15), LocalDate.of(2027, 1, 20),
                Set.of(), 1, 0, 1, 1));
        return bloques;
    }

    private static void encolarBloquesDemo(ControladorReservas cr) {
        cr.agregarBloqueReserva(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15), 1, 1, 0, 0,
                Set.of(AtributoHabitacion.Balcon));
        cr.agregarBloqueReserva(LocalDate.of(2027, 1, 15), LocalDate.of(2027, 1, 20), 1, 0, 1, 1,
                Set.of());
    }

    @Test
    void obtenerDisponibilidadDelEscenarioDemo() {
        ControladorReservas cr = ControladorReservas.getInstance();
        encolarBloquesDemo(cr);

        DTReserva d = cr.obtenerDisponibilidad(bloquesDemo());

        assertFalse(d.isActiva());
        assertEquals(15000.0f, d.getMontoTotal(), 0.001f);
        assertEquals(TipoMoneda.USD, d.getMoneda());
        assertEquals(2, d.getDTRHPropuestas().size());

        // Ambos bloques caen en la habitacion 101 (1M+1I + Balcon): 5 noches x 1500.
        for (DTRHPropuesta p : d.getDTRHPropuestas()) {
            assertEquals(Set.of(AtributoHabitacion.Balcon), p.getAtributosHabitacion());
            assertEquals(7500.0f, p.getSubtotal(), 0.001f);
        }
    }

    @Test
    void crearReservaPersisteYDevuelveTitularGestionado() {
        ControladorReservas cr = ControladorReservas.getInstance();
        ControladorPersonas cp = ControladorPersonas.getInstance();

        Pasajero juan = TestUtil.altaPasajero("Juan", "Perez", "5.123.456-7",
                com.pms.utils.TipoDocumento.CEDULA, LocalDate.of(1995, 3, 10));
        Pasajero ana = TestUtil.altaPasajero("Ana", "Lopez", "4.987.654-3",
                com.pms.utils.TipoDocumento.DNI, LocalDate.of(2010, 7, 5));

        TitularReserva titular = TestUtil.demoTitular();
        encolarBloquesDemo(cr);
        TitularReserva gestionado = cr.crearReserva(titular);

        assertNotNull(gestionado.getId());

        Reserva reserva = cr.getReservas().get(0);
        assertTrue(reserva.isActiva());
        assertEquals(15000.0f, reserva.getMontoTotal(), 0.001f);
        assertEquals(TipoMoneda.USD, reserva.getMoneda());
        assertEquals(2, reserva.getRHPropuestas().size());

        cp.vincularPasajeroAReserva(juan, reserva.getId());
        cp.vincularPasajeroAReserva(ana, reserva.getId());

        assertEquals(2, cp.pasajerosDeReserva(reserva.getId()).size());
        assertEquals(2, cp.pasajerosDeReservasDeTitular(gestionado.getId()).size());
        assertTrue(cr.perteneceReservaATitular(reserva.getId(), gestionado.getId()));
    }

    @Test
    void pasajerosPorEdadFiltraPorAniosCumplidos() {
        ControladorPersonas cp = ControladorPersonas.getInstance();
        TestUtil.altaPasajero("Juan", "Perez", "5.123.456-7",
                com.pms.utils.TipoDocumento.CEDULA, LocalDate.of(1995, 3, 10));
        TestUtil.altaPasajero("Ana", "Lopez", "4.987.654-3",
                com.pms.utils.TipoDocumento.DNI, LocalDate.of(2010, 7, 5));

        Set<Pasajero> menores = cp.pasajerosPorEdad(0, 17, LocalDate.of(2027, 1, 1));

        assertEquals(1, menores.size());
        Pasajero ana = menores.iterator().next();
        assertEquals("Ana", ana.getNombres().get(0));
        assertEquals("Lopez", ana.getApellidos().get(0));
    }

    @Test
    void rhPropuestasPorCheckinCheckoutYEnRango() {
        ControladorReservas cr = ControladorReservas.getInstance();
        encolarBloquesDemo(cr);
        cr.crearReserva(TestUtil.demoTitular());

        assertEquals(2, cr.mostrarRHPropuestasPorRangoFechasEnEstadia(
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31)).size());
        assertEquals(1, cr.mostrarRHPropuestasPorCheckin(LocalDate.of(2027, 1, 10)).size());
        assertEquals(1, cr.mostrarRHPropuestasPorCheckout(LocalDate.of(2027, 1, 20)).size());
    }

    @Test
    void mostrarReservasYPorTitular() {
        ControladorReservas cr = ControladorReservas.getInstance();
        encolarBloquesDemo(cr);
        TitularReserva gestionado = cr.crearReserva(TestUtil.demoTitular());

        Set<DTReserva> todas = cr.mostrarReservas();
        assertEquals(1, todas.size());
        DTReserva r = todas.iterator().next();
        assertTrue(r.isActiva());
        assertEquals("PendienteDePago", r.getEstado().name());

        assertEquals(1, cr.mostrarReservasPorTitular(gestionado.getId()).size());
    }

    @Test
    void eliminarReservaConPagosNoBorra() {
        ControladorReservas cr = ControladorReservas.getInstance();
        encolarBloquesDemo(cr);
        cr.crearReserva(TestUtil.demoTitular());
        long reservaId = cr.getReservas().get(0).getId();

        new RepositorioReservas().agregarPago(reservaId, 5000.0f, TipoMoneda.USD, LocalDate.of(2027, 1, 5));

        assertFalse(cr.eliminarReserva(reservaId));
        assertEquals(1, cr.mostrarReservas().size());
        assertEquals(1, cr.mostrarPagos(reservaId).size());
    }

    @Test
    void eliminarReservaSinPagosBorra() {
        ControladorReservas cr = ControladorReservas.getInstance();
        encolarBloquesDemo(cr);
        cr.crearReserva(TestUtil.demoTitular());
        long reservaId = cr.getReservas().get(0).getId();

        assertTrue(cr.eliminarReserva(reservaId));
        assertTrue(cr.mostrarReservas().isEmpty());
    }

    @Test
    void trasCrearReservaSeLimpianLosBloquesPendientes() {
        ControladorReservas cr = ControladorReservas.getInstance();
        encolarBloquesDemo(cr);
        cr.crearReserva(TestUtil.demoTitular());

        // Segundo llamado sin nuevos bloques: lo que exista es lo pendiente.
        DTReserva d = cr.obtenerDisponibilidad(Set.of());
        assertTrue(d.getDTRHPropuestas().isEmpty());
    }

    // --------------------------------------- Slice 3: explicit-room reservations

    private static long idHabitacion(int numero) {
        return ControladorHabitaciones.getInstance().getHabitaciones().stream()
                .filter(h -> h.getNumero() == numero)
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private TitularReserva altaTitularDemo() {
        return ControladorPersonas.getInstance().altaTitular(
                List.of("Agustin"), List.of("Rivero"), "091000000", "22000000", "agustin@example.com");
    }

    @Test
    void crearReservaParaPersisteActivaConSubtotalYPropuesta() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();

        Reserva r = cr.crearReservaPara(titular, idHabitacion(103),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0);

        assertNotNull(r.getId());
        assertTrue(r.isActiva());
        assertEquals(EstadoReserva.PendienteDePago, r.getEstado());
        assertEquals(TipoMoneda.USD, r.getMoneda());
        assertEquals(0.0f, r.getMontoSenia(), 0.001f);
        assertEquals(5400.0f, r.getMontoTotal(), 0.001f); // 3 noches x 1800 (hab 103)

        assertEquals(1, r.getRHPropuestas().size());
        RHPropuesta p = r.getRHPropuestas().iterator().next();
        assertEquals(103, p.getHabitacion().getNumero());
        assertEquals(LocalDate.of(2026, 11, 1), p.getFechaInicio());
        assertEquals(LocalDate.of(2026, 11, 4), p.getFechaFin());
        assertEquals(5400.0f, p.getSubtotal(), 0.001f);

        assertEquals(1, cr.getReservas().size());
    }

    @Test
    void crearReservaParaConAtributosLosGuardaEnLaPropuesta() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();

        // La habitacion 103 tiene {Balcon}; se piden atributos distintos.
        Reserva r = cr.crearReservaPara(titular, idHabitacion(103),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0,
                Set.of(AtributoHabitacion.Jacuzzi));

        assertEquals(1, r.getRHPropuestas().size());
        RHPropuesta p = r.getRHPropuestas().iterator().next();
        assertEquals(Set.of(AtributoHabitacion.Jacuzzi), p.getAtributosHabitacion());

        // Persistido: el overload de 7 argumentos conserva los atributos de la habitacion.
        Reserva r2 = cr.crearReservaPara(titular, idHabitacion(104),
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), 1, 1, 0);
        RHPropuesta p2 = r2.getRHPropuestas().iterator().next();
        assertEquals(Set.of(AtributoHabitacion.VistaAlMar), p2.getAtributosHabitacion());
    }

    @Test
    void crearReservaParaRechazaSolapamientoConReservaActiva() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();
        long id103 = idHabitacion(103);
        cr.crearReservaPara(titular, id103, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0);

        // Comparte la noche 2026-11-02 con la reserva activa.
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> cr.crearReservaPara(titular, id103,
                        LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 3), 1, 0, 0));
        assertTrue(ex.getMessage().contains("ocupada"), ex.getMessage());

        // Tramo identico al borde: [2026-11-01, 2026-11-02) comparte la noche 2026-11-01.
        assertThrows(IllegalStateException.class,
                () -> cr.crearReservaPara(titular, id103,
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2), 1, 0, 0));

        assertEquals(1, cr.getReservas().size());
    }

    @Test
    void crearReservaParaPermiteTramoAdyacenteSinSolape() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();
        long id103 = idHabitacion(103);
        cr.crearReservaPara(titular, id103, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0);

        Reserva r = cr.crearReservaPara(titular, id103,
                LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 6), 1, 1, 0);

        assertNotNull(r.getId());
        assertTrue(r.isActiva());
        assertEquals(3600.0f, r.getMontoTotal(), 0.001f); // 2 noches x 1800
        assertEquals(2, cr.getReservas().size());
    }

    @Test
    void crearReservaParaRechazaFechasYBedsInvalidos() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();
        long id103 = idHabitacion(103);

        // checkOut == checkIn
        assertThrows(IllegalArgumentException.class,
                () -> cr.crearReservaPara(titular, id103,
                        LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 4), 1, 0, 0));
        // checkOut < checkIn
        assertThrows(IllegalArgumentException.class,
                () -> cr.crearReservaPara(titular, id103,
                        LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 4), 1, 0, 0));
        // beds negativas
        assertThrows(IllegalArgumentException.class,
                () -> cr.crearReservaPara(titular, id103,
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), -1, 0, 0));
        // titular nulo
        assertThrows(IllegalArgumentException.class,
                () -> cr.crearReservaPara(null, id103,
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 0, 0));
        // habitacion inexistente
        assertThrows(IllegalArgumentException.class,
                () -> cr.crearReservaPara(titular, 9999L,
                        LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 0, 0));

        assertTrue(cr.getReservas().isEmpty());
    }

    @Test
    void crearReservaParaReutilizaTitularExistente() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();

        Reserva r1 = cr.crearReservaPara(titular, idHabitacion(101),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), 1, 0, 0);
        Reserva r2 = cr.crearReservaPara(titular, idHabitacion(102),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), 1, 0, 0);

        assertEquals(1, ControladorPersonas.getInstance().getTitulares().size());
        assertEquals(r1.getTitular().getId(), r2.getTitular().getId());
    }

    @Test
    void listarReservasExponeTitularHabitacionYFechas() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva agustin = altaTitularDemo();
        TitularReserva maria = ControladorPersonas.getInstance().altaTitular(
                List.of("Maria"), List.of("Perez"), "092000000", "22000001", "maria@example.com");

        cr.crearReservaPara(agustin, idHabitacion(103),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0);
        cr.crearReservaPara(maria, idHabitacion(201),
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 7), 2, 0, 0);

        List<ReservaVista> vistas = cr.listarReservas();

        assertEquals(2, vistas.size());
        ReservaVista agustinVista = vistas.stream()
                .filter(v -> v.titular().equals("Agustin Rivero"))
                .findFirst()
                .orElseThrow();
        assertEquals(Set.of(103), agustinVista.numerosHabitacion());
        assertEquals(LocalDate.of(2026, 11, 1), agustinVista.checkIn());
        assertEquals(LocalDate.of(2026, 11, 4), agustinVista.checkOut());
        assertEquals(EstadoReserva.PendienteDePago, agustinVista.estado());
        assertTrue(agustinVista.activa());

        ReservaVista mariaVista = vistas.stream()
                .filter(v -> v.titular().equals("Maria Perez"))
                .findFirst()
                .orElseThrow();
        assertEquals(Set.of(201), mariaVista.numerosHabitacion());
        assertEquals(LocalDate.of(2026, 11, 5), mariaVista.checkIn());
        assertEquals(LocalDate.of(2026, 11, 7), mariaVista.checkOut());
    }

    // --------------------------------------- Slice 5: bridge persistirPropuestas

    private Habitacion habitacionDesacoplada(int numero) {
        return ControladorHabitaciones.getInstance().getHabitaciones().stream()
                .filter(h -> h.getNumero() == numero)
                .findFirst()
                .orElseThrow();
    }

    /** Propuesta DETACHED tal como la devuelve buscarHuecos: solo escalares + habitacion con id. */
    private RHPropuesta propuestaDesacoplada(int numero, LocalDate ini, LocalDate fin,
                                             float subtotal, int m, int i, int cunas) {
        Habitacion h = habitacionDesacoplada(numero);
        RHPropuesta p = new RHPropuesta(ini, fin, h.getAtributos(), subtotal,
                TipoMoneda.USD, m, i, cunas);
        p.setHabitacion(h);
        return p;
    }

    @Test
    void persistirPropuestasGuardaReservaDesdePropuestasDesacopladas() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();

        RHPropuesta p = propuestaDesacoplada(103, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 4), 5400.0f, 1, 1, 0);
        Set<RHPropuesta> propuestas = new LinkedHashSet<>();
        propuestas.add(p);

        Reserva r = cr.persistirPropuestas(titular, propuestas);

        assertNotNull(r.getId());
        assertTrue(r.isActiva());
        assertEquals(EstadoReserva.PendienteDePago, r.getEstado());
        assertEquals(TipoMoneda.USD, r.getMoneda());
        assertEquals(0.0f, r.getMontoSenia(), 0.001f);
        assertEquals(5400.0f, r.getMontoTotal(), 0.001f);
        assertEquals(1, r.getRHPropuestas().size());

        // La instancia detached original NO se reutiliza como gestionada.
        assertNull(p.getId());

        // Visible en la vista de lectura (fetch completo de habitacion).
        List<ReservaVista> vistas = cr.listarReservas();
        assertEquals(1, vistas.size());
        ReservaVista v = vistas.get(0);
        assertEquals("Agustin Rivero", v.titular());
        assertEquals(Set.of(103), v.numerosHabitacion());
        assertEquals(LocalDate.of(2026, 11, 1), v.checkIn());
        assertEquals(LocalDate.of(2026, 11, 4), v.checkOut());
        assertEquals(EstadoReserva.PendienteDePago, v.estado());
        assertTrue(v.activa());

        // Titular reutilizado, sin duplicar.
        assertEquals(1, ControladorPersonas.getInstance().getTitulares().size());
    }

    @Test
    void persistirPropuestasRechazaEntradaInvalidaSinPersistir() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();
        Set<RHPropuesta> validas = new LinkedHashSet<>();
        validas.add(propuestaDesacoplada(103, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 4), 5400.0f, 1, 1, 0));

        assertThrows(IllegalArgumentException.class, () -> cr.persistirPropuestas(null, validas));
        assertThrows(IllegalArgumentException.class, () -> cr.persistirPropuestas(titular, null));
        assertThrows(IllegalArgumentException.class, () -> cr.persistirPropuestas(titular, Set.of()));

        assertTrue(cr.getReservas().isEmpty());
    }

    @Test
    void persistirPropuestasAcumulaTotalYHabitaciones() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();

        Set<RHPropuesta> propuestas = new LinkedHashSet<>();
        propuestas.add(propuestaDesacoplada(101, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 3), 3000.0f, 1, 0, 0));
        propuestas.add(propuestaDesacoplada(103, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 4), 5400.0f, 1, 1, 0));

        Reserva r = cr.persistirPropuestas(titular, propuestas);

        assertEquals(2, r.getRHPropuestas().size());
        assertEquals(8400.0f, r.getMontoTotal(), 0.001f);

        ReservaVista v = cr.listarReservas().get(0);
        assertEquals(Set.of(101, 103), v.numerosHabitacion());
        assertEquals(LocalDate.of(2026, 11, 1), v.checkIn());
        assertEquals(LocalDate.of(2026, 11, 4), v.checkOut());
    }

    // --------------------------------------- PR#2: atomic reservation edit

    @Test
    void actualizarReservaCambiaTitularFechasCamasEstado() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva agustin = altaTitularDemo();
        TitularReserva maria = ControladorPersonas.getInstance().altaTitular(
                List.of("Maria"), List.of("Perez"), "092000000", "22000001", "maria@example.com");
        long h103 = idHabitacion(103);

        Reserva original = cr.crearReservaPara(agustin, h103,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0);

        Reserva r = cr.actualizarReserva(original.getId(), h103, LocalDate.of(2026, 11, 2),
                maria, h103, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6),
                2, 1, 1, EstadoReserva.SeniaPaga);

        assertEquals(maria.getId(), r.getTitular().getId());
        assertEquals(EstadoReserva.SeniaPaga, r.getEstado());
        assertEquals(1, r.getRHPropuestas().size());
        RHPropuesta p = r.getRHPropuestas().iterator().next();
        assertEquals(LocalDate.of(2026, 11, 2), p.getFechaInicio());
        assertEquals(LocalDate.of(2026, 11, 6), p.getFechaFin());
        assertEquals(2, p.getCamasMatrimoniales());
        assertEquals(1, p.getCamasIndividuales());
        assertEquals(1, p.getCunas());
        assertEquals(7200.0f, p.getSubtotal(), 0.001f); // 4 noches x 1800 (hab 103)
        assertEquals(7200.0f, r.getMontoTotal(), 0.001f);

        // Persistido: una sola reserva con el titular y el estado nuevos.
        List<ReservaVista> vistas = cr.listarReservas();
        assertEquals(1, vistas.size());
        assertEquals("Maria Perez", vistas.get(0).titular());
        assertEquals(EstadoReserva.SeniaPaga, vistas.get(0).estado());

        // edicionDe refresca el prefill desde la reserva ya editada (task 3.3).
        ControladorReservas.EdicionReserva ed = cr.edicionDe(original.getId(), h103,
                LocalDate.of(2026, 11, 3));
        assertEquals("Maria Perez", ed.titular());
        assertEquals(LocalDate.of(2026, 11, 2), ed.checkIn());
        assertEquals(LocalDate.of(2026, 11, 6), ed.checkOut());
        assertEquals(2, ed.camasMatrimoniales());
        assertEquals(1, ed.camasIndividuales());
        assertEquals(1, ed.cunas());
        assertEquals(EstadoReserva.SeniaPaga, ed.estado());
    }

    @Test
    void actualizarReservaRecalculaSubtotalYTotal() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();

        Set<RHPropuesta> propuestas = new LinkedHashSet<>();
        propuestas.add(propuestaDesacoplada(101, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 3), 3000.0f, 1, 0, 0)); // 2 noches x 1500
        propuestas.add(propuestaDesacoplada(103, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 4), 5400.0f, 1, 1, 0)); // 3 noches x 1800
        Reserva original = cr.persistirPropuestas(titular, propuestas);
        assertEquals(8400.0f, original.getMontoTotal(), 0.001f);

        Reserva r = cr.actualizarReserva(original.getId(), idHabitacion(101), LocalDate.of(2026, 11, 1),
                titular, idHabitacion(101), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5),
                1, 0, 0, EstadoReserva.PendienteDePago);

        assertEquals(2, r.getRHPropuestas().size());
        RHPropuesta editada = r.getRHPropuestas().stream()
                .filter(x -> x.getHabitacion().getNumero() == 101)
                .findFirst().orElseThrow();
        assertEquals(6000.0f, editada.getSubtotal(), 0.001f); // 4 noches x 1500
        assertEquals(11400.0f, r.getMontoTotal(), 0.001f);    // 6000 + 5400

        // La otra propuesta queda intacta.
        RHPropuesta intacta = r.getRHPropuestas().stream()
                .filter(x -> x.getHabitacion().getNumero() == 103)
                .findFirst().orElseThrow();
        assertEquals(5400.0f, intacta.getSubtotal(), 0.001f);
    }

    @Test
    void actualizarReservaNoSeSolapaConsigoMisma() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();
        long h103 = idHabitacion(103);
        Reserva original = cr.crearReservaPara(titular, h103,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0);

        Reserva r = cr.actualizarReserva(original.getId(), h103, LocalDate.of(2026, 11, 2),
                titular, h103, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4),
                1, 1, 0, EstadoReserva.PendienteDePago);

        assertEquals(5400.0f, r.getMontoTotal(), 0.001f);
        assertEquals(1, r.getRHPropuestas().size());
        assertEquals(1, cr.getReservas().size());
    }

    @Test
    void actualizarReservaRechazaSolapeConOtra() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();
        long h103 = idHabitacion(103);
        Reserva a = cr.crearReservaPara(titular, h103,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0);
        cr.crearReservaPara(titular, h103,
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 15), 1, 1, 0);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> cr.actualizarReserva(a.getId(), h103, LocalDate.of(2026, 11, 2),
                        titular, h103, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12),
                        1, 1, 0, EstadoReserva.PendienteDePago));
        assertTrue(ex.getMessage().contains("ocupada"), ex.getMessage());

        // Nada persistido: A conserva su rango original y siguen 2 reservas.
        assertEquals(2, cr.getReservas().size());
        Reserva aLeida = cr.getReservas().stream()
                .filter(r -> r.getId().equals(a.getId()))
                .findFirst().orElseThrow();
        assertEquals(1, aLeida.getRHPropuestas().size());
        RHPropuesta p = aLeida.getRHPropuestas().iterator().next();
        assertEquals(LocalDate.of(2026, 11, 1), p.getFechaInicio());
        assertEquals(LocalDate.of(2026, 11, 4), p.getFechaFin());
        assertEquals(5400.0f, aLeida.getMontoTotal(), 0.001f);
    }

    @Test
    void actualizarReservaCambiaHabitacionYSustituyePropuesta() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();
        Reserva original = cr.crearReservaPara(titular, idHabitacion(101),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), 1, 0, 0); // 2 x 1500 = 3000

        Reserva r = cr.actualizarReserva(original.getId(), idHabitacion(101), LocalDate.of(2026, 11, 1),
                titular, idHabitacion(102), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3),
                1, 1, 0, EstadoReserva.PendienteDePago);

        assertEquals(1, r.getRHPropuestas().size()); // la vieja 101 se reemplaza, no se suma
        RHPropuesta p = r.getRHPropuestas().iterator().next();
        assertEquals(102, p.getHabitacion().getNumero().intValue());
        assertEquals(4400.0f, p.getSubtotal(), 0.001f);  // 2 x 2200 (hab 102)
        assertEquals(4400.0f, r.getMontoTotal(), 0.001f);

        assertEquals(Set.of(102), cr.listarReservas().get(0).numerosHabitacion());
    }

    @Test
    void actualizarReservaSoloTocaPropuestaDeLaNocheClickeada() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();

        Set<RHPropuesta> propuestas = new LinkedHashSet<>();
        propuestas.add(propuestaDesacoplada(101, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 3), 3000.0f, 1, 0, 0));
        propuestas.add(propuestaDesacoplada(103, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 4), 5400.0f, 1, 1, 0));
        Reserva original = cr.persistirPropuestas(titular, propuestas);

        Reserva r = cr.actualizarReserva(original.getId(), idHabitacion(103), LocalDate.of(2026, 11, 2),
                titular, idHabitacion(103), LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 4),
                1, 1, 0, EstadoReserva.PendienteDePago);

        assertEquals(2, r.getRHPropuestas().size());
        RHPropuesta tocada = r.getRHPropuestas().stream()
                .filter(x -> x.getHabitacion().getNumero().intValue() == 103)
                .findFirst().orElseThrow();
        assertEquals(LocalDate.of(2026, 11, 2), tocada.getFechaInicio());
        assertEquals(LocalDate.of(2026, 11, 4), tocada.getFechaFin());
        assertEquals(3600.0f, tocada.getSubtotal(), 0.001f); // 2 noches x 1800

        RHPropuesta intacta = r.getRHPropuestas().stream()
                .filter(x -> x.getHabitacion().getNumero().intValue() == 101)
                .findFirst().orElseThrow();
        assertEquals(LocalDate.of(2026, 11, 1), intacta.getFechaInicio());
        assertEquals(3000.0f, intacta.getSubtotal(), 0.001f);

        assertEquals(6600.0f, r.getMontoTotal(), 0.001f); // 3000 + 3600
    }

    @Test
    void actualizarReservaRechazaEntradasInvalidas() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();
        long h103 = idHabitacion(103);
        Reserva original = cr.crearReservaPara(titular, h103,
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4), 1, 1, 0);
        long rid = original.getId();

        // checkOut == checkIn
        assertThrows(IllegalArgumentException.class,
                () -> cr.actualizarReserva(rid, h103, LocalDate.of(2026, 11, 2),
                        titular, h103, LocalDate.of(2026, 11, 4), LocalDate.of(2026, 11, 4),
                        1, 1, 0, EstadoReserva.PendienteDePago));
        // checkOut < checkIn
        assertThrows(IllegalArgumentException.class,
                () -> cr.actualizarReserva(rid, h103, LocalDate.of(2026, 11, 2),
                        titular, h103, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 4),
                        1, 1, 0, EstadoReserva.PendienteDePago));
        // beds negativas
        assertThrows(IllegalArgumentException.class,
                () -> cr.actualizarReserva(rid, h103, LocalDate.of(2026, 11, 2),
                        titular, h103, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4),
                        -1, 1, 0, EstadoReserva.PendienteDePago));
        // titular nulo
        assertThrows(IllegalArgumentException.class,
                () -> cr.actualizarReserva(rid, h103, LocalDate.of(2026, 11, 2),
                        null, h103, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4),
                        1, 1, 0, EstadoReserva.PendienteDePago));
        // reserva inexistente
        assertThrows(IllegalArgumentException.class,
                () -> cr.actualizarReserva(9999L, h103, LocalDate.of(2026, 11, 2),
                        titular, h103, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4),
                        1, 1, 0, EstadoReserva.PendienteDePago));
        // sin propuesta en esa noche (habitacion distinta a la clickeada)
        assertThrows(IllegalArgumentException.class,
                () -> cr.actualizarReserva(rid, idHabitacion(101), LocalDate.of(2026, 11, 2),
                        titular, h103, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4),
                        1, 1, 0, EstadoReserva.PendienteDePago));
        // habitacion nueva inexistente
        assertThrows(IllegalArgumentException.class,
                () -> cr.actualizarReserva(rid, h103, LocalDate.of(2026, 11, 2),
                        titular, 9999L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4),
                        1, 1, 0, EstadoReserva.PendienteDePago));

        // Nada persistido: la reserva conserva su propuesta y rango originales.
        assertEquals(1, cr.getReservas().size());
        Reserva persistida = cr.getReservas().get(0);
        assertEquals(1, persistida.getRHPropuestas().size());
        RHPropuesta p = persistida.getRHPropuestas().iterator().next();
        assertEquals(LocalDate.of(2026, 11, 1), p.getFechaInicio());
        assertEquals(LocalDate.of(2026, 11, 4), p.getFechaFin());
        assertEquals(5400.0f, persistida.getMontoTotal(), 0.001f);
    }

    // --------------------------------------- PR#3: atomic automatic placement

    @Test
    void crearReservaAutomaticaEligeHabitacionLibre() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();
        LocalDate ini = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 4);

        Reserva r = cr.crearReservaAutomatica(titular, ini, fin, 1, 1, 0, 0,
                Set.of(AtributoHabitacion.Balcon));

        assertNotNull(r.getId());
        assertTrue(r.isActiva());
        assertEquals(EstadoReserva.PendienteDePago, r.getEstado());
        assertEquals(TipoMoneda.USD, r.getMoneda());
        assertEquals(0.0f, r.getMontoSenia(), 0.001f);
        assertEquals(4500.0f, r.getMontoTotal(), 0.001f); // 3 noches x 1500 (hab 101)

        assertEquals(1, r.getRHPropuestas().size());
        RHPropuesta p = r.getRHPropuestas().iterator().next();
        assertEquals(101, p.getHabitacion().getNumero().intValue());
        assertEquals(ini, p.getFechaInicio());
        assertEquals(fin, p.getFechaFin());
        assertEquals(4500.0f, p.getSubtotal(), 0.001f);
        assertEquals(Set.of(AtributoHabitacion.Balcon), p.getAtributosHabitacion());

        assertEquals(1, cr.getReservas().size());

        // La habitacion elegida (101) queda ocupada: la segunda colocacion sobre
        // el mismo rango elige otra libre (103, siguiente exacta por numero).
        Reserva r2 = cr.crearReservaAutomatica(titular, ini, fin, 1, 1, 0, 0,
                Set.of(AtributoHabitacion.Balcon));
        assertEquals(103, r2.getRHPropuestas().iterator().next().getHabitacion().getNumero().intValue());
        assertEquals(5400.0f, r2.getMontoTotal(), 0.001f); // 3 noches x 1800
        assertEquals(2, cr.getReservas().size());
    }

    @Test
    void crearReservaAutomaticaRespetaAtributosYCantidad() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();

        // Atributos: {VistaAlMar} con 3 pasajeros -> 104 (102 tiene atributo extra).
        Reserva porAtributos = cr.crearReservaAutomatica(titular,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 4),
                1, 1, 0, 0, Set.of(AtributoHabitacion.VistaAlMar));
        RHPropuesta pAtributos = porAtributos.getRHPropuestas().iterator().next();
        assertEquals(104, pAtributos.getHabitacion().getNumero().intValue());
        assertEquals(Set.of(AtributoHabitacion.VistaAlMar), pAtributos.getAtributosHabitacion());
        assertEquals(6300.0f, pAtributos.getSubtotal(), 0.001f); // 3 noches x 2100

        // Cantidad: 2 matrimoniales (4 pasajeros) + {Balcon} -> 103 (101 queda fuera por capacidad).
        Reserva porCantidad = cr.crearReservaAutomatica(titular,
                LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 7),
                2, 0, 0, 0, Set.of(AtributoHabitacion.Balcon));
        RHPropuesta pCantidad = porCantidad.getRHPropuestas().iterator().next();
        assertEquals(103, pCantidad.getHabitacion().getNumero().intValue());
        assertEquals(2, pCantidad.getCamasMatrimoniales());
        assertEquals(0, pCantidad.getCamasIndividuales());
        assertEquals(0, pCantidad.getCunas());
        assertEquals(3600.0f, pCantidad.getSubtotal(), 0.001f); // 2 noches x 1800

        // Los bebes suman a la capacidad: 1 matrimonial + 2 bebes (4 pax) -> 103.
        Reserva conBebes = cr.crearReservaAutomatica(titular,
                LocalDate.of(2026, 12, 8), LocalDate.of(2026, 12, 10),
                1, 0, 2, 0, Set.of(AtributoHabitacion.Balcon));
        assertEquals(103, conBebes.getRHPropuestas().iterator().next().getHabitacion().getNumero().intValue());
        assertEquals(3600.0f, conBebes.getMontoTotal(), 0.001f); // 2 noches x 1800
    }

    @Test
    void crearReservaAutomaticaLanzaSinHabitacion() {
        ControladorReservas cr = ControladorReservas.getInstance();
        TitularReserva titular = altaTitularDemo();
        LocalDate ini = LocalDate.of(2026, 11, 1);
        LocalDate fin = LocalDate.of(2026, 11, 5);

        // Hotel vacio: no existe ninguna habitacion.
        new RepositorioHabitaciones().eliminarTodas();
        IllegalStateException vacio = assertThrows(IllegalStateException.class,
                () -> cr.crearReservaAutomatica(titular, ini, fin, 1, 0, 0, 0, Set.of()));
        assertTrue(vacio.getMessage().contains("sin habitacion disponible"), vacio.getMessage());
        assertTrue(cr.getReservas().isEmpty());

        // Todas ocupadas: una reserva por habitacion sobre el mismo rango.
        TestUtil.sembrarHabitaciones();
        for (Habitacion h : ControladorHabitaciones.getInstance().getHabitaciones()) {
            cr.crearReservaPara(titular, h.getId(), ini, fin, 1, 0, 0);
        }

        IllegalStateException ocupadas = assertThrows(IllegalStateException.class,
                () -> cr.crearReservaAutomatica(titular, ini, fin, 1, 0, 0, 0, Set.of()));
        assertTrue(ocupadas.getMessage().contains("sin habitacion disponible"), ocupadas.getMessage());
        assertEquals(6, cr.getReservas().size());
    }
}