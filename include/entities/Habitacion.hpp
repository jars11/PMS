#ifndef HABITACION_HPP
#define HABITACION_HPP

#include "../utils.hpp"
#include "Pasajero.hpp"
#include <set>

class Habitacion {
    private:
        int numero;
        std::set<AtributoHabitacion> atributos;
        std::set<Pasajero*> ocupantes;

    public:
        // Constructor
        Habitacion(int numero, const std::set<AtributoHabitacion>& atributos);

        // Getters
        int getNumero() const;
        std::set<AtributoHabitacion> getAtributos() const;
        std::set<Pasajero*> getOcupantes() const;

        // Métodos para manejar ocupantes
        void agregarOcupante(Pasajero* pasajero);
        void eliminarOcupante(Pasajero* pasajero);

        // Destructor
        ~Habitacion();
};

#endif