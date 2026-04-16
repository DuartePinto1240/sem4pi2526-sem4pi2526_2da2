# Acceptance Criteria — Sprint 1

---

## US001 — Technical constraints

**As** Project Manager, **I want** the team to follow the technical constraints and concerns
of the project.

### Acceptance Criteria
- The solution is implemented in Java as the main programming language.
- The project uses Maven as the build automation tool.
- The project structure supports ANTLR integration.
- The system supports data persistence either in-memory or via a relational database (RDBMS).
- Test coverage of Java controller and domain packages must not fall below 90%.
- Any commit must leave the system in a valid state (compiles and passes all tests).

---

## US002 — Project repository

**As** Project Manager, **I want** the team to use the defined project repository in GitHub
and setup a GitHub tool for project management.

### Acceptance Criteria
- The team uses the GitHub repository created in the context of the GitHub Classroom.
- A GitHub project management tool (e.g. GitHub Projects) is configured and in use.
- The repository is updated regularly (e.g. daily commits with meaningful messages).
- Failure to commit regularly will negatively affect the team's assessment.

---

## US003 — Project structure

**As** Project Manager, **I want** the team to configure the project structure to
facilitate and accelerate the development of upcoming user stories.

### Acceptance Criteria
- The project structure supports the envisioned architecture (e.g. layered/DDD structure).
- The structure includes support for adopted technologies, namely ANTLR.
- The module layout is consistent and agreed upon by the team before development begins.
- The structure compiles successfully from a clean checkout.

---

## US004 — Continuous integration server

**As** Project Manager, **I want** the team to setup a continuous integration server.

### Acceptance Criteria
- GitHub Actions/Workflows are configured for the repository.
- The CI pipeline runs automatically on every push to the main branch.
- The pipeline includes compilation and execution of all automated tests.
- Build results and metrics are published as part of the nightly build.
- A failing build is visible and reported to the team.

---

## US005 — Automated deployment

**As** Project Manager, **I want** the team to add to the project the necessary scripts so
that build, execution, and deployment can be executed effortlessly on a Unix compatible
machine.

### Acceptance Criteria
- Scripts are provided for all major tasks: build, test, run, and deploy.
- Scripts are compatible with Unix/Linux systems.
- A `readme.md` file exists in the root folder explaining how to build, deploy, and execute
  the solution.
- All scripts are version-controlled in the repository.

---

## US010 — Domain model

**As** Project Manager, **I want** the team to elaborate a Domain Model using DDD.

### Acceptance Criteria
- A domain model is produced using DDD concepts and notation.
- All classes are classified as Aggregate Root, Entity, or Value Object.
- Aggregate boundaries are clearly defined and justified.
- The model covers all relevant domain concepts from the system specification.
- The model is documented in the repository in the `docs` folder in Markdown format.
- Diagrams are produced using PlantUML and included in vector format (PNG).

---

# Acceptance Criteria — Aggregates (US011)

---

## Aggregate: User

**Invariant:** A user can only be enabled if their security clearance is active and not
expired.

### Acceptance Criteria
- A user cannot be enabled if their `SecurityClearance` has an expiry date in the past.
- A user cannot be enabled if no `SecurityClearance` is associated.
- A `SkillsAssessment` can be added to a user at any time; the most recent one is used for
  validity checks.
- The system rejects any attempt to enable a user with an expired clearance with a
  meaningful error message.
- All state changes to `SecurityClearance` and `SkillsAssessment` are made through the
  `User` aggregate root.

---

## Aggregate: Manufacturer

**Invariant:** A manufacturer name must be unique across the system.

### Acceptance Criteria
- The system rejects the registration of a manufacturer with a name that already exists.
- A manufacturer stores at minimum a name and a country.
- Manufacturer data can be referenced by `EngineModel` and `AircraftModel` aggregates but
  cannot be modified through them.
- The system returns a meaningful error message when a duplicate manufacturer name is
  submitted.

---

## Aggregate: Engine model

**Invariant:** The thrust specification must be complete (both sea-level and cruise values
present) and TSFC must be strictly positive.

### Acceptance Criteria
- The system rejects the creation of an engine model if the sea-level thrust value is
  missing or zero.
- The system rejects the creation of an engine model if the cruise thrust value is missing
  or zero.
- The system rejects the creation of an engine model if TSFC is zero or negative.
- The combination of name and manufacturer must be unique.
- The system returns a meaningful error message identifying which field failed validation.

---

## Aggregate: Aircraft model

**Invariant:** An aircraft model must have at least one certified engine model; the same
engine model cannot be certified twice; the engine type must be compatible with the aircraft
model.

### Acceptance Criteria
- The system rejects the addition of an engine model that is already certified for the same
  aircraft model.
- The system rejects the addition of an engine model whose type is incompatible with the
  aircraft model's motorization.
- The system rejects the removal of a certified engine model if it is the last one
  associated with the aircraft model.
- The system rejects the removal of a certified engine model if there are existing aircraft
  using that engine configuration.
- The combination of model name and manufacturer must be unique.
- All physical flight characteristics (Cl, Cd, MTOW, MZFW, wing area, cruise speed) must
  be provided and positive.

---

## Aggregate: Aircraft

**Invariant:** The total number of seats in the cabin configuration must not exceed the
maximum capacity defined by the aircraft model.

### Acceptance Criteria
- The system rejects the registration of an aircraft if the total number of seats across all
  classes exceeds the model's maximum capacity.
- The aircraft registration number must be unique across the system.
- A decommissioned aircraft cannot be assigned to new flights.
- The cabin configuration can be updated, but the updated total must still respect the model
  capacity.
- The operational status transitions from ACTIVE to DECOMMISSIONED are one-way and cannot
  be reversed.

---

## Aggregate: Air transport company

**Invariant:** Each ATCC collaborator must have a unique email address within the system;
a disabled collaborator cannot perform any system operation.

### Acceptance Criteria
- The system rejects the addition of a collaborator whose email already exists in the
  system.
- A disabled ATCC cannot register aircraft, routes, or manage pilots.
- Disabling a collaborator does not remove their historical data from the system.
- The company is identified by a unique combination of IATA (2 letters) and ICAO (2–3
  letters) codes.
- The system rejects the registration of a company with a duplicate IATA or ICAO code.

---

## Aggregate: Air control area

**Invariant:** The area code must be unique across the system; the geographic boundary must
be a valid non-empty polygon.

### Acceptance Criteria
- The system rejects the registration of an air control area with a duplicate area code.
- The system rejects the registration of an air control area with an empty or invalid
  geographic boundary polygon.
- An air control area can be created via a bootstrap process.
- The area code and boundary can only be modified through the aggregate root.
- The system returns a meaningful error message when either validation fails.

---

## Aggregate: Airport

**Invariant:** The IATA code (3 letters) and ICAO code (4 letters) must each be globally
unique and conform to their respective formats.

### Acceptance Criteria
- The system rejects the registration of an airport with a duplicate IATA code.
- The system rejects the registration of an airport with a duplicate ICAO code.
- The system rejects an IATA code that is not exactly 3 letters.
- The system rejects an ICAO code that is not exactly 4 letters.
- An airport must be associated with exactly one air control area.
- Coordinates (latitude, longitude) and altitude must be valid numeric values.
- An airport can be created via a bootstrap process.

---

## Aggregate: Flight route

**Invariant:** The route name must be unique; a route cannot be deactivated if there are
planned flights after the deactivation date.

### Acceptance Criteria
- The route name must follow the format: 2-letter company code followed by up to 4 digits
  (e.g. TP123).
- The system rejects the creation of a route with a duplicate name.
- The system rejects the deactivation of a route if flights are planned after the given
  deactivation date.
- A deactivated route cannot have new flights created on it.
- The origin and destination airports must be different.
- The route must be associated with a registered air transport company.

---

## Aggregate: Flight

**Invariant:** The flight designator must be unique across the system; a decommissioned
aircraft cannot be assigned to a flight.

### Acceptance Criteria
- The flight designator must follow the format `xxn(n)(n)(n)(a)` where `xx` is the airline
  code, `n(n)(n)(n)` is the numeric flight number, and `(a)` is an optional suffix.
- The system rejects the creation of a flight with a duplicate designator.
- The system rejects the assignment of a decommissioned aircraft to a flight.
- The flight must reference a valid and active flight route.
- The assigned pilot must belong to the same company as the flight route.
- Departure date/time must be before the scheduled arrival date/time.

---

## Aggregate: Flight plan

**Invariant:** The arrival airport of leg N must match the departure airport of leg N+1;
fuel quantity per leg must be strictly positive; segment start and end coordinates must
differ.

### Acceptance Criteria
- The system rejects the addition of a leg whose departure airport does not match the
  arrival airport of the previous leg.
- The system rejects a leg with a fuel quantity of zero or negative.
- The system rejects a segment whose start and end coordinates are identical.
- Each leg must have exactly one departure airport and one arrival airport.
- Each leg must contain at least one segment.
- The flight plan status is set to DRAFT upon creation and can only transition to VALIDATED
  after successful validation.
- A flight plan can be created from a DSL file conforming to the Core Flight DSL.
- The pilot assigned to the flight plan must belong to the same company as the flight route.

---

## Aggregate: Weather data

**Invariant:** Only one weather record may exist per air control area per date; all numeric
weather values must be positive.

### Acceptance Criteria
- The system rejects the registration of a weather record if one already exists for the same
  area and date.
- The system rejects weather condition values (temperature, pressure, wind speed) that are
  zero or negative.
- Weather data can be imported in bulk from a CSV file.
- Weather data must be associated with a registered air control area.
- The system returns a meaningful error message identifying which field failed validation.

---

## Aggregate: Simulation

**Invariant:** A simulation must include at least one valid flight plan; all flight processes
must complete their position updates before the simulation advances to the next time step.

### Acceptance Criteria
- The system rejects the start of a simulation with no associated flight plans.
- All flight plans included in the simulation must have VALIDATED status.
- The simulation progresses step by step; no time step advances before all flight processes
  have submitted their position updates for the current step.
- Safety violations are detected and logged in real time during the simulation.
- A safety violation event records the timestamp, positions, and velocity vectors of the
  involved aircraft.
- The simulation produces a final report indicating pass or fail, total number of flights,
  and all safety violation events with their timestamps and positions.
- The report is saved to a file upon simulation completion.