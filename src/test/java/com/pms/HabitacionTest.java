package com.pms;

import com.pms.entities.Habitacion;
import com.pms.utils.AtributoHabitacion;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Invariantes de la entidad Habitacion: validacion compartida de campos
 * (create + update) y mutacion controlada con recalculo de capacidad.
 */
class HabitacionTest {

    @Test
    void validarCamposAceptaValoresValidos() {
        Habitacion.validarCampos(101, 1, 2, 0, 0, 1500.0f);
        Habitacion.validarCampos(1, 0, 0, 0, 0, 0.0f);
    }

    @Test
    void validarCamposRechazaNumeroNoPositivo() {
        assertThrows(IllegalArgumentException.class,
                () -> Habitacion.validarCampos(0, 1, 1, 0, 0, 1500.0f));
        assertThrows(IllegalArgumentException.class,
                () -> Habitacion.validarCampos(-5, 1, 1, 0, 0, 1500.0f));
    }

    @Test
    void validarCamposRechazaCamasYtarifaNegativas() {
        assertThrows(IllegalArgumentException.class,
                () -> Habitacion.validarCampos(101, -1, 1, 0, 0, 1500.0f));
        assertThrows(IllegalArgumentException.class,
                () -> Habitacion.validarCampos(101, 1, -1, 0, 0, 1500.0f));
        assertThrows(IllegalArgumentException.class,
                () -> Habitacion.validarCampos(101, 1, 1, -1, 0, 1500.0f));
        assertThrows(IllegalArgumentException.class,
                () -> Habitacion.validarCampos(101, 1, 1, 0, -1, 1500.0f));
        assertThrows(IllegalArgumentException.class,
                () -> Habitacion.validarCampos(101, 1, 1, 0, 0, -0.01f));
    }

    @Test
    void actualizarCambiaCamposYRecalculaCapacidad() {
        Habitacion h = new Habitacion(101, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0);
        h.setTarifaPorNoche(1500.0f);
        assertEquals(3, h.capacidad());

        h.actualizar(102, Set.of(AtributoHabitacion.VistaAlMar, AtributoHabitacion.Jacuzzi),
                2, 1, 2, 3, 1900.0f);

        assertEquals(102, h.getNumero());
        assertEquals(Set.of(AtributoHabitacion.VistaAlMar, AtributoHabitacion.Jacuzzi), h.getAtributos());
        assertEquals(2, h.getCantidadMatrimoniales());
        assertEquals(1, h.getCantidadIndividuales());
        assertEquals(2, h.getCantidadCunas());
        assertEquals(3, h.getColchones());
        assertEquals(1900.0f, h.getTarifaPorNoche(), 0.001f);
        // capacidad = 2*2 + 1 = 5; cunas y colchones NO cuentan.
        assertEquals(5, h.capacidad());
    }

    @Test
    void actualizarRechazaCamasNegativasYNoMuta() {
        Habitacion h = new Habitacion(101, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0);
        h.setTarifaPorNoche(1500.0f);

        assertThrows(IllegalArgumentException.class,
                () -> h.actualizar(101, Set.of(), -1, 1, 0, 0, 1500.0f));

        assertEquals(101, h.getNumero());
        assertEquals(Set.of(AtributoHabitacion.Balcon), h.getAtributos());
        assertEquals(1, h.getCantidadMatrimoniales());
        assertEquals(1500.0f, h.getTarifaPorNoche(), 0.001f);
    }
}
