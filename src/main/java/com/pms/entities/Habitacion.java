package com.pms.entities;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import com.pms.utils.AtributoHabitacion;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Hotel room. Natural equality is by room number (not by id), matching the C++
 * design where the room number is the stable business key.
 */
@Entity
@Table(name = "habitacion", uniqueConstraints = @UniqueConstraint(columnNames = "numero"))
public class Habitacion {

    /** Default cleaning state for a new room: a freshly registered room is ready. */
    public static final boolean LIMPIA_POR_DEFECTO = true;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer numero;

    private float tarifaPorNoche;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "habitacion_atributos", joinColumns = @JoinColumn(name = "habitacion_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "atributo")
    private Set<AtributoHabitacion> atributos = new TreeSet<>();

    // default 0 para que hbm2ddl update pueda agregar las columnas a una tabla existente
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int cantidadMatrimoniales;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int cantidadIndividuales;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int cantidadCunas;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int colchones;

    // Operational toggle, no structural invariant: dedicated setter, NOT part of
    // validarCampos/actualizar. default true via hbm2ddl update on existing rows.
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean limpia = LIMPIA_POR_DEFECTO;

    protected Habitacion() {
    }

    public Habitacion(int numero, Set<AtributoHabitacion> atributos,
                      int cantidadMatrimoniales, int cantidadIndividuales,
                      int cantidadCunas, int colchones) {
        this.numero = numero;
        this.atributos = new TreeSet<>(atributos != null ? atributos : Set.of());
        this.cantidadMatrimoniales = cantidadMatrimoniales;
        this.cantidadIndividuales = cantidadIndividuales;
        this.cantidadCunas = cantidadCunas;
        this.colchones = colchones;
        this.tarifaPorNoche = 0.0f;
    }

    /**
     * Guarda compartida por los caminos de alta y de actualizacion. Lanza
     * IllegalArgumentException cuando numero &lt;= 0, alguna cantidad de camas
     * es negativa o la tarifa es negativa.
     */
    public static void validarCampos(int numero, int cantidadMatrimoniales,
                                     int cantidadIndividuales, int cantidadCunas,
                                     int colchones, float tarifaPorNoche) {
        if (numero <= 0) {
            throw new IllegalArgumentException("numero invalido: " + numero);
        }
        if (cantidadMatrimoniales < 0) {
            throw new IllegalArgumentException("cantidadMatrimoniales negativa");
        }
        if (cantidadIndividuales < 0) {
            throw new IllegalArgumentException("cantidadIndividuales negativa");
        }
        if (cantidadCunas < 0) {
            throw new IllegalArgumentException("cantidadCunas negativa");
        }
        if (colchones < 0) {
            throw new IllegalArgumentException("colchones negativos");
        }
        if (tarifaPorNoche < 0) {
            throw new IllegalArgumentException("tarifaPorNoche negativa");
        }
    }

    /**
     * Camino de actualizacion controlado: valida los campos nuevos y reemplaza
     * el conjunto completo de atributos. Los getters y capacidad() ya existentes
     * siguen siendo la unica fuente de lectura.
     */
    public void actualizar(int numero, Set<AtributoHabitacion> atributos,
                           int cantidadMatrimoniales, int cantidadIndividuales,
                           int cantidadCunas, int colchones, float tarifaPorNoche) {
        validarCampos(numero, cantidadMatrimoniales, cantidadIndividuales,
                cantidadCunas, colchones, tarifaPorNoche);
        this.numero = numero;
        this.atributos = new TreeSet<>(atributos != null ? atributos : Set.of());
        this.cantidadMatrimoniales = cantidadMatrimoniales;
        this.cantidadIndividuales = cantidadIndividuales;
        this.cantidadCunas = cantidadCunas;
        this.colchones = colchones;
        this.tarifaPorNoche = tarifaPorNoche;
    }

    public Long getId() {
        return id;
    }

    public Integer getNumero() {
        return numero;
    }

    public Set<AtributoHabitacion> getAtributos() {
        return atributos;
    }

    public int getCantidadMatrimoniales() {
        return cantidadMatrimoniales;
    }

    public int getCantidadIndividuales() {
        return cantidadIndividuales;
    }

    public int getCantidadCunas() {
        return cantidadCunas;
    }

    public int getColchones() {
        return colchones;
    }

    /**
     * Capacidad derivada de las camas reales: cada matrimonial cuenta como dos
     * personas y cada individual como una. Cunas y colchones NO suman capacidad;
     * los colchones se cuentan aparte solo si el llamador los habilita.
     */
    public int capacidad() {
        return 2 * cantidadMatrimoniales + cantidadIndividuales;
    }

    public float getTarifaPorNoche() {
        return tarifaPorNoche;
    }

    public void setTarifaPorNoche(float tarifa) {
        this.tarifaPorNoche = tarifa;
    }

    public boolean isLimpia() {
        return limpia;
    }

    public void setLimpia(boolean limpia) {
        this.limpia = limpia;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Habitacion)) {
            return false;
        }
        Habitacion that = (Habitacion) o;
        return Objects.equals(numero, that.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero);
    }

    @Override
    public String toString() {
        return "Habitacion{" + numero + ", " + atributos + ", tarifa=" + tarifaPorNoche + '}';
    }
}