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
import com.pms.persistence.RepositorioHabitaciones;
import com.pms.persistence.RepositorioReservas;
import com.pms.persistence.RepositorioSolicitudes;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.EstadoReserva;
import com.pms.utils.NivelCapacidad;
import com.pms.utils.TipoMoneda;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

    // ------------------------------------------------------------ absent surface

    @Test
    void reservaNoDeclaraFlagEfectiva() {
        assertThrows(NoSuchFieldException.class, () -> Reserva.class.getDeclaredField("efectiva"));
        assertThrows(NoSuchMethodException.class, () -> Reserva.class.getDeclaredMethod("isEfectiva"));
        assertThrows(NoSuchMethodException.class,
                () -> Reserva.class.getDeclaredMethod("setEfectiva", boolean.class));
    }

    @Test
    void solicitudNoDeclaraPromoverEfectiva() {
        assertThrows(NoSuchMethodException.class,
                () -> Solicitud.class.getDeclaredMethod("promoverEfectiva", Reserva.class));
    }

    @Test
    void reservaYSolicitudNoDeclaranReferenciaCruzada() {
        for (java.lang.reflect.Field f : Reserva.class.getDeclaredFields()) {
            assertNotEquals(Solicitud.class, f.getType(), "Reserva declara el campo " + f.getName());
        }
        for (java.lang.reflect.Field f : Solicitud.class.getDeclaredFields()) {
            assertNotEquals(Reserva.class, f.getType(), "Solicitud declara el campo " + f.getName());
        }
    }

    @Test
    void reservaNoSobrescribeEqualsNiHashCode() throws NoSuchMethodException {
        assertEquals(Object.class, Reserva.class.getMethod("equals", Object.class).getDeclaringClass());
        assertEquals(Object.class, Reserva.class.getMethod("hashCode").getDeclaringClass());
    }

    // --------------------------------------------------------------- schema

    private static Map<String, String> columnasDeMetadata(String filtro) {
        Map<String, String> resultado = new HashMap<>();
        List<?> filas = JpaUtil.enTransaccion(em -> em.createNativeQuery(
                        "select table_name, column_name, is_nullable from information_schema.columns " +
                                "where table_schema = 'PUBLIC' " + filtro)
                .getResultList());
        for (Object fila : filas) {
            Object[] f = (Object[]) fila;
            resultado.put(f[0] + "." + f[1], (String) f[2]);
        }
        return resultado;
    }

    /** Restricciones UNIQUE de contrato_reserva: nombre -> columnas en orden. */
    private static Map<String, List<String>> uniquesDeContrato() {
        Map<String, List<String>> resultado = new TreeMap<>();
        List<?> filas = JpaUtil.enTransaccion(em -> em.createNativeQuery(
                        "select tc.constraint_name, kcu.column_name, kcu.ordinal_position " +
                                "from information_schema.table_constraints tc " +
                                "join information_schema.key_column_usage kcu " +
                                "  on tc.constraint_schema = kcu.constraint_schema " +
                                " and tc.constraint_name = kcu.constraint_name " +
                                "where tc.table_schema = 'PUBLIC' and tc.table_name = 'CONTRATO_RESERVA' " +
                                "  and tc.constraint_type = 'UNIQUE' " +
                                "order by tc.constraint_name, kcu.ordinal_position")
                .getResultList());
        for (Object fila : filas) {
            Object[] f = (Object[]) fila;
            resultado.computeIfAbsent((String) f[0], k -> new ArrayList<>()).add((String) f[1]);
        }
        return resultado;
    }

    /** Numero de contratos persistidos (se conserva en el rollback del caso cero bloques). */
    private static long contarContratos() {
        return JpaUtil.enTransaccion(em ->
                em.createQuery("select count(c) from ContratoReserva c", Long.class).getSingleResult());
    }

    @Test
    void esquemaSinColumnaEfectiva() {
        Map<String, String> columnas = columnasDeMetadata(
                "and table_name = 'RESERVA' and column_name = 'EFECTIVA'");
        assertTrue(columnas.isEmpty(), "RESERVA.EFECTIVA sigue existiendo: " + columnas);
    }

    @Test
    void esquemaConCardinalidadesFinales() {
        Map<String, String> columnas = columnasDeMetadata(
                "and ((table_name = 'CONTRATO_RESERVA' and column_name in ('SOLICITUD_ID', 'RESERVA_ID')) " +
                        "or (table_name = 'SOLICITUD' and column_name = 'TITULAR_ID') " +
                        "or (table_name = 'RESERVA' and column_name = 'TITULAR_ID'))");

        assertEquals("NO", columnas.get("CONTRATO_RESERVA.SOLICITUD_ID"));
        assertEquals("NO", columnas.get("CONTRATO_RESERVA.RESERVA_ID"));
        assertEquals("YES", columnas.get("SOLICITUD.TITULAR_ID"));
        assertEquals("NO", columnas.get("RESERVA.TITULAR_ID"));

        Map<String, List<String>> uniques = uniquesDeContrato();
        assertEquals(2, uniques.size(), "se esperaban dos UNIQUE de una columna: " + uniques);
        for (List<String> cols : uniques.values()) {
            assertEquals(1, cols.size(), "la restriccion par sigue existiendo: " + uniques);
        }
        Set<String> lados = new LinkedHashSet<>();
        for (List<String> cols : uniques.values()) {
            lados.addAll(cols);
        }
        assertEquals(Set.of("SOLICITUD_ID", "RESERVA_ID"), lados);
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
    void contratoRechazaSegundoContratoParaLaMismaSolicitud() {
        TitularReserva titular = titularDemo();
        RepositorioReservas repoReservas = new RepositorioReservas();
        Reserva r1 = repoReservas.guardar(new Reserva(titular));
        Reserva r2 = repoReservas.guardar(new Reserva(titular));

        assertThrows(PersistenceException.class, () -> JpaUtil.enTransaccion(em -> {
            Solicitud s = new Solicitud(em.merge(titular));
            s.addContrato(new ContratoReserva(s, em.find(Reserva.class, r1.getId()),
                    PerfilRelajacion.POR_DEFECTO));
            s.addContrato(new ContratoReserva(s, em.find(Reserva.class, r2.getId()),
                    PerfilRelajacion.POR_DEFECTO));
            em.persist(s);
            return null;
        }));

        assertEquals(0, new RepositorioSolicitudes().findAll().size());
    }

    @Test
    void contratoRechazaSegundoContratoParaLaMismaReserva() {
        TitularReserva titular = titularDemo();
        Reserva r = new RepositorioReservas().guardar(new Reserva(titular));

        Solicitud s1 = new Solicitud(titular);
        s1.addContrato(new ContratoReserva(s1, r, PerfilRelajacion.POR_DEFECTO));
        new RepositorioSolicitudes().guardar(s1);

        assertThrows(PersistenceException.class, () -> JpaUtil.enTransaccion(em -> {
            Solicitud s2 = new Solicitud(em.merge(titular));
            s2.addContrato(new ContratoReserva(s2, em.find(Reserva.class, r.getId()),
                    PerfilRelajacion.POR_DEFECTO));
            em.persist(s2);
            return null;
        }));

        assertEquals(1, new RepositorioSolicitudes().findAll().size());
    }

    @Test
    void solicitudSinTitularSePersiste() {
        Solicitud guardada = new RepositorioSolicitudes().guardar(new Solicitud(null));

        assertNotNull(guardada.getId());
        assertNull(new RepositorioSolicitudes().findById(guardada.getId()).getTitular());
    }

    @Test
    void reservaSinTitularEsRechazada() {
        assertThrows(PersistenceException.class,
                () -> new RepositorioReservas().guardar(new Reserva(null)));
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
    void repositorioResuelveReservaYContratoPorContrato() {
        TitularReserva titular = titularDemo();
        Reserva r = new RepositorioReservas().guardar(new Reserva(titular));

        Solicitud s = new Solicitud(titular);
        s.addContrato(new ContratoReserva(s, r, PerfilRelajacion.POR_DEFECTO));
        Solicitud guardada = new RepositorioSolicitudes().guardar(s);

        RepositorioSolicitudes repo = new RepositorioSolicitudes();
        Reserva resuelta = repo.findReservaDeSolicitud(guardada.getId());
        ContratoReserva contrato = repo.findContratoDeSolicitud(guardada.getId());

        assertNotNull(resuelta);
        assertEquals(r.getId(), resuelta.getId());
        assertNotNull(contrato);
        assertEquals(r.getId(), contrato.getReserva().getId());
        assertEquals(guardada.getId(), contrato.getSolicitud().getId());
    }

    @Test
    void repositorioSinContratoDevuelveNull() {
        TitularReserva titular = titularDemo();
        Solicitud s = new Solicitud(titular);
        s.addRHSolicitada(new RHSolicitada(LocalDate.of(2027, 4, 1), LocalDate.of(2027, 4, 3),
                Set.of(), 1, 0, 0, 0));
        Solicitud guardada = new RepositorioSolicitudes().guardar(s);

        RepositorioSolicitudes repo = new RepositorioSolicitudes();
        assertNull(repo.findReservaDeSolicitud(guardada.getId()));
        assertNull(repo.findContratoDeSolicitud(guardada.getId()));
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

        assertEquals(r.getId(), repo.findReservaDeSolicitud(s.getId()).getId());
        assertEquals(r.getId(), repo.findContratoDeSolicitud(s.getId()).getReserva().getId());
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

        assertEquals(1, s.getContratos().size());
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
        assertEquals(1, s.getContratos().size());
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
        assertEquals(1, s.getContratos().size());
    }

    @Test
    void crearReservaSinBloquesPendientesNoPersisteNada() {
        TestUtil.sembrarHabitaciones();
        TitularReserva titular = titularDemo();

        // Sin bloques pendientes no hay RHSolicitada que espejar: la invariante 1..* se rompe.
        assertThrows(IllegalStateException.class, () -> controlador().crearReserva(titular));

        assertEquals(0, new RepositorioReservas().findAll().size());
        assertEquals(0, new RepositorioSolicitudes().findAll().size());
        assertEquals(0, contarContratos());
    }

    @Test
    void crearReservaConTodosLosBloquesSaltadosNoPersisteNada() {
        TestUtil.sembrarHabitaciones();
        TitularReserva titular = titularDemo();
        ControladorReservas cr = controlador();

        // 9 matrimoniales no entran en ninguna habitacion: todos los bloques se saltan.
        cr.agregarBloqueReserva(LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 4), 9, 0, 0, 0, Set.of());

        assertThrows(IllegalStateException.class, () -> cr.crearReserva(titular));

        assertEquals(0, new RepositorioReservas().findAll().size());
        assertEquals(0, new RepositorioSolicitudes().findAll().size());
        assertEquals(0, contarContratos());
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
        assertEquals(1, s.getContratos().size());
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
        assertEquals(1, s.getContratos().size());
    }

    // ----------------------------------------------------- replacement

    /** Candidata mejorada en memoria: propuestas y titular, sin pagos. */
    private static Reserva reservaMejorada(TitularReserva titular, int numero,
                                           LocalDate ini, LocalDate fin) {
        return reservaMejorada(titular, habitacion(numero), ini, fin);
    }

    /** Variante con una habitacion ya resuelta (puede estar borrada: solo importa su id). */
    private static Reserva reservaMejorada(TitularReserva titular, Habitacion h,
                                           LocalDate ini, LocalDate fin) {
        Reserva r = new Reserva(titular);
        RHPropuesta p = new RHPropuesta(ini, fin, h.getAtributos(), 4500.0f,
                TipoMoneda.USD, 1, 0, 0);
        p.setHabitacion(h);
        r.addRHPropuesta(p);
        r.setActiva(true);
        r.setMontoTotal(4500.0f);
        r.setMontoSenia(0.0f);
        r.setMoneda(TipoMoneda.USD);
        r.setEstado(EstadoReserva.PendienteDePago);
        return r;
    }

    @Test
    void reemplazarReservaMigraPagosYConservaSolicitudPerfilYTitular() {
        TestUtil.sembrarHabitaciones();
        TitularReserva titular = titularDemo();
        PerfilRelajacion perfil = new PerfilRelajacion(true, true, true, NivelCapacidad.CAPACIDAD_TOTAL);

        Reserva vieja = controlador().crearReservaPara(titular, idHabitacion(103),
                LocalDate.of(2027, 11, 1), LocalDate.of(2027, 11, 4), 1, 1, 0,
                Set.of(), perfil);
        Solicitud solicitud = new RepositorioSolicitudes().findAll().get(0);
        Long contratoId = solicitud.getContratos().iterator().next().getId();

        RepositorioReservas repoReservas = new RepositorioReservas();
        repoReservas.agregarPago(vieja.getId(), 500.0f, TipoMoneda.USD, LocalDate.of(2027, 10, 1));

        Reserva nueva = reservaMejorada(titular, 101,
                LocalDate.of(2027, 11, 1), LocalDate.of(2027, 11, 4));

        Reserva reemplazada = controlador().reemplazarReserva(vieja.getId(), nueva);

        assertNotNull(reemplazada.getId());
        assertNull(repoReservas.findById(vieja.getId()), "la reserva vieja se elimina");
        assertEquals(1, repoReservas.findPagosByReservaId(reemplazada.getId()).size(),
                "el pago migra a la reserva nueva");
        assertEquals(0, repoReservas.findPagosByReservaId(vieja.getId()).size());

        RepositorioSolicitudes repo = new RepositorioSolicitudes();
        assertNotNull(repo.findById(solicitud.getId()), "la solicitud sobrevive");
        assertEquals(1, repo.findAll().get(0).getContratos().size());

        ContratoReserva contrato = repo.findContratoDeSolicitud(solicitud.getId());
        assertEquals(contratoId, contrato.getId(), "se reutiliza el contrato existente");
        assertEquals(reemplazada.getId(), contrato.getReserva().getId());
        assertTrue(contrato.isFechasRelajadas());
        assertTrue(contrato.isAtributosRelajados());
        assertTrue(contrato.isColchonesExtra());
        assertEquals(NivelCapacidad.CAPACIDAD_TOTAL, contrato.getNivelCapacidad());
        assertEquals(titular.getId(), reemplazada.getTitular().getId());
    }

    @Test
    void reemplazoFallidoConservaReservaPagosYContrato() {
        TestUtil.sembrarHabitaciones();
        TitularReserva titular = titularDemo();

        Reserva vieja = controlador().crearReservaPara(titular, idHabitacion(103),
                LocalDate.of(2027, 11, 10), LocalDate.of(2027, 11, 13), 1, 1, 0);
        Solicitud solicitud = new RepositorioSolicitudes().findAll().get(0);
        new RepositorioReservas().agregarPago(vieja.getId(), 300.0f, TipoMoneda.USD,
                LocalDate.of(2027, 10, 2));

        // La reserva nueva sin titular viola la cardinalidad y revierte todo.
        assertThrows(IllegalArgumentException.class,
                () -> controlador().reemplazarReserva(vieja.getId(), new Reserva(null)));

        RepositorioReservas repoReservas = new RepositorioReservas();
        assertNotNull(repoReservas.findById(vieja.getId()));
        assertEquals(1, repoReservas.findAll().size());
        assertEquals(1, repoReservas.findPagosByReservaId(vieja.getId()).size());
        assertEquals(vieja.getId(),
                new RepositorioSolicitudes().findReservaDeSolicitud(solicitud.getId()).getId());
    }

    @Test
    void reemplazoFallidoAMitadDeTransaccionRevierteReservaPagosYContrato() {
        TestUtil.sembrarHabitaciones();
        TitularReserva titular = titularDemo();

        Reserva vieja = controlador().crearReservaPara(titular, idHabitacion(103),
                LocalDate.of(2027, 11, 10), LocalDate.of(2027, 11, 13), 1, 1, 0);
        Solicitud solicitud = new RepositorioSolicitudes().findAll().get(0);
        Long contratoId = solicitud.getContratos().iterator().next().getId();
        RepositorioReservas repoReservas = new RepositorioReservas();
        repoReservas.agregarPago(vieja.getId(), 300.0f, TipoMoneda.USD,
                LocalDate.of(2027, 10, 2));

        // Habitacion real que se borra: su id queda apuntando a una fila inexistente.
        // La propuesta transitoria de la candidata la referencia, de modo que el
        // flush dentro de reemplazarReserva viola el FK despues de em.persist(nueva).
        ControladorHabitaciones.getInstance().altaHabitacion(999, Set.of(), 1, 0, 0, 0, 100.0f);
        Habitacion fantasma = habitacion(999);
        assertTrue(new RepositorioHabitaciones().eliminar(fantasma.getId()));
        assertNotNull(fantasma.getId());

        Reserva nueva = reservaMejorada(titular, fantasma,
                LocalDate.of(2027, 11, 10), LocalDate.of(2027, 11, 13));

        assertThrows(PersistenceException.class,
                () -> controlador().reemplazarReserva(vieja.getId(), nueva));
        assertNotNull(nueva.getId(),
                "el fallo debe ocurrir despues de em.persist(nueva), no en un guard previo");

        // Rollback: la reserva vieja, su pago y el contrato quedan intactos.
        assertEquals(1, repoReservas.findAll().size());
        assertNotNull(repoReservas.findById(vieja.getId()));
        assertEquals(1, repoReservas.findPagosByReservaId(vieja.getId()).size());
        assertEquals(vieja.getId(),
                new RepositorioSolicitudes().findReservaDeSolicitud(solicitud.getId()).getId());

        ContratoReserva contrato = new RepositorioSolicitudes()
                .findContratoDeSolicitud(solicitud.getId());
        assertEquals(contratoId, contrato.getId());
        assertEquals(vieja.getId(), contrato.getReserva().getId());
    }

    @Test
    void eliminarReservaConPagosDevuelveFalseYConservaReservaYPagos() {
        TestUtil.sembrarHabitaciones();
        TitularReserva titular = titularDemo();

        Reserva r = controlador().crearReservaPara(titular, idHabitacion(103),
                LocalDate.of(2027, 11, 20), LocalDate.of(2027, 11, 23), 1, 1, 0);
        new RepositorioReservas().agregarPago(r.getId(), 200.0f, TipoMoneda.USD,
                LocalDate.of(2027, 10, 3));

        assertFalse(controlador().eliminarReserva(r.getId()));
        assertNotNull(new RepositorioReservas().findById(r.getId()));
        assertEquals(1, new RepositorioReservas().findPagosByReservaId(r.getId()).size());
    }

    @Test
    void eliminarReservaBorraContratoYReservaPeroConservaSolicitud() {
        TestUtil.sembrarHabitaciones();
        TitularReserva titular = titularDemo();

        Reserva r = controlador().crearReservaPara(titular, idHabitacion(103),
                LocalDate.of(2027, 12, 10), LocalDate.of(2027, 12, 13), 1, 1, 0);
        Solicitud solicitud = new RepositorioSolicitudes().findAll().get(0);
        assertNotNull(new RepositorioSolicitudes().findContratoDeSolicitud(solicitud.getId()));

        assertTrue(controlador().eliminarReserva(r.getId()));

        RepositorioSolicitudes repo = new RepositorioSolicitudes();
        assertNull(new RepositorioReservas().findById(r.getId()));
        assertNull(repo.findContratoDeSolicitud(solicitud.getId()));
        assertNotNull(repo.findById(solicitud.getId()));
    }

    // ------------------------------------------- cleanup order (source)

    @Test
    void mainLimpiarDatosBorraContratosYSolicitudesAntesQueReservas() throws IOException {
        String fuente = Files.readString(Path.of("src/main/java/com/pms/Main.java"));

        int contratos = fuente.indexOf("new RepositorioSolicitudes().eliminarContratos()");
        int solicitudes = fuente.indexOf("new RepositorioSolicitudes().eliminarTodas()");
        int reservas = fuente.indexOf("new RepositorioReservas().eliminarTodas()");

        assertTrue(contratos >= 0, "Main.limpiarDatos debe borrar los contratos");
        assertTrue(solicitudes >= 0, "Main.limpiarDatos debe borrar las solicitudes");
        assertTrue(reservas >= 0, "Main.limpiarDatos debe borrar las reservas");
        assertTrue(contratos < solicitudes, "los contratos van antes que las solicitudes");
        assertTrue(solicitudes < reservas, "las solicitudes van antes que las reservas");
    }

    @Test
    void seleccionAbandonadaNoPersisteNada() {
        TestUtil.sembrarHabitaciones();
        TitularReserva titular = titularDemo();

        // Control negativo: una reserva y una solicitud persistidas a proposito SI
        // son visibles, lo que prueba que las consultas detectan filas.
        Reserva persistida = new RepositorioReservas().guardar(
                reservaMejorada(titular, 101, LocalDate.of(2027, 12, 1), LocalDate.of(2027, 12, 3)));
        Solicitud solicitudPersistida = new RepositorioSolicitudes().guardar(new Solicitud(titular));
        assertNotNull(persistida.getId());
        assertNotNull(solicitudPersistida.getId());
        assertEquals(1, new RepositorioReservas().findAll().size());
        assertEquals(1, new RepositorioSolicitudes().findAll().size());
        assertEquals(0, contarContratos());

        // ...y al limpiarlas las consultas vuelven a cero.
        new RepositorioSolicitudes().eliminarTodas();
        new RepositorioReservas().eliminarTodas();
        assertEquals(0, new RepositorioReservas().findAll().size());
        assertEquals(0, new RepositorioSolicitudes().findAll().size());

        // Candidatas efimeras: se construyen y se descartan sin seleccionar.
        reservaMejorada(titular, 101, LocalDate.of(2027, 12, 1), LocalDate.of(2027, 12, 3));
        reservaMejorada(titular, 102, LocalDate.of(2027, 12, 1), LocalDate.of(2027, 12, 3));

        assertEquals(0, new RepositorioReservas().findAll().size());
        assertEquals(0, new RepositorioSolicitudes().findAll().size());
        assertEquals(0, contarContratos());
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
