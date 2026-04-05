#ifndef FECHA_H
#define FECHA_H

class Fecha {
    private:
        int dia;
        int mes;
        int anio;

        int diasEnMes(int mes, int anio) const;
        int diasEnAnio(int anio) const;
        bool esBisiesto(int anio) const;

    public:
        // Constructor
        Fecha(int dia, int mes, int anio);

        // Getters
        int getDia() const;
        int getMes() const;
        int getAnio() const;

        // Comparar fechas
        bool operator==(const Fecha& other) const;
        bool operator!=(const Fecha& other) const;
        bool operator<(const Fecha& other) const;
        bool operator<=(const Fecha& other) const;
        bool operator>(const Fecha& other) const;
        bool operator>=(const Fecha& other) const;

        // Calcular diferencia entre fechas
        int dias(const Fecha& desde, const Fecha& hasta) const;
        int anios(const Fecha& desde, const Fecha& hasta) const;

        // Sumar días a la fecha
        Fecha operator+(int dias) const;

        // Destructor
        ~Fecha();
};

#endif // FECHA_H