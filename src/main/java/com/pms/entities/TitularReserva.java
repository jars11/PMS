package com.pms.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.List;

/** Reservation holder (the person who pays and owns the reservation). */
@Entity
@DiscriminatorValue("TITULAR")
public class TitularReserva extends Persona {

    private String celular;
    private String telefonoFijo;
    private String email;

    protected TitularReserva() {
    }

    public TitularReserva(List<String> nombres, List<String> apellidos,
                          String celular, String telefonoFijo, String email) {
        super(nombres, apellidos);
        this.celular = celular;
        this.telefonoFijo = telefonoFijo;
        this.email = email;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getTelefonoFijo() {
        return telefonoFijo;
    }

    public void setTelefonoFijo(String telefonoFijo) {
        this.telefonoFijo = telefonoFijo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}