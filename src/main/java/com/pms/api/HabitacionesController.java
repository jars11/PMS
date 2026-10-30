package com.pms.api;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.entities.Habitacion;
import com.pms.utils.AtributoHabitacion;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * Alta y reordenamiento de habitaciones (seccion admin "Habitaciones"). Ambas
 * operaciones delegan en {@link ControladorHabitaciones}: el alta usa
 * {@code altaHabitacion} (prioridad al final de la lista) y el orden
 * {@code reordenar} (prioridad 1 para el primer id). Los errores de negocio se
 * traducen solos via {@link ApiExceptionHandler}: numero duplicado es 409 y una
 * habitacion inexistente es 400.
 *
 * <p>El listado {@code GET /api/habitaciones} sigue en {@link CatalogoController}.</p>
 */
@RestController
@RequestMapping("/api")
public class HabitacionesController {

    private final ControladorHabitaciones habitaciones = ControladorHabitaciones.getInstance();

    /** Body del alta: mismos datos que {@code ControladorHabitaciones.altaHabitacion}. */
    public record AltaHabitacionRequest(
            Integer numero,
            Set<AtributoHabitacion> atributos,
            int camasMatrimoniales,
            int camasIndividuales,
            int cunas,
            int colchones,
            float tarifaPorNoche) {
    }

    /** Body del reordenamiento: ids en el orden deseado (el primero es prioridad 1). */
    public record OrdenRequest(List<Long> ids) {
    }

    /** Alta de habitacion: devuelve la vista de la habitacion creada y su prioridad. */
    @PostMapping("/habitaciones")
    public ResponseEntity<HabitacionVistaDto> crear(@RequestBody AltaHabitacionRequest req) {
        // Un numero ausente cae en la validacion compartida (numero invalido -> 400).
        int numero = req.numero() == null ? 0 : req.numero();
        habitaciones.altaHabitacion(numero, req.atributos(), req.camasMatrimoniales(),
                req.camasIndividuales(), req.cunas(), req.colchones(), req.tarifaPorNoche());
        return ResponseEntity.ok(HabitacionVistaDto.de(habitacionCreada(numero)));
    }

    /** Reordena las habitaciones segun la lista de ids y devuelve ese orden. */
    @PutMapping("/habitaciones/orden")
    public List<HabitacionVistaDto> reordenar(@RequestBody OrdenRequest req) {
        return habitaciones.reordenar(req.ids()).stream()
                .map(HabitacionVistaDto::de)
                .toList();
    }

    private Habitacion habitacionCreada(int numero) {
        return habitaciones.getHabitaciones().stream()
                .filter(h -> h.getNumero() != null && h.getNumero() == numero)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("habitacion no reflejada en el listado: " + numero));
    }
}
