package com.pms.utils;

/**
 * Room attributes. Declaration order matches the C++ enum class
 * (Ordered by the enum ordinal, exactly like std::set&lt;AtributoHabitacion&gt;
 * iterates by underlying enum value in the C++ source).
 */
public enum AtributoHabitacion {
    Balcon,
    VistaAlMar,
    Jacuzzi,
    Twin,
    DosAmbientes;

    @Override
    public String toString() {
        return name();
    }
}