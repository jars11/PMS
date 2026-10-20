package com.pms;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;
import com.pms.controllers.ControladorReservas;
import com.pms.dtos.DTRHPropuesta;
import com.pms.dtos.DTRHSolicitada;
import com.pms.dtos.DTReserva;
import com.pms.entities.Pasajero;
import com.pms.entities.Reserva;
import com.pms.entities.TitularReserva;
import com.pms.persistence.JpaUtil;
import com.pms.persistence.RepositorioHabitaciones;
import com.pms.persistence.RepositorioPersonas;
import com.pms.persistence.RepositorioReservas;
import com.pms.persistence.RepositorioSolicitudes;
import com.pms.utils.AtributoHabitacion;
import com.pms.utils.Fechas;
import com.pms.utils.TipoDocumento;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Interactive-free demo mirroring the C++ main.cpp scenario: rooms, titular,
 * passengers, availability of two blocks, creation of the reservation,
 * age query and the January 2027 proposals.
 *
 * <p>Re-runs are idempotent: the databases are cleared at startup, so every
 * execution reproduces the same output.
 */
public final class Main {

    private Main() {
    }

    private static void imprimirFecha(LocalDate f) {
        System.out.print(f.getDayOfMonth() + "/" + f.getMonthValue() + "/" + f.getYear());
    }

    private static void imprimirPropuesta(DTRHPropuesta p) {
        System.out.print("  - Habitacion propuesta: ");
        imprimirFecha(p.getFechaInicio());
        System.out.print(" -> ");
        imprimirFecha(p.getFechaFin());
        System.out.print(" | Camas: " + p.getCamasMatrimoniales() + " matrimoniales, "
                + p.getCamasIndividuales() + " individuales, "
                + p.getCunas() + " cunas"
                + " | Subtotal: USD " + String.format(Locale.US, "%.2f", p.getSubtotal()));

        Set<AtributoHabitacion> attrs = p.getAtributosHabitacion();
        if (attrs.isEmpty()) {
            System.out.print(" | Atributos: (ninguno)");
        } else {
            System.out.print(" | Atributos: ");
            boolean primero = true;
            for (AtributoHabitacion a : attrs) {
                if (!primero) {
                    System.out.print(", ");
                }
                primero = false;
                System.out.print(a);
            }
        }
        System.out.println();
    }

    private static void imprimirReserva(DTReserva r) {
        System.out.println("  - Reserva: activa=" + (r.isActiva() ? "si" : "no")
                + " | total=" + String.format(Locale.US, "%.2f", r.getMontoTotal())
                + " " + r.getMoneda()
                + " | estado=" + r.getEstado());
        for (DTRHPropuesta p : r.getDTRHPropuestas()) {
            imprimirPropuesta(p);
        }
    }

    private static void limpiarDatos() {
        // Orden por FKs: contratos -> solicitudes -> reservas -> personas -> habitaciones.
        new RepositorioSolicitudes().eliminarContratos();
        new RepositorioSolicitudes().eliminarTodas();
        new RepositorioReservas().eliminarTodas();
        new RepositorioPersonas().eliminarTodas();
        new RepositorioHabitaciones().eliminarTodas();
    }

    public static void main(String[] args) {
        limpiarDatos();

        // 1) Hotel rooms.
        ControladorHabitaciones ch = ControladorHabitaciones.getInstance();
        ch.altaHabitacion(101, Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0, 1500.0f);
        ch.altaHabitacion(102, Set.of(AtributoHabitacion.VistaAlMar, AtributoHabitacion.Jacuzzi), 1, 1, 0, 0, 2200.0f);
        ch.altaHabitacion(103, Set.of(AtributoHabitacion.Balcon), 1, 2, 0, 0, 1800.0f);
        ch.altaHabitacion(104, Set.of(AtributoHabitacion.VistaAlMar), 1, 2, 0, 0, 2100.0f);
        ch.altaHabitacion(201, Set.of(AtributoHabitacion.Balcon), 2, 2, 0, 0, 2600.0f);
        ch.altaHabitacion(202, Set.of(AtributoHabitacion.DosAmbientes), 2, 2, 0, 0, 2400.0f);

        // 2) Titular.
        TitularReserva titular = new TitularReserva(List.of("Agustin"), List.of("Rivero"),
                "091000000", "22000000", "agustin@example.com");

        // 3) Passengers.
        ControladorPersonas cp = ControladorPersonas.getInstance();
        cp.altaPasajero(List.of("Juan"), List.of("Perez"), "5.123.456-7",
                TipoDocumento.CEDULA, "Uruguaya", LocalDate.of(1995, 3, 10));
        cp.altaPasajero(List.of("Ana"), List.of("Lopez"), "4.987.654-3",
                TipoDocumento.DNI, "Uruguaya", LocalDate.of(2010, 7, 5));

        ControladorReservas cr = ControladorReservas.getInstance();

        // ================= Escenario A: disponibilidad inicial =================
        System.out.println("=== Disponibilidad ===");

        // Bloque 1: pareja + 1 individual, con balcon (10/1 al 15/1/2027).
        cr.agregarBloqueReserva(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15),
                1, 1, 0, 0, Set.of(AtributoHabitacion.Balcon));
        // Bloque 2: pareja + bebe con cuna, sin atributos (15/1 al 20/1/2027).
        cr.agregarBloqueReserva(LocalDate.of(2027, 1, 15), LocalDate.of(2027, 1, 20),
                1, 0, 1, 1, Set.of());

        Set<DTRHSolicitada> bloques = new TreeSet<>();
        bloques.add(new DTRHSolicitada(LocalDate.of(2027, 1, 10), LocalDate.of(2027, 1, 15),
                Set.of(AtributoHabitacion.Balcon), 1, 1, 0, 0));
        bloques.add(new DTRHSolicitada(LocalDate.of(2027, 1, 15), LocalDate.of(2027, 1, 20),
                Set.of(), 1, 0, 1, 1));

        DTReserva disponibilidad = cr.obtenerDisponibilidad(bloques);
        imprimirReserva(disponibilidad);

        // ================= Escenario B: crear y listar reservas =================
        System.out.println();
        System.out.println("=== Reserva creada ===");
        titular = cr.crearReserva(titular);

        // Vincular pasajeros a la reserva recien creada del titular.
        long reservaId = 0;
        for (Reserva r : cr.getReservas()) {
            if (r.getTitular().getId().equals(titular.getId()) && r.isActiva()) {
                reservaId = r.getId();
                break;
            }
        }
        for (Pasajero p : cp.getPasajeros()) {
            cp.vincularPasajeroAReserva(p, reservaId);
        }

        System.out.println("Reservas del sistema:");
        for (DTReserva r : cr.mostrarReservas()) {
            imprimirReserva(r);
        }

        System.out.println();
        System.out.println("=== Pasajeros por edad (0-17 anios, 1/1/2027) ===");
        Set<Pasajero> menores = cp.pasajerosPorEdad(0, 17, LocalDate.of(2027, 1, 1));
        if (menores.isEmpty()) {
            System.out.println("  (ninguno)");
        }
        for (Pasajero p : menores) {
            System.out.println("  - " + p.getNombres().get(0) + " " + p.getApellidos().get(0)
                    + " (" + Fechas.aniosEntre(p.getFechaNacimiento(), LocalDate.of(2027, 1, 1)) + " anios)");
        }

        System.out.println();
        System.out.println("=== RHPropuestas en enero 2027 ===");
        Set<DTRHPropuesta> enEnero = cr.mostrarRHPropuestasPorRangoFechasEnEstadia(
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 31));
        System.out.println("  Cantidad: " + enEnero.size());
        for (DTRHPropuesta p : enEnero) {
            imprimirPropuesta(p);
        }

        System.out.println();
        System.out.println("=== Reservas del titular (id " + titular.getId() + ") ===");
        Set<DTReserva> delTitular = cr.mostrarReservasPorTitular(titular.getId());
        System.out.println("  Cantidad: " + delTitular.size());
        for (DTReserva r : delTitular) {
            imprimirReserva(r);
        }

        JpaUtil.cerrarTodas();
    }
}