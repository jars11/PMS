#include "../include/DTFecha.h"
#include <stdexcept>
#include <ctime>

// Constructor
DTFecha::DTFecha(int dia, int mes, int anio) : dia(dia), mes(mes), anio(anio) {
    if (dia < 1 || mes < 1 || mes > 12 || anio < 0 ||
        (mes == 2 && (dia > 29 || (dia == 29 && (anio % 4 != 0 || (anio % 100 == 0 && anio % 400 != 0))))) ||
        ((mes == 4 || mes == 6 || mes == 9 || mes == 11) && dia > 30) ||
        (dia > 31)) {
        throw std::invalid_argument("Fecha inválida");
    }
}

// Getters
int DTFecha::getDia() const {
    return dia;
}

int DTFecha::getMes() const {
    return mes;
}

int DTFecha::getAnio() const {
    return anio;
}

// Comparar fechas
bool DTFecha::operator==(const DTFecha& other) const {
    return dia == other.dia && mes == other.mes && anio == other.anio;
}

bool DTFecha::operator!=(const DTFecha& other) const {
    return !(*this == other);
}

bool DTFecha::operator<(const DTFecha& other) const {
    if (anio != other.anio) return anio < other.anio;
    if (mes != other.mes) return mes < other.mes;
    return dia < other.dia;
}

bool DTFecha::operator<=(const DTFecha& other) const {
    return *this < other || *this == other;
}

bool DTFecha::operator>(const DTFecha& other) const {
    return !(*this <= other);
}

bool DTFecha::operator>=(const DTFecha& other) const {
    return !(*this < other);
}

// Calcular diferencia entre fechas
int DTFecha::operator-(const DTFecha& other) const {
    std::tm fecha1 = {};
    fecha1.tm_year = anio - 1900;
    fecha1.tm_mon = mes - 1;
    fecha1.tm_mday = dia;

    std::tm fecha2 = {};
    fecha2.tm_year = other.anio - 1900;
    fecha2.tm_mon = other.mes - 1;
    fecha2.tm_mday = other.dia;

    std::time_t tiempo1 = std::mktime(&fecha1);
    std::time_t tiempo2 = std::mktime(&fecha2);

    if (tiempo1 == -1 || tiempo2 == -1) {
        throw std::invalid_argument("Error al calcular la diferencia de fechas");
    }

    return static_cast<int>(std::difftime(tiempo1, tiempo2) / (60 * 60 * 24));
}

// Sumar días a la fecha
DTFecha DTFecha::operator+(int dias) const {
    int nuevoDia = dia + dias;
    int nuevoMes = mes;
    int nuevoAnio = anio;

    while (nuevoDia > 30) { // Simplified month length
        nuevoDia -= 30;
        nuevoMes++;
        if (nuevoMes > 12) {
            nuevoMes = 1;
            nuevoAnio++;
        }
    }

    return DTFecha(nuevoDia, nuevoMes, nuevoAnio);
}

// Sumar dos fechas
DTFecha DTFecha::operator+(const DTFecha& other) const {
    int nuevoDia = dia + other.dia;
    int nuevoMes = mes + other.mes;
    int nuevoAnio = anio + other.anio;

    while (nuevoDia > 30) { // Simplified month length
        nuevoDia -= 30;
        nuevoMes++;
    }

    while (nuevoMes > 12) {
        nuevoMes -= 12;
        nuevoAnio++;
    }

    return DTFecha(nuevoDia, nuevoMes, nuevoAnio);
}

// Destructor
DTFecha::~DTFecha() {
    // No dynamic memory to free
}