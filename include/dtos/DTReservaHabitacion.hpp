#ifndef DTRESERVAHABITACION_HPP
#define DTRESERVAHABITACION_HPP

#include <set>
#include "../utils.hpp"
#include "Fecha.hpp"

class DTReservaHabitacion {
    private:
        Fecha fechaInicio;
        Fecha fechaFin;
        std::set<AtributoHabitacion> atributosHabitacion;

    public:
        // Constructor
        DTReservaHabitacion(Fecha fechaInicio, Fecha fechaFin, std::set<AtributoHabitacion> atributosHabitacion);

        // Getters
        Fecha getFechaInicio() const;
        Fecha getFechaFin() const;
        std::set<AtributoHabitacion> getAtributosHabitacion() const;

        bool operator==(const DTReservaHabitacion& other) const;

        // Destructor
        ~DTReservaHabitacion();
};

#endif