package com.pms;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.controllers.ControladorReservas.OcupacionHabitacion;
import com.pms.entities.Habitacion;
import com.pms.entities.RHPropuesta;
import com.pms.entities.Reserva;
import com.pms.entities.TitularReserva;
import com.pms.persistence.JpaUtil;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.EstadoReserva;
import com.pms.utils.TipoMoneda;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Puente entre el dialogo (parejas / personas individuales / bebes) y la
 * asignacion fisica de camas, colchones y cunas.
 */
class ControladorReservasPersonasTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);

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

    private static OcupacionHabitacion porNumero(List<OcupacionHabitacion> matriz, int numero) {
        return matriz.stream()
                .filter(o -> o.habitacion().getNumero() == numero)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void crearReservaParaPersonasGuardaCategoriasYCamas() {
        Reserva r = cr.crearReservaParaPersonas(altaTitular(), idHabitacion(103),
                INICIO, INICIO.plusDays(3), 1, 2, 2, Set.of());

        RHPropuesta p = r.getRHPropuestas().iterator().next();
        assertEquals(1, p.getCamasMatrimoniales());
        assertEquals(2, p.getCamasIndividuales());
        assertEquals(0, p.getCunas(), "103 no tiene cunas");
        assertEquals(1, p.getCantMatrimonios());
        assertEquals(2, p.getCantPersonasIndividuales());
        assertEquals(2, p.getCantBebes());
        assertEquals(4, p.getCantPersonas());

        OcupacionHabitacion.BloqueNoche bloque = porNumero(
                cr.matrizOcupacion(INICIO, INICIO.plusDays(14)), 103).bloques().get(0);
        assertEquals(4, bloque.personas());
        assertEquals(2, bloque.bebes());
    }

    @Test
    void crearReservaParaPersonasUsaColchonesCuandoHacenFalta() {
        ControladorHabitaciones.getInstance().altaHabitacion(
                999, Set.of(), 1, 0, 0, 2, 1000f);
        long h999 = idHabitacion(999);

        Reserva r = cr.crearReservaParaPersonas(altaTitular(), h999,
                INICIO, INICIO.plusDays(2), 2, 0, 0, Set.of());

        RHPropuesta p = r.getRHPropuestas().iterator().next();
        assertEquals(1, p.getCamasMatrimoniales());
        assertEquals(2, p.getCamasIndividuales(), "la pareja sobrante usa dos colchones");
        assertEquals(2, p.getCantMatrimonios());
        assertEquals(0, p.getCantPersonasIndividuales());
        assertEquals(4, p.getCantPersonas());
    }

    @Test
    void actualizarReservaPersonasRecalculaLasCamas() {
        long h103 = idHabitacion(103);
        Reserva original = cr.crearReservaParaPersonas(altaTitular(), h103,
                INICIO, INICIO.plusDays(2), 1, 0, 0, Set.of());

        Reserva actualizada = cr.actualizarReservaPersonas(original.getId(), h103, INICIO,
                altaTitular(), h103, INICIO, INICIO.plusDays(3), 1, 2, 0,
                EstadoReserva.PendienteDePago, Set.of());

        RHPropuesta p = actualizada.getRHPropuestas().iterator().next();
        assertEquals(1, p.getCamasMatrimoniales());
        assertEquals(2, p.getCamasIndividuales());
        assertEquals(1, p.getCantMatrimonios());
        assertEquals(2, p.getCantPersonasIndividuales());
        assertEquals(4, p.getCantPersonas());
    }

    @Test
    void crearReservaAutomaticaPersonasNoCuentaBebesParaLaCapacidad() {
        Reserva r = cr.crearReservaAutomaticaPersonas(altaTitular(),
                INICIO, INICIO.plusDays(3), 1, 0, 2,
                Set.of(AtributoHabitacion.Balcon));

        RHPropuesta p = r.getRHPropuestas().iterator().next();
        assertEquals(101, p.getHabitacion().getNumero().intValue(),
                "con 2 personas alcanza la habitacion mas chica con Balcon");
        assertEquals(1, p.getCantMatrimonios());
        assertEquals(2, p.getCantBebes());
        assertEquals(101, p.getHabitacion().getNumero().intValue());
    }

    @Test
    void actualizarReservaPersonasRechazaCapacidadInsuficiente() {
        long h103 = idHabitacion(103);
        Reserva original = cr.crearReservaParaPersonas(altaTitular(), h103,
                INICIO, INICIO.plusDays(2), 1, 0, 0, Set.of());

        assertThrows(IllegalStateException.class, () -> cr.actualizarReservaPersonas(
                original.getId(), h103, INICIO, altaTitular(), h103,
                INICIO, INICIO.plusDays(3), 3, 0, 0,
                EstadoReserva.PendienteDePago, Set.of()));
    }
}
