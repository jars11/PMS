package com.pms.persistence;

import com.pms.entities.Pasajero;
import com.pms.entities.Persona;
import com.pms.entities.Reserva;
import com.pms.entities.TitularReserva;

import java.util.List;

/**
 * Thin repository over EntityManager for Persona / Pasajero / TitularReserva queries.
 */
public class RepositorioPersonas {

    /** Persists a new passenger. */
    public Pasajero guardarPasajero(Pasajero pasajero) {
        return JpaUtil.enTransaccion(em -> {
            em.persist(pasajero);
            return pasajero;
        });
    }

    /** Persists a new titular. */
    public TitularReserva guardarTitular(TitularReserva titular) {
        return JpaUtil.enTransaccion(em -> {
            em.persist(titular);
            return titular;
        });
    }

    /** All passengers. */
    public List<Pasajero> findAllPasajeros() {
        return JpaUtil.enTransaccion(em -> em.createQuery("select p from Pasajero p", Pasajero.class).getResultList());
    }

    /** All reservation holders (titulares). */
    public List<TitularReserva> findAllTitulares() {
        return JpaUtil.enTransaccion(em ->
                em.createQuery("select t from TitularReserva t", TitularReserva.class).getResultList());
    }

    /** Titular by database id, or null. */
    public TitularReserva findTitularById(long id) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery("select t from TitularReserva t where t.id = :id", TitularReserva.class)
                        .setParameter("id", id)
                        .getResultList().stream().findFirst().orElse(null));
    }

    /** All persons. */
    public List<Persona> findAll() {
        return JpaUtil.enTransaccion(em -> em.createQuery("select p from Persona p", Persona.class).getResultList());
    }

    /** Links a passenger to a reservation through the reserva_pasajero join
     *  table (C++ vincularPasajeroAReserva). */
    public void vincularPasajero(long reservaId, Pasajero pasajero) {
        JpaUtil.enTransaccion(em -> {
            Reserva r = em.createQuery(
                            "select r from Reserva r left join fetch r.pasajeros where r.id = :id",
                            Reserva.class)
                    .setParameter("id", reservaId)
                    .getResultList().stream().findFirst().orElseThrow(
                            () -> new IllegalArgumentException("Reserva " + reservaId + " no existe"));
            Pasajero gestionado = em.merge(pasajero);
            r.getPasajeros().add(gestionado);
            return null;
        });
    }

    /** Passenger by id, or null. */
    public Pasajero findPasajeroById(long id) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery("select p from Pasajero p where p.id = :id", Pasajero.class)
                        .setParameter("id", id)
                        .getResultList().stream().findFirst().orElse(null));
    }

    /** Passengers linked to one reservation. */
    public List<Pasajero> pasajerosDeReserva(long reservaId) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery("select distinct p from Pasajero p join p.reservas r where r.id = :rid", Pasajero.class)
                        .setParameter("rid", reservaId)
                        .getResultList());
    }

    /** Passengers linked to any reservation of one titular. */
    public List<Pasajero> pasajerosDeReservasDeTitular(long titularId) {
        return JpaUtil.enTransaccion(em ->
                em.createQuery("select distinct p from Pasajero p join p.reservas r where r.titular.id = :tid", Pasajero.class)
                        .setParameter("tid", titularId)
                        .getResultList());
    }

    /** Deletes every person. Returns the number of deleted rows. */
    public long eliminarTodas() {
        return JpaUtil.enTransaccion(em -> {
            List<Persona> todas = em.createQuery("select p from Persona p", Persona.class).getResultList();
            long n = 0;
            for (Persona p : todas) {
                em.remove(p);
                n++;
            }
            return n;
        });
    }
}