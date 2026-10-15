package com.pms.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Bootstrap and lifecycle helper for JPA.
 *
 * <p>Two persistence units are available (see META-INF/persistence.xml):
 * {@code pms} (H2 file database, used by the Main demo) and {@code pms-test}
 * (in-memory H2, used by the JUnit tests). The default unit can be switched
 * with {@link #setUnidadPorDefecto(String)} so the singletons keep working
 * against the test database without any state of their own.
 */
public final class JpaUtil {

    private static final ConcurrentHashMap<String, EntityManagerFactory> FACTORIAS = new ConcurrentHashMap<>();

    private static volatile String unidadPorDefecto = "pms";

    private JpaUtil() {
    }

    /** Current default persistence unit name. */
    public static String getUnidadPorDefecto() {
        return unidadPorDefecto;
    }

    /** Switches the default persistence unit (tests point it at "pms-test"). */
    public static void setUnidadPorDefecto(String unidad) {
        unidadPorDefecto = unidad;
    }

    /** EntityManagerFactory of the default persistence unit. */
    public static EntityManagerFactory getEntityManagerFactory() {
        return getEntityManagerFactory(unidadPorDefecto);
    }

    /** EntityManagerFactory of the named persistence unit (cached). */
    public static EntityManagerFactory getEntityManagerFactory(String unidad) {
        return FACTORIAS.computeIfAbsent(unidad, Persistence::createEntityManagerFactory);
    }

    /** Closes one cached factory. */
    public static void cerrar(String unidad) {
        EntityManagerFactory f = FACTORIAS.remove(unidad);
        if (f != null && f.isOpen()) {
            f.close();
        }
    }

    /** Closes every cached factory (flushes the file DB on demo exit). */
    public static void cerrarTodas() {
        Set<String> unidades = FACTORIAS.keySet();
        for (String u : unidades) {
            cerrar(u);
        }
    }

    /**
     * Runs the given work inside one resource-local transaction and returns
     * its result. Rolls back on any RuntimeException.
     */
    public static <T> T enTransaccion(Function<EntityManager, T> trabajo) {
        EntityManager em = getEntityManagerFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            T resultado = trabajo.apply(em);
            em.getTransaction().commit();
            return resultado;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}