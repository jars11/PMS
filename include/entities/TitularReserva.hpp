#ifndef TITULARRESERVA_HPP
#define TITULARRESERVA_HPP

#include "Persona.hpp"
#include "Pago.hpp"
#include <string>
#include <vector>

class TitularReserva : public Persona {
    private:
        std::string celular;
        std::string telefonoFijo;
        std::string email;
        std::vector<Pago*> pagosAsociados;

    public:
        // Constructor
        TitularReserva(const std::vector<std::string>& nombres, const std::vector<std::string>& apellidos,
                       const std::string& celular, const std::string& telefonoFijo, const std::string& email);


        // Getters
        std::string getCelular() const;
        std::string getTelefonoFijo() const;
        std::string getEmail() const;

        // Setters
        void setCelular(const std::string& celular);
        void setTelefonoFijo(const std::string& telefonoFijo);
        void setEmail(const std::string& email);

        // Destructor
        ~TitularReserva() {}
};

#endif