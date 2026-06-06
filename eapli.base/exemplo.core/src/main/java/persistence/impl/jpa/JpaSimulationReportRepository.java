package persistence.impl.jpa;

import domain.simulationreport.SimulationReport;
import domain.simulationreport.SimulationReportId;
import eapli.framework.domain.repositories.TransactionalContext;
import eapli.framework.infrastructure.repositories.impl.jpa.JpaAutoTxRepository;
import repositories.SimulationReportRepository;

import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class JpaSimulationReportRepository
        extends JpaAutoTxRepository<SimulationReport, SimulationReportId, SimulationReportId>
        implements SimulationReportRepository {

    public JpaSimulationReportRepository(final String persistenceUnitName,
                                          final Map extendedProperties) {
        super(persistenceUnitName, extendedProperties, "id");
    }

    public JpaSimulationReportRepository(final TransactionalContext autoTx) {
        super(autoTx, "id");
    }

    @Override
    public Iterable<SimulationReport> findAll() {
        return match("1=1");
    }
}
