#include "../include/Persona.hpp"

// Constructor
Persona::Persona(const std::vector<std::string>& nombres, const std::vector<std::string>& apellidos)
    : nombres(nombres), apellidos(apellidos) {}

// Getters
std::vector<std::string> Persona::getNombres() const {
    return nombres;
}

std::vector<std::string> Persona::getApellidos() const {
    return apellidos;
}

// Destructor
Persona::~Persona() {}