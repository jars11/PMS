# solicitud-contrato-reserva Specification

## Purpose

Persist the requested set that mirrors a reserva (`Solicitud` + `RHSolicitada`) and the `ContratoReserva` relaxation contract, which is the single persisted link that identifies the selected reserva of a solicitud. Money, estado, and payments remain on `Reserva`.

## Requirements

### Requirement: Solicitud persisted

`Solicitud` SHALL be a persisted entity holding the requested titular, which MAY be null for anonymous requests, and `1..* RHSolicitada`. It SHALL mirror only the request and MUST NOT duplicate `Reserva` money, estado, or payments.
(Previously: the titular was required.)

#### Scenario: round-trip with two blocks

- GIVEN a `Solicitud` with two `RHSolicitada` blocks
- WHEN it is persisted and reloaded
- THEN it holds both blocks
- AND it holds its titular

#### Scenario: no duplicated reserva state

- GIVEN a stored `Solicitud`
- WHEN it is re-read
- THEN no `Reserva` money or estado field is duplicated onto it

#### Scenario: anonymous request

- GIVEN a `Solicitud` constructed without a titular
- WHEN it is persisted and reloaded
- THEN it is accepted
- AND `getTitular()` is null

### Requirement: RHSolicitada persisted, detached from RHPropuesta

`RHSolicitada` SHALL be a persisted entity with dates, wanted attributes, and `cantMatrimonios`/`cantIndividuales`/`cantBebes`/`cantCunas`. It MUST NOT hold any association to `RHPropuesta`.

#### Scenario: round-trip of request data

- GIVEN an `RHSolicitada` with attributes and counts
- WHEN it is persisted and reloaded
- THEN dates, attributes, and counts round-trip

#### Scenario: no RHPropuesta reference

- GIVEN the `RHSolicitada` model
- WHEN it is inspected
- THEN it exposes no `RHPropuesta` reference

### Requirement: Repository access

A repository SHALL persist and look up `Solicitud` and `ContratoReserva`, and SHALL resolve a solicitud's reserva and contract exclusively through `ContratoReserva`. No lookup SHALL filter or return by an effectiveness flag. A solicitud with no contract SHALL resolve to no reserva and no contract.
(Previously: effective lookups filtered `r.efectiva = true`.)

#### Scenario: by id and all

- GIVEN a persisted `Solicitud`
- WHEN `findById`/`findAll` run
- THEN the entity is returned

#### Scenario: reserva via contract

- GIVEN a solicitud linked by contract to reserva r
- WHEN the reserva-by-solicitud lookup runs
- THEN r is returned

#### Scenario: contract via contract

- GIVEN a solicitud linked by contract c
- WHEN the contract lookup runs
- THEN c is returned

#### Scenario: no contract

- GIVEN a solicitud without a contract
- WHEN either lookup runs
- THEN null is returned

### Requirement: ContratoReserva association class

`ContratoReserva` SHALL be the single persisted link between `Solicitud` and `Reserva` and SHALL carry `fechasRelajadas`, `atributosRelajados`, `colchonesExtra`, and `nivelCapacidad`. Neither `Solicitud` nor `Reserva` SHALL hold a direct reference to the other. `solicitud_id` and `reserva_id` SHALL each be NOT NULL and UNIQUE, replacing the `(solicitud_id, reserva_id)` pair constraint. Each solicitud SHALL have at most one contract, and each reserva created through the controller creation paths SHALL have exactly one contract. `RepositorioReservas.guardar` is a documented lower-level escape hatch: it MAY persist a contract-less reserva for fixtures and legacy callers, and it is outside the controller creation invariant.
(Previously: unique only on the pair, so many contracts per solicitud and per reserva were possible.)

#### Scenario: four axes round-trip

- GIVEN a contract with all four axes set
- WHEN it is persisted and reloaded
- THEN all four round-trip
- AND `nivelCapacidad` is stored as its enum name

#### Scenario: no direct link fields

- GIVEN `Solicitud` and `Reserva`
- WHEN their declared fields are inspected
- THEN neither declares a field of the other entity type

#### Scenario: duplicate solicitud rejected

- GIVEN a contract for solicitud s
- WHEN a second contract for s and another reserva is inserted
- THEN the unique constraint rejects it

#### Scenario: duplicate reserva rejected

- GIVEN a contract for reserva r
- WHEN a second contract for r and another solicitud is inserted
- THEN the unique constraint rejects it

#### Scenario: every created reserva has one contract

- GIVEN a reserva created by any controller creation path
- WHEN its contracts are counted
- THEN the count is exactly one

### Requirement: NivelCapacidad mapping

`NivelCapacidad` SHALL map `DISTRIBUCION_CAMAS`/`CAPACIDAD_POR_HABITACION`/`CAPACIDAD_TOTAL` to `relajacionDistribucion` 0/1/2, with a `deRelajacion(int)` lookup that rejects unknown values.

#### Scenario: valid mapping

- GIVEN 0, 1, or 2
- WHEN `deRelajacion` runs
- THEN the matching level is returned

#### Scenario: invalid value

- GIVEN an out-of-range value
- WHEN `deRelajacion` runs
- THEN it throws `IllegalArgumentException`

### Requirement: PerfilRelajacion default

`PerfilRelajacion` SHALL be a value record `(fechasRelajadas, atributosRelajados, colchonesExtra, nivelCapacidad)` whose `POR_DEFECTO` is the strict profile `(false, false, false, DISTRIBUCION_CAMAS)`.

#### Scenario: strict default

- GIVEN `POR_DEFECTO`
- WHEN it is read
- THEN its four fields equal the locked strict profile

#### Scenario: value semantics

- GIVEN two records with equal fields
- WHEN they are compared
- THEN they are equal

### Requirement: Creation paths persist Solicitud + ContratoReserva and link them to the new Reserva

Each of `crearReserva`, `crearReservaPara`, `crearReservaAutomatica`, and `persistirPropuestas` SHALL, inside its single creation transaction, persist exactly one `Reserva`, one `Solicitud` (with its mirrored `RHSolicitada` blocks), and one `ContratoReserva` carrying the path's relaxation profile. Rows SHALL be created in the order titular -> reserva -> solicitud -> contract. No path SHALL set an effectiveness flag. `PerfilRelajacion` overloads SHALL be kept; legacy signatures SHALL remain and delegate with `POR_DEFECTO`.
(Previously: each path also called `r.setEfectiva(true)` on the new reserva.)

#### Scenario: explicit path

- GIVEN valid arguments
- WHEN `crearReservaPara` runs
- THEN one solicitud with one mirrored `RHSolicitada` persists
- AND one `POR_DEFECTO` contract links the new reserva
- AND no effectiveness flag is written

#### Scenario: automatic path

- GIVEN valid arguments
- WHEN `crearReservaAutomatica` runs
- THEN one solicitud/block carrying `cantBebes` persists
- AND one `POR_DEFECTO` contract links the new reserva
- AND no effectiveness flag is written

#### Scenario: mirror only consumed blocks

- GIVEN pending blocks where some are skipped by the existing filters
- WHEN `crearReserva` runs
- THEN the solicitud has one `RHSolicitada` per consumed block
- AND skipped blocks are not mirrored

#### Scenario: no consumed blocks

- GIVEN no pending blocks, or pending blocks that all fail the existing filters
- WHEN `crearReserva` runs
- THEN it throws `IllegalStateException`
- AND no reserva, solicitud, or contract persists

#### Scenario: detached proposals

- GIVEN detached `RHPropuesta`s
- WHEN `persistirPropuestas` runs
- THEN one `RHSolicitada` per proposal persists with `cantBebes = 0`
- AND one `POR_DEFECTO` contract links the new reserva
- AND no effectiveness flag is written

#### Scenario: back-compat overloads

- GIVEN existing callers using the legacy signatures
- WHEN they run
- THEN behavior is unchanged
- AND the profile is `POR_DEFECTO`

#### Scenario: atomic failure

- GIVEN a creation that throws mid-transaction
- WHEN it rolls back
- THEN no solicitud, contract, or reserva persists

### Requirement: Both persistence units register new entities

The three new `@Entity` classes SHALL be registered in both the `pms` and `pms-test` units. Because `hbm2ddl.auto=update` cannot drop `RESERVA.EFECTIVA` or the old pair unique constraint, the draft `pms` file database SHALL be recreated instead of updated.
(Previously: the populated file was extended in place with `update`, relying on defaulted columns.)

#### Scenario: pms-test schema

- GIVEN the `pms-test` unit
- WHEN the schema is built
- THEN `solicitud`, `rhsolicitada`, and `contrato_reserva` map and round-trip

#### Scenario: draft database recreation

- GIVEN the old `data/pms.mv.db`
- WHEN it is recreated
- THEN no `EFECTIVA` column and no pair unique constraint remain
- AND the new cardinality constraints are present

### Requirement: Test cleanup FK order

Cleanup SHALL delete `ContratoReserva` rows before reservas and solicitudes. `ControladorReservas.eliminarReserva(id)` SHALL delete the reserva's contracts before removing the reserva. `TestUtil.limpiarBase()` and `Main.limpiarDatos()` SHALL follow the order contract -> reserva/solicitud without FK violations.
(Previously: solicitudes were deleted before reservas and contracts were removed only through cascade.)

#### Scenario: limpiarBase with contracts

- GIVEN seeded solicitudes, contracts, and reservas
- WHEN `limpiarBase()` runs
- THEN all rows clear with no FK violation

#### Scenario: eliminarReserva with a contract

- GIVEN a reserva with a contract and no payments
- WHEN `eliminarReserva(id)` runs
- THEN the contract and reserva are deleted
- AND the solicitud survives

#### Scenario: Main cleanup order

- GIVEN `Main.limpiarDatos()`
- WHEN its source is inspected
- THEN contracts are deleted before reservas
- AND no FK violation can occur

### Requirement: Effective-reserva surface is absent

The effective-reserva surface MUST be structurally absent. `Reserva` MUST NOT declare an `efectiva` field, `isEfectiva()`, or `setEfectiva(boolean)`; `Solicitud` MUST NOT declare `promoverEfectiva(Reserva)`; and the `RESERVA` table MUST NOT contain an `EFECTIVA` column.

#### Scenario: no flag on Reserva

- GIVEN the compiled `Reserva` class
- WHEN it is inspected by reflection and H2 metadata
- THEN `getDeclaredField("efectiva")` throws `NoSuchFieldException`
- AND `isEfectiva()` and `setEfectiva(boolean)` throw `NoSuchMethodException`
- AND `RESERVA` has no `EFECTIVA` column

#### Scenario: no promotion method

- GIVEN the compiled `Solicitud` class
- WHEN `getMethod("promoverEfectiva", Reserva.class)` runs
- THEN it throws `NoSuchMethodException`

### Requirement: Ephemeral reserva candidates

Reserva candidates SHALL be built in memory and offered for selection. Only the selected candidate SHALL be persisted; losing candidates MUST NOT be written to any table.

#### Scenario: one candidate selected

- GIVEN several in-memory candidates
- WHEN one is selected
- THEN exactly one reserva, one solicitud, and one contract persist
- AND no other candidate is stored

#### Scenario: selection abandoned

- GIVEN in-memory candidates
- WHEN selection is abandoned
- THEN zero reservas, solicitudes, or contracts persist

### Requirement: Payment migration on reserva replacement

When an improvement or replacement flow replaces a solicitud's reserva, its `Pago` records SHALL be migrated to the new reserva before the old reserva is deleted. The existing "a reserva with payments cannot be deleted" guard MUST NOT block this flow, and SHALL keep blocking every other deletion path.

#### Scenario: paid reserva replaced

- GIVEN a reserva with payments and a selected improved reserva
- WHEN the replacement completes
- THEN every payment references the new reserva
- AND the old reserva is deleted and its contract row is re-pointed to the new reserva
- AND no payment-block rejection occurs

#### Scenario: replacement fails

- GIVEN a reserva with payments
- WHEN the replacement transaction rolls back
- THEN the old reserva keeps its payments
- AND no new reserva or contract persists

#### Scenario: direct delete still blocked

- GIVEN a paid reserva
- WHEN `eliminarReserva` is called outside a replacement flow
- THEN it returns false
- AND the reserva and its payments remain

### Requirement: Schema reflects final cardinalities

The schema SHALL enforce `contrato_reserva.solicitud_id` NOT NULL and UNIQUE, `contrato_reserva.reserva_id` NOT NULL and UNIQUE, `solicitud.titular_id` nullable, and `reserva.titular_id` NOT NULL. Automated schema assertions SHALL verify these constraints and SHALL verify that the old `(solicitud_id, reserva_id)` unique constraint is absent.

#### Scenario: unique per contract side

- GIVEN a contract for a solicitud
- WHEN a second contract for the same solicitud is inserted
- THEN the unique constraint rejects it
- AND the same rejection applies to a second contract for the same reserva

#### Scenario: titular nullability

- GIVEN a solicitud without a titular
- WHEN it is persisted
- THEN it is accepted
- AND a reserva without a titular is rejected

### Requirement: Test-first coverage for every behavior change

Each behavior change in this delta SHALL have a RED test that fails before the change and a GREEN test that passes after it.

#### Scenario: RED/GREEN per change

- GIVEN the behavior changes in this delta
- WHEN they are implemented
- THEN each has a test that failed before and passes after
- AND `mvn -B test` is green
