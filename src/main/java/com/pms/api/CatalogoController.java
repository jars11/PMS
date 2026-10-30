package com.pms.api;

import com.pms.controllers.ControladorHabitaciones;
import com.pms.controllers.ControladorPersonas;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only REST endpoints for rooms and reservation holders, reusing the
 * existing business controllers unchanged.
 */
@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final ControladorHabitaciones habitaciones = ControladorHabitaciones.getInstance();
    private final ControladorPersonas personas = ControladorPersonas.getInstance();

    @GetMapping("/habitaciones")
    public List<HabitacionVistaDto> habitaciones() {
        return habitaciones.getHabitaciones().stream().map(HabitacionVistaDto::de).toList();
    }

    @GetMapping("/titulares")
    public List<TitularVistaDto> titulares() {
        return personas.getTitulares().stream().map(TitularVistaDto::de).toList();
    }
}
