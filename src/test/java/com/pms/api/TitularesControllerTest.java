package com.pms.api;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.pms.TestUtil;
import com.pms.persistence.JpaUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST de titulares (T12d): alta con documento y busqueda por documento, que es
 * el flujo con el que el formulario web autocompleta al titular. MockMvc
 * standalone sobre la unidad en memoria {@code pms-test}, igual que
 * {@link ApiRestTest}.
 */
class TitularesControllerTest {

    private MockMvc mockMvc;

    @BeforeAll
    static void antesDeTodo() {
        JpaUtil.setUnidadPorDefecto("pms-test");
    }

    @BeforeEach
    void preparar() {
        TestUtil.limpiarBase();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TitularesController(), new CatalogoController())
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(
                        Jackson2ObjectMapperBuilder.json()
                                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                .build()))
                .build();
    }

    private static String jsonTitular(String numeroDocumento, String tipoDocumento,
                                      String nombres, String apellidos) {
        return "{\"nombres\":[" + nombres + "]"
                + ",\"apellidos\":[" + apellidos + "]"
                + ",\"numeroDocumento\":\"" + numeroDocumento + "\""
                + ",\"tipoDocumento\":\"" + tipoDocumento + "\""
                + ",\"celular\":\"091000000\""
                + ",\"telefonoFijo\":\"22000000\""
                + ",\"email\":\"maria@example.com\"}";
    }

    @Test
    void crearTitularDevuelve201ConDocumentoYQuedaEnElCatalogo() throws Exception {
        mockMvc.perform(post("/api/titulares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTitular("41234567", "CEDULA", "\"Maria\"", "\"Lopez\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Maria Lopez"))
                .andExpect(jsonPath("$.numeroDocumento").value("41234567"))
                .andExpect(jsonPath("$.tipoDocumento").value("CEDULA"))
                .andExpect(jsonPath("$.celular").value("091000000"))
                .andExpect(jsonPath("$.email").value("maria@example.com"));

        mockMvc.perform(get("/api/titulares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Maria Lopez"))
                .andExpect(jsonPath("$[0].numeroDocumento").value("41234567"))
                .andExpect(jsonPath("$[0].tipoDocumento").value("CEDULA"));
    }

    @Test
    void buscarTitularExistenteDevuelveSusDatos() throws Exception {
        mockMvc.perform(post("/api/titulares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTitular("41234567", "DNI", "\"Maria\"", "\"Lopez\"")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/titulares/buscar").param("documento", "41234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Maria Lopez"))
                .andExpect(jsonPath("$.numeroDocumento").value("41234567"))
                .andExpect(jsonPath("$.tipoDocumento").value("DNI"))
                .andExpect(jsonPath("$.celular").value("091000000"))
                .andExpect(jsonPath("$.email").value("maria@example.com"));
    }

    @Test
    void buscarTitularInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/titulares/buscar").param("documento", "99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.mensaje").value("titular no existe para documento: 99999999"));
    }

    @Test
    void buscarSinDocumentoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/titulares/buscar").param("documento", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("documento es obligatorio"));
    }

    @Test
    void crearTitularSinDocumentoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/titulares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTitular("", "CEDULA", "\"Maria\"", "\"Lopez\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("numeroDocumento es obligatorio"));

        mockMvc.perform(get("/api/titulares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void crearTitularSinNombresDevuelve400() throws Exception {
        mockMvc.perform(post("/api/titulares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTitular("41234567", "CEDULA", "", "\"Lopez\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("nombres y apellidos son obligatorios"));

        mockMvc.perform(get("/api/titulares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
