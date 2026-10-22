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
     * Variante con titulo de ventana. Por defecto ignora el titulo y delega en
     * {@link #confirmar(String)}; la UI lo usa para mostrar avisos + pregunta en
     * una sola ventana.
     */
    default boolean confirmar(String titulo, String pregunta) {
        return confirmar(pregunta);
    }

    /**
     * Notifica al usuario un mensaje informativo (no requiere respuesta).
     *
     * @param mensaje texto a mostrar
     */
    void notificar(String mensaje);
}
