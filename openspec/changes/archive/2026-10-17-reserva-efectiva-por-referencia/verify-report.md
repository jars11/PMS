```yaml
schema: gentle-ai.verify-result/v1
evidence_revision: sha256:669af2fe8b8049b3cffb191ef072f6889292a3a7451458392c3d94b8619f3c51
verdict: pass_with_warnings
blockers: 0
critical_findings: 0
requirements: 11/11
scenarios: 34/34
test_command: mvn -B test
test_exit_code: 0
test_output_hash: sha256:84a57ddf12cd03b9f441789fb8bc0bc3c81e48f694002f759b6a4683cc085e8f
build_command: mvn -B -DskipTests package
build_exit_code: 0
build_output_hash: sha256:4448ecdcfe5cead1e22605970ab68aef09124e9e45e6a61b78e69d9ab3dadfeb
```

# Verification Report

**Change**: `reserva-efectiva-por-referencia`
**Artifact store**: openspec
**Mode**: Strict TDD (config `strict_tdd: true`)
**Evidence revision**: `sha256:669af2fe8b8049b3cffb191ef072f6889292a3a7451458392c3d94b8619f3c51` (SHA-256 of `git diff` stdout followed by `proposal.md`, `design.md`, `tasks.md`, the delta spec, and the baseline spec, concatenated byte-for-byte; 79,671 bytes).
**Supersedes**: the previous `fail` report in this file (evidence revision `sha256:a6bde05a…`, 223 tests, C1 blockers).

## Supersession of the Previous FAIL

This re-verification was triggered by the corrective pass recorded in `apply-progress.md`. The previous verdict is withdrawn in favor of `pass_with_warnings`.

| Prior finding | Status now | Evidence |
|---|---|---|
| C1 — `replacement fails` had no mid-transaction rollback test | **RESOLVED** | `SolicitudContratoReservaTest.reemplazoFallidoAMitadDeTransaccionRevierteReservaPagosYContrato` (`:722`) starts a real transaction, persists the new reserva (`nueva.getId() != null`), fails on the `RHPROPUESTA.HABITACION_ID` FK, and asserts rollback of reserva, payment, and contract link. The old pre-validation test (`:699`) remains a separate case. |
| W1 — apply task count 29 vs 25 | **RESOLVED** | `tasks.md` has exactly 25 checked boxes; `apply-progress.md` now reports 25/25. |
| W2 — "old contract deleted" vs reuse | **RESOLVED** | The delta scenario, design U4 row, and task 3.4 now say the contract is re-pointed to the new reserva. |
| W3 — vacuous abandoned-selection test | **RESOLVED** | `seleccionAbandonadaNoPersisteNada` (`:814`) persists a negative control, proves the repositories detect rows, clears them, then abandons transient candidates and asserts zero reservas/solicitudes/contracts. |
| W4 — no persisted TDD evidence table | **RESOLVED** | `apply-progress.md` contains a 25-row TDD Cycle Evidence table with safety net, RED, GREEN, triangulation, and refactor columns. |
| W5 — exactly-one invariant contradicted `RepositorioReservas.guardar` | **PARTIALLY RESOLVED** | The delta requirement now scopes exactly-one to controller creation paths and documents `guardar` as a lower-level escape hatch. The public escape hatch itself remains by design (WARNING W5). |
| W6 — zero-block solicitud could persist | **RESOLVED** | `ControladorReservas.crearReserva` counts consumed blocks and throws `IllegalStateException` inside the transaction when zero are consumed (`:299`). Two tests cover empty and all-skipped input, and the delta adds a `no consumed blocks` scenario. |

## Completeness

| Metric | Value |
|---|---|
| Tasks total | 25 |
| Tasks complete | 25 |
| Tasks incomplete | 0 |

`tasks.md` contains 25 checkboxes (Phase 1: 8, Phase 2: 3, Phase 3: 6, Phase 4: 4, Phase 5: 4), all checked. `apply-progress.md` reports 25/25. The previous 29/29 mismatch is closed.

## Build & Tests Execution

### Tests

Command: `mvn -B test`

Exit code: `0`

Raw summary lines:

```text
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.264 s -- in com.pms.SolicitudContratoReservaTest
[INFO] Tests run: 226, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  18.474 s
```

- 226 tests passed, 0 failed, 0 errors, 0 skipped.
- 25 test classes executed. `target/surefire-reports` contains 26 report files; `com.pms.ui.ScratchRatioTest` is a stale report from an earlier run and is not part of this execution.
- `SolicitudContratoReservaTest` now carries 38 tests (35 before the corrective pass; +C1 and +2 W6 tests).
- Output hash: `sha256:84a57ddf12cd03b9f441789fb8bc0bc3c81e48f694002f759b6a4683cc085e8f` (SHA-256 of the complete stdout+stderr file).

C1 runtime evidence in the same run:

```text
WARN: SQL Error: 23506, SQLState: 23506
ERROR: Violacion de una restriccion de Integridad Referencial:
"FK1MFLCHHB8V0U6VBUF4LHTHYF: PUBLIC.RHPROPUESTA FOREIGN KEY(HABITACION_ID)
REFERENCES PUBLIC.HABITACION(ID) (CAST(507 AS BIGINT))"
```

This is the expected mid-transaction failure; the test passed, so the transaction rolled back.

### Build

Command: `mvn -B -DskipTests package`

Exit code: `0`

Raw summary lines:

```text
[INFO] --- compiler:3.13.0:compile (default-compile) @ pms ---
[INFO] Nothing to compile - all classes are up to date.
[INFO] Tests are skipped.
[INFO] --- jar:3.4.1:jar (default-jar) @ pms ---
[INFO] BUILD SUCCESS
[INFO] Total time:  2.863 s
```

- Output hash: `sha256:4448ecdcfe5cead1e22605970ab68aef09124e9e45e6a61b78e69d9ab3dadfeb`.
- The production classes were already compiled from the corrective-pass source, so the compiler reported nothing to compile. The jar plugin emitted no `Building jar` line and `target/pms-1.0.0.jar` kept its previous timestamp (`2026-10-17 05:50:10`, before this run at `05:54:10`), so the package succeeded without regenerating the jar. A `mvn -B -DskipTests clean package` is suggested if a clean-room jar artifact is required (S8).

### Coverage

Not available. `openspec/config.yaml` declares `coverage.available: false` and no coverage command. No line or branch coverage was measured.

## Structural Verification

| Claim | Evidence | Result |
|-------|----------|--------|
| No `Reserva.efectiva`, `isEfectiva()`, or `setEfectiva(boolean)` | Production grep finds no match. The only `efectiva` hits under `src/main/java` are `capacidadEfectiva(...)` in `ManejadorBusquedaHuecos` (effective capacity, unrelated). `reservaNoDeclaraFlagEfectiva` asserts `NoSuchFieldException`/`NoSuchMethodException` against the compiled class. | ✅ |
| No `Solicitud.promoverEfectiva` | Production grep finds no match; only the reflection test references it. `solicitudNoDeclaraPromoverEfectiva` passes. | ✅ |
| No `RESERVA.EFECTIVA` column | `esquemaSinColumnaEfectiva` passes against `pms-test`. Read-only query of `data/pms.mv.db` returns `EFECTIVA_COLUMNS = 0`; `RESERVA` has 9 columns (`ID, ACTIVA, ESTADO, LATECHECKOUT, MONEDA, MONTOPAGADO, MONTOSENIA, MONTOTOTAL, TITULAR_ID`). | ✅ |
| `contrato_reserva.solicitud_id` NOT NULL + UNIQUE | `ContratoReserva.java:27-28` (single-column unique) and `:36-37` (`optional = false`, `nullable = false`). Metadata: `NO`; `UKR5Y7FH6U0A6RP8BRA4XVBFFU6 UNIQUE SOLICITUD_ID 1`. | ✅ |
| `contrato_reserva.reserva_id` NOT NULL + UNIQUE | `ContratoReserva.java:27-28` and `:40-41`. Metadata: `NO`; `UK3H4HLJ46AQ62M7Y9YWXH05U6R UNIQUE RESERVA_ID 1`. | ✅ |
| Old `(solicitud_id, reserva_id)` pair unique is gone | `esquemaConCardinalidadesFinales` asserts exactly two UNIQUE constraints, each with one column, covering `SOLICITUD_ID` and `RESERVA_ID`. File DB shows only the two single-column UNIQUEs. | ✅ |
| `solicitud.titular_id` nullable | `Solicitud.java:31-33` has no `optional=false`/`nullable=false`; metadata reports `YES`. | ✅ |
| `reserva.titular_id` NOT NULL | `Reserva.java:60-61` (`optional = false`, `nullable = false`); metadata reports `NO`. | ✅ |
| Creation persists one reserva, one solicitud, one contract | `ControladorReservas.persistirReservaYSolicitud` (`:322-327`) is the single seam; all four creation paths call it. Tests assert one solicitud and one contract per path. | ✅ |
| Replacement migrates pagos before deleting the old reserva | `ControladorReservas.reemplazarReserva` (`:714`): `em.persist(nueva)` (`:743`), `contrato.setReserva(nueva)` (`:747`), bulk `Pago` update (`:751`), `em.remove(viejaGestionada)` (`:763`). The paid-replacement test asserts the payment moved and the old reserva is gone. | ✅ |
| Direct delete still blocks paid reservas | `ControladorReservas.eliminarReserva` (`:775`) counts pagos and returns `false` before touching the contract or reserva. `eliminarReservaConPagosDevuelveFalseYConservaReservaYPagos` passes. | ✅ |
| Zero-consumed-block guard | `ControladorReservas.crearReserva` increments `consumidos` per mirrored block (`:293`) and throws `IllegalStateException` when zero (`:299`), inside `JpaUtil.enTransaccion`. Both W6 tests assert zero reservas/solicitudes/contracts. | ✅ |
| Cleanup order contract -> solicitud -> reserva | `Main.limpiarDatos` (`:83-85`), `TestUtil.limpiarBase` (`:34-36`), and `eliminarReserva` (`:790-793`) all delete contracts first. `mainLimpiarDatosBorraContratosYSolicitudesAntesQueReservas` asserts source order. | ✅ |

## Spec Compliance Matrix

Legend: ✅ COMPLIANT, ⚠️ PARTIAL, ❌ UNTESTED/FAILING.

### ADDED Requirements

| Requirement | Scenario | Evidence | Result |
|---|---|---|---|
| Effective-reserva surface is absent | no flag on Reserva | `reservaNoDeclaraFlagEfectiva`; `esquemaSinColumnaEfectiva`; production grep | ✅ COMPLIANT |
| Effective-reserva surface is absent | no promotion method | `solicitudNoDeclaraPromoverEfectiva` | ✅ COMPLIANT |
| Ephemeral reserva candidates | one candidate selected | `crearReservaMiraSoloBloquesConsumidos` (`:486`): two queued blocks, one infeasible; asserts one solicitud, one mirrored block, one contract. The creation seam persists one reserva per solicitud. See S1 for the missing direct reserva count. | ✅ COMPLIANT |
| Ephemeral reserva candidates | selection abandoned | `seleccionAbandonadaNoPersisteNada` (`:814`): negative control proves rows are visible, cleanup proves queries return to zero, then two transient candidates are abandoned and zero rows of each type remain. | ✅ COMPLIANT |
| Payment migration on reserva replacement | paid reserva replaced | `reemplazarReservaMigraPagosYConservaSolicitudPerfilYTitular` (`:659`): payment moved, old reserva gone, same contract id re-pointed, solicitud/profile/titular preserved | ✅ COMPLIANT |
| Payment migration on reserva replacement | replacement fails | `reemplazoFallidoAMitadDeTransaccionRevierteReservaPagosYContrato` (`:722`): transaction starts, new reserva is persisted (`nueva.getId() != null`), FK failure is raised, and old reserva/payment/contract link survive. `reemplazoFallidoConservaReservaPagosYContrato` (`:699`) stays as the pre-validation case. | ✅ COMPLIANT |
| Payment migration on reserva replacement | direct delete still blocked | `eliminarReservaConPagosDevuelveFalseYConservaReservaYPagos` (`:764`) | ✅ COMPLIANT |
| Schema reflects final cardinalities | unique per contract side | `contratoRechazaSegundoContratoParaLaMismaSolicitud`; `contratoRechazaSegundoContratoParaLaMismaReserva`; `esquemaConCardinalidadesFinales` | ✅ COMPLIANT |
| Schema reflects final cardinalities | titular nullability | `solicitudSinTitularSePersiste`; `reservaSinTitularEsRechazada`; `esquemaConCardinalidadesFinales` | ✅ COMPLIANT |
| Test-first coverage for every behavior change | RED/GREEN per change | `apply-progress.md` TDD Cycle Evidence table (25 rows, RED/GREEN/safety-net columns) and the green 226-test run. RED logs are self-attested, not replayable (see Limitations). | ✅ COMPLIANT |

### MODIFIED Requirements

| Requirement | Scenario | Evidence | Result |
|---|---|---|---|
| Solicitud persisted | round-trip with two blocks | `solicitudRoundTripConDosBloquesYTitular` | ✅ COMPLIANT |
| Solicitud persisted | no duplicated reserva state | Source inspection: `Solicitud.java:27-39` declares only `id`, `titular`, `rhsolicitadas`, `contratos`; no money, estado, or pagos field. No dedicated runtime assertion (S2). | ✅ COMPLIANT (source inspection) |
| Solicitud persisted | anonymous request | `solicitudSinTitularSePersiste` reloads and asserts `getTitular() == null` | ✅ COMPLIANT |
| Repository access | by id and all | `repositorioSolicitudesFindByIdYFindAll` | ✅ COMPLIANT |
| Repository access | reserva via contract | `repositorioResuelveReservaYContratoPorContrato` (`findReservaDeSolicitud`) | ✅ COMPLIANT |
| Repository access | contract via contract | `repositorioResuelveReservaYContratoPorContrato` (`findContratoDeSolicitud`) | ✅ COMPLIANT |
| Repository access | no contract | `repositorioSinContratoDevuelveNull` | ✅ COMPLIANT |
| ContratoReserva association class | four axes round-trip | `contratoReservaRoundTripConEjesYEnumComoTexto` | ✅ COMPLIANT |
| ContratoReserva association class | no direct link fields | `reservaYSolicitudNoDeclaranReferenciaCruzada` | ✅ COMPLIANT |
| ContratoReserva association class | duplicate solicitud rejected | `contratoRechazaSegundoContratoParaLaMismaSolicitud` | ✅ COMPLIANT |
| ContratoReserva association class | duplicate reserva rejected | `contratoRechazaSegundoContratoParaLaMismaReserva` | ✅ COMPLIANT |
| ContratoReserva association class | every created reserva has one contract | Creation tests assert `contratos.size() == 1` for `crearReservaPara`, `crearReservaAutomatica`, `crearReserva`, and `persistirPropuestas`. The requirement is now correctly scoped to controller creation paths, with `guardar` documented as an escape hatch (W5). | ✅ COMPLIANT |
| Creation paths persist Solicitud + ContratoReserva and link them to the new Reserva | explicit path | `crearReservaParaPersisteSolicitudContratoYReservaEfectiva` | ✅ COMPLIANT |
| Creation paths persist Solicitud + ContratoReserva and link them to the new Reserva | automatic path | `crearReservaAutomaticaMiraCantBebes` | ✅ COMPLIANT |
| Creation paths persist Solicitud + ContratoReserva and link them to the new Reserva | mirror only consumed blocks | `crearReservaMiraSoloBloquesConsumidos` | ✅ COMPLIANT |
| Creation paths persist Solicitud + ContratoReserva and link them to the new Reserva | no consumed blocks | `crearReservaSinBloquesPendientesNoPersisteNada` (`:535`); `crearReservaConTodosLosBloquesSaltadosNoPersisteNada` (`:548`); guard at `ControladorReservas:299` | ✅ COMPLIANT |
| Creation paths persist Solicitud + ContratoReserva and link them to the new Reserva | detached proposals | `persistirPropuestasMiraUnaSolicitadaPorPropuestaConCeroBebes` | ✅ COMPLIANT |
| Creation paths persist Solicitud + ContratoReserva and link them to the new Reserva | back-compat overloads | Legacy overloads are exercised by the creation tests and delegate with `POR_DEFECTO`; profile assertions pass | ✅ COMPLIANT |
| Creation paths persist Solicitud + ContratoReserva and link them to the new Reserva | atomic failure | `creacionFallidaRevierteSinPersistirSolicitudContratoNiReserva`; `creacionFallidaSinHabitacionNoPersisteNada`; W6 guard tests also assert zero rows | ✅ COMPLIANT |
| Both persistence units register new entities | pms-test schema | `persistence.xml` registers `Solicitud`, `RHSolicitada`, `ContratoReserva` in both units; metadata/round-trip tests pass | ✅ COMPLIANT |
| Both persistence units register new entities | draft database recreation | Read-only `INFORMATION_SCHEMA` query against the unchanged `data/pms.mv.db`; see Draft Database Evidence | ✅ COMPLIANT |
| Test cleanup FK order | limpiarBase with contracts | `TestUtil.limpiarBase` deletes contracts first; the full 226-test suite seeds and clears contracts without FK violations | ✅ COMPLIANT |
| Test cleanup FK order | eliminarReserva with a contract | `eliminarReservaBorraContratoYReservaPeroConservaSolicitud` | ✅ COMPLIANT |
| Test cleanup FK order | Main cleanup order | `mainLimpiarDatosBorraContratosYSolicitudesAntesQueReservas` + `Main.java:83-85` | ✅ COMPLIANT |

**Compliance summary**: 34/34 scenarios compliant, 0 partial, 0 failing, 0 untested. 11/11 requirements fully evidenced. The two previous partial requirements (`Ephemeral reserva candidates` and `Payment migration on reserva replacement`) are now fully covered; `Test-first coverage` is satisfied by the persisted TDD table plus the green suite.

## Correctness (Static Evidence)

| Requirement | Status | Notes |
|---|---|---|
| Effective surface removed | ✅ Implemented | No production `efectiva` surface remains; reflection tests run against compiled classes. |
| Contract is the only link | ✅ Implemented | `ContratoReserva` holds both FKs; neither entity declares a field of the other type. |
| Per-side cardinality | ✅ Implemented | Both FKs `optional=false`/`nullable=false`; two single-column unique constraints. |
| Titular nullability split | ✅ Implemented | `Solicitud.titular` nullable; `Reserva.titular` NOT NULL at annotation and DB level. |
| Atomic creation | ✅ Implemented | All four paths run inside `JpaUtil.enTransaccion`; contract is created in the same transaction as reserva and solicitud. |
| Zero-block rejection | ✅ Implemented | `crearReserva` throws inside the transaction before persistence when no block is consumed. |
| Payment migration before delete | ✅ Implemented | JPQL bulk update precedes `em.remove` of the old reserva. |
| Direct paid delete guard | ✅ Implemented | `eliminarReserva` returns false before deleting contracts or the reserva. |
| Cleanup order | ✅ Implemented | `Main`, `TestUtil`, and `eliminarReserva` delete contracts first. |

## Coherence (Design)

| Decision | Followed? | Notes |
|---|---|---|
| D1: Both contract FKs NOT NULL + single UNIQUE, no pointer | ✅ Yes | Annotation and both schemas match. |
| D2: SQL at-most-one + single creation seam; per-path count guard | ⚠️ Partial | Single seam exists; no per-path contract count guard (accepted deviation). The corrective pass added a zero-consumed-block guard, which closes the empty-solicitud gap. |
| D3: `reemplazarReserva` persists new, re-points contract, migrates pagos, reloads/removes old | ✅ Yes | Code order matches; paid-replacement and rollback tests assert behavior. |
| D4: Contract -> reserva/solicitud cleanup; keep `eliminarReserva` guard; add `eliminarContratos()` | ✅ Yes | `Main`, `TestUtil`, `eliminarReserva`, and the repository helper match. |
| D5: Rename lookups to `findReservaDeSolicitud`/`findContratoDeSolicitud`, one join fetch, null if none | ✅ Yes | Both queries use a single join fetch and no flag predicate. |
| D6: Recreate draft DB; `Reserva.titular` NOT NULL; `Solicitud.titular` nullable | ✅ Yes | File DB metadata confirms the nullability split; the DB was recreated in the first apply and was not rebuilt in this pass. |
| D7: No `equals`/`hashCode` override; reflection guard | ✅ Yes | `reservaNoSobrescribeEqualsNiHashCode` passes. |
| D8: U1-U5 RED-first; U6 guards; U7 migration | ⚠️ Partial | Guards and migration present; the TDD table is now persisted, but the RED logs are self-attested and not replayable. |

## TDD Compliance (Strict TDD active)

| Check | Result | Details |
|---|---|---|
| TDD Evidence reported | ✅ | `apply-progress.md` contains a 25-row TDD Cycle Evidence table with RED, GREEN, triangulation, and safety-net columns. W4 is resolved. |
| All tasks have tests | ✅ 25/25 | Each Phase 1-5 task maps to a test or a source-inspection assertion (Main cleanup). |
| RED confirmed (tests exist) | ⚠️ Partial | The table records RED batches and exact failure messages (e.g., 2 W6 failures before the guard, 1+1 Phase 3 errors), but the logs are not persisted and cannot be replayed from the uncommitted tree. |
| GREEN confirmed (tests pass) | ✅ 25/25 | `mvn -B test`: 226 passed, 0 failed. |
| Triangulation adequate | ✅ | C1 now proves a real mid-transaction rollback, and W3 has a negative control. Residual S1 (direct reserva count) and S9 (empty-queue precondition) remain as suggestions. |
| Safety Net for modified files | ⚠️ Partial | The 209-test baseline is reported by the apply; it was not independently re-run before the change in this verification. |

**TDD Compliance**: 4/6 checks fully passed; 2 partial (evidence replayability and safety-net provenance). No strict-TDD blocker remains. Persist the RED logs if archive requires replayable evidence.

### Test Layer Distribution

| Layer | Tests | Files | Tools |
|---|---|---:|---|
| Unit | 60 | 12 | JUnit 5 (pure logic/UI model) |
| Integration | 166 | 13 | JUnit 5 + in-memory H2 (`pms-test`) |
| E2E | 0 | 0 | not configured |
| **Total** | **226** | **25** | |

The classification is an inspection estimate: integration tests are those that open `JpaUtil.enTransaccion` / the `pms-test` unit; unit tests are pure logic and UI model tests. The three new tests are integration tests.

### Changed File Coverage

Coverage analysis skipped — no coverage tool detected (`coverage.available: false`).

### Assertion Quality

| File | Line | Assertion / test | Issue | Severity |
|---|---|---|---|---|
| `src/test/java/com/pms/SolicitudContratoReservaTest.java` | 486-509 | `crearReservaMiraSoloBloquesConsumidos` | Asserts one solicitud, one block, and one contract but not `assertEquals(1, reservas.size())` or that the skipped candidate left no reserva. | SUGGESTION |
| `src/test/java/com/pms/SolicitudContratoReservaTest.java` | 535-545 | `crearReservaSinBloquesPendientesNoPersisteNada` | Does not call `limpiarBloquesPendientes()` first, so the "no pending blocks" GIVEN can depend on the singleton queue state left by an earlier test. The all-skipped test covers the same guard independently. | SUGGESTION |
| `src/test/java/com/pms/SolicitudContratoReservaTest.java` | 722-761 | `reemplazoFallidoAMitadDeTransaccionRevierteReservaPagosYContrato` | Forces the failure immediately after persisting the new reserva; the contract re-point and bulk payment update are not independently forced to roll back. The scenario itself is covered. | SUGGESTION |

**Assertion quality**: 0 CRITICAL, 0 WARNING, 3 SUGGESTION. No tautologies or ghost loops found in the changed tests. The previous CRITICAL (pre-transaction-only failure) and WARNING (vacuous abandoned selection) are resolved.

### Quality Metrics

**Linter**: ➖ Not available (no linter configured).
**Type Checker**: ✅ `javac` via `mvn -B -DskipTests package` (release 21) — build passed.

## Draft Database Evidence

`data/pms.mv.db` is unchanged by this pass: length 65,536 bytes, `CreationTime` 2026-10-17 05:16:17, `LastWriteTime` 2026-10-17 05:19:58, SHA-256 `a4d771b674995d54bb4c95eccc408d312959b303dd39993334d36394814496ab` before and after the read-only query. The corrective pass did not rebuild it, as documented in `apply-progress.md`.

Read-only verification command (H2 2.2.224 `RunScript`, `ACCESS_MODE_DATA=r`):

```text
java -cp <h2-2.2.224.jar> org.h2.tools.RunScript \
  -url "jdbc:h2:file:./data/pms;ACCESS_MODE_DATA=r" -user sa \
  -script <verify-metadata.sql> -showResults
```

Raw result:

```text
CONTRATO_RESERVA RESERVA_ID NO
CONTRATO_RESERVA SOLICITUD_ID NO
RESERVA TITULAR_ID NO
SOLICITUD TITULAR_ID YES
CONSTRAINT_FF PRIMARY KEY ID 1
FKIY71VQBOT4XBKO911LOVF1KTO FOREIGN KEY RESERVA_ID 1
FKRP0431QTH3IEWAM77LXHFYMLI FOREIGN KEY SOLICITUD_ID 1
UK3H4HLJ46AQ62M7Y9YWXH05U6R UNIQUE RESERVA_ID 1
UKR5Y7FH6U0A6RP8BRA4XVBFFU6 UNIQUE SOLICITUD_ID 1
EFECTIVA_COLUMNS 0
RESERVA columns (9): ID, ACTIVA, ESTADO, LATECHECKOUT, MONEDA, MONTOPAGADO, MONTOSENIA, MONTOTOTAL, TITULAR_ID
RUNSCRIPT_EXIT=0
```

The DB file hash was identical before and after the query. No destructive DB operation was run in this re-verification. An initial metadata query referenced the non-existent `COLUMN_LIST` column and failed; it was retried with the `KEY_COLUMN_USAGE` join shown above. That failed attempt did not change the `pms.mv.db` hash, but it is another trace-hygiene item under S5.

## Findings

### CRITICAL

None. C1 is resolved: `reemplazoFallidoAMitadDeTransaccionRevierteReservaPagosYContrato` starts a real transaction, persists the new reserva, fails on an FK inside it, and asserts the old reserva, its payment, and the contract link are intact. The implementation transaction was not weakened.

### WARNING

**W5 — `RepositorioReservas.guardar` remains a public contract-less escape hatch (documented, accepted).**
Evidence: `RepositorioReservas.guardar` is public and does not create a `ContratoReserva`; its Javadoc calls it an "escape hatch". The delta requirement now scopes exactly-one to "each reserva created through the controller creation paths" and documents the escape hatch, so the spec no longer claims an unconditional invariant. Tests use it for fixtures (`contratoRechazaSegundoContratoParaLaMismaSolicitud`, `repositorioResuelveReservaYContratoPorContrato`, `seleccionAbandonadaNoPersisteNada`). The residual risk is a future production caller bypassing the seam. This is an accepted design decision, not a spec contradiction.
Fix (optional): narrow `guardar` to package-private or add a test-only fixture helper.

### SUGGESTION

**S1 — Add a direct reserva-count assertion to `crearReservaMiraSoloBloquesConsumidos`.**
Add `assertEquals(1, new RepositorioReservas().findAll().size())` so the "exactly one reserva / no other candidate" part of the scenario is asserted directly rather than inferred from solicitud/contract counts.

**S2 — Add a reflection assertion for the "no duplicated reserva state" scenario.**
Source inspection confirms `Solicitud` has no money/estado/pagos field, but no test asserts it. Extend `reservaYSolicitudNoDeclaranReferenciaCruzada` (or add a sibling test) to reject fields named `monto*`, `estado`, or `pagos`.

**S3 — Update the cached test count after archive.**
`openspec/config.yaml:26` still says `verified_test_count: 209`; the current suite is 226. Update it when archiving so the next preflight does not use a stale baseline.

**S4 — Remove stale "efectiva" terminology from test names and comments.**
`crearReservaParaPersisteSolicitudContratoYReservaEfectiva` (`:421`) and the class Javadoc (`:48-51`, "effective-reservation invariant") still use the removed vocabulary. Rename/update them to avoid misleading future readers.

**S5 — Clean up stale draft-DB trace evidence.**
`data/pms.trace.db` still records an earlier failed `monto_total` query; the actual column is `MONTOTOTAL`. Delete the stale trace or re-record it with the correct columns. This is evidence hygiene, not a schema defect.

**S6 — Use one `RepositorioSolicitudes` instance in `Main.limpiarDatos`.**
`Main.java:83-84` constructs two separate instances for `eliminarContratos()` and `eliminarTodas()`. A single local is clearer.

**S7 — Force a later failure point in the C1 test (optional).**
The current test fails after `em.persist(nueva)`/flush but before `contrato.setReserva` and the bulk payment update. A failure injected after those steps would prove their rollback independently. The required scenario is already covered.

**S8 — Run a clean-room package build when a fresh jar matters.**
`mvn -B -DskipTests package` succeeded but did not regenerate `target/pms-1.0.0.jar` because classes and jar were up to date. Use `mvn -B -DskipTests clean package` if the artifact itself must be rebuilt for the archive.

**S9 — Reset the pending-block queue in the empty-input W6 test.**
Call `controlador().limpiarBloquesPendientes()` at the start of `crearReservaSinBloquesPendientesNoPersisteNada` so the GIVEN "no pending blocks" is explicit and order-independent.

## Deviations Evaluated

1. **Seam ordering (`persistirReservaYSolicitud` persists reserva before solicitud, with the contract cascaded from the solicitud).**
   Acceptable. The contract's `reserva_id` FK requires the reserva row first; the solicitud persist then cascades the contract after the solicitud row. This satisfies the spec's row-level order and matches design D3.

2. **Phase 2/3 coupling (lookups and flag-write removal landed together rather than as separate seam-then-replace steps).**
   Acceptable. Removing `Reserva.efectiva` is compile-coupled: the old lookups filtered on `r.efectiva` and the controller wrote `setEfectiva(true)`. The final code matches design D5 and the tests cover the final behavior.

3. **Direct `java -cp` instead of `mvn exec:java` for the draft-DB rebuild.**
   Acceptable. `pom.xml` pins `exec-maven-plugin` `<mainClass>com.pms.ui.PmsApp</mainClass>`, so a direct classpath invocation of `com.pms.Main` is the correct workaround. The recreated DB evidence from the first apply is consistent and unchanged.

4. **Contract reuse on replacement.**
   Resolved by W2: the implementation re-points the existing contract (same id, same solicitud, same relaxation profile), and the delta scenario, design U4 row, and task 3.4 now say so. No functional deviation.

5. **No extra per-path contract count guard.**
   Acceptable. The per-side `UNIQUE` enforces at most one contract per solicitud/reserva, and the single `persistirReservaYSolicitud` seam creates exactly one for every controller path. W6 added the missing zero-block guard; W5 documents the repository escape hatch.

6. **Zero-consumed-block guard added in the corrective pass.**
   Acceptable and required by the `1..* RHSolicitada` wording. It throws inside the existing transaction without weakening atomicity, and the new tests plus the full suite pass.

## Evidence

### Commands and raw results

```text
> mvn -B test
exit code: 0
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.264 s -- in com.pms.SolicitudContratoReservaTest
[INFO] Tests run: 226, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  18.474 s
SHA-256(stdout+stderr): 84a57ddf12cd03b9f441789fb8bc0bc3c81e48f694002f759b6a4683cc085e8f
```

```text
> mvn -B -DskipTests package
exit code: 0
[INFO] Nothing to compile - all classes are up to date.
[INFO] Tests are skipped.
[INFO] BUILD SUCCESS
[INFO] Total time:  2.863 s
SHA-256(stdout+stderr): 4448ecdcfe5cead1e22605970ab68aef09124e9e45e6a61b78e69d9ab3dadfeb
```

```text
> git status --porcelain
 M src/main/java/com/pms/Main.java
 M src/main/java/com/pms/controllers/ControladorReservas.java
 M src/main/java/com/pms/entities/ContratoReserva.java
 M src/main/java/com/pms/entities/Reserva.java
 M src/main/java/com/pms/entities/Solicitud.java
 M src/main/java/com/pms/persistence/RepositorioReservas.java
 M src/main/java/com/pms/persistence/RepositorioSolicitudes.java
 M src/test/java/com/pms/MatrizOcupacionTest.java
 M src/test/java/com/pms/SolicitudContratoReservaTest.java
 M src/test/java/com/pms/TestUtil.java
?? openspec/
```

The changed files match the design's file-change table plus the test-fixture adaptations required by task 1.7. This re-verification did not modify `src/`, tests, or other SDD artifacts; only this report was written.

### C1 execution path

```text
reemplazoFallidoAMitadDeTransaccionRevierteReservaPagosYContrato (:722)
  -> reemplazarReserva(vieja.getId(), nueva)            (:714)
  -> JpaUtil.enTransaccion starts                      (JpaUtil:72)
  -> pre-guards pass (titular present, no pagos)
  -> em.persist(nueva)                                 (ControladorReservas:743) -> id assigned
  -> em.flush()                                        -> FK1MFLCHHB8V0U6VBUF4LHTHYF violation
  -> JpaUtil rollback                                  (JpaUtil:77-79)
  -> asserts: nueva.getId() != null; one reserva; old reserva present;
             one payment on old reserva; same contract id; contract points to old reserva
```

The old pre-validation test `reemplazoFallidoConservaReservaPagosYContrato` (`:699`) remains separate and still fails at the null-titular guard before the transaction.

### Test class breakdown

`SolicitudContratoReservaTest` 38; `ControladorReservasTest` 28; `ControladorReservasMovimientoTest` 10; `ManejadorBusquedaHuecosTest` 10; `CalendarioOcupacionModelTest` 15; `CompatibilidadMovimientoTest` 11; `EstadoArrastreTest` 11; `ControladorHabitacionesTest` 9; `LateCheckoutLimpiezaTest` 9; `MatrizOcupacionTest` 9; `SemillaDemoTest` 9; `GeometriaBarraTest` 7; `PanelPropuestaEstadiaTest` 7; `ServicioPropuestasDemoTest` 7; `ManejadorAtributosHabsTest` 6; `FechasTest` 5; `HabitacionTest` 5; `InteraccionConsolaTest` 5; `RepositorioHabitacionesTest` 5; `MensajesMovimientoTest` 3; `RepositorioPersonasTest` 2; `SesionUsuarioTest` 2; `MotivoRechazoTest` 2; `RepositorioTest` 1. Aggregate: 226 across 25 classes.

## Limitations

- **RED/GREEN history is self-attested.** `apply-progress.md` records RED batches and failure messages, but the logs are not persisted as files and all changes are uncommitted, so RED cannot be independently replayed. The green suite is independently verified.
- **The draft H2 file is gitignored and was not rebuilt in this pass.** I verified its schema with a read-only connection and confirmed the file hash did not change: `a4d771b674995d54bb4c95eccc408d312959b303dd39993334d36394814496ab`. Row data was not inspected; no requirement depends on row content. The recreate evidence remains from the first apply (`CreationTime` 05:16:17, `LastWriteTime` 05:19:58).
- **The apply's own H2 Shell output was not persisted as a file.** `data/pms.trace.db` records a stale failed `monto_total` query from the first apply. I re-ran the metadata queries independently; the final schema is correct.
- **No coverage tool is configured.** Line/branch coverage for the changed files is unavailable. The package build type-checks the code with `javac` release 21.
- **The package command did not regenerate the jar.** `mvn -B -DskipTests package` succeeded with "Nothing to compile" and the jar kept its prior timestamp; see S8.
- **C1 forces the failure immediately after the new reserva is persisted.** The contract re-point and bulk payment update follow that point and are not independently forced to roll back; the scenario's required rollback behavior is still proven.
- **The abandoned-selection scenario has no production selection seam.** The test uses a negative control plus transient candidates; this is the accepted fix for W3, not an end-to-end selection workflow.
- **The ledger deviation from `apply-progress.md` is not a code defect.** The native attempt ledger was contradictory (`begin` exhausted, `reset` non-terminal, `finish` no active attempt), and the maintainer explicitly authorized the corrective pass outside the ledger. No attempt ID or ledger transition exists; `apply-progress.md` is the authoritative record. This limitation is recorded for traceability only.

## Verdict

**PASS WITH WARNINGS.**

The previous FAIL is superseded. C1 is resolved by a real mid-transaction rollback test that persists the new reserva, fails on an FK, and proves the old reserva, its payment, and the contract link survive. W1, W2, W3, W4, and W6 are resolved; W5's spec contradiction is resolved while the documented public escape hatch remains as an accepted residual risk. The delta now has 11/11 requirements and 34/34 scenarios covered, with `mvn -B test` green at 226 tests and `mvn -B -DskipTests package` green. The remaining items are one documented warning (W5) and suggestions about evidence hygiene, stale test names, the cached test count, the empty-queue precondition, and a clean-room jar build. The change is archive-ready under Strict TDD with those documented warnings.
