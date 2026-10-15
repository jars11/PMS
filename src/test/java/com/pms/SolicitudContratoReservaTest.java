package com.pms;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.dtos.PerfilRelajacion;
import com.pms.entities.ContratoReserva;
import com.pms.entities.Habitacion;
import com.pms.entities.Reserva;
import com.pms.entities.RHPropuesta;
import com.pms.entities.RHSolicitada;
import com.pms.entities.Solicitud;
import com.pms.entities.TitularReserva;
import com.pms.persistence.JpaUtil;
import com.pms.persistence.RepositorioReservas;
import com.pms.persistence.RepositorioSolicitudes;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.NivelCapacidad;
import com.pms.utils.TipoMoneda;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vocabulary and persistence of the requested set (Solicitud), the relaxation
 * contract (ContratoReserva) and the effective-reservation invariant.
 */
class SolicitudContratoReservaTest {

    @BeforeAll
    static void antesDeTodo() {
        JpaUtil.setUnidadPorDefecto("pms-test");
    }

    @BeforeEach
    void limpiar() {
        TestUtil.limpiarBase();
    }

    private static TitularReserva titularDemo() {
        return ControladorPersonas.getInstance().altaTitular(
                List.of("Agustin"), List.of("Rivero"), "091000000", "22000000", "agustin@example.com");
    }

    // ------------------------------------------------------------- vocabulary

    @Test
    void nivelCapacidadMapeaRelajacionDistribucion() {
        assertSame(NivelCapacidad.DISTRIBUCION_CAMAS, NivelCapacidad.deRelajacion(0));
        assertSame(NivelCapacidad.CAPACIDAD_POR_HABITACION, NivelCapacidad.deRelajacion(1));
        assertSame(NivelCapacidad.CAPACIDAD_TOTAL, NivelCapacidad.deRelajacion(2));

        assertEquals(0, NivelCapacidad.DISTRIBUCION_CAMAS.getRelajacionDistribucion());
        assertEquals(1, NivelCapacidad.CAPACIDAD_POR_HABITACION.getRelajacionDistribucion());
        assertEquals(2, NivelCapacidad.CAPACIDAD_TOTAL.getRelajacionDistribucion());
    }

    @Test
    void nivelCapacidadRechazaValorFueraDeRango() {
        assertThrows(IllegalArgumentException.class, () -> NivelCapacidad.deRelajacion(3));
        assertThrows(IllegalArgumentException.class, () -> NivelCapacidad.deRelajacion(-1));
    }

    @Test
    void nivelCapacidadCongelaOrdenDeDeclaracion() {
        assertEquals(List.of(NivelCapacidad.DISTRIBUCION_CAMAS,
                        NivelCapacidad.CAPACIDAD_POR_HABITACION,
                        NivelCapacidad.CAPACIDAD_TOTAL),
                List.of(NivelCapacidad.values()));
    }

    @Test
    void perfilRelajacionPorDefectoEsElPerfilEstricto() {
        assertEquals(false, PerfilRelajacion.POR_DEFECTO.fechasRelajadas());
        assertEquals(false, PerfilRelajacion.POR_DEFECTO.atributosRelajados());
        assertEquals(false, PerfilRelajacion.POR_DEFECTO.colchonesExtra());
        assertSame(NivelCapacidad.DISTRIBUCION_CAMAS, PerfilRelajacion.POR_DEFECTO.nivelCapacidad());
    }

    @Test
    void perfilRelajacionTieneSemanticaDeValor() {
        PerfilRelajacion a = new PerfilRelajacion(true, false, true, NivelCapacidad.CAPACIDAD_TOTAL);
        PerfilRelajacion b = new PerfilRelajacion(true, false, true, NivelCapacidad.CAPACIDAD_TOTAL);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new PerfilRelajacion(false, false, true, NivelCapacidad.CAPACIDAD_TOTAL));
    }

    // ------------------------------------------------------------ persistence

    @Test
    void solicitudRoundTripConDosBloquesYTitular() {
        TitularReserva titular = titularDemo();

        Solicitud s = new Solicitud(titular);
        s.addRHSolicitada(new RHSolicitada(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15),
                Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0));
        s.addRHSolicitada(new RHSolicitada(LocalDate.of(2027, 1, 15), LocalDate.of(2027, 1, 20),
                Set.of(), 1, 0, 1, 1));

        RepositorioSolicitudes repo = new RepositorioSolicitudes();
        Solicitud guardada = repo.guardar(s);
        assertNotNull(guardada.getId());

        Solicitud leida = repo.findById(guardada.getId());
        assertNotNull(leida);
        assertNotNull(leida.getTitular());
        assertEquals("Agustin", leida.getTitular().getNombres().get(0));
        assertEquals(2, leida.getRHSolicitadas().size());

        RHSolicitada conBebe = leida.getRHSolicitadas().stream()
                .filter(b -> b.getFechaInicio().equals(LocalDate.of(2027, 1, 15)))
                .findFirst().orElseThrow();
        assertEquals(LocalDate.of(2027, 1, 20), conBebe.getFechaFin());
        assertEquals(Set.of(), conBebe.getAtributosHabitacion());
        assertEquals(1, conBebe.getCantMatrimonios());
        assertEquals(0, conBebe.getCantIndividuales());
        assertEquals(1, conBebe.getCantBebes());
        assertEquals(1, conBebe.getCantCunas());

        RHSolicitada conAtributo = leida.getRHSolicitadas().stream()
                .filter(b -> b.getFechaInicio().equals(LocalDate.of(2027, 1, 10)))
                .findFirst().orElseThrow();
        assertEquals(Set.of(AtributoHabitacion.Balcon), conAtributo.getAtributosHabitacion());
        assertEquals(1, conAtributo.getCantIndividuales());
    }

    @Test
    void contratoReservaRoundTripConEjesYEnumComoTexto() {
        TitularReserva titular = titularDemo();
        Reserva r = new RepositorioReservas().guardar(new Reserva(titular));

        Solicitud s = new Solicitud(titular);
        s.addContrato(new ContratoReserva(s, r,
                new PerfilRelajacion(true, true, true, NivelCapacidad.CAPACIDAD_POR_HABITACION)));

        RepositorioSolicitudes repo = new RepositorioSolicitudes();
        Solicitud guardada = repo.guardar(s);

        Solicitud leida = repo.findById(guardada.getId());
        assertEquals(1, leida.getContratos().size());
        ContratoReserva c = leida.getContratos().iterator().next();
        assertTrue(c.isFechasRelajadas());
        assertTrue(c.isAtributosRelajados());
        assertTrue(c.isColchonesExtra());
        assertEquals(NivelCapacidad.CAPACIDAD_POR_HABITACION, c.getNivelCapacidad());
        assertEquals(r.getId(), c.getReserva().getId());

        // El enum se guarda como su nombre (EnumType.STRING), no como ordinal.
        String almacenado = JpaUtil.enTransaccion(em ->
                (String) em.createNativeQuery(
                                "select nivel_capacidad from contrato_reserva where id = " + c.getId())
                        .getSingleResult());
        assertEquals("CAPACIDAD_POR_HABITACION", almacenado);
    }

    @Test
    void reservaEfectivaPorDefectoFalseYRoundTripTrue() {
        TitularReserva titular = titularDemo();
        RepositorioReservas repo = new RepositorioReservas();

        Reserva r = repo.guardar(new Reserva(titular));
        assertFalse(repo.findById(r.getId()).isEfectiva());

        r.setEfectiva(true);
        repo.guardar(r);
        assertTrue(repo.findById(r.getId()).isEfectiva());
    }

    @Test
    void contratoReservaRechazaDuplicadoSolicitudReserva() {
        TitularReserva titular = titularDemo();
        Reserva r = new RepositorioReservas().guardar(new Reserva(titular));

        assertThrows(PersistenceException.class, () -> JpaUtil.enTransaccion(em -> {
            Reserva gestionada = em.find(Reserva.class, r.getId());
            Solicitud s = new Solicitud(em.merge(titular));
            s.addContrato(new ContratoReserva(s, gestionada, PerfilRelajacion.POR_DEFECTO));
            s.addContrato(new ContratoReserva(s, gestionada, PerfilRelajacion.POR_DEFECTO));
            em.persist(s);
            return null;
        }));

        // La transaccion hizo rollback: no quedo ninguna solicitud ni contrato.
        assertEquals(0, new RepositorioSolicitudes().findAll().size());
    }

    // -------------------------------------------------------------- invariant

    @Test
    void promoverEfectivaMantieneUnaSolaReservaEfectiva() {
        TitularReserva t = new TitularReserva(List.of("A"), List.of("B"), "1", "2", "e");

        Solicitud s = new Solicitud(t);
        Reserva a = new Reserva(t);
        Reserva b = new Reserva(t);
        s.addContrato(new ContratoReserva(s, a, PerfilRelajacion.POR_DEFECTO));
        s.addContrato(new ContratoReserva(s, b, PerfilRelajacion.POR_DEFECTO));

        assertFalse(a.isEfectiva());
        assertFalse(b.isEfectiva());

        s.promoverEfectiva(a);
        assertTrue(a.isEfectiva());
        assertEquals(1, s.getContratos().stream().map(ContratoReserva::getReserva)
                .filter(Reserva::isEfectiva).count());

        s.promoverEfectiva(b);
        assertFalse(a.isEfectiva());
        assertTrue(b.isEfectiva());
        assertEquals(1, s.getContratos().stream().map(ContratoReserva::getReserva)
                .filter(Reserva::isEfectiva).count());
    }

    // ------------------------------------------------------------- repository

    @Test
    void repositorioSolicitudesFindByIdYFindAll() {
        TitularReserva titular = titularDemo();
        RepositorioSolicitudes repo = new RepositorioSolicitudes();

        Solicitud s1 = new Solicitud(titular);
        s1.addRHSolicitada(new RHSolicitada(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 12),
                Set.of(), 1, 0, 0, 0));
        Solicitud s2 = new Solicitud(titular);
        s2.addRHSolicitada(new RHSolicitada(LocalDate.of(2027, 2, 1), LocalDate.of(2027, 2, 3),
                Set.of(AtributoHabitacion.Jacuzzi), 1, 1, 0, 0));

        repo.guardar(s1);
        repo.guardar(s2);

        assertNotNull(repo.findById(s1.getId()));
        assertEquals(2, repo.findAll().size());
    }

    @Test
    void repositorioSolicitudesFindReservaEfectiva() {
        TitularReserva titular = titularDemo();
        Reserva r = new RepositorioReservas().guardar(new Reserva(titular));

        Solicitud s = new Solicitud(titular);
        s.addContrato(new ContratoReserva(s, r, PerfilRelajacion.POR_DEFECTO));
        Solicitud guardada = new RepositorioSolicitudes().guardar(s);

        RepositorioSolicitudes repo = new RepositorioSolicitudes();
        // Aun no promovida: no hay reserva efectiva.
        assertNull(repo.findReservaEfectiva(guardada.getId()));

        JpaUtil.enTransaccion(em -> {
            em.find(Reserva.class, r.getId()).setEfectiva(true);
            return null;
        });

        Reserva efectiva = repo.findReservaEfectiva(guardada.getId());
        assertNotNull(efectiva);
        assertEquals(r.getId(), efectiva.getId());
    }

    // -------------------------------------------------------------- per-path

    private static ControladorReservas controlador() {
        return ControladorReservas.getInstance();
    }

    private static Habitacion habitacion(int numero) {
        return ControladorHabitaciones.getInstance().getHabitaciones().stream()
                .filter(h -> h.getNumero() == numero)
                .findFirst()
                .orElseThrow();
    }

    private static long idHabitacion(int numero) {
        return habitacion(numero).getId();
    }

    /** Detached proposal as buscarHuecos returns it: scalars plus a room with id. */
    private static RHPropuesta propuestaDesacoplada(int numero, LocalDate ini, LocalDate fin,
                                                    float subtotal, int m, int i, int cunas) {
        Habitacion h = habitacion(numero);
        RHPropuesta p = new RHPropuesta(ini, fin, h.getAtributos(), subtotal, TipoMoneda.USD, m, i, cunas);
        p.setHabitacion(h);
        return p;
    }

    @Test
    void crearReservaParaPersisteSolicitudContratoYReservaEfectiva() {
        TestUtil.sembrarHabitaciones();
        TitularReserva titular = titularDemo();

        Reserva r = controlador().crearReservaPara(titular, idHabitacion(103),
                LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 4), 1, 1, 0,
                Set.of(AtributoHabitacion.Jacuzzi));

        RepositorioSolicitudes repo = new RepositorioSolicitudes();
        List<Solicitud> solicitudes = repo.findAll();
        assertEquals(1, solicitudes.size());
        Solicitud s = solicitudes.get(0);
        assertEquals(1, s.getRHSolicitadas().size());

        RHSolicitada bloque = s.getRHSolicitadas().iterator().next();
        assertEquals(LocalDate.of(2027, 3, 1), bloque.getFechaInicio());
        assertEquals(LocalDate.of(2027, 3, 4), bloque.getFechaFin());
        assertEquals(Set.of(AtributoHabitacion.Jacuzzi), bloque.getAtributosHabitacion());
        assertEquals(1, bloque.getCantMatrimonios());
        assertEquals(1, bloque.getCantIndividuales());
        assertEquals(0, bloque.getCantBebes());
        assertEquals(0, bloque.getCantCunas());

        assertEquals(1, s.getContratos().size());
        ContratoReserva c = s.getContratos().iterator().next();
        assertFalse(c.isFechasRelajadas());
        assertFalse(c.isAtributosRelajados());
        assertFalse(c.isColchonesExtra());
        assertEquals(NivelCapacidad.DISTRIBUCION_CAMAS, c.getNivelCapacidad());
        assertEquals(r.getId(), c.getReserva().getId());

        assertTrue(r.isEfectiva());
        assertTrue(new RepositorioReservas().findById(r.getId()).isEfectiva());
        assertEquals(r.getId(), repo.findReservaEfectiva(s.getId()).getId());
    }

    @Test
    void crearReservaAutomaticaMiraCantBebes() {
        TestUtil.sembrarHabitaciones();
        TitularReserva titular = titularDemo();

        Reserva r = controlador().crearReservaAutomatica(titular,
                LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 4), 1, 0, 2, 0,
                Set.of(AtributoHabitacion.Balcon));

        RepositorioSolicitudes repo = new RepositorioSolicitudes();
        List<Solicitud> solicitudes = repo.findAll();
        assertEquals(1, solicitudes.size());
        Solicitud s = solicitudes.get(0);
        assertEquals(1, s.getRHSolicitadas().size());

        RHSolicitada bloque = s.getRHSolicitadas().iterator().next();
        assertEquals(LocalDate.of(2027, 3, 1), bloque.getFechaInicio());
        assertEquals(LocalDate.of(2027, 3, 4), bloque.getFechaFin());
        assertEquals(Set.of(AtributoHabitacion.Balcon), bloque.getAtributosHabitacion());
        assertEquals(1, bloque.getCantMatrimonios());
        assertEquals(0, bloque.getCantIndividuales());
        assertEquals(2, bloque.getCantBebes());
        assertEquals(0, bloque.getCantCunas());

        assertTrue(r.isEfectiva());
        assertEquals(NivelCapacidad.DISTRIBUCION_CAMAS,
                s.getContratos().iterator().next().getNivelCapacidad());
    }

    @Test
    void crearReservaMiraSoloBloquesConsumidos() {
        TestUtil.sembrarHabitaciones();
        TitularReserva titular = titularDemo();
        ControladorReservas cr = controlador();

        // Bloque factible (habitacion 101) + bloque imposible por capacidad.
        cr.agregarBloqueReserva(LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 4), 1, 1, 0, 0,
                Set.of(AtributoHabitacion.Balcon));
        cr.agregarBloqueReserva(LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 4), 9, 0, 0, 0,
                Set.of());

        cr.crearReserva(titular);

        List<Solicitud> solicitudes = new RepositorioSolicitudes().findAll();
        assertEquals(1, solicitudes.size());
        Solicitud s = solicitudes.get(0);
        assertEquals(1, s.getRHSolicitadas().size());

        RHSolicitada bloque = s.getRHSolicitadas().iterator().next();
        assertEquals(LocalDate.of(2027, 5, 1), bloque.getFechaInicio());
        assertEquals(LocalDate.of(2027, 5, 4), bloque.getFechaFin());
        assertEquals(Set.of(AtributoHabitacion.Balcon), bloque.getAtributosHabitacion());
    }

    @Test
    void crearReservaMiraTodosLosBloquesConsumidos() {
        TestUtil.sembrarHabitaciones();
        ControladorReservas cr = controlador();

        cr.agregarBloqueReserva(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15), 1, 1, 0, 0,
                Set.of(AtributoHabitacion.Balcon));
        cr.agregarBloqueReserva(LocalDate.of(2027, 1, 15), LocalDate.of(2027, 1, 20), 1, 0, 1, 1,
                Set.of());

        cr.crearReserva(titularDemo());

        Solicitud s = new RepositorioSolicitudes().findAll().get(0);
        assertEquals(2, s.getRHSolicitadas().size());

        Set<LocalDate> inicios = new LinkedHashSet<>();
        for (RHSolicitada bloque : s.getRHSolicitadas()) {
            inicios.add(bloque.getFechaInicio());
        }
        assertEquals(Set.of(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15)), inicios);
    }

    @Test
    void persistirPropuestasMiraUnaSolicitadaPorPropuestaConCeroBebes() {
        TestUtil.sembrarHabitaciones();
        TitularReserva titular = titularDemo();

        Set<RHPropuesta> propuestas = new LinkedHashSet<>();
        propuestas.add(propuestaDesacoplada(101, LocalDate.of(2027, 6, 1),
                LocalDate.of(2027, 6, 3), 3000.0f, 1, 0, 0));
        propuestas.add(propuestaDesacoplada(103, LocalDate.of(2027, 6, 1),
                LocalDate.of(2027, 6, 4), 5400.0f, 1, 1, 1));

        Reserva r = controlador().persistirPropuestas(titular, propuestas);

        Solicitud s = new RepositorioSolicitudes().findAll().get(0);
        assertEquals(2, s.getRHSolicitadas().size());
        for (RHSolicitada bloque : s.getRHSolicitadas()) {
            assertEquals(0, bloque.getCantBebes());
        }

        RHSolicitada de101 = s.getRHSolicitadas().stream()
                .filter(b -> b.getFechaInicio().equals(LocalDate.of(2027, 6, 1))
                        && b.getFechaFin().equals(LocalDate.of(2027, 6, 3)))
                .findFirst().orElseThrow();
        assertEquals(Set.of(AtributoHabitacion.Balcon), de101.getAtributosHabitacion());
        assertEquals(0, de101.getCantCunas());

        assertTrue(r.isEfectiva());
        assertEquals(1, s.getContratos().size());
        assertEquals(NivelCapacidad.DISTRIBUCION_CAMAS,
                s.getContratos().iterator().next().getNivelCapacidad());
    }

    @Test
    void crearReservaParaConPerfilExplicitoLoGuardaEnElContrato() {
        TestUtil.sembrarHabitaciones();
        TitularReserva titular = titularDemo();
        PerfilRelajacion perfil = new PerfilRelajacion(true, true, true, NivelCapacidad.CAPACIDAD_TOTAL);

        Reserva r = controlador().crearReservaPara(titular, idHabitacion(103),
                LocalDate.of(2027, 9, 1), LocalDate.of(2027, 9, 4), 1, 1, 0,
                Set.of(), perfil);

        Solicitud s = new RepositorioSolicitudes().findAll().get(0);
        ContratoReserva c = s.getContratos().iterator().next();
        assertTrue(c.isFechasRelajadas());
        assertTrue(c.isAtributosRelajados());
        assertTrue(c.isColchonesExtra());
        assertEquals(NivelCapacidad.CAPACIDAD_TOTAL, c.getNivelCapacidad());
        assertTrue(r.isEfectiva());
    }

    @Test
    void persistirPropuestasConPerfilExplicitoLoGuardaEnElContrato() {
        TestUtil.sembrarHabitaciones();
        TitularReserva titular = titularDemo();
        PerfilRelajacion perfil = new PerfilRelajacion(false, true, true, NivelCapacidad.CAPACIDAD_POR_HABITACION);

        Set<RHPropuesta> propuestas = new LinkedHashSet<>();
        propuestas.add(propuestaDesacoplada(103, LocalDate.of(2027, 10, 1),
                LocalDate.of(2027, 10, 3), 3600.0f, 1, 1, 0));

        controlador().persistirPropuestas(titular, propuestas, perfil);

        Solicitud s = new RepositorioSolicitudes().findAll().get(0);
        ContratoReserva c = s.getContratos().iterator().next();
        assertFalse(c.isFechasRelajadas());
        assertTrue(c.isAtributosRelajados());
        assertTrue(c.isColchonesExtra());
        assertEquals(NivelCapacidad.CAPACIDAD_POR_HABITACION, c.getNivelCapacidad());
    }

    @Test
    void creacionFallidaRevierteSinPersistirSolicitudContratoNiReserva() {
        TestUtil.sembrarHabitaciones();
        TitularReserva titular = titularDemo();
        ControladorReservas cr = controlador();
        long id103 = idHabitacion(103);

        cr.crearReservaPara(titular, id103,
                LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 4), 1, 1, 0);

        RepositorioSolicitudes repo = new RepositorioSolicitudes();
        assertEquals(1, repo.findAll().size());
        assertEquals(1, new RepositorioReservas().findAll().size());

        // Solapamiento: la transaccion revierte y no agrega solicitud, contrato ni reserva.
        assertThrows(IllegalStateException.class, () -> cr.crearReservaPara(titular, id103,
                LocalDate.of(2027, 7, 2), LocalDate.of(2027, 7, 3), 1, 0, 0));

        List<Solicitud> solicitudes = repo.findAll();
        assertEquals(1, solicitudes.size());
        assertEquals(1, solicitudes.get(0).getContratos().size());
        assertEquals(1, new RepositorioReservas().findAll().size());
    }

    @Test
    void creacionFallidaSinHabitacionNoPersisteNada() {
        TitularReserva titular = titularDemo();

        // Hotel vacio: el manejador no encuentra habitacion y la transaccion revierte.
        assertThrows(IllegalStateException.class, () -> controlador().crearReservaAutomatica(titular,
                LocalDate.of(2027, 8, 1), LocalDate.of(2027, 8, 4), 1, 0, 0, 0, Set.of()));

        assertEquals(0, new RepositorioSolicitudes().findAll().size());
        assertEquals(0, new RepositorioReservas().findAll().size());
    }
}
