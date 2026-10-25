package com.pms.api;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.pms.TestUtil;
import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.entities.Reserva;
import com.pms.entities.TitularReserva;
import com.pms.persistence.JpaUtil;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST integration tests for the calendar endpoints (occupancy, late-checkout,
 * move, resize). Standalone MockMvc against the in-memory pms-test unit, same
 * as the rest of the suite. The endpoints delegate to ControladorReservas, so
 * the effects are also asserted through that controller's read views.
 */
class CalendarioControllerTest {

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
                .standaloneSetup(new CalendarioController())
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

    // --------------------------------------- ocupacion

    @Test
    void ocupacionDevuelveBloqueDeReservaActiva() throws Exception {
        Reserva r = crearReserva(101, "2026-11-01", "2026-11-04", 1, 1, 0);

        mockMvc.perform(get("/api/calendario").param("desde", "2026-11-01").param("hasta", "2026-11-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6));

        var ocup = ControladorReservas.getInstance().matrizOcupacion(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15));
        var bloques = ocup.stream()
                .filter(o -> o.habitacion().getNumero() == 101)
                .findFirst().orElseThrow().bloques();
        assertEquals(1, bloques.size());
        assertEquals(r.getId(), bloques.get(0).reservaId());
        assertEquals(LocalDate.of(2026, 11, 1), bloques.get(0).fechaInicio());
        assertEquals(LocalDate.of(2026, 11, 4), bloques.get(0).fechaFin());
    }

    @Test
    void lateCheckoutEsFalsoSinReserva() throws Exception {
        long h101 = idHabitacion(101);
        mockMvc.perform(get("/api/calendario/late-checkout")
                        .param("habitacionId", String.valueOf(h101))
                        .param("dia", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hayLateCheckout").value(false));
    }

    // --------------------------------------- mover

    @Test
    void moverReservaCambiaDeHabitacion() throws Exception {
        long h101 = idHabitacion(101);
        long h103 = idHabitacion(103);
        Reserva r = crearReserva(101, "2026-11-01", "2026-11-04", 1, 0, 0);

        mockMvc.perform(post("/api/reservas/" + r.getId() + "/mover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"habitacionOrigenId\":" + h101
                                + ",\"noche\":\"2026-11-01\",\"habitacionDestinoId\":" + h103 + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movida").value(true));

        var ocup = ControladorReservas.getInstance().matrizOcupacion(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15));
        assertTrue(ocup.stream().filter(o -> o.habitacion().getNumero() == 101)
                .findFirst().orElseThrow().bloques().isEmpty());
        assertEquals(1, ocup.stream().filter(o -> o.habitacion().getNumero() == 103)
                .findFirst().orElseThrow().bloques().size());
    }

    @Test
    void moverReservaRechazaPorCapacidad() throws Exception {
        long h201 = idHabitacion(201);
        long h101 = idHabitacion(101);
        // 2 matrimoniales = 4 personas; 101 (1M + 1I) no alcanza.
        Reserva r = crearReserva(201, "2026-11-01", "2026-11-04", 2, 0, 0);

        mockMvc.perform(post("/api/reservas/" + r.getId() + "/mover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"habitacionOrigenId\":" + h201
                                + ",\"noche\":\"2026-11-01\",\"habitacionDestinoId\":" + h101 + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.motivo").value("CAPACIDAD_INSUFICIENTE"))
                .andExpect(jsonPath("$.mensaje").value("No es posible mover a la habitación seleccionada"));
    }

    // --------------------------------------- redimensionar

    @Test
    void redimensionarReservaExtiendeLaEstadia() throws Exception {
        long h101 = idHabitacion(101);
        Reserva r = crearReserva(101, "2026-11-01", "2026-11-04", 1, 1, 0);

        mockMvc.perform(post("/api/reservas/" + r.getId() + "/redimensionar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"habitacionId\":" + h101
                                + ",\"noche\":\"2026-11-02\",\"nuevoCheckIn\":\"2026-11-01\",\"nuevoCheckOut\":\"2026-11-06\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkOut").value("2026-11-06"))
                .andExpect(jsonPath("$.checkIn").value("2026-11-01"));
    }

    @Test
    void redimensionarReservaRechazaSolape() throws Exception {
        long h101 = idHabitacion(101);
        Reserva a = crearReserva(101, "2026-11-01", "2026-11-04", 1, 1, 0);
        crearReserva(101, "2026-11-10", "2026-11-15", 1, 1, 0);

        mockMvc.perform(post("/api/reservas/" + a.getId() + "/redimensionar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"habitacionId\":" + h101
                                + ",\"noche\":\"2026-11-02\",\"nuevoCheckIn\":\"2026-11-01\",\"nuevoCheckOut\":\"2026-11-12\"}"))
                .andExpect(status().isConflict());
    }
}
