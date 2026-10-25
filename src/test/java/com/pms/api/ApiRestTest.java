package com.pms.api;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.pms.TestUtil;
import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST integration tests for the web PoC. Uses standalone MockMvc (no Spring
 * context, no embedded Tomcat, no SeedDataRunner) so the resource-local JPA
 * stack keeps running against the in-memory {@code pms-test} unit, exactly as
 * the rest of the test suite. Covers login, listing, create and edit, plus the
 * mapped error statuses (400/404/409).
 */
class ApiRestTest {

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
                .standaloneSetup(
                        new AuthController(new AuthService()),
                        new CatalogoController(),
                        new ReservasController())
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

    private static String jsonLogin(String usuario, String password) {
        return "{\"usuario\":\"" + usuario + "\",\"password\":\"" + password + "\"}";
    }

    private static String jsonReserva(Long titularId, long habitacionId, String checkIn, String checkOut,
                                      int matrimoniales, int individuales, int cunas) {
        return "{\"titularId\":" + (titularId == null ? "null" : titularId)
                + ",\"habitacionId\":" + habitacionId
                + ",\"checkIn\":\"" + checkIn + "\""
                + ",\"checkOut\":\"" + checkOut + "\""
                + ",\"camasMatrimoniales\":" + matrimoniales
                + ",\"camasIndividuales\":" + individuales
                + ",\"cunas\":" + cunas + "}";
    }

    // --------------------------------------- login

    @Test
    void loginAdminDevuelveRolAdmin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin("admin", "a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.admin").value(true));
    }

    @Test
    void loginRecepcionistaDevuelveRolRecepcionista() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin("recepcionista", "r")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("RECEPCIONISTA"))
                .andExpect(jsonPath("$.admin").value(false));
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin("admin", "incorrecta")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginConUsuarioInexistenteDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin("nadie", "a")))
                .andExpect(status().isUnauthorized());
    }

    // --------------------------------------- catalogo

    @Test
    void listarHabitacionesDevuelveLasSeisDemo() throws Exception {
        mockMvc.perform(get("/api/habitaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].numero").exists());
    }

    @Test
    void listarTitularesDevuelveElTitularDemo() throws Exception {
        mockMvc.perform(get("/api/titulares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Agustin Rivero"));
    }

    // --------------------------------------- crear

    @Test
    void crearReservaDevuelve201YQuedaEnElListado() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonReserva(titular.getId(), idHabitacion(103),
                                "2026-11-01", "2026-11-04", 1, 1, 0)))
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
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void crearReservaSinTitularDevuelve400() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonReserva(null, idHabitacion(103),
                                "2026-11-01", "2026-11-04", 1, 1, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("titularId es obligatorio"));
    }

    @Test
    void crearReservaSolapadaDevuelve409() throws Exception {
        long h103 = idHabitacion(103);
        String primera = jsonReserva(titular.getId(), h103, "2026-11-01", "2026-11-04", 1, 1, 0);
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(primera))
                .andExpect(status().isCreated());

        // Comparte la noche 2026-11-02 con la reserva activa.
        mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonReserva(titular.getId(), h103, "2026-11-02", "2026-11-03", 1, 0, 0)))
                .andExpect(status().isConflict());
    }

    // --------------------------------------- editar

    @Test
    void editarReservaCambiaFechas() throws Exception {
        long h103 = idHabitacion(103);
        String creada = mockMvc.perform(post("/api/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonReserva(titular.getId(), h103, "2026-11-01", "2026-11-04", 1, 1, 0)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = extraerId(creada);

        mockMvc.perform(put("/api/reservas/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonReserva(titular.getId(), h103, "2026-11-01", "2026-11-06", 2, 0, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.checkIn").value("2026-11-01"))
                .andExpect(jsonPath("$.checkOut").value("2026-11-06"))
                .andExpect(jsonPath("$.numerosHabitacion[0]").value(103));

        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].checkOut").value("2026-11-06"));
    }

    @Test
    void editarReservaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(put("/api/reservas/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonReserva(titular.getId(), idHabitacion(103),
                                "2026-11-01", "2026-11-04", 1, 1, 0)))
                .andExpect(status().isNotFound());
    }

    /** Extrae el id numerico del JSON de una reserva creada (campo "id"). */
    private static long extraerId(String json) {
        int i = json.indexOf("\"id\":");
        if (i < 0) {
            throw new IllegalStateException("sin campo id en: " + json);
        }
        int inicio = i + 5;
        int fin = inicio;
        while (fin < json.length() && Character.isDigit(json.charAt(fin))) {
            fin++;
        }
        return Long.parseLong(json.substring(inicio, fin));
    }

}
