#include "../../include/dtos/DTReservaHabitacion.hpp"

// Constructor
DTReservaHabitacion::DTReservaHabitacion(Fecha fechaInicio, Fecha fechaFin, std::set<AtributoHabitacion> atributosHabitacion)
    : fechaInicio(fechaInicio), fechaFin(fechaFin), atributosHabitacion(atributosHabitacion) {}

// Getters
Fecha DTReservaHabitacion::getFechaInicio() const {
    return fechaInicio;
}

Fecha DTReservaHabitacion::getFechaFin() const {
    return fechaFin;
}

std::set<AtributoHabitacion> DTReservaHabitacion::getAtributosHabitacion() const {
    return atributosHabitacion;
}

bool DTReservaHabitacion::operator==(const DTReservaHabitacion& other) const {
    return fechaInicio == other.fechaInicio &&
           fechaFin == other.fechaFin &&
           atributosHabitacion == other.atributosHabitacion;
}

// Destructor
DTReservaHabitacion::~DTReservaHabitacion() {}