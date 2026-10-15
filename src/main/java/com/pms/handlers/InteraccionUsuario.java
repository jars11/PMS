package com.pms.handlers;

/**
 * Canal de interacción con el usuario durante la búsqueda de huecos.
 *
 * El algoritmo buscarHuecos nunca hace I/O directo: pregunta y notifica a
 * través de esta interfaz. La consola o la UI la implementan; los tests usan un
 * doble que responde con una secuencia fija.
 */
public interface InteraccionUsuario {

    /**
     * Pregunta al usuario y devuelve true si acepta.
     *
     * @param pregunta texto de la pregunta (ya redactada por el algoritmo)
     * @return true si el usuario acepta, false si rechaza
     */
    boolean confirmar(String pregunta);

    /**
     * Notifica al usuario un mensaje informativo (no requiere respuesta).
     *
     * @param mensaje texto a mostrar
     */
    void notificar(String mensaje);
}
