package com.pms;

import com.pms.ui.SemillaDemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the web port of the PMS.
 *
 * <p>Boot is used only as the REST/HTTP layer: business logic keeps running on
 * the existing {@code controllers} + {@code handlers} + {@code repositories}
 * and the existing {@code JpaUtil}/{@code persistence.xml} JPA stack, exactly
 * as the Swing app did. We deliberately do NOT add {@code starter-data-jpa}, so
 * Spring does not fight the resource-local EntityManagerFactory.
 *
 * <p>At startup, demo data is seeded when the database is empty (same seed the
 * Swing {@code PmsApp} used), so the SPA has rooms and reservations to show.
 */
@SpringBootApplication
public class PmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PmsApplication.class, args);
    }

}
