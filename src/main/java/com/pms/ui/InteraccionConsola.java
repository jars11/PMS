package com.pms.ui;

import com.pms.handlers.InteraccionUsuario;

import java.io.PrintStream;
import java.util.Scanner;

/**
 * Implementación de consola de {@link InteraccionUsuario}: pregunta por stdin y
 * muestra las notificaciones por stdout. Inyectable con Scanner/PrintStream
 * para poder testearla sin tocar la consola real.
 */
public class InteraccionConsola implements InteraccionUsuario {

    private final Scanner scanner;
    private final PrintStream out;

    public InteraccionConsola() {
        this(new Scanner(System.in), System.out);
    }

    public InteraccionConsola(Scanner scanner, PrintStream out) {
        this.scanner = scanner;
        this.out = out;
    }

    @Override
    public boolean confirmar(String pregunta) {
        out.print("? " + pregunta + " [s/n]: ");
        out.flush();
        if (!scanner.hasNextLine()) {
            return false;
        }
        String respuesta = scanner.nextLine().trim().toLowerCase();
        return respuesta.equals("s")
                || respuesta.equals("si")
                || respuesta.equals("sí")
                || respuesta.equals("y")
                || respuesta.equals("yes");
    }

    @Override
    public void notificar(String mensaje) {
        out.println("  - " + mensaje);
    }
}
