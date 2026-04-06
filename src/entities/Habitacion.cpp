#include "../../include/entities/Habitacion.hpp"

// Constructor
Habitacion::Habitacion(int numero, const std::set<AtributoHabitacion>& atributos)
    : numero(numero), atributos(atributos) {}

// Getters
int Habitacion::getNumero() const {
    return numero;
}
std::set<AtributoHabitacion> Habitacion::getAtributos() const {
    return atributos;
}
std::set<Pasajero*> Habitacion::getOcupantes() const {
    return ocupantes;
}

// Métodos para manejar ocupantes
void Habitacion::agregarOcupante(Pasajero* pasajero) {
    ocupantes.insert(pasajero);
}
void Habitacion::eliminarOcupante(Pasajero* pasajero) {
    ocupantes.erase(pasajero);
}

// Destructor
Habitacion::~Habitacion() {}

