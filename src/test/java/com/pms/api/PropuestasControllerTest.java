package com.pms.api;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.pms.TestUtil;
import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.entities.Reserva;
import com.pms.entities.TitularReserva;
import com.pms.persistence.JpaUtil;
import com.pms.utils.TipoMoneda;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST tests for the "Propuesta de estadía" endpoints. The search reuses
 * {@code ManejadorBusquedaHuecos.buscarHuecos} with a non-interactive
 * interaction (it never relaxes the request on its own) and the persist reuses
 * {@code ControladorReservas.persistirPropuestas}; the effects are asserted
 * through the existing reservation list view as well.
 */
class PropuestasControllerTest {

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
                .standaloneSetup(new PropuestasController(), new ReservasController())
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

    private static String jsonBloqueBusqueda(String checkIn, String checkOut, String atributos,
                                             int matrimoniales, int individuales, int bebes, int cunas) {
        return "{\"fechaInicio\":\"" + checkIn + "\""
                + ",\"fechaFin\":\"" + checkOut + "\""
                + ",\"atributos\":[" + atributos + "]"
                + ",\"camasMatrimoniales\":" + matrimoniales
                + ",\"camasIndividuales\":" + individuales
                + ",\"bebes\":" + bebes
                + ",\"cunas\":" + cunas + "}";
    }

    private static String jsonBusqueda(String... bloques) {
        return "{\"bloques\":[" + String.join(",", bloques) + "]}";
    }

    private static String jsonBloquePersistir(long habitacionId, String checkIn, String checkOut,
                                              int matrimoniales, int individuales, int cunas,
                                              String atributos) {
        return "{\"habitacionId\":" + habitacionId
                + ",\"checkIn\":\"" + checkIn + "\""
                + ",\"checkOut\":\"" + checkOut + "\""
                + ",\"camasMatrimoniales\":" + matrimoniales
                + ",\"camasIndividuales\":" + individuales
                + ",\"cunas\":" + cunas
                + ",\"atributos\":[" + atributos + "]}";
    }

    private static String jsonPersistir(Long titularId, String... bloques) {
        return "{\"titularId\":" + (titularId == null ? "null" : titularId)
                + ",\"bloques\":[" + String.join(",", bloques) + "]}";
    }

    // --------------------------------------- buscar

    @Test
    void buscarDevuelveLaPropuestaMasBarataQueCubreElBloque() throws Exception {
        mockMvc.perform(post("/api/propuestas/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBusqueda(jsonBloqueBusqueda("2026-11-01", "2026-11-04",
                                "", 1, 0, 0, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propuestas.length()").value(1))
                .andExpect(jsonPath("$.propuestas[0].bloques.length()").value(1))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].habitacionId").value(idHabitacion(101)))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].numero").value(101))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].checkIn").value("2026-11-01"))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].checkOut").value("2026-11-04"))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].subtotal").value(4500.0))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].moneda").value("USD"))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].camasMatrimoniales").value(1))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].camasIndividuales").value(0))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].cunas").value(0))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].atributos.length()").value(1))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].atributos[0]").value("Balcon"));
    }

    @Test
    void buscarRespetaLosAtributosPedidos() throws Exception {
        mockMvc.perform(post("/api/propuestas/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBusqueda(jsonBloqueBusqueda("2026-11-01", "2026-11-04",
                                "\"VistaAlMar\"", 1, 0, 0, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propuestas.length()").value(1))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].numero").value(104))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].subtotal").value(6300.0))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].atributos.length()").value(1))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].atributos[0]").value("VistaAlMar"));
    }

    @Test
    void buscarSinCapacidadDevuelveListaVacia() throws Exception {
        mockMvc.perform(post("/api/propuestas/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBusqueda(jsonBloqueBusqueda("2026-11-01", "2026-11-04",
                                "", 99, 0, 0, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propuestas.length()").value(0));

        assertTrue(ControladorReservas.getInstance().getReservas().isEmpty());
    }

    @Test
    void buscarDosBloquesDevuelveUnaPropuestaConLosDosBloques() throws Exception {
        mockMvc.perform(post("/api/propuestas/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBusqueda(
                                jsonBloqueBusqueda("2026-11-01", "2026-11-03", "", 1, 0, 0, 0),
                                jsonBloqueBusqueda("2026-11-03", "2026-11-05", "", 1, 0, 0, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propuestas.length()").value(1))
                .andExpect(jsonPath("$.propuestas[0].bloques.length()").value(2))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].numero").value(101))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].checkIn").value("2026-11-01"))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].checkOut").value("2026-11-03"))
                .andExpect(jsonPath("$.propuestas[0].bloques[0].subtotal").value(3000.0))
                .andExpect(jsonPath("$.propuestas[0].bloques[1].numero").value(101))
                .andExpect(jsonPath("$.propuestas[0].bloques[1].checkIn").value("2026-11-03"))
                .andExpect(jsonPath("$.propuestas[0].bloques[1].checkOut").value("2026-11-05"))
                .andExpect(jsonPath("$.propuestas[0].bloques[1].subtotal").value(3000.0));
    }

    @Test
    void buscarSinBloquesDevuelve400() throws Exception {
        mockMvc.perform(post("/api/propuestas/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBusqueda()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Agregá al menos un bloque de estadía."));
    }

    @Test
    void buscarConCheckOutNoPosteriorDevuelve400() throws Exception {
        mockMvc.perform(post("/api/propuestas/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBusqueda(jsonBloqueBusqueda("2026-11-04", "2026-11-04",
                                "", 1, 0, 0, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El check-out debe ser posterior al check-in."));
    }

    @Test
    void buscarConCantidadesNegativasDevuelve400() throws Exception {
        mockMvc.perform(post("/api/propuestas/buscar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBusqueda(jsonBloqueBusqueda("2026-11-01", "2026-11-04",
                                "", 1, -1, 0, 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Las cantidades no pueden ser negativas."));

        assertTrue(ControladorReservas.getInstance().getReservas().isEmpty());
    }

    // --------------------------------------- persistir

    @Test
    void persistirCreaLaReserva() throws Exception {
        mockMvc.perform(post("/api/propuestas/persistir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPersistir(titular.getId(), jsonBloquePersistir(idHabitacion(103),
                                "2026-11-01", "2026-11-04", 1, 1, 0, "\"Balcon\""))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.titular").value("Agustin Rivero"))
                .andExpect(jsonPath("$.numerosHabitacion[0]").value(103))
                .andExpect(jsonPath("$.checkIn").value("2026-11-01"))
                .andExpect(jsonPath("$.checkOut").value("2026-11-04"))
                .andExpect(jsonPath("$.estado").value("PendienteDePago"))
                .andExpect(jsonPath("$.activa").value(true));

        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numerosHabitacion[0]").value(103));

        // subtotal = noches * tarifa (3 noches * 1800) en la unica propuesta persistida.
        List<Reserva> reservas = ControladorReservas.getInstance().getReservas();
        assertEquals(1, reservas.size());
        Reserva r = reservas.get(0);
        assertEquals(5400.0f, r.getMontoTotal(), 0.001f);
        assertEquals(TipoMoneda.USD, r.getMoneda());
        assertEquals(5400.0f, r.getRHPropuestas().iterator().next().getSubtotal(), 0.001f);
    }

    @Test
    void persistirDosBloquesSumaElMontoTotal() throws Exception {
        mockMvc.perform(post("/api/propuestas/persistir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPersistir(titular.getId(),
                                jsonBloquePersistir(idHabitacion(101), "2026-11-01", "2026-11-04",
                                        1, 0, 0, ""),
                                jsonBloquePersistir(idHabitacion(102), "2026-11-05", "2026-11-07",
                                        1, 0, 0, ""))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numerosHabitacion.length()").value(2))
                .andExpect(jsonPath("$.checkIn").value("2026-11-01"))
                .andExpect(jsonPath("$.checkOut").value("2026-11-07"));

        Reserva r = ControladorReservas.getInstance().getReservas().get(0);
        // 3 noches * 1500 (N° 101) + 2 noches * 2200 (N° 102).
        assertEquals(8900.0f, r.getMontoTotal(), 0.001f);
    }

    @Test
    void persistirTitularInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/propuestas/persistir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPersistir(999999L, jsonBloquePersistir(idHabitacion(103),
                                "2026-11-01", "2026-11-04", 1, 0, 0, "\"Balcon\""))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("titular no existe: 999999"));

        assertTrue(ControladorReservas.getInstance().getReservas().isEmpty());
    }

    @Test
    void persistirSinTitularDevuelve400() throws Exception {
        mockMvc.perform(post("/api/propuestas/persistir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPersistir(null, jsonBloquePersistir(idHabitacion(103),
                                "2026-11-01", "2026-11-04", 1, 0, 0, "\"Balcon\""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("titularId es obligatorio"));

        assertTrue(ControladorReservas.getInstance().getReservas().isEmpty());
    }

    @Test
    void persistirHabitacionInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/propuestas/persistir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPersistir(titular.getId(), jsonBloquePersistir(999999L,
                                "2026-11-01", "2026-11-04", 1, 0, 0, "\"Balcon\""))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("habitacion inexistente: 999999"));

        assertTrue(ControladorReservas.getInstance().getReservas().isEmpty());
    }

    @Test
    void persistirSinBloquesDevuelve400() throws Exception {
        mockMvc.perform(post("/api/propuestas/persistir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPersistir(titular.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Agregá al menos un bloque de estadía."));

        assertTrue(ControladorReservas.getInstance().getReservas().isEmpty());
    }

    @Test
    void persistirConCheckOutNoPosteriorDevuelve400() throws Exception {
        mockMvc.perform(post("/api/propuestas/persistir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPersistir(titular.getId(), jsonBloquePersistir(idHabitacion(103),
                                "2026-11-04", "2026-11-01", 1, 0, 0, "\"Balcon\""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El check-out debe ser posterior al check-in."));

        assertTrue(ControladorReservas.getInstance().getReservas().isEmpty());
    }
}
