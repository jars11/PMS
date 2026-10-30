package com.pms.api;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.pms.TestUtil;
import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.entities.Reserva;
import com.pms.entities.TitularReserva;
import com.pms.persistence.JpaUtil;
import com.pms.persistence.RepositorioReservas;
import com.pms.utils.TipoMoneda;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST tests for the reservation endpoints driven from the web calendar: the
 * edit prefill read, the payment-guarded delete and the move-compatibility
 * read the calendar uses to grey out destination rows. All of them delegate to
 * {@link ControladorReservas} ({@code edicionDe} / {@code eliminarReserva} /
 * {@code compatibilidadMovimiento}); the effects are asserted through that
 * controller's read views as well.
 */
class ReservasEdicionEliminarTest {

    private MockMvc mockMvc;
    private TitularReserva titular;

    @BeforeAll
    static void antesDeTodo() {
        JpaUtil.setUnidadPorDefecto("pms-test");
    }

    @BeforeEach
    void preparar() {
        TestUtil.limpiarBase();
        TestUtil.sembrarHabitaciones();
        titular = ControladorPersonas.getInstance().altaTitular(
                List.of("Agustin"), List.of("Rivero"), "091000000", "22000000", "agustin@example.com");

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ReservasController())
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()))
                .build();
    }

    private long idHabitacion(int numero) {
        return ControladorHabitaciones.getInstance().getHabitaciones().stream()
                .filter(h -> h.getNumero() == numero)
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private Reserva crearReserva(int numero, String ini, String fin, int m, int i, int c) {
        return ControladorReservas.getInstance().crearReservaPara(titular, idHabitacion(numero),
                LocalDate.parse(ini), LocalDate.parse(fin), m, i, c);
    }

    // --------------------------------------- edicion

    @Test
    void edicionDevuelveLosDatosQuePrellenaElFormulario() throws Exception {
        long h101 = idHabitacion(101);
        Reserva r = crearReserva(101, "2026-11-01", "2026-11-04", 2, 1, 1);

        mockMvc.perform(get("/api/reservas/" + r.getId() + "/edicion")
                        .param("habitacionId", String.valueOf(h101))
                        .param("noche", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservaId").value(r.getId()))
                .andExpect(jsonPath("$.habitacionId").value(h101))
                .andExpect(jsonPath("$.noche").value("2026-11-02"))
                .andExpect(jsonPath("$.titular").value("Agustin Rivero"))
                .andExpect(jsonPath("$.titularId").value(titular.getId()))
                .andExpect(jsonPath("$.checkIn").value("2026-11-01"))
                .andExpect(jsonPath("$.checkOut").value("2026-11-04"))
                .andExpect(jsonPath("$.camasMatrimoniales").value(2))
                .andExpect(jsonPath("$.camasIndividuales").value(1))
                .andExpect(jsonPath("$.cunas").value(1))
                .andExpect(jsonPath("$.estado").value("PendienteDePago"))
                .andExpect(jsonPath("$.lateCheckout").value(false));
    }

    @Test
    void edicionDeReservaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/reservas/999999/edicion")
                        .param("habitacionId", String.valueOf(idHabitacion(101)))
                        .param("noche", "2026-11-02"))
                .andExpect(status().isNotFound());
    }

    @Test
    void edicionSinPropuestaEnEsaNocheDevuelve400() throws Exception {
        long h101 = idHabitacion(101);
        Reserva r = crearReserva(101, "2026-11-01", "2026-11-04", 1, 0, 0);

        mockMvc.perform(get("/api/reservas/" + r.getId() + "/edicion")
                        .param("habitacionId", String.valueOf(h101))
                        .param("noche", "2026-12-24"))
                .andExpect(status().isBadRequest());
    }

    // --------------------------------------- late check-out

    private static String jsonLateCheckout(boolean lateCheckout) {
        return "{\"lateCheckout\":" + lateCheckout + "}";
    }

    @Test
    void marcarYDesmarcarLateCheckoutDevuelveLaVistaActualizada() throws Exception {
        long h101 = idHabitacion(101);
        Reserva r = crearReserva(101, "2026-11-01", "2026-11-04", 1, 0, 0);

        mockMvc.perform(post("/api/reservas/" + r.getId() + "/late-checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLateCheckout(true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(r.getId()))
                .andExpect(jsonPath("$.lateCheckout").value(true));

        mockMvc.perform(get("/api/reservas/" + r.getId() + "/edicion")
                        .param("habitacionId", String.valueOf(h101))
                        .param("noche", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lateCheckout").value(true));

        mockMvc.perform(post("/api/reservas/" + r.getId() + "/late-checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLateCheckout(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lateCheckout").value(false));

        mockMvc.perform(get("/api/reservas/" + r.getId() + "/edicion")
                        .param("habitacionId", String.valueOf(h101))
                        .param("noche", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lateCheckout").value(false));
    }

    @Test
    void marcarLateCheckoutConCheckInElMismoDiaDevuelve409YSinMutar() throws Exception {
        long h101 = idHabitacion(101);
        Reserva r = crearReserva(101, "2026-11-01", "2026-11-04", 1, 0, 0);
        // La siguiente reserva hace check-in el dia de salida de la primera.
        crearReserva(101, "2026-11-04", "2026-11-06", 1, 0, 0);

        mockMvc.perform(post("/api/reservas/" + r.getId() + "/late-checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLateCheckout(true)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        "no se puede marcar late check-out: hay una reserva con check-in el mismo día"));

        mockMvc.perform(get("/api/reservas/" + r.getId() + "/edicion")
                        .param("habitacionId", String.valueOf(h101))
                        .param("noche", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lateCheckout").value(false));
    }

    @Test
    void listarExponeElFlagLateCheckoutQuePrecargaElFormulario() throws Exception {
        Reserva r = crearReserva(101, "2026-11-01", "2026-11-04", 1, 0, 0);
        ControladorReservas.getInstance().marcarLateCheckout(r.getId(), true);

        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(r.getId()))
                .andExpect(jsonPath("$[0].lateCheckout").value(true));
    }

    // --------------------------------------- compatibilidad de movimiento

    /**
     * Una entrada por habitacion, en el orden de la vista (numero). La 102 pide
     * Vista al mar + Jacuzzi y la 101 solo tiene Balcon: mover ese bloque a la
     * 101 se rechaza por atributos, aunque el calendario heuristica local no lo
     * veia (bug de la reserva 17 a la 101).
     */
    @Test
    void compatibilidadDevuelveElMotivoPorHabitacion() throws Exception {
        long h101 = idHabitacion(101);
        long h102 = idHabitacion(102);
        long h103 = idHabitacion(103);
        Reserva r = crearReserva(102, "2026-11-01", "2026-11-04", 1, 1, 0);

        mockMvc.perform(get("/api/reservas/" + r.getId() + "/compatibilidad")
                        .param("habitacionOrigenId", String.valueOf(h102))
                        .param("noche", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].habitacionId").value(h101))
                .andExpect(jsonPath("$[0].motivo").value("ATRIBUTOS_FALTANTES"))
                // La habitacion del bloque sigue siendo compatible consigo misma.
                .andExpect(jsonPath("$[1].habitacionId").value(h102))
                .andExpect(jsonPath("$[1].motivo").value(nullValue()))
                .andExpect(jsonPath("$[2].habitacionId").value(h103))
                .andExpect(jsonPath("$[2].motivo").value("ATRIBUTOS_FALTANTES"));
    }

    @Test
    void compatibilidadMarcaCapacidadCuandoLasCamasNoAlcanzan() throws Exception {
        long h101 = idHabitacion(101);
        long h103 = idHabitacion(103);
        // 1 matrimonial + 2 individuales no entran en la 101 (1 matrimonial + 1 individual).
        Reserva r = crearReserva(103, "2026-11-01", "2026-11-04", 1, 2, 0);

        mockMvc.perform(get("/api/reservas/" + r.getId() + "/compatibilidad")
                        .param("habitacionOrigenId", String.valueOf(h103))
                        .param("noche", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].habitacionId").value(h101))
                .andExpect(jsonPath("$[0].motivo").value("CAPACIDAD_INSUFICIENTE"));
    }

    @Test
    void compatibilidadNoDevuelveMotivoCuandoLaHabitacionCumple() throws Exception {
        long h101 = idHabitacion(101);
        long h103 = idHabitacion(103);
        Reserva r = crearReserva(101, "2026-11-01", "2026-11-04", 1, 1, 0);

        mockMvc.perform(get("/api/reservas/" + r.getId() + "/compatibilidad")
                        .param("habitacionOrigenId", String.valueOf(h101))
                        .param("noche", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].habitacionId").value(h103))
                .andExpect(jsonPath("$[2].motivo").value(nullValue()));
    }

    @Test
    void compatibilidadDeReservaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/reservas/999999/compatibilidad")
                        .param("habitacionOrigenId", String.valueOf(idHabitacion(101)))
                        .param("noche", "2026-11-02"))
                .andExpect(status().isNotFound());
    }

    // --------------------------------------- eliminar

    @Test
    void eliminarReservaSinPagosDevuelve204YBorraLaReserva() throws Exception {
        Reserva r = crearReserva(101, "2026-11-01", "2026-11-04", 1, 0, 0);

        mockMvc.perform(delete("/api/reservas/" + r.getId()))
                .andExpect(status().isNoContent());

        assertEquals(0, ControladorReservas.getInstance().listarReservas().size());
    }

    @Test
    void eliminarReservaConPagosDevuelve409YMensajeExacto() throws Exception {
        Reserva r = crearReserva(101, "2026-11-01", "2026-11-04", 1, 0, 0);
        new RepositorioReservas().agregarPago(r.getId(), 5000.0f, TipoMoneda.USD,
                LocalDate.of(2026, 10, 5));

        mockMvc.perform(delete("/api/reservas/" + r.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje")
                        .value("No se puede eliminar: la reserva tiene pagos asociados."));

        assertEquals(1, ControladorReservas.getInstance().listarReservas().size());
        assertEquals(1, ControladorReservas.getInstance().mostrarPagos(r.getId()).size());
    }
}
