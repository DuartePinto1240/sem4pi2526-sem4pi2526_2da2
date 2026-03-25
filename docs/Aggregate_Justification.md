# Aggregate Justification

## Why each aggregate has the boundary it has

### User
The user is solely responsible for their own security state and skills assessment.
`SecurityClearance` and `SkillsAssessment` have no meaning without the user — they are
descriptive attributes, not entities with a lifecycle of their own. `Role` is kept outside
because it is referenced globally and managed independently (it could be assigned to many
users).

### Role
`Role` is a globally referenced configuration entity. It is managed independently because
the same role can be assigned to many users. Keeping it as its own aggregate avoids coupling
the user lifecycle to role management, and allows new roles to be introduced without
modifying the `User` aggregate.

### Manufacturer
A simple reference entity. It has no sub-objects with identity of their own; it is
referenced by `EngineModel` and `AircraftModel` but does not control them. Keeping it as a
separate aggregate avoids coupling between the manufacturer catalogue and the model
catalogues.

### Engine model / Aircraft model
Each model is an independent configuration unit with its own integrity rules (e.g., an
aircraft model must have at least one certified compatible engine). Separating them allows
the lifecycle of each catalogue to be managed autonomously. The `CertifiedEngineRef` inside
`AircraftModel` is a list of references (VOs), not instances of `EngineModel` — the
aggregate boundary does not cross into the other aggregate.

### Aircraft
A concrete aircraft belongs to a company but has its own lifecycle (decommissioning, cabin
configuration changes). The aggregate enforces the invariant that the total number of seats
does not exceed the model's capacity. The company is referenced but not controlled within
this aggregate.

### Air transport company
The company directly controls its collaborators (ATCC) because the business rules for
activation and deactivation are the company's responsibility. An `ATCC` has no meaning
outside the context of its company. Pilots, although also collaborators, are `User` entities
with the `Pilot` role — the link to the company is by reference, since a pilot has a
lifecycle in the user management system.

### Air control area
The air control area is a geographic configuration entity managed independently by
Backoffice Operators. It is referenced by `Airport`, `WeatherData`, and `Simulation`, but it
does not control any of them — those aggregates merely hold a reference to it. `AreaCode`
and `GeographicBoundary` are VOs because they are descriptive attributes of the area with no
lifecycle of their own. Keeping the area as its own aggregate allows its boundaries and code
to be managed without triggering changes in the aggregates that reference it.

### Airport
An airport has its own global identity (IATA/ICAO codes) and is referenced across multiple
aggregates — `FlightRoute`, `FlightLeg`, and `AirControlArea` — but it is not owned by any
of them. `AirportCode` and `Coordinates` are VOs because they describe the airport without
having a lifecycle independent from it. Keeping the airport as a separate aggregate ensures
that changes to airport data do not require transactional access to flight plans or routes.

### Flight route
A route is a configuration entity of the company, but it is referenced by flights
independently. Keeping the route as its own aggregate allows it to be deactivated without
directly impacting the flight aggregates (which store only the reference). `RouteName` is a
VO because it encapsulates the format rule (company initials followed by up to 4 digits) and
has no lifecycle independent of the route itself. The origin and destination airports are
referenced by ID — the route does not own them.

### Flight
A flight is an instance of a route with a unique designator. It references the route,
aircraft, and pilot by ID without containing them. The main invariants are the uniqueness of
the designator and the coherence of the schedule.

### Flight plan
This is the richest aggregate in the domain. It contains `FlightLeg`, `Segment`, and
`NodeJunction` as internal entities because the set of invariants that links them (airport
sequence between legs, segment continuity, uniqueness of the flight identifier within the
file) can only be enforced with transactional access to the entire graph. None of these
objects makes sense outside the plan that frames them. The VOs (`Coordinates`,
`WindCondition`, `AltitudeSlot`) are descriptive attributes with no identity.

### Weather data
Each weather record is independent (area + date). It is referenced by the flight plan and
the simulation, but managed autonomously by the weather service. Keeping it separate allows
the service to be scaled or replaced without affecting the other aggregates.

### Simulation
The simulation is the execution aggregate. It controls the complete lifecycle of the
execution and the events it produces. `SimulationReport` and `SafetyViolation` are internal
entities because they are accumulated during the simulation and only make sense in its
context — they do not exist before or after the simulation that generated them. The
parameters are VOs because they are fixed at the start and never change during execution.