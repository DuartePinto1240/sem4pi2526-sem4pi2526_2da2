# Glossary

| **Term or Expression** | **Termo ou Expressão** | **Description (Definition and Information)** |
|:---|:---|:---|
| **Admin** | **Administrador** | System user with permission to manage backoffice users and collaborators of flight control entities. |
| **Air control area** | **Área de controlo aéreo** | Geographic area of air traffic control, with a unique code and valid boundaries. Context for weather data and simulations. Managed by Backoffice Operators. |
| **Air transport company** | **Companhia de transporte aéreo** | Airline registered in the system, identified by unique IATA (2 letters) and ICAO (2–3 letters) codes. Owns a fleet of aircraft and defines flight routes. |
| **Aircraft** | **Aeronave** | Concrete instance of an aircraft model belonging to an air transport company. Identified by a unique registration number. Has cabin configuration, crew size, and operational status. |
| **Aircraft model** | **Modelo de aeronave** | Motorized commercial aircraft model. Characterized by MTOW, MZFW, maximum fuel capacity, service ceiling, cruise speed, wing area, lift coefficient (Cl), and drag coefficient (Cd). |
| **Altitude slot** | **Slot de altitude** | Permitted altitude interval within a segment, with an associated width. Defined per segment in the flight plan. Value object; has no identity of its own. |
| **Airport** | **Aeroporto** | Node in the air traffic network registered in an air control area. Identified by globally unique IATA (3 letters) and ICAO (4 letters) codes. Has coordinates, altitude, town, and country. |
| **Airport code** | **Código de aeroporto** | Value object encapsulating the IATA and ICAO codes of an airport. Immutable and globally unique. |
| **Area code** | **Código de área** | Unique identifier of an air control area. Immutable value object. |
| **ATCC** | **Colaborador de companhia aérea** | Air Transport Company Collaborator. System user belonging to an air transport company, responsible for managing fleet, routes, and pilots. Entity managed within the Air transport company aggregate. |
| **Backoffice operator** | **Operador de backoffice** | System user responsible for feeding the system with base data: areas, airports, aircraft models, engine models, companies, etc. |
| **Cabin configuration** | **Configuração de cabine** | Value object describing the number of seats per class and crew size of an aircraft. Replaced entirely when updated. |
| **Certified engine ref** | **Referência de motor certificado** | Value object representing a reference to an engine model certified for use in a specific aircraft model. Stored as a typed reference list within the aircraft model aggregate. |
| **Company code** | **Código de companhia** | Value object encapsulating the IATA and ICAO codes of an air transport company. Immutable; has no lifecycle of its own. |
| **Coordinates** | **Coordenadas** | Value object representing a geographic position as latitude and longitude. Used in segments, nodes, and airports. Immutable. |
| **DSL source** | **Fonte DSL** | Value object storing the raw text of the DSL file that originated a flight plan. Immutable after import. |
| **Engine model** | **Modelo de motor** | Certified engine model for use in aircraft. Characterized by motorization type, thrust specification, and thrust specific fuel consumption (TSFC). Aggregate root. |
| **Flight** | **Voo** | Instance of a flight route with a specific date and time. Identified by a unique flight designator. References a route, aircraft, and pilot. Aggregate root. |
| **Flight control operator (FCO)** | **Operador de controlo de voo** | System user belonging to a flight control entity. Tests flight plans and runs air control simulations. |
| **Flight designator** | **Designador de voo** | Value object representing the unique code of a flight, composed of the airline code, numeric flight number, and optional operational suffix (format: `xxn(n)(n)(n)(a)`). Encapsulates the format rule. |
| **Flight DSL** | **DSL de voo** | Domain-specific language for formally describing flight plans. Defined by an ANTLR grammar with lexer and parser rules. Imported from external tools. |
| **Flight characteristics** | **Características de voo** | Value object grouping the immutable physical parameters of an aircraft model: Cl, Cd, MTOW, MZFW, wing area, cruise speed, and service ceiling. Has no independent lifecycle. |
| **Flight leg** | **Leg de voo** | Non-stop segment within a flight plan between two airports. Entity inside the flight plan aggregate. Contains departure information, arrival information, and fuel quantity. Has identity based on its sequence order within the plan. |
| **Flight log entry** | **Entrada de log de voo** | Record of a single simulation step for a specific flight: position, timestamp, and step number. Stored in a separate file per flight by the flight logging server. |
| **Flight logging server** | **Servidor de logging de voos** | UDP server that receives position updates from flight processes at each simulation step. Includes an HTTP server with an AJAX-updated status page. |
| **Flight plan** | **Plano de voo** | Detailed plan for a flight, including aircraft, fuel load, pilot, and route. Initial status is "draft" until validated. Can be created from a DSL file. Aggregate root; the most complex aggregate in the domain. |
| **Flight route** | **Rota de voo** | Route between two airports, defined by an air transport company. Unique name composed of the company's initials followed by up to 4 digits (e.g., TP123). Aggregate root. |
| **Flight schedule** | **Horário de voo** | Value object containing the planned departure and arrival date/time of a flight. Replaceable without affecting flight identity. |
| **Geographic boundary** | **Fronteira geográfica** | Value object describing the geographic polygon delimiting an air control area. Must be non-empty and valid. |
| **Manufacturer** | **Fabricante** | Manufacturer of aircraft or engines. Stores name and country. Referenced by aircraft and engine models. Aggregate root. |
| **Monthly report** | **Relatório mensal** | Statistical report generated by a flight control operator. Serves as the foundation for other future report types, ensuring consistent structure and branding. |
| **Node / junction** | **Nó / junção** | Connection point between two or more segments, allowing an aircraft to transition between contiguous segments. Entity within the flight plan aggregate; identity based on coordinates. |
| **Operational status** | **Estado operacional** | Value object representing the operational state of an aircraft (active or decommissioned). Enum-like type with no identity of its own. |
| **Pilot** | **Piloto** | System user belonging to an air transport company, certified to pilot one or more aircraft models. Creates and manages flight plans. |
| **Role** | **Papel** | Functional role assigned to a user in the system (e.g., Admin, Pilot, FCO). Managed independently as its own aggregate; may evolve to support multiple roles per user. |
| **Route name** | **Nome de rota** | Value object encapsulating the name of a flight route. Format: company initials (2 letters) followed by up to 4 digits. Encapsulates the format rule. |
| **Safety violation** | **Violação de segurança** | Event detected during a simulation where two or more aircraft violate separation safety rules. Entity within the simulation aggregate; recorded with timestamp, positions, and velocity vectors. |
| **Security clearance** | **Autorização de segurança** | Value object representing an active security clearance assigned to a user, with an automatic expiry date. Managed by the Admin or Backoffice Operator. |
| **Segment** | **Segmento** | Linear path between two geographic points within a flight leg. Entity within the flight plan aggregate. Characterized by start/end coordinates, permitted altitude slots, width per slot, and wind conditions. Invariant: start and end coordinates must differ. |
| **Semantic validation** | **Validação semântica** | Process of validating a flight plan after lexical and syntactic analysis. Verifies domain coherence: leg sequence, identifier uniqueness, positive values, route coherence, and segment consistency. |
| **Simulation** | **Simulação** | Coordinated execution of multiple flights in an area over a time interval. Aggregate root. Implemented in C using processes, pipes, signals, threads, shared memory, and semaphores. |
| **Simulation parameters** | **Parâmetros de simulação** | Value object containing the configuration of a simulation: time range, geographic area, safety thresholds, and performance settings. Immutable after the simulation starts. |
| **Simulation report** | **Relatório de simulação** | Report generated at the end of a simulation. Entity within the simulation aggregate; accumulates data throughout execution. Contains execution status per flight, safety violation events, and an overall pass/fail result. |
| **Skills assessment** | **Avaliação de competências** | Value object recording a user's periodic skills assessment, required every 5 years by regulation. Immutable after creation; replaced by a new instance when renewed. Managed by the Admin or Backoffice Operator. |
| **Thrust spec** | **Especificação de impulso** | Value object describing an engine model's thrust: sea-level value and cruise speed value. Both values must be present and positive. Has no identity — it describes the engine model. |
| **TSFC (Thrust Specific Fuel Consumption)** | **Consumo específico de combustível** | Measure of engine fuel efficiency relative to thrust output, expressed in N/N/s. Used in flight range and fuel consumption estimation. |
| **User** | **Utilizador** | Any person with access to the system. Identified by a unique email from a valid domain, name, and phone number. Requires authentication for any operation. Aggregate root. |
| **Velocity vector** | **Vetor velocidade** | Value object representing the velocity of an aircraft at the moment a safety violation is detected. Immutable after being recorded. |
| **Weather condition** | **Condição meteorológica** | Value object containing a set of weather readings (temperature, pressure, wind) for a specific area and date. Replaceable when the weather data record is updated. |
| **Weather data** | **Dados meteorológicos** | Meteorological record for a specific air control area and date. Aggregate root; identified by area + timestamp. Can be imported from multiple external sources. |
| **Weather person** | **Pessoa de meteorologia** | System user who registers, imports, and consults weather data in the system. |
| **Weather service** | **Serviço meteorológico** | Centralized, AI-enhanced service shared across all system instances. Provides weather data to flight plans and simulations. |
| **Wind condition** | **Condição de vento** | Value object describing the wind on a segment: direction (angle relative to North) and speed (m/s). Has no identity of its own. |