package com.pms;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.entities.Pasajero;
import com.pms.entities.TitularReserva;
import com.pms.persistence.RepositorioHabitaciones;
import com.pms.persistence.RepositorioPersonas;
import com.pms.persistence.RepositorioReservas;
import com.pms.persistence.RepositorioSolicitudes;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.TipoDocumento;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Shared seeding/cleanup for tests that exercise the singletons against the
 * in-memory pms-test persistence unit.
 */
public final class TestUtil {

    private TestUtil() {
    }

    /**
     * Resets every table. Order matters because of FKs: contratos first (their
     * solicitud_id/reserva_id point at both sides), then solicitudes, reservas,
     * personas and habitaciones.
     */
    public static void limpiarBase() {
        new RepositorioSolicitudes().eliminarContratos();
        new RepositorioSolicitudes().eliminarTodas();
        new RepositorioReservas().eliminarTodas();
        new RepositorioPersonas().eliminarTodas();
        new RepositorioHabitaciones().eliminarTodas();
        ControladorReservas.getInstance().limpiarBloquesPendientes();
    }

    /** The same six rooms as the Main demo (room number as business key). */
    public static void sembrarHabitaciones() {
        ControladorHabitaciones ch = ControladorHabitaciones.getInstance();
        ch.altaHabitacion(101, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0, 1500.0f);
        ch.altaHabitacion(102, Set.of(AtributoHabitacion.VistaAlMar, AtributoHabitacion.Jacuzzi), 1, 1, 0, 0, 2200.0f);
        ch.altaHabitacion(103, Set.of(AtributoHabitacion.Balcon), 1, 2, 0, 0, 1800.0f);
        ch.altaHabitacion(104, Set.of(AtributoHabitacion.VistaAlMar), 1, 2, 0, 0, 2100.0f);
        ch.altaHabitacion(201, Set.of(AtributoHabitacion.Balcon), 2, 2, 0, 0, 2600.0f);
        ch.altaHabitacion(202, Set.of(AtributoHabitacion.DosAmbientes), 2, 2, 0, 0, 2400.0f);
    }

    /** Registers the demo titular (celular, telefonoFijo, email). */
    public static TitularReserva demoTitular() {
        return new TitularReserva(List.of("Agustin"), List.of("Rivero"),
                "091000000", "22000000", "agustin@example.com");
    }

    /** Registers a passenger and returns the managed instance. */
    public static Pasajero altaPasajero(String nombre, String apellido,
                                        String numeroDocumento, TipoDocumento tipoDocumento,
                                        LocalDate fechaNacimiento) {
        return ControladorPersonas.getInstance().altaPasajero(
                List.of(nombre), List.of(apellido), numeroDocumento, tipoDocumento, "Uruguaya", fechaNacimiento);
    }
}