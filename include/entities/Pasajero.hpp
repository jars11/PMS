#ifndef PASAJERO_HPP
#define PASAJERO_HPP

#include "../utils.hpp"
#include "Persona.hpp"
#include "Habitacion.hpp"
#include "../dtos/Fecha.hpp"
#include <string>
#include <vector>

class Pasajero : public Persona {
    private:
        std::string numeroDocumento;
        TipoDocumento tipoDocumento;
        std::string nacionalidad;
        Fecha fechaNacimiento;
        Habitacion* habitacion;

    public:
        // Constructor
        Pasajero(const std::vector<std::string>& nombres, const std::vector<std::string>& apellidos,
                 const std::string& numeroDocumento, const TipoDocumento tipoDocumento, const std::string& nacionalidad, const Fecha& fechaNacimiento);

        // Getters
        std::string getNumeroDocumento() const;
        TipoDocumento getTipoDocumento() const;
        std::string getTipoDocumentoString() const;
        std::string getNacionalidad() const;
        Fecha getFechaNacimiento() const;
        Habitacion* getHabitacion() const;

        //Setters
        void setHabitacion(Habitacion* habitacion);

        // Destructor
        ~Pasajero();
};

#endif