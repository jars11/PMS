package com.pms.entities;

import com.pms.dtos.DTRHSolicitada;
import com.pms.utils.AtributoHabitacion;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

/**
 * Persisted requested block: dates, wanted attributes and passenger counts.
 * No association to {@link RHPropuesta} (the accepted match is recorded
 * separately on {@link ContratoReserva}).
 */
@Entity
@Table(name = "rhsolicitada")
public class RHSolicitada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "solicitud_id")
    private Solicitud solicitud;

    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rhsolicitada_atributos", joinColumns = @JoinColumn(name = "rhsolicitada_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "atributo")
    private Set<AtributoHabitacion> atributosHabitacion = new TreeSet<>();

    private int cantMatrimonios;

    private int cantIndividuales;

    private int cantBebes;

    private int cantCunas;

    protected RHSolicitada() {
    }

    public RHSolicitada(LocalDate fechaInicio, LocalDate fechaFin,
                        Set<AtributoHabitacion> atributosHabitacion,
                        int cantMatrimonios, int cantIndividuales, int cantBebes, int cantCunas) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.atributosHabitacion = new TreeSet<>(atributosHabitacion != null ? atributosHabitacion : Set.of());
        this.cantMatrimonios = cantMatrimonios;
        this.cantIndividuales = cantIndividuales;
        this.cantBebes = cantBebes;
        this.cantCunas = cantCunas;
    }

    /** Construye un bloque persistido a partir del DTO transitorio de busqueda. */
    public static RHSolicitada de(DTRHSolicitada dtrhs) {
        return new RHSolicitada(dtrhs.getFechaInicio(), dtrhs.getFechaFin(),
                dtrhs.getAtributosHabitacion(), dtrhs.getCantMatrimonios(),
                dtrhs.getCantIndividuales(), dtrhs.getCantBebes(), dtrhs.getCantCunas());
    }

    public Long getId() {
        return id;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(Solicitud solicitud) {
        this.solicitud = solicitud;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public Set<AtributoHabitacion> getAtributosHabitacion() {
        return atributosHabitacion;
    }

    public int getCantMatrimonios() {
        return cantMatrimonios;
    }

    public int getCantIndividuales() {
        return cantIndividuales;
    }

    public int getCantBebes() {
        return cantBebes;
    }

    public int getCantCunas() {
        return cantCunas;
    }
}
