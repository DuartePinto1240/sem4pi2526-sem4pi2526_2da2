package controller;

import domain.aircraft.Aircraft;
import domain.aircraftmodel.AircraftModel;
import persistence.PersistenceContext;
import repositories.AircraftModelRepository;
import repositories.AircraftRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ListCompanyFleetController {

    private final AircraftRepository      aircraftRepo;
    private final AircraftModelRepository aircraftModelRepo;

    public ListCompanyFleetController() {
        this.aircraftRepo      = PersistenceContext.getInstance().aircraftRepository();
        this.aircraftModelRepo = PersistenceContext.getInstance().aircraftModelRepository();
    }

    ListCompanyFleetController(final AircraftRepository aircraftRepo,
                               final AircraftModelRepository aircraftModelRepo) {
        this.aircraftRepo      = aircraftRepo;
        this.aircraftModelRepo = aircraftModelRepo;
    }

    // US072 — list all fleet
    public Iterable<Aircraft> listFleet(final String companyIata) {
        return aircraftRepo.findByCompanyIata(companyIata);
    }

    // US072a — list by model id
    public Iterable<Aircraft> listByModel(final String companyIata,
                                          final Long modelId) {
        return aircraftRepo.findByCompanyIataAndModelId(companyIata, modelId);
    }

    // US072b — list by maker (filter done here using model repo)
    public Iterable<Aircraft> listByMaker(final String companyIata,
                                          final String manufacturer) {
        final List<Aircraft> result = new ArrayList<>();
        for (final Aircraft a : aircraftRepo.findByCompanyIata(companyIata)) {
            final Optional<AircraftModel> model =
                    aircraftModelRepo.ofIdentity(a.aircraftModelId());
            if (model.isPresent() &&
                    model.get().manufacturer()
                            .equalsIgnoreCase(manufacturer.trim())) {
                result.add(a);
            }
        }
        return result;
    }

    // US072c — list by minimum capacity
    public Iterable<Aircraft> listByCapacity(final String companyIata,
                                             final int minSeats) {
        final List<Aircraft> result = new ArrayList<>();
        for (final Aircraft a : aircraftRepo.findByCompanyIata(companyIata)) {
            if (a.cabinConfiguration().totalSeats() >= minSeats) {
                result.add(a);
            }
        }
        return result;
    }
    public Iterable<String> usedManufacturers(final String companyIata) {
        final List<String> manufacturers = new ArrayList<>();
        for (final Aircraft a : aircraftRepo.findByCompanyIata(companyIata)) {
            final Optional<AircraftModel> model =
                    aircraftModelRepo.ofIdentity(a.aircraftModelId());
            model.map(AircraftModel::manufacturer)
                    .filter(m -> !manufacturers.contains(m))
                    .ifPresent(manufacturers::add);
        }
        return manufacturers;
    }

    // US072d — list sorted by registration (alphabetical)
    public Iterable<Aircraft> listByModelName(final String companyIata) {
        final List<Aircraft> list = new ArrayList<>();
        aircraftRepo.findByCompanyIata(companyIata).forEach(list::add);
        list.sort(Comparator.comparing(Aircraft::registration));
        return list;
    }
    // US072d — list sorted by age (oldest first)
    public Iterable<Aircraft> listByAge(final String companyIata) {
        final List<Aircraft> list = new ArrayList<>();
        aircraftRepo.findByCompanyIata(companyIata).forEach(list::add);
        list.sort(Comparator.comparing(Aircraft::registrationDate));
        return list;
    }

    // helper — get all available models for filtering in UI
    public Iterable<AircraftModel> availableModels() {
        return aircraftModelRepo.findAll();
    }
}