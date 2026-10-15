package com.pms.utils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Date helpers replacing the custom C++ Fecha class.
 *
 * <p>Semantics mirror the fixed C++ {@code Fecha::dias} / {@code Fecha::anios}
 * (serial-day difference, inclusive "from" date and exclusive "to" date;
 * full completed years for age):
 * <ul>
 *   <li>{@code diasEntre(10/1, 15/1)} = 5 nights.</li>
 *   <li>{@code aniosEntre(2010-07-05, 2027-01-01)} = 16 (birthday not yet reached in 2027).</li>
 * </ul>
 *
 * <p>An inverted range (from after to) returns 0 instead of a negative value or an
 * exception (documented simplification).
 */
public final class Fechas {

    private Fechas() {
    }

    /** Days between two dates (from inclusive, to exclusive). Returns 0 for inverted ranges. */
    public static long diasEntre(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(desde, hasta);
    }

    /** Full years between two dates. Returns 0 for inverted ranges. */
    public static long aniosEntre(LocalDate nacimiento, LocalDate hoy) {
        if (hoy.isBefore(nacimiento)) {
            return 0;
        }
        return ChronoUnit.YEARS.between(nacimiento, hoy);
    }
}