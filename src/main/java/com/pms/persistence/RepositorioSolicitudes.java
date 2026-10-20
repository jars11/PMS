package com.pms.persistence;

import com.pms.entities.ContratoReserva;
import com.pms.entities.Reserva;
import com.pms.entities.Solicitud;

import java.util.List;

/**
 * Thin repository over EntityManager for Solicitud / ContratoReserva queries.
 * Queries that return solicitudes fetch their blocks and contracts so detached
 * results are safe to read.
 */
public class RepositorioSolicitudes {

    /** Persists a new solicitud (or merges an existing one). */
    public Solicitud guardar(Solicitud solicitud) {
        return JpaUtil.enTransaccion(em -> {
            if (solicitud.getId() == null) {
                em.persist(solicitud);
                return solicitud;
            }
            return em.merge(solicitud);
        });
    }

    /** Solicitud by id (with its blocks and contracts), or null. */
    public Solicitud findById(long id) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery(
                                "select distinct s from Solicitud s " +
                                        "left join fetch s.rhsolicitadas left join fetch s.contratos " +
                                        "where s.id = :id", Solicitud.class)
                        .setParameter("id", id)
                        .getResultList().stream().findFirst().orElse(null));
    }

    /** All solicitudes with their blocks and contracts. */
    public List<Solicitud> findAll() {
        return JpaUtil.enTransaccion(em ->
                em.createQuery(
                                "select distinct s from Solicitud s " +
                                        "left join fetch s.rhsolicitadas left join fetch s.contratos",
                                Solicitud.class)
                        .getResultList());
    }

    /**
     * Contract linking one solicitud to its reserva, or null when the
     * solicitud has no contract. Single join fetch so the detached result
     * carries its reserva; at most one row by the per-side UNIQUE.
     */
    public ContratoReserva findContratoDeSolicitud(long solicitudId) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery(
                                "select c from ContratoReserva c join fetch c.reserva " +
                                        "where c.solicitud.id = :sid", ContratoReserva.class)
                        .setParameter("sid", solicitudId)
                        .getResultList().stream().findFirst().orElse(null));
    }

    /**
     * Reserva linked by contract to one solicitud, or null when the solicitud
     * has no contract. This is the ONLY solicitud -> reserva resolution; there
     * is no effectiveness flag anywhere.
     */
    public Reserva findReservaDeSolicitud(long solicitudId) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery(
                                "select c from ContratoReserva c join fetch c.reserva " +
                                        "where c.solicitud.id = :sid", ContratoReserva.class)
                        .setParameter("sid", solicitudId)
                        .getResultList().stream()
                        .map(ContratoReserva::getReserva)
                        .findFirst().orElse(null));
    }

    /** Deletes every ContratoReserva row (cleanup before reservas/solicitudes). */
    public long eliminarContratos() {
        return JpaUtil.enTransaccion(em ->
                (long) em.createQuery("delete from ContratoReserva c").executeUpdate());
    }

    /** Deletes every solicitud (cascades blocks and contracts). */
    public long eliminarTodas() {
        return JpaUtil.enTransaccion(em -> {
            List<Solicitud> todas = em.createQuery("select s from Solicitud s", Solicitud.class).getResultList();
            long n = 0;
            for (Solicitud s : todas) {
                em.remove(s);
                n++;
            }
            return n;
        });
    }
}
