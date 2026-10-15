package com.pms.ui;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.dtos.DTRHSolicitada;
import com.pms.entities.RHPropuesta;
import com.pms.handlers.InteraccionUsuario;
import com.pms.handlers.ManejadorBusquedaHuecos;
import com.pms.utils.AtributoHabitacion;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Demostración end-to-end de {@code buscarHuecos} por consola.
 *
 * <p>Si no hay habitaciones cargadas, siembra unas de ejemplo; luego busca una
 * estadía con {@link InteraccionConsola} respondiendo las relajaciones y muestra
 * las alternativas encontradas.
 *
 * <p>Ejecutar: {@code mvn exec:java -Dexec.mainClass=com.pms.ui.DemoBusquedaHuecos}
 * (o correr la clase desde el IDE).
 */
public final class DemoBusquedaHuecos {

    private DemoBusquedaHuecos() {
    }

    public static void main(String[] args) {
        ControladorHabitaciones ch = ControladorHabitaciones.getInstance();
        if (ch.getHabitaciones().isEmpty()) {
            sembrarDemo(ch);
        }

        LocalDate ini = LocalDate.now().plusDays(20);
        LocalDate fin = ini.plusDays(4);
        Set<DTRHSolicitada> solicitudes = new LinkedHashSet<>();
        solicitudes.add(new DTRHSolicitada(ini, fin, Set.of(AtributoHabitacion.VistaAlMar), 1, 1, 0, 0));

        System.out.println("Buscando del " + ini + " al " + fin + " ...");
        InteraccionUsuario consola = new InteraccionConsola();
        Set<Set<RHPropuesta>> alternativas =
                new ManejadorBusquedaHuecos().buscarHuecos(solicitudes, consola);
        imprimir(alternativas);
    }

    private static void sembrarDemo(ControladorHabitaciones ch) {
        ch.altaHabitacion(101, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0, 1500f);
        ch.altaHabitacion(102, Set.of(AtributoHabitacion.VistaAlMar), 1, 1, 0, 0, 2200f);
        ch.altaHabitacion(103, Set.of(AtributoHabitacion.Balcon), 1, 2, 0, 0, 1800f);
        ch.altaHabitacion(201, Set.of(AtributoHabitacion.Balcon, AtributoHabitacion.Jacuzzi), 2, 2, 0, 0, 2600f);
    }

    private static void imprimir(Set<Set<RHPropuesta>> alternativas) {
        if (alternativas.isEmpty()) {
            System.out.println("Sin propuestas.");
            return;
        }
        int i = 1;
        for (Set<RHPropuesta> alternativa : alternativas) {
            System.out.println("Alternativa " + i++ + ":");
            for (RHPropuesta p : alternativa) {
                System.out.printf("  Habitación %s | %s a %s | %dM %dI %dC | %s%n",
                        p.getHabitacion().getNumero(), p.getFechaInicio(), p.getFechaFin(),
                        p.getCamasMatrimoniales(), p.getCamasIndividuales(), p.getCunas(),
                        p.getAtributosHabitacion());
            }
        }
    }
}
