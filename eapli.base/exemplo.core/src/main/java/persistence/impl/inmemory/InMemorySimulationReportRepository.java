// ─── persistence/impl/inmemory/InMemorySimulationReportRepository.java ─────

package persistence.impl.inmemory;

import domain.simulationreport.SimulationReport;
import domain.simulationreport.SimulationReportId;
import eapli.framework.infrastructure.repositories.impl.inmemory.InMemoryDomainRepository;
import repositories.SimulationReportRepository;

public class InMemorySimulationReportRepository
        extends InMemoryDomainRepository<SimulationReport, SimulationReportId>
        implements SimulationReportRepository {

    @Override
    public Iterable<SimulationReport> findAll() {
        return super.findAll();
    }
}