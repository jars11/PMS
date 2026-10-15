package com.pms.utils;

/**
 * Reasons a reservation move can be rejected. DECLARATION ORDER IS THE
 * PRECEDENCE CONTRACT: {@code CompatibilidadMovimiento.evaluar} returns the
 * first applicable value walking the enum in ordinal order, so reordering these
 * constants silently changes which reason wins. Tests freeze the order.
 */
public enum MotivoRechazo {
    ATRIBUTOS_FALTANTES,
    DISTRIBUCION_CAMAS,
    CAPACIDAD_INSUFICIENTE,
    NO_DISPONIBLE,
    SOLAPE_LATE_CHECKOUT,
    LIMPIEZA_LATE_CHECKOUT
}
