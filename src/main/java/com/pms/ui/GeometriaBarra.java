package com.pms.ui;

import java.time.LocalDate;

/**
 * Pure geometry of the occupancy bar. The bar spans the check-in day through the
 * check-out day INCLUSIVE; every covered day exposes the fraction {@code [x0,x1]}
 * of its own cell that the bar paints. Rendering is independent from
 * availability, which stays half-open {@code [fechaInicio, fechaFin)} — the
 * check-out day's tail is a rendering-only fact.
 *
 * <p>The check-in branch is evaluated first: for a one-night stay the check-in
 * day is {@code [0.25,1]} and the check-out tail lives on the NEXT day.
 */
public final class GeometriaBarra {

    /** Left inset of the bar inside the check-in day cell. */
    public static final double INSET_CHECKIN = 0.25;
    /** Right bound of the departing tail on the check-out day (normal). */
    public static final double INSET_CHECKOUT_NORMAL = 0.25;
    /** Right bound of the departing tail on the check-out day (late check-out). */
    public static final double INSET_CHECKOUT_LATE = 0.75;

    private GeometriaBarra() {
    }

    /** A stay block as the calendar sees it: inclusive start and end days. */
    public record Bloque(LocalDate fechaInicio, LocalDate fechaFin, boolean lateCheckout) {
    }

    /** Horizontal segment {@code [x0,x1]} within a single day cell, in [0,1]. */
    public record Segmento(double x0, double x1) {
    }

    /**
     * Segment of the bar inside {@code dia}'s cell, or {@code null} when the day
     * is not covered by {@code [fechaInicio, fechaFin]} (both inclusive).
     */
    public static Segmento segmento(Bloque bloque, LocalDate dia) {
        if (bloque == null || dia == null || bloque.fechaInicio() == null || bloque.fechaFin() == null) {
            return null;
        }
        if (dia.equals(bloque.fechaInicio())) {
            return new Segmento(INSET_CHECKIN, 1.0);
        }
        if (dia.equals(bloque.fechaFin())) {
            return new Segmento(0.0, bloque.lateCheckout() ? INSET_CHECKOUT_LATE : INSET_CHECKOUT_NORMAL);
        }
        if (dia.isAfter(bloque.fechaInicio()) && dia.isBefore(bloque.fechaFin())) {
            return new Segmento(0.0, 1.0);
        }
        return null;
    }
}
