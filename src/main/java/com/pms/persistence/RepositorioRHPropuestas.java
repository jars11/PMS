package com.pms.persistence;

import com.pms.entities.RHPropuesta;

import java.time.LocalDate;
import java.util.List;

/**
 * Thin repository over EntityManager for RHPropuesta queries.
 */
public class RepositorioRHPropuestas {

    /** Proposals whose stay overlaps [desde, hasta) (inclusive from, exclusive to). */
    public List<RHPropuesta> findEnRango(LocalDate desde, LocalDate hasta) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery(
                                "select p from RHPropuesta p " +
                                        "where p.fechaInicio < :hasta and :desde < p.fechaFin", RHPropuesta.class)
                        .setParameter("desde", desde)
                        .setParameter("hasta", hasta)
                        .getResultList());
    }

    /**
     * Propuestas de reservas ACTIVAS cuyo stay solapa [desde, hasta) (desde
     * inclusive, hasta exclusivo). Se traen la habitacion y la reserva
     * (join fetch) para que la matriz de ocupacion trabaje sin lazy access y
     * pueda distinguir reservas activas de inactivas. NO reemplaza a
     * {@link #findEnRango(LocalDate, LocalDate)} (sin filtro, se mantiene igual).
     *
     * <p>El borde izquierdo es INCLUSIVO sobre {@code fechaFin}: una estadia que
     * termina exactamente en {@code desde} no ocupa esa noche, pero su cola de
     * check-out si debe pintarse en la primera columna visible del calendario.
     */
    public List<RHPropuesta> findActivasEnRango(LocalDate desde, LocalDate hasta) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery(
                                "select distinct p from RHPropuesta p " +
                                        "join fetch p.habitacion join fetch p.reserva r " +
                                        "where r.activa = true and p.fechaInicio < :hasta and :desde <= p.fechaFin",
                                RHPropuesta.class)
                        .setParameter("desde", desde)
                        .setParameter("hasta", hasta)
                        .getResultList());
    }

    /** Proposals whose stay starts exactly on the given date. */
    public List<RHPropuesta> findByFechaInicio(LocalDate checkin) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery("select p from RHPropuesta p where p.fechaInicio = :checkin", RHPropuesta.class)
                        .setParameter("checkin", checkin)
                        .getResultList());
    }

    /** Proposals whose stay ends exactly on the given date. */
    public List<RHPropuesta> findByFechaFin(LocalDate checkout) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery("select p from RHPropuesta p where p.fechaFin = :checkout", RHPropuesta.class)
                        .setParameter("checkout", checkout)
                        .getResultList());
    }
}