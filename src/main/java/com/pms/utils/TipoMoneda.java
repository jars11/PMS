package com.pms.utils;

/** Currency of the amounts. Declaration order matches the C++ enum class. */
public enum TipoMoneda {
    UYU,
    USD;

    @Override
    public String toString() {
        return name();
    }
}