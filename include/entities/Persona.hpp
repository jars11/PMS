#ifndef PERSONA_HPP
#define PERSONA_HPP

#include <vector>
#include <string>

class Persona {
    private:
        std::vector<std::string> nombres;
        std::vector<std::string> apellidos;

    public:
        // Constructor
        Persona(const std::vector<std::string>& nombres, const std::vector<std::string>& apellidos)
            : nombres(nombres), apellidos(apellidos) {}

        // Getters
        std::vector<std::string> getNombres() const {
            return nombres;
        }

        std::vector<std::string> getApellidos() const {
            return apellidos;
        }

        // Destructor
        ~Persona() {}
};

#endif