#ifndef DTFECHA_H
#define DTFECHA_H

class DTFecha {
private:
    int dia;
    int mes;
    int anio;

public:
    // Constructor
    DTFecha(int dia, int mes, int anio);

    // Getters
    int getDia() const;
    int getMes() const;
    int getAnio() const;

    // Comparar fechas
    bool operator==(const DTFecha& other) const;
    bool operator!=(const DTFecha& other) const;
    bool operator<(const DTFecha& other) const;
    bool operator<=(const DTFecha& other) const;
    bool operator>(const DTFecha& other) const;
    bool operator>=(const DTFecha& other) const;

    // Calcular diferencia entre fechas
    int operator-(const DTFecha& other) const;

    // Sumar días a la fecha
    DTFecha operator+(int dias) const;

    // Sumar dos fechas (no común, pero implementado según solicitud)
    DTFecha operator+(const DTFecha& other) const;

    // Destructor
    ~DTFecha();
};

#endif // DTFECHA_H