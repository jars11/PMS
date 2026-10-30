package com.pms.controllers;

import com.pms.entities.Pasajero;
import com.pms.entities.TitularReserva;
import com.pms.persistence.RepositorioPersonas;
import com.pms.utils.Fechas;
import com.pms.utils.TipoDocumento;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Passengers and their reservation links. Singleton and stateless, mirroring
 * the C++ ControladorPersonas (the pasajerosPorReserva map is replaced by the
 * JPA many-to-many join table).
 */
public class ControladorPersonas {

    private static final ControladorPersonas INSTANCIA = new ControladorPersonas();

    private final RepositorioPersonas repositorio = new RepositorioPersonas();

    private ControladorPersonas() {
    }

    public static ControladorPersonas getInstance() {
        return INSTANCIA;
    }

    /** Registers a new passenger and returns the managed instance (so callers get the DB id). */
    public Pasajero altaPasajero(List<String> nombres, List<String> apellidos,
                                 String numeroDocumento, TipoDocumento tipoDocumento,
                                 String nacionalidad, LocalDate fechaNacimiento) {
        Pasajero pasajero = new Pasajero(nombres, apellidos, numeroDocumento, tipoDocumento, nacionalidad, fechaNacimiento);
        return repositorio.guardarPasajero(pasajero);
    }

    /** All registered passengers. */
    public List<Pasajero> getPasajeros() {
        return repositorio.findAllPasajeros();
    }

    /**
     * Registers a new reservation holder and returns the managed instance (so
     * the caller gets the DB id). Envuelve el {@code guardarTitular} que ya
     * existia sin uso. Sobrecarga sin documento: la usan los llamadores que
     * todavia no lo piden (Swing, seed y tests previos).
     */
    public TitularReserva altaTitular(List<String> nombres, List<String> apellidos,
                                      String celular, String telefonoFijo, String email) {
        return altaTitular(nombres, apellidos, null, null, celular, telefonoFijo, email);
    }

    /**
     * Registers a new reservation holder with its document (the web form asks
     * for the document first) and returns the managed instance.
     */
    public TitularReserva altaTitular(List<String> nombres, List<String> apellidos,
                                      String numeroDocumento, TipoDocumento tipoDocumento,
                                      String celular, String telefonoFijo, String email) {
        TitularReserva titular = new TitularReserva(nombres, apellidos,
                numeroDocumento, tipoDocumento, celular, telefonoFijo, email);
        return repositorio.guardarTitular(titular);
    }

    /** Titular cuyo documento coincide exactamente, o null si no existe. */
    public TitularReserva buscarTitularPorDocumento(String numeroDocumento) {
        return repositorio.findTitularPorDocumento(numeroDocumento);
    }

    /** All reservation holders (titulares). */
    public List<TitularReserva> getTitulares() {
        return repositorio.findAllTitulares();
    }

    /** Links a passenger to a reservation (C++ vincularPasajeroAReserva). */
    public void vincularPasajeroAReserva(Pasajero pasajero, long reservaId) {
        repositorio.vincularPasajero(reservaId, pasajero);
    }

    /** Passengers linked to one reservation. */
    public Set<Pasajero> pasajerosDeReserva(long reservaId) {
        return new LinkedHashSet<>(repositorio.pasajerosDeReserva(reservaId));
    }

    /** Passengers linked to any reservation of the given titular. */
    public Set<Pasajero> pasajerosDeReservasDeTitular(long titularId) {
        return new LinkedHashSet<>(repositorio.pasajerosDeReservasDeTitular(titularId));
    }

    /** Passengers whose age (full years) at "hoy" is within [minEdad, maxEdad]. */
    public Set<Pasajero> pasajerosPorEdad(int minEdad, int maxEdad, LocalDate hoy) {
        Set<Pasajero> resultado = new LinkedHashSet<>();
        for (Pasajero p : repositorio.findAllPasajeros()) {
            long edad = Fechas.aniosEntre(p.getFechaNacimiento(), hoy);
            if (edad >= minEdad && edad <= maxEdad) {
                resultado.add(p);
            }
        }
        return resultado;
    }
}