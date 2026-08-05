#ifndef DTCAPACIDADFALTANTE_HPP
#define DTCAPACIDADFALTANTE_HPP

#include "Fecha.hpp"

class DTCapacidadFaltante {
    private:
        Fecha noche;
        int capacidad;
        int cantPasajeros;

    public:
        DTCapacidadFaltante(Fecha noche, int capacidad, int cantPasajeros);

        // Getters
        Fecha getNoche() const;
        int getCapacidad() const;
        int getCantPasajeros() const;

        bool operator==(const DTCapacidadFaltante& other) const;

        // Destructor
        ~DTCapacidadFaltante();
};

#endif