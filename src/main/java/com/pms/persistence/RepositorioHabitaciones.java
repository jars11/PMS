package com.pms.persistence;

import com.pms.entities.Habitacion;
import com.pms.utils.AtributoHabitacion;

import java.util.List;
import java.util.Set;

/**
 * Thin repository over EntityManager for Habitacion queries.
 */
public class RepositorioHabitaciones {

    /** Persists a new room. */
    public Habitacion guardar(Habitacion habitacion) {
        return JpaUtil.enTransaccion(em -> {
            em.persist(habitacion);
            return habitacion;
        });
    }

    /** All rooms with their attributes. */
    public List<Habitacion> findAll() {
        return JpaUtil.enTransaccion(em ->
                em.createQuery("select distinct h from Habitacion h left join fetch h.atributos", Habitacion.class)
                        .getResultList());
    }

    /** Room by database id, or null. */
    public Habitacion findById(long id) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery("select h from Habitacion h left join fetch h.atributos where h.id = :id", Habitacion.class)
                        .setParameter("id", id)
                        .getResultList().stream().findFirst().orElse(null));
    }

    /** Updates an existing room (merge). */
    public Habitacion actualizar(Habitacion habitacion) {
        return JpaUtil.enTransaccion(em -> em.merge(habitacion));
    }

    /** Deletes a room by id. Returns false when the id does not exist. */
    public boolean eliminar(long id) {
        return JpaUtil.enTransaccion(em -> {
            Habitacion h = em.find(Habitacion.class, id);
            if (h == null) {
                return false;
            }
            em.remove(h);
            return true;
        });
    }

    /** Room by business key (numero), or null. */
    public Habitacion findByNumero(int numero) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery("select h from Habitacion h left join fetch h.atributos where h.numero = :numero", Habitacion.class)
                        .setParameter("numero", numero)
                        .getResultList().stream().findFirst().orElse(null));
    }

    /** True when a room with the given numero already exists. */
    public boolean existeNumero(int numero) {
        return JpaUtil.enTransaccion(em -> {
            Long n = em.createQuery("select count(h) from Habitacion h where h.numero = :numero", Long.class)
                    .setParameter("numero", numero)
                    .getSingleResult();
            return n != null && n > 0;
        });
    }

    /** True when some RHPropuesta references the room (delete guard). */
    public boolean estaEnUso(long id) {
        return JpaUtil.enTransaccion(em -> {
            Long n = em.createQuery("select count(p) from RHPropuesta p where p.habitacion.id = :hid", Long.class)
                    .setParameter("hid", id)
                    .getSingleResult();
            return n != null && n > 0;
        });
    }

    /** Rooms that contain every requested attribute. */
    public List<Habitacion> buscarPorAtributos(Set<AtributoHabitacion> atributos) {
        return JpaUtil.enTransaccion(em -> {
            List<Habitacion> todas = em.createQuery(
                            "select distinct h from Habitacion h left join fetch h.atributos", Habitacion.class)
                    .getResultList();
            return todas.stream()
                    .filter(h -> h.getAtributos().containsAll(atributos))
                    .toList();
        });
    }

    /** Deletes every room. Returns the number of deleted rows. */
    public long eliminarTodas() {
        return JpaUtil.enTransaccion(em -> {
            List<Habitacion> todas = em.createQuery("select h from Habitacion h", Habitacion.class).getResultList();
            long n = 0;
            for (Habitacion h : todas) {
                em.remove(h);
                n++;
            }
            return n;
        });
    }
}