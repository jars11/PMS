package com.pms;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorReservas;
import com.pms.dtos.DTRHSolicitada;
import com.pms.entities.RHPropuesta;
import com.pms.handlers.InteraccionUsuario;
import com.pms.handlers.ManejadorBusquedaHuecos;
import com.pms.persistence.JpaUtil;
import com.pms.utils.AtributoHabitacion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests del algoritmo {@code buscarHuecos}. Cada test siembra sólo las
 * habitaciones que necesita para controlar la disponibilidad, y usa un doble de
 * {@link InteraccionUsuario} con respuestas guionadas.
 */
class ManejadorBusquedaHuecosTest {

    private static final LocalDate INI = LocalDate.of(2027, 1, 10);
    private static final LocalDate FIN = LocalDate.of(2027, 1, 15);

    @BeforeAll
    static void antesDeTodo() {
        JpaUtil.setUnidadPorDefecto("pms-test");
    }

    @BeforeEach
    void limpiar() {
        TestUtil.limpiarBase();
    }

    /** Doble guionado: responde con la cola y registra preguntas/notificaciones. */
    static final class InteraccionGuion implements InteraccionUsuario {
        private final Deque<Boolean> respuestas = new ArrayDeque<>();
        final List<String> preguntas = new ArrayList<>();
        final List<String> notificaciones = new ArrayList<>();

        InteraccionGuion(Boolean... respuestas) {
            this.respuestas.addAll(Arrays.asList(respuestas));
        }

        @Override
        public boolean confirmar(String pregunta) {
            preguntas.add(pregunta);
            return !respuestas.isEmpty() && respuestas.poll();
        }

        @Override
        public void notificar(String mensaje) {
            notificaciones.add(mensaje);
        }
    }

    private DTRHSolicitada solicitud(Set<AtributoHabitacion> atributos, int matrimonios, int individuales) {
        return solicitud(atributos, matrimonios, individuales, 0, 0);
    }

    private DTRHSolicitada solicitud(Set<AtributoHabitacion> atributos, int matrimonios, int individuales,
                                     int bebes, int cunas) {
        return new DTRHSolicitada(INI, FIN, atributos, matrimonios, individuales, bebes, cunas);
    }

    private RHPropuesta unica(Set<Set<RHPropuesta>> resultado) {
        assertEquals(1, resultado.size(), "se esperaba una sola alternativa");
        Set<RHPropuesta> alternativa = resultado.iterator().next();
        assertEquals(1, alternativa.size(), "se esperaba una sola propuesta");
        return alternativa.iterator().next();
    }

    @Test
    void fitExactoSinPreguntas() {
        habitacion(101, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0, 1500f);
        InteraccionGuion inter = new InteraccionGuion();

        Set<Set<RHPropuesta>> res = new ManejadorBusquedaHuecos()
                .buscarHuecos(Set.of(solicitud(Set.of(AtributoHabitacion.Balcon), 1, 1)), inter);

        assertTrue(inter.preguntas.isEmpty(), "no debería preguntar nada: " + inter.preguntas);
        RHPropuesta rp = unica(res);
        assertEquals(101, rp.getHabitacion().getNumero().intValue());
    }

    /**
     * Las camas de la propuesta son las PEDIDAS (un subconjunto de las de la
     * habitacion), no todas las camas del cuarto. La cantidad de personas se
     * deriva de esas camas.
     */
    @Test
    void laPropuestaUsaLasCamasPedidasYNoTodasLasDeLaHabitacion() {
        habitacion(101, Set.of(), 3, 3, 0, 0, 1000f);
        InteraccionGuion inter = new InteraccionGuion();

        Set<Set<RHPropuesta>> res = new ManejadorBusquedaHuecos()
                .buscarHuecos(Set.of(solicitud(Set.of(), 1, 1)), inter);

        RHPropuesta rp = unica(res);
        assertEquals(1, rp.getCamasMatrimoniales(), "solo la cama matrimonial pedida");
        assertEquals(1, rp.getCamasIndividuales(), "solo la cama individual pedida");
        assertEquals(3, rp.getCantPersonas(), "el conteo de personas es el pedido");
        assertEquals(3, RHPropuesta.personasDerivadas(
                rp.getCamasMatrimoniales(), rp.getCamasIndividuales()));
    }

    @Test
    void capacidadTotalInsuficienteConColchonesOfreceYUsaColchones() {
        // capacidad 2, con 2 colchones llega a 4; se piden 4 personas
        habitacion(101, Set.of(), 1, 0, 0, 2, 1000f);
        InteraccionGuion inter = new InteraccionGuion(true);

        Set<Set<RHPropuesta>> res = new ManejadorBusquedaHuecos()
                .buscarHuecos(Set.of(solicitud(Set.of(), 1, 2)), inter);

        assertFalse(inter.preguntas.isEmpty());
        assertTrue(inter.preguntas.get(0).toLowerCase().contains("colchones"), inter.preguntas.get(0));
        RHPropuesta rp = unica(res);
        assertEquals(101, rp.getHabitacion().getNumero().intValue());
        assertEquals(2, rp.getCamasIndividuales(), "2 individuales = 0 del cuarto + 2 colchones");
    }

    @Test
    void capacidadInsuficienteOfreceOtrasFechasYDevuelveMapa() {
        // Dos solicitudes (6 personas) pero una sola habitación de capacidad 3:
        // la capacidad total no alcanza y se ofrecen otras fechas.
        habitacion(101, Set.of(), 1, 1, 0, 0, 1000f);
        InteraccionGuion inter = new InteraccionGuion(true); // acepta otras fechas

        Set<DTRHSolicitada> solicitudes = Set.of(
                solicitud(Set.of(), 1, 1, 0, 0),
                solicitud(Set.of(), 1, 1, 1, 1));
        Set<Set<RHPropuesta>> res = new ManejadorBusquedaHuecos().buscarHuecos(solicitudes, inter);

        assertFalse(inter.preguntas.isEmpty());
        assertTrue(inter.preguntas.get(0).toLowerCase().contains("fechas"), inter.preguntas.get(0));
        assertFalse(res.isEmpty());
        assertTrue(contieneHabitacion(res, 101));
    }

    @Test
    void declinarTodoDevuelveVacio() {
        habitacion(101, Set.of(), 1, 0, 0, 0, 1000f);
        InteraccionGuion inter = new InteraccionGuion(false); // rechaza todo

        Set<Set<RHPropuesta>> res = new ManejadorBusquedaHuecos()
                .buscarHuecos(Set.of(solicitud(Set.of(), 1, 1)), inter);

        assertTrue(res.isEmpty());
    }

    @Test
    void distribucionDeCamasInsuficienteOfreceRelajar() {
        habitacion(101, Set.of(), 2, 0, 0, 0, 1000f); // capacidad 4, 2 matrimoniales
        InteraccionGuion inter = new InteraccionGuion(true);

        Set<Set<RHPropuesta>> res = new ManejadorBusquedaHuecos()
                .buscarHuecos(Set.of(solicitud(Set.of(), 1, 1)), inter);

        assertFalse(inter.preguntas.isEmpty());
        assertTrue(inter.preguntas.get(0).toLowerCase().contains("camas"), inter.preguntas.get(0));
        RHPropuesta rp = unica(res);
        assertEquals(101, rp.getHabitacion().getNumero().intValue());
    }

    @Test
    void atributosInsuficientesOfreceRelajarYNotificaFaltantes() {
        habitacion(101, Set.of(AtributoHabitacion.Balcon, AtributoHabitacion.VistaAlMar), 1, 1, 0, 0, 1500f);
        InteraccionGuion inter = new InteraccionGuion(true);

        Set<AtributoHabitacion> pedidos = Set.of(AtributoHabitacion.Balcon,
                AtributoHabitacion.VistaAlMar, AtributoHabitacion.Jacuzzi);
        Set<Set<RHPropuesta>> res = new ManejadorBusquedaHuecos()
                .buscarHuecos(Set.of(solicitud(pedidos, 1, 1)), inter);

        assertFalse(inter.preguntas.isEmpty());
        assertTrue(inter.preguntas.get(0).toLowerCase().contains("atributos"), inter.preguntas.get(0));
        assertTrue(inter.notificaciones.stream().anyMatch(m -> m.contains("Jacuzzi")),
                "debería avisar que falta Jacuzzi: " + inter.notificaciones);
        RHPropuesta rp = unica(res);
        assertEquals(101, rp.getHabitacion().getNumero().intValue());
    }

    @Test
    void fechasRelajadasDevuelveMapaDeDisponibilidad() {
        habitacion(101, Set.of(), 1, 1, 0, 0, 1000f);
        // Ocupo el 12 y 13 (dos noches) para partir la disponibilidad
        ControladorReservas cr = ControladorReservas.getInstance();
        cr.agregarBloqueReserva(LocalDate.of(2027, 1, 12), LocalDate.of(2027, 1, 14),
                1, 1, 0, 0, Set.of());
        cr.crearReserva(TestUtil.demoTitular());

        InteraccionGuion inter = new InteraccionGuion(true);

        Set<Set<RHPropuesta>> res = new ManejadorBusquedaHuecos()
                .buscarHuecos(Set.of(solicitud(Set.of(), 1, 1)), inter);

        assertFalse(inter.preguntas.isEmpty());
        assertFalse(res.isEmpty());
        assertTrue(contieneHabitacion(res, 101));
    }

    @Test
    void nivel2SinAtributosPedidosDevuelveUnaAlternativa() {
        // Sin atributos, el mínimo que cubre la capacidad es un solo cuarto.
        habitacion(101, Set.of(), 2, 2, 0, 0, 1000f);
        habitacion(102, Set.of(), 2, 2, 0, 0, 1000f);
        InteraccionGuion inter = new InteraccionGuion();

        Set<Set<RHPropuesta>> res = new ManejadorBusquedaHuecos()
                .buscarHuecos(Set.of(solicitud(Set.of(), 1, 1)), inter, false, 2, false, false, 15);

        assertEquals(1, res.size(), "sin atributos: una sola alternativa");
        assertEquals(1, res.iterator().next().size(), "un solo cuarto alcanza la capacidad");
    }

    @Test
    void nivel2ConAtributosRelajadosDevuelveUnaAlternativaPorCoberturaMaximal() {
        // Capacidad: 1 habitación; ni 101 ni 102 cubren {Balcón, VistaAlMar},
        // así que las coberturas maximales son {Balcón} y {VistaAlMar}.
        habitacion(101, Set.of(AtributoHabitacion.Balcon), 2, 2, 0, 0, 1000f);
        habitacion(102, Set.of(AtributoHabitacion.VistaAlMar), 2, 2, 0, 0, 1000f);
        InteraccionGuion inter = new InteraccionGuion();

        Set<DTRHSolicitada> solicitudes = Set.of(solicitud(
                Set.of(AtributoHabitacion.Balcon, AtributoHabitacion.VistaAlMar), 1, 1));
        Set<Set<RHPropuesta>> res = new ManejadorBusquedaHuecos()
                .buscarHuecos(solicitudes, inter, false, 2, true, false, 15);

        assertEquals(2, res.size(), "una alternativa por cobertura maximal");
        assertTrue(contieneHabitacion(res, 101));
        assertTrue(contieneHabitacion(res, 102));
    }

    @Test
    void nivel2SinRelajarCuandoNoAlcanzaOfreceRelajarAtributos() {
        habitacion(101, Set.of(AtributoHabitacion.Balcon), 2, 2, 0, 0, 1000f);
        habitacion(102, Set.of(AtributoHabitacion.VistaAlMar), 2, 2, 0, 0, 1000f);
        InteraccionGuion inter = new InteraccionGuion(true); // acepta relajar atributos

        Set<DTRHSolicitada> solicitudes = Set.of(solicitud(
                Set.of(AtributoHabitacion.Balcon, AtributoHabitacion.VistaAlMar), 1, 1));
        Set<Set<RHPropuesta>> res = new ManejadorBusquedaHuecos()
                .buscarHuecos(solicitudes, inter, false, 2, false, false, 15);

        assertFalse(inter.preguntas.isEmpty());
        assertTrue(inter.preguntas.get(0).toLowerCase().contains("atributos"), inter.preguntas.get(0));
        assertEquals(2, res.size(), "tras relajar, dos coberturas maximales");
    }

    /**
     * Caso real: tres matrimonios (6 personas) para una noche en habitaciones de
     * 2 matrimoniales + 2 individuales. Se debe ofrecer relajar la distribucion y
     * encontrar 201/202/203, sin excepcion.
     */
    @Test
    void tresMatrimoniosRelajanDistribucionYEncuentranHabitacion() {
        habitacion(201, Set.of(), 2, 2, 0, 0, 2600f);
        habitacion(202, Set.of(), 2, 2, 0, 0, 2400f);
        habitacion(203, Set.of(), 2, 2, 0, 0, 2300f);
        LocalDate dia = LocalDate.of(2026, 10, 22);
        DTRHSolicitada s = new DTRHSolicitada(dia, dia.plusDays(1), Set.of(), 3, 0, 0, 0);
        InteraccionGuion inter = new InteraccionGuion(true);

        Set<Set<RHPropuesta>> res = new ManejadorBusquedaHuecos()
                .buscarHuecos(Set.of(s), inter);

        assertFalse(res.isEmpty(), "deberia encontrar 201/202/203. preguntas="
                + inter.preguntas + " notifs=" + inter.notificaciones);
        RHPropuesta rp = res.iterator().next().iterator().next();
        assertEquals(203, rp.getHabitacion().getNumero().intValue(),
                "203 es la mas barata de las tres");
        assertEquals(3, rp.getCantMatrimonios());
        assertEquals(6, rp.getCantPersonas(), "tres parejas son seis personas");
        assertEquals(2, rp.getCamasMatrimoniales());
        assertEquals(2, rp.getCamasIndividuales());
    }

    /**
     * Dos bloques DISTINTOS de un matrimonio cada uno para la misma noche deben
     * conservarse como dos solicitudes y ocupar dos habitaciones.
     */
    @Test
    void dosBloquesIgualesDeUnMatrimonioOcupanDosHabitaciones() {
        habitacion(201, Set.of(), 2, 2, 0, 0, 2600f);
        habitacion(202, Set.of(), 2, 2, 0, 0, 2400f);
        habitacion(203, Set.of(), 2, 2, 0, 0, 2300f);
        LocalDate dia = LocalDate.of(2026, 10, 22);
        DTRHSolicitada a = new DTRHSolicitada(dia, dia.plusDays(1), Set.of(), 1, 0, 0, 0);
        DTRHSolicitada b = new DTRHSolicitada(dia, dia.plusDays(1), Set.of(), 1, 0, 0, 0);
        InteraccionGuion inter = new InteraccionGuion();
        assertTrue(ControladorHabitaciones.getInstance().habitacionesLibresEnNoche(dia).size() >= 2,
                "deberia haber al menos dos habitaciones libres esa noche");

        Set<Set<RHPropuesta>> res = new ManejadorBusquedaHuecos()
                .buscarHuecos(List.of(a, b), inter);

        assertFalse(res.isEmpty(), "deberia encontrar dos habitaciones. preguntas="
                + inter.preguntas + " notifs=" + inter.notificaciones);
        assertEquals(1, res.size(), "debe quedar una sola propuesta (la mas barata)");
        Set<RHPropuesta> conjunto = res.iterator().next();
        assertEquals(2, conjunto.size(),
                "cada bloque debe quedar en una habitacion distinta");
        Set<Integer> numeros = conjunto.stream()
                .map(p -> p.getHabitacion().getNumero())
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of(202, 203), numeros, "202+203 es el par mas barato");
        float total = (float) conjunto.stream().mapToDouble(RHPropuesta::getSubtotal).sum();
        assertEquals(4700.0f, total, 0.001f, "1 noche x (2400 + 2300)");
    }

    private boolean contieneHabitacion(Set<Set<RHPropuesta>> resultado, int numero) {
        return resultado.stream().flatMap(Set::stream)
                .anyMatch(rp -> rp.getHabitacion().getNumero() == numero);
    }

    private void habitacion(int numero, Set<AtributoHabitacion> atributos,
                            int matrimoniales, int individuales, int cunas, int colchones, float tarifa) {
        ControladorHabitaciones.getInstance()
                .altaHabitacion(numero, atributos, matrimoniales, individuales, cunas, colchones, tarifa);
    }
}
