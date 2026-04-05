#include "../../include/entities/TitularReserva.hpp"

// Constructor
TitularReserva::TitularReserva(const std::vector<std::string>& nombres, const std::vector<std::string>& apellidos,
                               const std::string& celular, const std::string& telefonoFijo, const std::string& email)
    : Persona(nombres, apellidos), celular(celular), telefonoFijo(telefonoFijo), email(email) {}

// Getters
std::string TitularReserva::getCelular() const {
    return celular;
}
std::string TitularReserva::getTelefonoFijo() const {
    return telefonoFijo;
}
std::string TitularReserva::getEmail() const {
    return email;
}

// Setters
void TitularReserva::setCelular(const std::string& celular) {
    this->celular = celular;
}
void TitularReserva::setTelefonoFijo(const std::string& telefonoFijo) {
    this->telefonoFijo = telefonoFijo;
}
void TitularReserva::setEmail(const std::string& email) {
    this->email = email;
}