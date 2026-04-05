#include "../../include/entities/Pasajero.hpp"

// Constructor
Pasajero::Pasajero(const std::vector<std::string>& nombres, const std::vector<std::string>& apellidos,
                   const std::string& numeroDocumento, const TipoDocumento tipoDocumento, const std::string& nacionalidad, const Fecha& fechaNacimiento)
    : Persona(nombres, apellidos), numeroDocumento(numeroDocumento), tipoDocumento(tipoDocumento), nacionalidad(nacionalidad), fechaNacimiento(fechaNacimiento) {}

// Getters
std::string Pasajero::getNumeroDocumento() const {
    return numeroDocumento;
}
TipoDocumento Pasajero::getTipoDocumento() const {
    return tipoDocumento;
}
std::string Pasajero::getTipoDocumentoString() const {
    switch (tipoDocumento) {
        case TipoDocumento::DNI:
            return "DNI";
        case TipoDocumento::PASAPORTE:
            return "PASAPORTE";
        case TipoDocumento::CEDULA:
            return "CEDULA";
        case TipoDocumento::OTRO:
            return "OTRO";
        default:
            return "DESCONOCIDO";
    }
}
std::string Pasajero::getNacionalidad() const {
    return nacionalidad;
}
Fecha Pasajero::getFechaNacimiento() const {
    return fechaNacimiento;
}
Habitacion* Pasajero::getHabitacion() const {
    return habitacion;
}

//Setters
void Pasajero::setHabitacion(Habitacion* habitacion) {
    this->habitacion = habitacion;
}

// Destructor
Pasajero::~Pasajero() {}