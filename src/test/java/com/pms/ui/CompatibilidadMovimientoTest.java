package com.pms.ui;

import com.pms.utils.AtributoHabitacion;
import com.pms.utils.MotivoRechazo;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The single pure predicate that drives both row-greying (rule 4) and drop
 * validation (rule 3). Precedence is the MotivoRechazo enum order.
 */
class CompatibilidadMovimientoTest {

    private static final LocalDate D1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate D4 = LocalDate.of(2026, 11, 4);

    private static CompatibilidadMovimiento.BloqueMovido movido(
            Set<AtributoHabitacion> atributos, int m, int i, int cunas, boolean late) {
        return new CompatibilidadMovimiento.BloqueMovido(atributos, m, i, cunas, D1, D4, late);
    }

    private static CompatibilidadMovimiento.HabitacionDestino destino(
            Set<AtributoHabitacion> atributos, int m, int i, int cunas, int capacidad, boolean limpia,
            boolean libre, boolean checkInEnFin, boolean lateTerminaEnIni) {
        return new CompatibilidadMovimiento.HabitacionDestino(
                1L, atributos, m, i, cunas, capacidad, limpia, libre, checkInEnFin, lateTerminaEnIni);
    }

    @Test
    void compatibleDevuelveNull() {
        CompatibilidadMovimiento.BloqueMovido mov =
                movido(Set.of(AtributoHabitacion.Balcon), 1, 1, 0, false);
        CompatibilidadMovimiento.HabitacionDestino dst =
                destino(Set.of(AtributoHabitacion.Balcon, AtributoHabitacion.Jacuzzi), 2, 1, 0, 5, true, true, false, false);
        assertNull(CompatibilidadMovimiento.evaluar(mov, dst));
    }

    @Test
    void atributosFaltantes() {
        CompatibilidadMovimiento.BloqueMovido mov = movido(Set.of(AtributoHabitacion.Jacuzzi), 1, 1, 0, false);
        CompatibilidadMovimiento.HabitacionDestino dst = destino(Set.of(AtributoHabitacion.Balcon), 2, 1, 0, 5, true, true, false, false);
        assertEquals(MotivoRechazo.ATRIBUTOS_FALTANTES, CompatibilidadMovimiento.evaluar(mov, dst));
    }

    @Test
    void distribucionCamas() {
        CompatibilidadMovimiento.BloqueMovido mov = movido(Set.of(), 2, 0, 0, false);
        CompatibilidadMovimiento.HabitacionDestino dst = destino(Set.of(), 1, 0, 0, 2, true, true, false, false);
        assertEquals(MotivoRechazo.DISTRIBUCION_CAMAS, CompatibilidadMovimiento.evaluar(mov, dst));
    }

    @Test
    void distribucionCamasPorCunas() {
        CompatibilidadMovimiento.BloqueMovido mov = movido(Set.of(), 1, 0, 2, false);
        CompatibilidadMovimiento.HabitacionDestino dst = destino(Set.of(), 1, 0, 1, 2, true, true, false, false);
        assertEquals(MotivoRechazo.DISTRIBUCION_CAMAS, CompatibilidadMovimiento.evaluar(mov, dst));
    }

    @Test
    void capacidadInsuficiente() {
        // Camas alcanzan por conteo pero la capacidad total (2*m+i) no.
        CompatibilidadMovimiento.BloqueMovido mov = movido(Set.of(), 2, 0, 0, false); // necesita 4
        CompatibilidadMovimiento.HabitacionDestino dst = destino(Set.of(), 2, 0, 0, 3, true, true, false, false);
        assertEquals(MotivoRechazo.CAPACIDAD_INSUFICIENTE, CompatibilidadMovimiento.evaluar(mov, dst));
    }

    @Test
    void noDisponible() {
        CompatibilidadMovimiento.BloqueMovido mov = movido(Set.of(), 1, 1, 0, false);
        CompatibilidadMovimiento.HabitacionDestino dst = destino(Set.of(), 1, 1, 0, 3, true, false, false, false);
        assertEquals(MotivoRechazo.NO_DISPONIBLE, CompatibilidadMovimiento.evaluar(mov, dst));
    }

    @Test
    void solapeLateCheckout() {
        CompatibilidadMovimiento.BloqueMovido mov = movido(Set.of(), 1, 1, 0, true);
        CompatibilidadMovimiento.HabitacionDestino dst = destino(Set.of(), 1, 1, 0, 3, true, true, true, false);
        assertEquals(MotivoRechazo.SOLAPE_LATE_CHECKOUT, CompatibilidadMovimiento.evaluar(mov, dst));
    }

    @Test
    void solapeLateCheckoutNoAplicaSinLateEnElMovido() {
        CompatibilidadMovimiento.BloqueMovido mov = movido(Set.of(), 1, 1, 0, false);
        CompatibilidadMovimiento.HabitacionDestino dst = destino(Set.of(), 1, 1, 0, 3, true, true, true, false);
        assertNull(CompatibilidadMovimiento.evaluar(mov, dst));
    }

    @Test
    void limpiezaLateCheckout() {
        CompatibilidadMovimiento.BloqueMovido mov = movido(Set.of(), 1, 1, 0, false);
        CompatibilidadMovimiento.HabitacionDestino dst = destino(Set.of(), 1, 1, 0, 3, false, true, false, true);
        assertEquals(MotivoRechazo.LIMPIEZA_LATE_CHECKOUT, CompatibilidadMovimiento.evaluar(mov, dst));
    }

    @Test
    void limpiezaNoAplicaSiLaHabitacionEstaLimpia() {
        CompatibilidadMovimiento.BloqueMovido mov = movido(Set.of(), 1, 1, 0, false);
        CompatibilidadMovimiento.HabitacionDestino dst = destino(Set.of(), 1, 1, 0, 3, true, true, false, true);
        assertNull(CompatibilidadMovimiento.evaluar(mov, dst));
    }

    @Test
    void precedenciaDevuelveElPrimerMotivoAplicable() {
        CompatibilidadMovimiento.BloqueMovido mov =
                movido(Set.of(AtributoHabitacion.Jacuzzi), 3, 0, 0, false);
        CompatibilidadMovimiento.HabitacionDestino dst =
                destino(Set.of(), 1, 0, 0, 2, true, true, true, true);
        assertEquals(MotivoRechazo.ATRIBUTOS_FALTANTES, CompatibilidadMovimiento.evaluar(mov, dst));

        CompatibilidadMovimiento.BloqueMovido mov2 = movido(Set.of(), 2, 0, 0, false);
        CompatibilidadMovimiento.HabitacionDestino dst2 = destino(Set.of(), 1, 0, 0, 2, true, false, true, true);
        assertEquals(MotivoRechazo.DISTRIBUCION_CAMAS, CompatibilidadMovimiento.evaluar(mov2, dst2));

        CompatibilidadMovimiento.BloqueMovido mov3 = movido(Set.of(), 2, 0, 0, true);
        CompatibilidadMovimiento.HabitacionDestino dst3 = destino(Set.of(), 2, 0, 0, 3, false, false, true, true);
        assertEquals(MotivoRechazo.CAPACIDAD_INSUFICIENTE, CompatibilidadMovimiento.evaluar(mov3, dst3));

        CompatibilidadMovimiento.BloqueMovido mov4 = movido(Set.of(), 1, 1, 0, true);
        CompatibilidadMovimiento.HabitacionDestino dst4 = destino(Set.of(), 1, 1, 0, 3, false, false, true, true);
        assertEquals(MotivoRechazo.NO_DISPONIBLE, CompatibilidadMovimiento.evaluar(mov4, dst4));

        CompatibilidadMovimiento.BloqueMovido mov5 = movido(Set.of(), 1, 1, 0, true);
        CompatibilidadMovimiento.HabitacionDestino dst5 = destino(Set.of(), 1, 1, 0, 3, false, true, true, true);
        assertEquals(MotivoRechazo.SOLAPE_LATE_CHECKOUT, CompatibilidadMovimiento.evaluar(mov5, dst5));
    }
}
