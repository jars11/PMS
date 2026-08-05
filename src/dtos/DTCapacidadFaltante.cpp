#include "../../include/dtos/DTCapacidadFaltante.hpp"

DTCapacidadFaltante::DTCapacidadFaltante(Fecha noche, int capacidad, int cantPasajeros)
    : noche(noche), capacidad(capacidad), cantPasajeros(cantPasajeros) {}

// Getters
Fecha DTCapacidadFaltante::getNoche() const {
    return noche;
}
int DTCapacidadFaltante::getCapacidad() const {
    return capacidad;
}
int DTCapacidadFaltante::getCantPasajeros() const {
    return cantPasajeros;
}

bool DTCapacidadFaltante::operator==(const DTCapacidadFaltante& other) const {
    return noche == other.noche && capacidad == other.capacidad && cantPasajeros == other.cantPasajeros;
}

// Destructor
DTCapacidadFaltante::~DTCapacidadFaltante() {}