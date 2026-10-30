# PMS – Property Management System

Sistema de asignación inteligente de habitaciones para un hotel. Dada una solicitud de reserva (fechas, cantidad de pasajeros y preferencias), propone automáticamente una habitación concreta maximizando la continuidad de la estadía y las preferencias del huésped.

## Estado actual

Port **web end-to-end (PoC)**: backend **Spring Boot (REST)** reutilizando la lógica de negocio del prototipo, y frontend **React SPA**. La UI Swing original se conserva como demo legada, sin cambios.

| Componente | Estado |
|---|---|
| Backend REST (Spring Boot 3.2.5) | Completo |
| Frontend SPA (React 19 + Vite) | Completo (login, reservas, calendario, propuestas) |
| Calendario con drag & drop | Completo (crear, mover, redimensionar, navegación, paridad con Swing) |
| Propuesta de estadía | Completo (búsqueda de huecos, panel lateral, aceptar y crear reserva) |
| Lógica de asignación | Reutilizada (cadena de manejadores + búsqueda de huecos) |
| Persistencia | JPA/Hibernate + H2 (base de archivo) |
| Swing (legacy) | Conservado como demo |
| Tests | 373 tests (JUnit 5 + integración REST) |

## Stack

- **Java 21** + **Spring Boot 3.2.5** (REST, Tomcat embebido)
- **JPA / Hibernate 6.4** + **H2 2.2**
- **React 19** + **Vite** (SPA)
- **JUnit 5** + MockMvc / TestRestTemplate

## Requisitos

- JDK 21+ (probado con Temurin 21 LTS)
- Maven 3.9+
- Node 18+ (probado con Node 20/24) y npm

## Cómo correrlo

Backend (API REST en `http://localhost:8080`):

    mvn spring-boot:run

Frontend (dev server en `http://localhost:5173`, proxea `/api` → `:8080`):

    cd frontend
    npm install
    npm run dev

Al iniciar, el backend siembra datos de demo (habitaciones y reservas) cuando la base está vacía.

### Login (usuarios hardcodeados en memoria)

| Usuario | Password | Rol |
|---|---|---|
| `admin` | `a` | ADMIN |
| `recepcionista` | `r` | RECEPCIONISTA |

## Tests y build

    mvn test                     # backend: 373 tests (JUnit 5 + integración REST)
    cd frontend && npm run build # build de producción (Vite)
    cd frontend && npm run lint  # oxlint

La persistencia usa H2 embebida: la aplicación opera contra `data/pms.mv.db` (ignorada por git); los tests usan una base en memoria que se crea y destruye en cada corrida.

## API REST (resumen)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/auth/login` | Login (`usuario`/`password` → `rol`) |
| `GET` | `/api/habitaciones` | Listar habitaciones |
| `GET` | `/api/titulares` | Listar titulares |
| `POST` | `/api/titulares` | Alta de titular |
| `GET` | `/api/titulares/buscar` | Buscar titular por documento |
| `GET` | `/api/reservas` | Listar reservas |
| `POST` | `/api/reservas` | Crear reserva |
| `PUT` | `/api/reservas/{id}` | Editar reserva |
| `DELETE` | `/api/reservas/{id}` | Eliminar (409 si tiene pagos asociados) |
| `GET` | `/api/reservas/{id}/edicion` | Datos para prellenar la edición |
| `GET` | `/api/reservas/{id}/compatibilidad` | Compatibilidad de movimiento entre habitaciones |
| `POST` | `/api/reservas/{id}/mover` | Mover reserva a otra habitación/noche |
| `POST` | `/api/reservas/{id}/redimensionar` | Redimensionar check-in/check-out |
| `POST` | `/api/reservas/{id}/late-checkout` | Alternar late check-out |
| `GET` | `/api/calendario?desde&hasta` | Ocupación por habitación (bloques) |
| `GET` | `/api/calendario/late-checkout` | Hay late check-out un día |
| `POST` | `/api/propuestas/buscar` | Buscar propuestas (huecos) |
| `POST` | `/api/propuestas/persistir` | Persistir propuestas |

## Estructura del código

    PMS/
    ├── pom.xml                    (build Maven, Java 21 + Spring Boot)
    ├── src/main/java/com/pms/
    │   ├── api/                   (controllers REST + DTOs de vista)
    │   ├── controllers/           (lógica de negocio: reservas, habitaciones, personas)
    │   ├── handlers/              (cadena de asignación + búsqueda de huecos + geometría)
    │   ├── entities/              (entidades JPA: Reserva, RHPropuesta, Pago, Persona, Habitacion…)
    │   ├── persistence/           (JpaUtil + repositorios)
    │   ├── dtos/                  (DTOs de negocio)
    │   ├── ui/                    (Swing legacy demo + SemillaDemo)
    │   └── PmsApplication.java    (entry point Spring Boot)
    ├── src/test/java/com/pms/     (tests JUnit 5 + integración REST)
    ├── frontend/                  (SPA React + Vite)
    ├── uml/                       (diagramas UML)
    ├── odd/                       (bitácora ODD de tareas)
    └── openspec/                  (specs y cambios)

## Diagramas

- Diagrama de conceptos

<img width="5643" height="3758" alt="Diagrama de Conceptos" src="https://github.com/user-attachments/assets/bf9acb08-cc3e-4679-9005-ebb7af76dffb" />

- Diagrama de clases de diseño

<img width="5156" height="3734" alt="Diagrama de Clases de Diseño" src="https://github.com/user-attachments/assets/d90ed3e2-a96f-4a41-8762-7850483b26b3" />

- Diagramas de comunicación de funciones clave

<img width="3382" height="1935" alt="Diagrama de Comunicación" src="https://github.com/user-attachments/assets/6baee31e-2a81-4031-9a04-a01ecacf0f58" />

## Notas

Proyecto originalmente modelado y prototipado en C++ (diseño orientado a objetos a partir de los diagramas UML de `uml/`). Se convirtió a **Java 21 + Maven + JPA/Hibernate + H2**, y luego se portó a web con **Spring Boot (REST) + React**, reutilizando la lógica de negocio existente (`controllers` + `handlers` + `repositories`) sin reescribirla. El calendario web replica la paridad funcional y visual del calendario Swing (drag & drop para crear/mover/redimensionar, tooltip, menú contextual, late check-out, navegación con Shift+rueda).
