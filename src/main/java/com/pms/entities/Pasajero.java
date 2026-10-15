package com.pms.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;

import com.pms.utils.TipoDocumento;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Passenger of a reservation. Linked to reservations through the
 * {@code reserva_pasajero} join table (the natural JPA replacement for the
 * C++ {@code pasajerosPorReserva} map in ControladorPersonas).
 */
@Entity
@DiscriminatorValue("PASAJERO")
public class Pasajero extends Persona {

    private LocalDate fechaNacimiento;

    @ManyToMany(mappedBy = "pasajeros")
    private Set<Reserva> reservas = new HashSet<>();

    protected Pasajero() {
    }

    public Pasajero(List<String> nombres, List<String> apellidos,
                    String numeroDocumento, TipoDocumento tipoDocumento,
                    String nacionalidad, LocalDate fechaNacimiento) {
        super(nombres, apellidos);
        setNumeroDocumento(numeroDocumento);
        setTipoDocumento(tipoDocumento);
        setNacionalidad(nacionalidad);
        this.fechaNacimiento = fechaNacimiento;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public Set<Reserva> getReservas() {
        return reservas;
    }
}