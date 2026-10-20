# Design: Contract as the single Solicitud-Reserva link

## Technical Approach

Replace `Reserva.efectiva`/accessors, `Solicitud.promoverEfectiva`, and the four `setEfectiva(true)` writes with `ContratoReserva` as the only link. Per-side NOT NULL + UNIQUE gives at-most-one; the four paths funneled through `persistirReservaYSolicitud` guarantee at least one. Reserva candidates remain in memory; only the selected one persists. `Reserva.titular` becomes NOT NULL, `Solicitud.titular` stays nullable. Recreate the draft H2 file; `hbm2ddl update` cannot drop columns/constraints.

## Architecture Decisions

| # | Decision | Alternatives | Rationale |
|---|---|---|---|
| D1 | Both contract FKs NOT NULL + single UNIQUE; no pointers or cross-collections. | Nullable pointer; pair UNIQUE. | Encodes both cardinalities; a pointer duplicates; the pair allowed many. |
| D2 | SQL gives at-most-one; `persistirReservaYSolicitud` is the only creation seam, plus per-path count guard. | Trigger; mandatory factory. | Portable; triggers are H2-specific. |
| D3 | `reemplazarReserva(oldId, nueva)`: persist new -> `contract.setReserva(new)` -> bulk `Pago` FK update -> flush/clear -> reload/remove old. | Full workflow; recreate contract; lift guard. | Proposal excludes a workflow, delta requires replacement; smallest seam reuses solicitud/contract, keeps the guard. Limits: no UI/selection/profile/mirror. |
| D4 | Contract -> reserva/solicitud; keep `eliminarReserva`'s guard; add `eliminarContratos()`; call first in `TestUtil`/`Main`. | Cascade; reservas first. | `Main` deletes neither, so a second run violates FKs. |
| D5 | Rename to `findReservaDeSolicitud`/`findContratoDeSolicitud`, one join fetch, null if none. | Keep flag-era names/queries. | "Efectiva" is gone; no callers lost. |
| D6 | Recreate `data/pms.mv.db`; `Reserva.titular` NOT NULL; `Solicitud.titular` nullable. | `update`; SQL migration. | `update` drops nothing; DB is a gitignored draft. |
| D7 | No `equals`/`hashCode` override; reflection guard. | id equality. | Id is null pre-persist; generated hashes corrupt holding sets. |
| D8 | U1-U5 RED-first; U6 guards; U7 migration. | Big-bang port. | One behavior per reviewable unit. |

## Data Flow

```
create: titular -> reserva -> solicitud (cascade contract)
read:   solicitudId -> ContratoReserva join fetch reserva -> Reserva | null
replace: new -> contract.setReserva(new) -> update Pago FK -> flush/clear -> remove old
delete: contract -> reserva; solicitud survives
```

FK integrity, not SQL order, proves the insert order.

## File Changes

| File | Action | Description |
|---|---|---|
| `src/main/java/com/pms/entities/Reserva.java` | Modify | Remove flag/accessors; titular NOT NULL. |
| `src/main/java/com/pms/entities/Solicitud.java` | Modify | Remove `promoverEfectiva`. |
| `src/main/java/com/pms/entities/ContratoReserva.java` | Modify | Per-side UNIQUE; add `setReserva`. |
| `src/main/java/com/pms/controllers/ControladorReservas.java` | Modify | Drop four flag writes; add replacement helper. |
| `src/main/java/com/pms/persistence/RepositorioSolicitudes.java` | Modify | Contract lookups; add `eliminarContratos`. |
| `src/main/java/com/pms/Main.java` | Modify | Contracts, solicitudes, reservas, then persons/rooms. |
| `src/test/java/com/pms/TestUtil.java` | Modify | Contracts before solicitudes/reservas. |
| `src/test/java/com/pms/SolicitudContratoReservaTest.java` | Modify | Replace flag tests; add cardinality, replacement, schema tests. |
| `data/pms.mv.db` | Delete | Rebuilt on next `pms` boot. |

## Interfaces / Contracts

```java
@Table(name = "contrato_reserva", uniqueConstraints = {
    @UniqueConstraint(columnNames = "solicitud_id"),
    @UniqueConstraint(columnNames = "reserva_id")})
void setReserva(Reserva reserva);

@ManyToOne(fetch = FetchType.EAGER, optional = false)
@JoinColumn(name = "titular_id", nullable = false)
private TitularReserva titular; // no efectiva

Reserva findReservaDeSolicitud(long solicitudId);
ContratoReserva findContratoDeSolicitud(long solicitudId);
long eliminarContratos();
Reserva reemplazarReserva(long reservaViejaId, Reserva nueva);
```

`reemplazarReserva` requires a transient `nueva` with proposals and titular, no payments.

## Testing Strategy

| # | Layer | RED (fails before) | GREEN change |
|---|---|---|---|
| U1 | Integration | Linked contract resolves reserva/contract; none -> null. | Rewrite/rename queries. |
| U2 | Unit + schema | Reflection finds flag/methods; `EFECTIVA` column. | Delete surface + four writes; rewrite flag assertions. |
| U3 | Integration + schema | Duplicate per side rejected; titular-less reserva rejected; metadata wrong. | Per-side UNIQUE; titular NOT NULL. |
| U4 | Integration | Pagos migrate; old reserva removed; contract re-pointed; solicitud survives; rollback intact; direct delete blocked. | Add setter + helper. |
| U5 | Source inspection | `Main.limpiarDatos` lacks cleanup before reservas. | Add `eliminarContratos`; update `Main`/`TestUtil`. |
| U6 | Guards (no RED) | - | One contract per path; abandoned selection persists zero; `equals`/`hashCode` inherited; `eliminarReserva` keeps solicitud. |
| U7 | Migration | - | Delete draft DB; `mvn -B test`; verify file metadata. |

Metadata (H2 `INFORMATION_SCHEMA`): no `EFECTIVA`; both FKs `NO`; two one-column UNIQUEs, no pair; `SOLICITUD.TITULAR_ID='YES'`, `RESERVA.TITULAR_ID='NO'`.

## Threat Matrix

N/A - no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary. Pure in-process JPA.

## Migration / Rollout

Delete `data/pms.mv.db`; next `pms` boot rebuilds the schema; `pms-test` recreates per run. No backfill: contracts exist; effectiveness data is discarded. Rollback: revert and delete the file; verify with the same metadata queries.

## Open Questions

- [ ] Keep the existing contract profile on replacement (chosen), or apply a new one?
- [ ] Keep `RepositorioReservas.guardar` public although it can persist contract-less reservas?
- [ ] On titular change, should `Solicitud.titular` update? Helper assumes not.
