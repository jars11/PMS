package com.pms.handlers;

import com.pms.dtos.DTDistribucionCamas;
import com.pms.dtos.DTDistribucionHabs;
import com.pms.dtos.DTRHSolicitada;

import java.util.Set;
import java.util.TreeSet;

/**
 * Nightly bed distribution: matrimoniales = passengers/2, individuales =
 * passengers%2, cunas from the block request. Stateless, same algorithm as
 * the C++ ManejadorDistribucionCamas.
 */
public class ManejadorDistribucionCamas {

    public Set<DTDistribucionCamas> distribucionCamas(DTRHSolicitada dtrhs, Set<DTDistribucionHabs> distHabs) {
        Set<DTDistribucionCamas> resultado = new TreeSet<>();

        for (DTDistribucionHabs dh : distHabs) {
            int totalMatrimoniales = 0;
            int totalIndividuales = 0;
            for (int c : dh.getPasajerosPorHabitacion()) {
                totalMatrimoniales += c / 2;
                totalIndividuales += c % 2;
            }
            int cunas = dtrhs.getCantCunas();
            resultado.add(new DTDistribucionCamas(dh.getNoche(), totalMatrimoniales, totalIndividuales, cunas));
        }

        return resultado;
    }
}