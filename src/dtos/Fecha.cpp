#include "../../include/dtos/Fecha.hpp"
#include <stdexcept>
#include <ctime>

// Constructor
Fecha::Fecha(int dia, int mes, int anio) : dia(dia), mes(mes), anio(anio) {
    if (dia < 1 || mes < 1 || mes > 12 || anio < 0 ||
        (mes == 2 && (dia > 28 || (dia == 29 && (anio % 4 != 0 || (anio % 100 == 0 && anio % 400 != 0))))) ||
        ((mes == 4 || mes == 6 || mes == 9 || mes == 11) && dia > 30) ||
        (dia > 31)) {
        throw std::invalid_argument("Fecha inválida");
    }
}

// Getters
int Fecha::getDia() const {
    return dia;
}
int Fecha::getMes() const {
    return mes;
}
int Fecha::getAnio() const {
    return anio;
}

int Fecha::diasEnMes(int mes, int anio) const {
    static const int diasPorMes[] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    if (mes == 2 && esBisiesto(anio)) {
        return 29;
    }
    return diasPorMes[mes - 1];
}

int Fecha::diasEnAnio(int anio) const {
    return esBisiesto(anio) ? 366 : 365;
}

bool Fecha::esBisiesto(int anio) const {
    return (anio % 4 == 0 && (anio % 100 != 0 || anio % 400 == 0));
}

// Comparar fechas
bool Fecha::operator==(const Fecha& other) const {
    return dia == other.dia && mes == other.mes && anio == other.anio;
}
bool Fecha::operator!=(const Fecha& other) const {
    return !(*this == other);
}
bool Fecha::operator<(const Fecha& other) const {
    if (anio != other.anio) return anio < other.anio;
    if (mes != other.mes) return mes < other.mes;
    return dia < other.dia;
}
bool Fecha::operator<=(const Fecha& other) const {
    return *this < other || *this == other;
}
bool Fecha::operator>(const Fecha& other) const {
    return !(*this <= other);
}
bool Fecha::operator>=(const Fecha& other) const {
    return !(*this < other);
}

// Calcular diferencia entre fechas
int Fecha::dias(const Fecha& desde, const Fecha& hasta) const {
    int totalDiasDesde = 0;
    int totalDiasHasta = 0;

    for (int y = 1900; y < anio; ++y) {
        totalDiasDesde += diasEnAnio(y);
    }
    for (int m = 1; m < mes; ++m) {
        totalDiasDesde += diasEnMes(m, anio);
    }
    totalDiasDesde += dia;

    for (int y = 1900; y < hasta.anio; ++y) {
        totalDiasHasta += diasEnAnio(y);
    }
    for (int m = 1; m < hasta.mes; ++m) {
        totalDiasHasta += diasEnMes(m, hasta.anio);
    }
    totalDiasHasta += hasta.dia;

    return totalDiasDesde - totalDiasHasta;
}
int Fecha::anios(const Fecha& desde, const Fecha& hasta) const {
    int aniosDiff = hasta.anio - desde.anio;

    if (hasta.mes < desde.mes || (hasta.mes == desde.mes && hasta.dia < desde.dia)) {
        aniosDiff--;
    }

    return aniosDiff;
}


// Sumar días a la fecha
Fecha Fecha::operator+(int dias) const {
    int nuevoDia = dia + dias;
    int nuevoMes = mes;
    int nuevoAnio = anio;

    while (nuevoDia > diasEnMes(nuevoMes, nuevoAnio)) {
        nuevoDia -= diasEnMes(nuevoMes, nuevoAnio);
        nuevoMes++;
        if (nuevoMes > 12) {
            nuevoMes = 1;
            nuevoAnio++;
        }
    }

    return Fecha(nuevoDia, nuevoMes, nuevoAnio);
}

// Destructor
Fecha::~Fecha() {
    // No dynamic memory to free
}