package com.michaelsmith.supply_coordinator_api.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.michaelsmith.supply_coordinator_api.vessel.Position;
import com.michaelsmith.supply_coordinator_api.vessel.Vessel;
import com.michaelsmith.supply_coordinator_api.vessel.VesselMode;
import com.michaelsmith.supply_coordinator_api.vessel.VesselService;
import com.michaelsmith.supply_coordinator_api.vessel.VesselState;
import com.michaelsmith.supply_coordinator_api.vessel.VesselType;

class SimulationEngineTest {
    private static final int LOW_FUEL_THRESHOLD = 25;
    private static final int MAX_RENDEZVOUS_TICKS = 100;

    @Test
    void initializesExpectedFleetWithStableNamesAndVariedDestinations() {
        VesselService service = new VesselService();
        List<Vessel> vessels = service.getVessels();
        List<Vessel> navalVessels = vessels.stream()
                .filter(vessel -> vessel.getType() == VesselType.NAVAL)
                .toList();
        List<Vessel> supplyVessels = vessels.stream()
                .filter(vessel -> vessel.getType() == VesselType.SUPPLY)
                .toList();
        List<Integer> vesselIds = vessels.stream()
                .map(Vessel::getId)
                .toList();
        Set<Position> navalDestinations = navalVessels.stream()
                .map(vessel -> vessel.getState().getDestination())
                .collect(Collectors.toSet());

        assertThat(vessels).hasSize(18);
        assertThat(navalVessels).hasSize(15);
        assertThat(supplyVessels).hasSize(3);
        assertThat(vesselIds).doesNotHaveDuplicates();
        assertThat(service.getVessels()).extracting(Vessel::getId)
                .containsExactlyElementsOf(vesselIds);
        assertThat(vessels).extracting(Vessel::getName).doesNotHaveDuplicates();
        assertThat(navalVessels).extracting(Vessel::getName).containsExactly(
                "NAVAL-01", "NAVAL-02", "NAVAL-03", "NAVAL-04", "NAVAL-05",
                "NAVAL-06", "NAVAL-07", "NAVAL-08", "NAVAL-09", "NAVAL-10",
                "NAVAL-11", "NAVAL-12", "NAVAL-13", "NAVAL-14", "NAVAL-15");
        assertThat(supplyVessels).extracting(Vessel::getName)
                .containsExactly("SUPPLY-01", "SUPPLY-02", "SUPPLY-03");
        assertThat(service.getPorts()).hasSize(6);
        assertThat(navalDestinations).containsExactlyInAnyOrderElementsOf(service.getPorts());
        assertThat(navalVessels).allSatisfy(vessel -> {
            assertThat(vessel.getState().getMode()).isEqualTo(VesselMode.MOVING);
            assertThat(vessel.getState().getDestination()).isNotNull();
        });
        assertThat(supplyVessels).allSatisfy(vessel -> {
            assertThat(vessel.getState().getMode()).isEqualTo(VesselMode.IDLE);
            assertThat(vessel.getState().getDestination()).isNull();
            assertThat(vessel.getState().getAssignedVesselId()).isNull();
        });
    }

    @Test
    void assignsNearestSupplyAndCompletesTheResupplyLifecycle() {
        VesselService service = new VesselService();
        SimulationEngine engine = new SimulationEngine(service);
        List<Vessel> vessels = service.getVessels();
        Vessel navalVessel = findByName(vessels, "NAVAL-01");
        VesselState navalState = navalVessel.getState();
        Position routeDestination = navalState.getDestination();
        Position positionBeforeRequest = navalState.getPosition();

        navalState.consumeFuel(navalState.getFuel() - LOW_FUEL_THRESHOLD);

        List<Vessel> availableSupplyVessels = vessels.stream()
                .filter(vessel -> vessel.getType() == VesselType.SUPPLY)
                .toList();
        Vessel expectedSupplyVessel = availableSupplyVessels.stream()
                .min(Comparator.comparingDouble(vessel -> squaredDistance(
                        vessel.getState().getPosition(), positionBeforeRequest)))
                .orElseThrow();

        engine.tick();

        VesselState expectedSupplyState = expectedSupplyVessel.getState();
        assertThat(navalState.getPosition()).isEqualTo(positionBeforeRequest);
        assertThat(navalState.getMode()).isEqualTo(VesselMode.WAITING_FOR_SUPPLY);
        assertThat(navalState.getAssignedVesselId()).isEqualTo(expectedSupplyVessel.getId());
        assertThat(expectedSupplyState.getMode()).isEqualTo(VesselMode.RESPONDING);
        assertThat(expectedSupplyState.getAssignedVesselId()).isEqualTo(navalVessel.getId());
        assertThat(expectedSupplyState.getDestination()).isEqualTo(positionBeforeRequest);
        assertThat(availableSupplyVessels)
                .filteredOn(vessel -> vessel != expectedSupplyVessel)
                .allSatisfy(vessel -> {
                    assertThat(vessel.getState().getMode()).isEqualTo(VesselMode.IDLE);
                    assertThat(vessel.getState().getAssignedVesselId()).isNull();
                });

        boolean rendezvousCompleted = false;
        for (int tick = 0; tick < MAX_RENDEZVOUS_TICKS; tick++) {
            engine.tick();

            if (navalState.getFuel() == 100
                    && navalState.getAssignedVesselId() == null) {
                rendezvousCompleted = true;
                break;
            }
        }

        assertThat(rendezvousCompleted).isTrue();
        assertThat(navalState.getFuel()).isEqualTo(100);
        assertThat(navalState.getMode()).isEqualTo(VesselMode.MOVING);
        assertThat(navalState.getDestination()).isEqualTo(routeDestination);
        assertThat(expectedSupplyState.getMode()).isEqualTo(VesselMode.IDLE);
        assertThat(expectedSupplyState.getAssignedVesselId()).isNull();
        assertThat(expectedSupplyState.getDestination()).isNull();

        Position positionAfterResupply = navalState.getPosition();
        engine.tick();

        assertThat(navalState.getPosition()).isNotEqualTo(positionAfterResupply);
        assertThat(navalState.getFuel()).isEqualTo(99);
    }

    private Vessel findByName(List<Vessel> vessels, String name) {
        return vessels.stream()
                .filter(vessel -> vessel.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private double squaredDistance(Position first, Position second) {
        double latitudeDelta = first.latitude() - second.latitude();
        double longitudeDelta = first.longitude() - second.longitude();

        return latitudeDelta * latitudeDelta + longitudeDelta * longitudeDelta;
    }
}
