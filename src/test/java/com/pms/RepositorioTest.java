package com.pms;

import com.pms.entities.Habitacion;
import com.pms.utils.AtributoHabitacion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * JPA round-trip against a real H2 FILE database in a temp directory:
 * persist with one EntityManagerFactory, close it, reopen a second factory
 * on the same file and read the entity back. Proves the mappings survive
 * process-equivalent shutdown/restart (what the pms file unit uses).
 */
class RepositorioTest {

    private static Map<String, Object> propiedades(Path dir) {
        Map<String, Object> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.url", "jdbc:h2:file:" + dir.resolve("pms").toAbsolutePath());
        props.put("hibernate.hbm2ddl.auto", "update");
        return props;
    }

    @Test
    void roundTripHabitacionEnBaseDeArchivoTemporal(@TempDir Path dir) {
        EntityManagerFactory f = Persistence.createEntityManagerFactory("pms-test", propiedades(dir));

        Habitacion habitacion = new Habitacion(101, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0);
        habitacion.setTarifaPorNoche(1500.0f);

        EntityManager em = f.createEntityManager();
        em.getTransaction().begin();
        em.persist(habitacion);
        em.getTransaction().commit();
        em.close();
        f.close();

        // Reopen the same file: the data must survive the factory close.
        f = Persistence.createEntityManagerFactory("pms-test", propiedades(dir));
        em = f.createEntityManager();
        Habitacion encontrada = em.find(Habitacion.class, habitacion.getId());
        em.close();
        f.close();

        assertNotNull(encontrada);
        assertEquals(101, encontrada.getNumero());
        assertEquals(Set.of(AtributoHabitacion.Balcon), encontrada.getAtributos());
        assertEquals(1, encontrada.getCantidadMatrimoniales());
        assertEquals(1, encontrada.getCantidadIndividuales());
        assertEquals(3, encontrada.capacidad());
        assertEquals(1500.0f, encontrada.getTarifaPorNoche(), 0.001f);
    }
}