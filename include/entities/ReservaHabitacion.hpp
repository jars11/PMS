#ifndef RESERVAHABITACION_HPP
#define RESERVAHABITACION_HPP

#include <set>
#include "../utils.hpp"
#include "../dtos/Fecha.hpp"

class ReservaHabitacion {
    private:
        Fecha fechaInicio;
        Fecha fechaFin;
        std::set<AtributoHabitacion> atributosHabitacion;

    public:
        // Constructor
        ReservaHabitacion(const Fecha& fechaInicio, const Fecha& fechaFin, const std::set<AtributoHabitacion>& atributosHabitacion);

        // Getters
        Fecha getFechaInicio() const;
        Fecha getFechaFin() const;
        std::set<AtributoHabitacion> getAtributosHabitacion() const;

        // Destructor
        ~ReservaHabitacion();
};

#endif