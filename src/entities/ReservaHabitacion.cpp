#include "../../include/entities/ReservaHabitacion.hpp"

// Constructor
ReservaHabitacion::ReservaHabitacion(const Fecha& fechaInicio, const Fecha& fechaFin, const std::set<AtributoHabitacion>& atributosHabitacion)
    : fechaInicio(fechaInicio), fechaFin(fechaFin), atributosHabitacion(atributosHabitacion) {}

// Getters
Fecha ReservaHabitacion::getFechaInicio() const {
    return fechaInicio;
}

Fecha ReservaHabitacion::getFechaFin() const {
    return fechaFin;
}

std::set<AtributoHabitacion> ReservaHabitacion::getAtributosHabitacion() const {
    return atributosHabitacion;
}

// Destructor
ReservaHabitacion::~ReservaHabitacion() {}