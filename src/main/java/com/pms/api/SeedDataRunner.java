package com.pms.api;

import com.pms.persistence.JpaUtil;
import com.pms.ui.SemillaDemo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds demo rooms on startup when the database is empty, mirroring what the
 * Swing {@code PmsApp} did at launch. Demo reservations are NOT auto-seeded
 * anymore (removed by request): the demo starts with an empty calendar and
 * reservations are created by hand.
 */
@Component
public class SeedDataRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        SemillaDemo.sembrarSiVacio();
    }
}
