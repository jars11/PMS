package com.pms.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Persisted requested set (the mirror of a Reserva): the titular plus one or
 * more requested blocks ({@link RHSolicitada}) and the relaxation contracts
 * ({@link ContratoReserva}) under which a reserva satisfied them. Mirrors only
 * the request; money, estado and payments stay on Reserva.
 */
@Entity
@Table(name = "solicitud")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "titular_id")
    private TitularReserva titular;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<RHSolicitada> rhsolicitadas = new LinkedHashSet<>();

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ContratoReserva> contratos = new LinkedHashSet<>();

    protected Solicitud() {
    }

    public Solicitud(TitularReserva titular) {
        this.titular = titular;
    }

    public Long getId() {
        return id;
    }

    public TitularReserva getTitular() {
        return titular;
    }

    public Set<RHSolicitada> getRHSolicitadas() {
        return rhsolicitadas;
    }

    public Set<ContratoReserva> getContratos() {
        return contratos;
    }

    public void addRHSolicitada(RHSolicitada rhsolicitada) {
        rhsolicitada.setSolicitud(this);
        rhsolicitadas.add(rhsolicitada);
    }

    public void addContrato(ContratoReserva contrato) {
        contrato.setSolicitud(this);
        contratos.add(contrato);
    }

    @Override
    public String toString() {
        return "Solicitud{id=" + id + ", bloques=" + rhsolicitadas.size()
                + ", contratos=" + contratos.size() + '}';
    }
}
