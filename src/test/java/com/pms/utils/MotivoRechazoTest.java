package com.pms.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * MotivoRechazo enum: its declaration order IS the precedence contract used by
 * CompatibilidadMovimiento. Asserting the values and their ordinals freezes that
 * order so a future reorder cannot silently change rejection precedence.
 */
class MotivoRechazoTest {

    @Test
    void valoresYOrdenDePrecedencia() {
        assertEquals(List.of(
                        MotivoRechazo.ATRIBUTOS_FALTANTES,
                        MotivoRechazo.DISTRIBUCION_CAMAS,
                        MotivoRechazo.CAPACIDAD_INSUFICIENTE,
                        MotivoRechazo.NO_DISPONIBLE,
                        MotivoRechazo.SOLAPE_LATE_CHECKOUT,
                        MotivoRechazo.LIMPIEZA_LATE_CHECKOUT),
                List.of(MotivoRechazo.values()));
    }

    @Test
    void ordinalesCodificanLaPrecedencia() {
        assertEquals(6, MotivoRechazo.values().length);
        assertEquals(0, MotivoRechazo.ATRIBUTOS_FALTANTES.ordinal());
        assertEquals(1, MotivoRechazo.DISTRIBUCION_CAMAS.ordinal());
        assertEquals(2, MotivoRechazo.CAPACIDAD_INSUFICIENTE.ordinal());
        assertEquals(3, MotivoRechazo.NO_DISPONIBLE.ordinal());
        assertEquals(4, MotivoRechazo.SOLAPE_LATE_CHECKOUT.ordinal());
        assertEquals(5, MotivoRechazo.LIMPIEZA_LATE_CHECKOUT.ordinal());
    }
}
