package com.pms.persistence;

import com.pms.entities.Pago;
import com.pms.entities.Reserva;
import com.pms.utils.TipoMoneda;

import java.time.LocalDate;
import java.util.List;

/**
 * Thin repository over EntityManager for Reserva / Pago queries.
 * Queries that return reservations always fetch rhPropuestas so DTO mapping
 * works on detached entities.
 */
public class RepositorioReservas {

    /**
     * Public escape hatch: persists a new reservation (or merges an existing
     * one) without creating its contract. The one-contract invariant is
     * guaranteed by the controller creation path
     * ({@code ControladorReservas.persistirReservaYSolicitud}); callers that
     * bypass it own the cardinality themselves. Kept for direct fixtures and
     * legacy callers.
     */
    public Reserva guardar(Reserva reserva) {
        return JpaUtil.enTransaccion(em -> {
            if (reserva.getId() == null) {
                em.persist(reserva);
                return reserva;
            }
            return em.merge(reserva);
        });
    }

    /** Reservation by id (with its proposals), or null. */
    public Reserva findById(long id) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery("select r from Reserva r left join fetch r.rhPropuestas where r.id = :id", Reserva.class)
                        .setParameter("id", id)
                        .getResultList().stream().findFirst().orElse(null));
    }

    /** All reservations with their proposals. */
    public List<Reserva> findAll() {
        return JpaUtil.enTransaccion(em ->
                em.createQuery("select distinct r from Reserva r left join fetch r.rhPropuestas", Reserva.class)
                        .getResultList());
    }

    /**
     * All reservations fully fetched (titular + proposals + rooms) so the read
     * view ({@code ReservaVista}) can be built on detached entities without any
     * lazy initialization.
     */
    public List<Reserva> findAllConDetalle() {
        return JpaUtil.enTransaccion(em ->
                em.createQuery(
                                "select distinct r from Reserva r left join fetch r.rhPropuestas p " +
                                        "left join fetch p.habitacion left join fetch r.titular", Reserva.class)
                        .getResultList());
    }

    /** Reservations of one titular with their proposals. */
    public List<Reserva> findByTitularId(long titularId) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery(
                                "select distinct r from Reserva r left join fetch r.rhPropuestas " +
                                        "where r.titular.id = :tid", Reserva.class)
                        .setParameter("tid", titularId)
                        .getResultList());
    }

    /**
     * True when an ACTIVE reservation occupies the room on any night of
     * [desde, hasta) (inclusive from, exclusive to). Mirrors the C++
     * habitacionLibreEnRango rule that only active reservations block.
     */
    public boolean habitacionOcupada(long habitacionId, LocalDate desde, LocalDate hasta) {
        Long n = JpaUtil.enTransaccion(em ->
                em.createQuery(
                                "select count(r) from Reserva r join r.rhPropuestas p " +
                                        "where r.activa = true and p.habitacion.id = :hid " +
                                        "and p.fechaInicio < :hasta and :desde < p.fechaFin", Long.class)
                        .setParameter("hid", habitacionId)
                        .setParameter("desde", desde)
                        .setParameter("hasta", hasta)
                        .getSingleResult());
        return n != null && n > 0;
    }

    /** Payments of one reservation. */
    public List<Pago> findPagosByReservaId(long reservaId) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery("select p from Pago p where p.reserva.id = :rid", Pago.class)
                        .setParameter("rid", reservaId)
                        .getResultList());
    }

    /** Number of payments of one reservation. */
    public long contarPagos(long reservaId) {
        Long n = JpaUtil.enTransaccion(em ->
                em.createQuery("select count(p) from Pago p where p.reserva.id = :rid", Long.class)
                        .setParameter("rid", reservaId)
                        .getSingleResult());
        return n != null ? n : 0L;
    }

    /** True when the reservation exists for that titular (C++ perteneceReservaATitular). */
    public boolean perteneceReservaATitular(long reservaId, long titularId) {
        Long n = JpaUtil.enTransaccion(em ->
                em.createQuery("select count(r) from Reserva r where r.id = :rid and r.titular.id = :tid", Long.class)
                        .setParameter("rid", reservaId)
                        .setParameter("tid", titularId)
                        .getSingleResult());
        return n != null && n > 0;
    }

    /** Adds a payment to an existing reservation (business helper over Reserva.addPago). */
    public void agregarPago(long reservaId, float monto, TipoMoneda moneda, LocalDate fecha) {
        JpaUtil.enTransaccion(em -> {
            Reserva r = em.find(Reserva.class, reservaId);
            if (r == null) {
                throw new IllegalArgumentException("Reserva " + reservaId + " no existe");
            }
            r.addPago(new Pago(monto, moneda, fecha, r));
            return null;
        });
    }

    /** Deletes every reservation (cascades proposals, payments and join rows). */
    public long eliminarTodas() {
        return JpaUtil.enTransaccion(em -> {
            List<Reserva> todas = em.createQuery("select r from Reserva r", Reserva.class).getResultList();
            long n = 0;
            for (Reserva r : todas) {
                em.remove(r);
                n++;
            }
            return n;
        });
    }
}