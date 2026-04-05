#ifndef PERSONA_HPP
#define PERSONA_HPP

#include <vector>
#include <string>

class Persona {
    private:
        int id;
        std::vector<std::string> nombres;
        std::vector<std::string> apellidos;

    public:
        // Constructor
        Persona(const std::vector<std::string>& nombres, const std::vector<std::string>& apellidos);

        // Getters
        int getId() const;
        std::vector<std::string> getNombres() const;
        std::vector<std::string> getApellidos() const;

        // Destructor
        ~Persona();
};

#endif