package com.pms.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import com.pms.utils.TipoDocumento;

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
        this(nombres, apellidos, null, null, celular, telefonoFijo, email);
    }

    /** Alta con documento (el formulario web pide primero el documento del titular). */
    public TitularReserva(List<String> nombres, List<String> apellidos,
                          String numeroDocumento, TipoDocumento tipoDocumento,
                          String celular, String telefonoFijo, String email) {
        super(nombres, apellidos);
        setNumeroDocumento(numeroDocumento);
        setTipoDocumento(tipoDocumento);
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