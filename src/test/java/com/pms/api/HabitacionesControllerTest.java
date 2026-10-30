package com.pms.api;

import com.pms.TestUtil;
import com.pms.controllers.ControladorHabitaciones;
import com.pms.persistence.JpaUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST de habitaciones (S1/S4): alta con prioridad al final de la lista y
 * reordenamiento por lista de ids. Standalone MockMvc, igual que el resto de
 * los tests REST, contra la unidad en memoria {@code pms-test}.
 */
class HabitacionesControllerTest {

    private MockMvc mockMvc;

    @BeforeAll
    static void antesDeTodo() {
        JpaUtil.setUnidadPorDefecto("pms-test");
    }

    @BeforeEach
    void preparar() {
        TestUtil.limpiarBase();
        TestUtil.sembrarHabitaciones();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HabitacionesController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private static long idDe(int numero) {
        return ControladorHabitaciones.getInstance().getHabitaciones().stream()
                .filter(h -> h.getNumero() == numero)
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private static String jsonAlta(int numero) {
        return "{\"numero\":" + numero
                + ",\"atributos\":[\"Balcon\"]"
                + ",\"camasMatrimoniales\":1"
                + ",\"camasIndividuales\":1"
                + ",\"cunas\":0"
                + ",\"colchones\":0"
                + ",\"tarifaPorNoche\":1500.0}";
    }

    private static String jsonOrden(List<Long> ids) {
        StringBuilder sb = new StringBuilder("{\"ids\":[");
        for (int i = 0; i < ids.size(); i++) {
            sb.append(i == 0 ? "" : ",").append(ids.get(i));
        }
        return sb.append("]}").toString();
    }

    @Test
    void elAltaDevuelveLaHabitacionCreadaConSuPrioridad() throws Exception {
        mockMvc.perform(post("/api/habitaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonAlta(301)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(301))
                .andExpect(jsonPath("$.prioridad").value(7))
                .andExpect(jsonPath("$.tarifaPorNoche").value(1500.0))
                .andExpect(jsonPath("$.capacidad").value(3));

        assertEquals(7, ControladorHabitaciones.getInstance().getHabitaciones().size());
    }

    @Test
    void elAltaConNumeroDuplicadoDevuelve409YNoCreaNada() throws Exception {
        mockMvc.perform(post("/api/habitaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonAlta(101)))
                .andExpect(status().isConflict());

        assertEquals(6, ControladorHabitaciones.getInstance().getHabitaciones().size());
    }

    @Test
    void reordenarDevuelveLasHabitacionesEnOrdenDePrioridad() throws Exception {
        mockMvc.perform(put("/api/habitaciones/orden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonOrden(List.of(idDe(202), idDe(101)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].numero").value(202))
                .andExpect(jsonPath("$[0].prioridad").value(1))
                .andExpect(jsonPath("$[1].numero").value(101))
                .andExpect(jsonPath("$[1].prioridad").value(2));

        assertEquals(List.of(202, 101, 102, 103, 104, 201),
                ControladorHabitaciones.getInstance().getHabitacionesPorPrioridad().stream()
                        .map(h -> h.getNumero()).toList());
    }

    @Test
    void reordenarConIdInexistenteDevuelve404() throws Exception {
        // getHabitacion(...) lanza IllegalArgumentException("habitacion inexistente")
        // y ApiExceptionHandler traduce ese mensaje a 404.
        mockMvc.perform(put("/api/habitaciones/orden")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonOrden(List.of(999999L))))
                .andExpect(status().isNotFound());
    }
}
