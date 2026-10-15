package com.pms.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;

import com.pms.utils.TipoDocumento;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract person. Single-table inheritance with Pasajero and TitularReserva
 * subclasses (natural keys: names, document, nationality).
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_persona", discriminatorType = DiscriminatorType.STRING)
public abstract class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "persona_nombres", joinColumns = @JoinColumn(name = "persona_id"))
    @Column(name = "nombre")
    private List<String> nombres = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "persona_apellidos", joinColumns = @JoinColumn(name = "persona_id"))
    @Column(name = "apellido")
    private List<String> apellidos = new ArrayList<>();

    private String numeroDocumento;

    @Enumerated(EnumType.STRING)
    private TipoDocumento tipoDocumento;

    private String nacionalidad;

    protected Persona() {
    }

    protected Persona(List<String> nombres, List<String> apellidos) {
        this.nombres = new ArrayList<>(nombres != null ? nombres : List.of());
        this.apellidos = new ArrayList<>(apellidos != null ? apellidos : List.of());
    }

    public Long getId() {
        return id;
    }

    public List<String> getNombres() {
        return nombres;
    }

    public List<String> getApellidos() {
        return apellidos;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public String getTipoDocumentoString() {
        return tipoDocumento != null ? tipoDocumento.name() : "DESCONOCIDO";
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNacionalidad() {
        return nacionalidad;
    }

    public void setNacionalidad(String nacionalidad) {
        this.nacionalidad = nacionalidad;
    }
}