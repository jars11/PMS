package com.pms;

import com.pms.entities.TitularReserva;
import com.pms.persistence.JpaUtil;
import com.pms.persistence.RepositorioPersonas;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Repositorio de personas contra la unidad en memoria pms-test: listado y
 * busqueda de titulares (los caminos que usa la pantalla de recepcion).
 */
class RepositorioPersonasTest {

    private final RepositorioPersonas repo = new RepositorioPersonas();

    @BeforeAll
    static void antesDeTodo() {
        JpaUtil.setUnidadPorDefecto("pms-test");
    }

    @BeforeEach
    void limpiar() {
        TestUtil.limpiarBase();
    }

    private TitularReserva sembrar(String nombre) {
        return repo.guardarTitular(new TitularReserva(
                List.of(nombre), List.of("Rivero"), "091000000", "22000000", "titular@example.com"));
    }

    @Test
    void findAllTitularesDevuelveTodosLosCargados() {
        sembrar("Agustin");
        sembrar("Maria");

        List<TitularReserva> titulares = repo.findAllTitulares();

        assertEquals(2, titulares.size());
    }

    @Test
    void findTitularByIdDevuelveElTitularONull() {
        TitularReserva guardado = sembrar("Agustin");

        TitularReserva encontrado = repo.findTitularById(guardado.getId());

        assertNotNull(encontrado);
        assertEquals(List.of("Agustin"), encontrado.getNombres());
        assertEquals(List.of("Rivero"), encontrado.getApellidos());
        assertEquals("091000000", encontrado.getCelular());

        assertNull(repo.findTitularById(9999L));
    }
}
