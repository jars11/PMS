package com.pms.handlers;

import com.pms.dtos.DTDistribucionHabs;
import com.pms.dtos.DTRHSolicitada;
import com.pms.entities.Habitacion;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Nightly passenger distribution over rooms (single-room-per-block
 * simplification, as the C++). Stateless, same algorithm as the C++
 * ManejadorDistribucionHabs.
 */
public class ManejadorDistribucionHabs {

    /** Empty set when no room fits the whole block. */
    public Set<DTDistribucionHabs> hayDistribucionHabs(DTRHSolicitada dtrhs) {
        Set<DTDistribucionHabs> resultado = new TreeSet<>();

        Habitacion room = new ManejadorAtributosHabs().brindarHabitacionesDisponibles(dtrhs);
        if (room == null) {
            return resultado;
        }

        int passengers = 2 * dtrhs.getCantMatrimonios() + dtrhs.getCantIndividuales() + dtrhs.getCantBebes();

        LocalDate noche = dtrhs.getFechaInicio();
        while (noche.isBefore(dtrhs.getFechaFin())) {
            resultado.add(new DTDistribucionHabs(noche, Collections.singletonList(passengers)));
            noche = noche.plusDays(1);
        }

        return resultado;
    }
}