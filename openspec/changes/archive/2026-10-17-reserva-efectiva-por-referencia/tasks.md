# Tasks: Contract as the single Solicitud-Reserva link

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 450-550 (prod ~90) |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Delivery strategy | single-pr |
| Chain strategy | n/a (single PR) |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: n/a (single PR)
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | PR | Focused test | Runtime harness | Rollback |
|------|------|----|--------------|-----------------|----------|
| 1 | Entities + schema (P1) | PR 1 | `mvn -B test -Dtest=SolicitudContratoReservaTest` | `mvn -B test` | entities + P1 tests |
| 2 | Lookups, replacement, cleanup | PR 1 | same | `mvn -B test` | same files |
| 3 | Draft DB (P5) | PR 1 | `mvn -B test` | `mvn exec:java -Dexec.mainClass=com.pms.Main` | `data/pms.mv.db` |

Single PR, 2000-line budget.

## Phase 1: Cardinalities

- [x] 1.1 RED: reflection in `SolicitudContratoReservaTest` - no `efectiva`/`isEfectiva`/`setEfectiva`, no `promoverEfectiva`.
- [x] 1.2 RED: H2 metadata - no `EFECTIVA`; contract FKs `NO`; two one-column UNIQUEs; no pair unique; `SOLICITUD.TITULAR_ID='YES'`, `RESERVA.TITULAR_ID='NO'`.
- [x] 1.3 RED: duplicate contract per side throws; `Reserva(null)` rejected; `Solicitud(null)` persists.
- [x] 1.4 GREEN: `Reserva.java` - remove flag/accessors; `titular` `nullable=false`.
- [x] 1.5 GREEN: `Solicitud.java` - remove `promoverEfectiva`.
- [x] 1.6 GREEN: `ContratoReserva.java` - per-side `@UniqueConstraint`; add `setReserva`.
- [x] 1.7 Adapt seven raw fixtures (`MatrizOcupacionTest:77`; `SolicitudContratoReservaTest:147,178,189,211,212,255`): drop flag tests; give each reserva a contract.
- [x] 1.8 Document `RepositorioReservas.guardar` as the public escape hatch; invariant covers controller-created reservas.

## Phase 2: Contract lookups

- [x] 2.1 Add both lookup names as seams delegating to flag-era methods.
- [x] 2.2 RED: contract-linked solicitud resolves reserva/contract; contract-less returns null; no cross-entity fields.
- [x] 2.3 GREEN: one join fetch per lookup, no flag filter; delete old methods; update `SolicitudContratoReservaTest:263,270,335`.

## Phase 3: Creation and replacement

- [x] 3.1 Update creation tests (`SolicitudContratoReservaTest:333-335,362,440,462`) to assert one contract.
- [x] 3.2 GREEN: drop four `setEfectiva(true)` writes (299, 403, 488, 695); keep titular -> reserva -> solicitud -> contract order.
- [x] 3.3 Stub `reemplazarReserva(long, Reserva)` throwing `UnsupportedOperationException`.
- [x] 3.4 RED: paid replacement migrates pagos, re-points the contract, deletes the old reserva, keeps solicitud, profile, titular; rollback keeps pagos; direct paid delete false.
- [x] 3.5 GREEN: new -> `setReserva` -> bulk `Pago` update -> flush/clear -> remove old, one transaction.
- [x] 3.6 Guards: each path yields one contract; abandoned selection persists nothing; zero consumed blocks persist nothing.

## Phase 4: Cleanup order

- [x] 4.1 RED: source test asserts `Main.limpiarDatos` clears contracts/solicitudes before reservas.
- [x] 4.2 GREEN: add `eliminarContratos()`; `Main` order contracts, solicitudes, reservas, personas, habitaciones.
- [x] 4.3 GREEN: `TestUtil.limpiarBase()` calls `eliminarContratos()` first.
- [x] 4.4 Guard: `eliminarReserva` deletes contract and reserva; solicitud survives.

## Phase 5: Draft DB

- [x] 5.1 Delete `data/pms.mv.db`.
- [x] 5.2 Rebuild via `mvn exec:java -Dexec.mainClass=com.pms.Main`; confirm recreation.
- [x] 5.3 Query the recreated file for 1.2's facts.
- [x] 5.4 `mvn -B test` and `mvn -B -DskipTests package` green.
