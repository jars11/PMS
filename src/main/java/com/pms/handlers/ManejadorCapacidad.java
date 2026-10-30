package com.pms.handlers;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.dtos.DTCapacidadFaltante;
import com.pms.dtos.DTRHSolicitada;
import com.pms.entities.Habitacion;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

/**
 * Per-night capacity check against the rooms free that night. Stateless,
 * same algorithm as the C++ ManejadorCapacidad.
 */
public class ManejadorCapacidad {

    /**
     * Empty set when every night of the block fits in some free room;
     * otherwise one DTCapacidadFaltante per night without capacity.
     */
    public Set<DTCapacidadFaltante> hayCapacidad(DTRHSolicitada dtRHS) {
        Set<DTCapacidadFaltante> resultado = new TreeSet<>();

        // Los bebes no consumen camas: las cunas son un recurso aparte y pueden
        // ser menos que los bebes.
        int passengers = 2 * dtRHS.getCantMatrimonios() + dtRHS.getCantIndividuales();

        ControladorHabitaciones ch = ControladorHabitaciones.getInstance();

        LocalDate noche = dtRHS.getFechaInicio();
        while (noche.isBefore(dtRHS.getFechaFin())) {
            Set<Habitacion> libres = ch.habitacionesLibresEnNoche(noche);
            int maxCapacidad = 0;
            for (Habitacion h : libres) {
                maxCapacidad = Math.max(maxCapacidad, ManejadorAtributosHabs.capacidadDe(h));
            }
            if (passengers > maxCapacidad) {
                resultado.add(new DTCapacidadFaltante(noche, maxCapacidad, passengers));
            }
            noche = noche.plusDays(1);
        }

        return resultado;
    }
}