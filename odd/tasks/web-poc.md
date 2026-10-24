# PMS → Web (PoC end-to-end)

Portar el PMS de Java Swing a web como spike/PoC. Backend Spring Boot (REST) reutilizando la lógica de negocio existente; frontend React SPA. Primera fase sin drag & drop.

## Specs

- S1. Stack: API REST en Java (Spring Boot) + frontend SPA (React).
- S2. Lógica de negocio: reutilizar al máximo. Extraer la lógica de Swing a servicios y exponerlos por REST. No reescribir todo.
- S3. Calendario: mantener drag & drop como objetivo, pero en el PoC arrancar con lista + formulario; D&D se agrega después.
- S4. Alcance PoC: login + listado de reservas + calendario básico + crear/editar reserva. El port completo se evalúa después.
- S5. Login: usuarios hardcodeados en memoria (admin/a, recepcionista/r), listos para crecer.
- S6. No borrar Swing ni romper los tests existentes en el PoC.

## Tasks

- T1 [S2,S6] Reubicar la lógica pura de `com.pms.ui` (CompatibilidadMovimiento y demás clases sin Swing) a `com.pms.handlers`/`utils`, eliminando el import de `controllers` a `ui`. Ruta: inline. Commit: feat(backend): relocate pure logic out of ui package.
- T2 [S1] Agregar Spring Boot (starter-web, parent/plugin) al `pom.xml` y clase `PmsApplication` reutilizando `persistence.xml`/H2. Ruta: inline. Commit: build(backend): add Spring Boot web bootstrap.
- T3 [S1,S5] Capa REST `com.pms.api`: login hardcodeado, listado, habitaciones, crear/editar reserva delegando a controllers existentes. Ruta: inline. Commit: feat(api): expose reservations via REST.
- T4 [S1,S4] Frontend React (`frontend/`): login, listado de reservas, calendario básico, formulario crear/editar. Ruta: inline. Commit: feat(frontend): reservations SPA (login, list, calendar, form).
- T5 [S4,S6] Verificación: tests REST (login, listar, crear, editar) + `mvn test` completo en verde. Ruta: inline. Commit: test(api): REST integration coverage.

## Log

- L1. `quiero que pases todo el proyecto actual a web, que deje de ser en java swing, pasalo todo a web`
- L2. Decisiones: stack Spring Boot + React; reutilizar lógica; calendario lista+formulario primero, D&D después; alcance spike/PoC end-to-end.
- L3. Login: usuarios hardcodeados en memoria (admin/a, recepcionista/r).
