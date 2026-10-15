package com.pms.ui;

import com.pms.utils.MotivoRechazo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The user-facing copy of the move rejection: one exact headline plus a concrete
 * detail line per possible reason.
 */
class MensajesMovimientoTest {

    @Test
    void mensajeDeRechazoEsExacto() {
        assertEquals("No es posible mover a la habitación seleccionada",
                MensajesMovimiento.RECHAZO_MOVIMIENTO);
    }

    @Test
    void cadaMotivoTieneUnDetalleConcreto() {
        for (MotivoRechazo motivo : MotivoRechazo.values()) {
            String detalle = MensajesMovimiento.detalle(motivo);
            assertNotNull(detalle, "detalle nulo para " + motivo);
            assertFalse(detalle.isBlank(), "detalle vacio para " + motivo);
        }
    }

    @Test
    void motivosDistintosTienenDetallesDistintos() {
        assertNotEquals(MensajesMovimiento.detalle(MotivoRechazo.SOLAPE_LATE_CHECKOUT),
                MensajesMovimiento.detalle(MotivoRechazo.LIMPIEZA_LATE_CHECKOUT));
        assertNotEquals(MensajesMovimiento.detalle(MotivoRechazo.ATRIBUTOS_FALTANTES),
                MensajesMovimiento.detalle(MotivoRechazo.NO_DISPONIBLE));
    }
}
