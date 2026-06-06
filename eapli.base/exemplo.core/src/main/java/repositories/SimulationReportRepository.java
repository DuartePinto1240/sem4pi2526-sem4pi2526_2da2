// ─── repositories/SimulationReportRepository.java ─────────────────────────

package repositories;

import domain.simulationreport.SimulationReport;
import domain.simulationreport.SimulationReportId;
import eapli.framework.domain.repositories.DomainRepository;

public interface SimulationReportRepository
        extends DomainRepository<SimulationReportId, SimulationReport> {

    Iterable<SimulationReport> findAll();
}