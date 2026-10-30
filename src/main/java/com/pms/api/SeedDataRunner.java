package com.pms.api;

import com.pms.persistence.JpaUtil;
import com.pms.ui.SemillaDemo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds demo rooms and reservations on startup when the database is empty,
 * mirroring what the Swing {@code PmsApp} did at launch. Reuses the existing
 * pure seed logic without reimplementing it.
 */
@Component
public class SeedDataRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        SemillaDemo.sembrarSiVacio();
        SemillaDemo.sembrarReservasDemo();
    }
}
