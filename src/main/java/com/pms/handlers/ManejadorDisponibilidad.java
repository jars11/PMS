package com.pms.handlers;

import com.pms.dtos.DTDistribucionCamas;
import com.pms.dtos.DTDistribucionHabs;
import com.pms.dtos.DTRHPropuesta;
import com.pms.dtos.DTRHSolicitada;
import com.pms.entities.Habitacion;
import com.pms.utils.Fechas;
import com.pms.utils.TipoMoneda;

import java.util.Set;
import java.util.TreeSet;

/**
 * Builds the final DTRHPropuesta for a block: all nights stay in ONE room,
 * subtotal = nights x nightly rate. Stateless, same algorithm as the C++
 * ManejadorDisponibilidad.
 */
public class ManejadorDisponibilidad {

    public Set<DTRHPropuesta> dtrhp(DTRHSolicitada dtrhs,
                                    Set<DTDistribucionHabs> distHabs,
                                    Set<DTDistribucionCamas> distCamas) {
        Set<DTRHPropuesta> resultado = new TreeSet<>();

        // distHabs is only used to drive the already-computed bed distribution.
        Habitacion room = new ManejadorAtributosHabs().brindarHabitacionesDisponibles(dtrhs);
        if (room == null) {
            return resultado;
        }

        long nights = Fechas.diasEntre(dtrhs.getFechaInicio(), dtrhs.getFechaFin());
        float subtotal = nights * room.getTarifaPorNoche();

        int camasMatrimoniales = 0;
        int camasIndividuales = 0;
        int cunas = 0;
        if (!distCamas.isEmpty()) {
            DTDistribucionCamas dc = distCamas.iterator().next();
            camasMatrimoniales = dc.getCamasMatrimoniales();
            camasIndividuales = dc.getCamasIndividuales();
            // Las cunas son un recurso y no pueden superar a los bebes.
            cunas = Math.min(dc.getCunas(), dtrhs.getCantBebes());
        }

        resultado.add(new DTRHPropuesta(dtrhs.getFechaInicio(), dtrhs.getFechaFin(),
                room.getAtributos(), subtotal, TipoMoneda.USD,
                camasMatrimoniales, camasIndividuales, cunas));

        return resultado;
    }
}