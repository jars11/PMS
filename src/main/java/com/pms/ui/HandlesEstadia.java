package com.pms.ui;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Geometria pura de las flechas que aparecen al seleccionar una reserva en el
 * calendario. En cada extremo de la barra se dibujan hasta dos triangulos
 * blancos sobre una placa del color de la reserva:
 *
 * <ul>
 *   <li>extremo de check-in: la flecha hacia AFUERA (izquierda) extiende el
 *       check-in hacia atras; la flecha hacia ADENTRO (derecha) lo acorta.</li>
 *   <li>extremo de check-out: la flecha hacia ADENTRO (izquierda) acorta la
 *       estadia; la de AFUERA (derecha) la extiende.</li>
 * </ul>
 *
 * <p>La placa se apoya DENTRO de la barra. La del check-out puede entrar en la
 * celda anterior (la ultima noche de la misma reserva) para tener el mismo
 * ancho que la del check-in. Si en un extremo solo hay una flecha disponible,
 * ocupa toda la placa.
 */
final class HandlesEstadia {

    /** Extremo de la estadia al que pertenece la flecha. */
    enum Borde {
        INICIO,
        FIN
    }

    /** Efecto de arrastrar la flecha sobre la estadia. */
    enum Direccion {
        ALARGAR,
        ACORTAR
    }

    /** Que flechas estan disponibles en cada extremo. */
    record Flechas(boolean alargarInicio, boolean acortarInicio,
                   boolean acortarFin, boolean alargarFin) {

        static final Flechas NINGUNA = new Flechas(false, false, false, false);

        boolean alguna() {
            return alargarInicio || acortarInicio || acortarFin || alargarFin;
        }
    }

    /** Zona clickeable de una flecha, en coordenadas de la tabla. */
    record Zona(Borde borde, Direccion direccion, Rectangle rect) {

        boolean contiene(int x, int y) {
            return rect.contains(x, y);
        }

        /** True cuando el triangulo debe apuntar a la izquierda. */
        boolean haciaIzquierda() {
            return (borde == Borde.INICIO && direccion == Direccion.ALARGAR)
                    || (borde == Borde.FIN && direccion == Direccion.ACORTAR);
        }
    }

    /** Ancho de cada triangulo. */
    static final int ANCHO_FLECHA = 9;

    /** Ancho total de la placa (dos flechas). */
    static final int ANCHO_PLACA = 2 * ANCHO_FLECHA;

    private HandlesEstadia() {
    }

    /**
     * Flechas que corresponden a una estadia: SIEMPRE dos por extremo visible
     * (una hacia adentro y una hacia afuera). El arrastre de cualquiera de las
     * dos permite agrandar o achicar libremente; el rango se clampa contra las
     * noches ocupadas y contra el borde de la ventana.
     */
    static Flechas flechas(boolean inicioVisible, boolean finVisible) {
        return new Flechas(inicioVisible, inicioVisible, finVisible, finVisible);
    }

    /**
     * Zonas de las flechas visibles. {@code barraInicio} y {@code barraFin} son
     * los rectangulos que ocupa la barra dentro de la celda de check-in y de
     * check-out; {@code celdaInicio}/{@code celdaFin} acotan la placa.
     */
    static List<Zona> zonas(Rectangle celdaInicio, Rectangle barraInicio,
                            Rectangle celdaFin, Rectangle barraFin,
                            Flechas flechas) {
        List<Zona> zonas = new ArrayList<>();
        if (flechas == null || !flechas.alguna()) {
            return zonas;
        }
        if (flechas.alargarInicio() || flechas.acortarInicio()) {
            int ini = Math.max(celdaInicio.x, barraInicio.x);
            int fin = Math.min(celdaInicio.x + celdaInicio.width, ini + ANCHO_PLACA);
            Rectangle placa = new Rectangle(ini, barraInicio.y,
                    Math.max(1, fin - ini), barraInicio.height);
            zonas.addAll(placa(Borde.INICIO, placa,
                    Direccion.ALARGAR, flechas.alargarInicio(),
                    Direccion.ACORTAR, flechas.acortarInicio()));
        }
        if (flechas.acortarFin() || flechas.alargarFin()) {
            int fin = Math.min(celdaFin.x + celdaFin.width,
                    barraFin.x + barraFin.width);
            // Misma placa que el check-in. Puede entrar en la celda anterior,
            // que es la ultima noche de ESTA misma reserva.
            int ini = fin - ANCHO_PLACA;
            Rectangle placa = new Rectangle(ini, barraFin.y,
                    Math.max(1, fin - ini), barraFin.height);
            zonas.addAll(placa(Borde.FIN, placa,
                    Direccion.ACORTAR, flechas.acortarFin(),
                    Direccion.ALARGAR, flechas.alargarFin()));
        }
        return zonas;
    }

    /** Zona bajo el punto indicado, o {@code null}. */
    static Zona en(List<Zona> zonas, int x, int y) {
        if (zonas == null) {
            return null;
        }
        for (Zona zona : zonas) {
            if (zona.contiene(x, y)) {
                return zona;
            }
        }
        return null;
    }

    private static List<Zona> placa(Borde borde, Rectangle placa,
                                    Direccion izquierda, boolean hayIzquierda,
                                    Direccion derecha, boolean hayDerecha) {
        List<Zona> zonas = new ArrayList<>();
        if (hayIzquierda && hayDerecha) {
            int mitad = placa.width / 2;
            zonas.add(new Zona(borde, izquierda,
                    new Rectangle(placa.x, placa.y, mitad, placa.height)));
            zonas.add(new Zona(borde, derecha,
                    new Rectangle(placa.x + mitad, placa.y, placa.width - mitad, placa.height)));
        } else if (hayIzquierda) {
            zonas.add(new Zona(borde, izquierda, placa));
        } else if (hayDerecha) {
            zonas.add(new Zona(borde, derecha, placa));
        }
        return zonas;
    }
}
