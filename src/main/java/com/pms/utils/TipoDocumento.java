package com.pms.utils;

/** Document type of a person. Declaration order matches the C++ enum class. */
public enum TipoDocumento {
    DNI,
    PASAPORTE,
    CEDULA,
    OTRO;

    @Override
    public String toString() {
        return name();
    }
}