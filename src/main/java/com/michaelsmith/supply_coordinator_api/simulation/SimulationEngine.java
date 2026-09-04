package com.michaelsmith.supply_coordinator_api.simulation;

import java.util.List;
import java.util.Random;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.michaelsmith.supply_coordinator_api.vessel.Position;
import com.michaelsmith.supply_coordinator_api.vessel.Vessel;
import com.michaelsmith.supply_coordinator_api.vessel.VesselMode;
import com.michaelsmith.supply_coordinator_api.vessel.VesselService;
import com.michaelsmith.supply_coordinator_api.vessel.VesselState;
import com.michaelsmith.supply_coordinator_api.vessel.VesselType;

@Component
public class SimulationEngine {
    //Application Constants
    private static final int LOW_FUEL_THRESHOLD = 25; // Call for aid at this level
    private static final double FUEL_CONSUMPTION_PER_TICK = 0.10; //Rate of fuel loss for all ships, individual levels will come later.
    private static final int RESUPPLY_HOLD_TICKS = 10; //Refueling delay
    private final VesselService service;
    private final Random random = new Random();

    SimulationEngine(VesselService service) {
        this.service = service;
    }

    //Main entry triggered every second. 
    @Scheduled(fixedRate = 1_000L)
    public synchronized void tick() {
        List<Vessel> vessels = service.getVessels();

        advanceNavalVessels(vessels);
        assignAvailableSupplyVessels(vessels);
        advanceSupplyVessels(vessels);
    }


    // Helper functions
    private void advanceNavalVessels(List<Vessel> vessels) {
        for (Vessel vessel : vessels) {
            if (vessel.getType() != VesselType.NAVAL) {
                continue;
            }

            VesselState state = vessel.getState();
            VesselMode mode = state.getMode();

            if (mode == VesselMode.WAITING_FOR_SUPPLY) {
                if (!hasValidAssignedSupply(vessels, vessel, VesselMode.RESPONDING)) {
                    state.requestResupply();
                }
                continue;
            }
            if (mode == VesselMode.RESUPPLYING) {
                if (!hasValidAssignedSupply(vessels, vessel, VesselMode.RESUPPLYING)) {
                    state.finishResupply();

                    if (state.getMode() == VesselMode.IDLE) {
                        assignNextPort(state);
                    }
                }
                continue;
            }
            if ((mode == VesselMode.IDLE || mode == VesselMode.MOVING)
                    && state.getFuel() <= LOW_FUEL_THRESHOLD) {
                state.requestResupply();
                continue;
            }
            if (mode == VesselMode.IDLE) {
                assignNextPort(state);
                continue;
            }
            if (mode != VesselMode.MOVING) {
                continue;
            }

            boolean arrived = state.advanceTowardDestination();
            state.consumeFuel(FUEL_CONSUMPTION_PER_TICK);
            
            if (state.getFuel() <= LOW_FUEL_THRESHOLD) {
                state.requestResupply();
            } else if (arrived) {
                assignNextPort(state);
            }
        }
    }

    private void advanceSupplyVessels(List<Vessel> vessels) {
        for (Vessel supplyVessel : vessels) {
            VesselState supplyState = supplyVessel.getState();

            if (supplyVessel.getType() != VesselType.SUPPLY) {
                continue;
            }

            Vessel navalVessel = findVesselById(vessels, supplyState.getAssignedVesselId());

            if (supplyState.getMode() == VesselMode.RESPONDING) {
                advanceRespondingSupply(supplyVessel, navalVessel);
            } else if (supplyState.getMode() == VesselMode.RESUPPLYING) {
                advanceResupplyHold(supplyVessel, navalVessel);
            }
        }
    }

    private void advanceRespondingSupply(Vessel supplyVessel, Vessel navalVessel) {
        VesselState supplyState = supplyVessel.getState();

        if (!hasValidAssignment(
                supplyVessel,
                navalVessel,
                VesselMode.WAITING_FOR_SUPPLY)
                || supplyState.getDestination() == null) {
            recoverInvalidAssignment(supplyVessel, navalVessel);
            return;
        }

        if (!supplyState.advanceTowardDestination()) {
            return;
        }

        navalVessel.getState().beginReceivingResupply();
        supplyState.beginResupplyHold(RESUPPLY_HOLD_TICKS);
    }

    private void advanceResupplyHold(Vessel supplyVessel, Vessel navalVessel) {
        VesselState supplyState = supplyVessel.getState();

        if (!hasValidAssignment(
                supplyVessel,
                navalVessel,
                VesselMode.RESUPPLYING)) {
            recoverInvalidAssignment(supplyVessel, navalVessel);
            return;
        }

        if (!supplyState.advanceResupplyHold()) {
            return;
        }

        VesselState navalState = navalVessel.getState();
        navalState.finishResupply();
        supplyState.releaseSupport();

        if (navalState.getMode() == VesselMode.IDLE) {
            assignNextPort(navalState);
        }
    }

    private void assignAvailableSupplyVessels(List<Vessel> vessels) {
        for (Vessel navalVessel : vessels) {
            VesselState navalState = navalVessel.getState();

            if (navalVessel.getType() != VesselType.NAVAL
                    || navalState.getMode() != VesselMode.RESUPPLY_REQUESTED) {
                continue;
            }

            Vessel supplyVessel = findNearestAvailableSupply(vessels, navalState.getPosition());

            if (supplyVessel == null) {
                continue;
            }

            supplyVessel.getState().respondTo(navalVessel.getId(), navalState.getPosition());
            navalState.waitForSupply(supplyVessel.getId());
        }
    }

    private Vessel findNearestAvailableSupply(List<Vessel> vessels, Position targetPosition) {
        Vessel nearestSupply = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (Vessel vessel : vessels) {
            VesselState state = vessel.getState();

            if (vessel.getType() != VesselType.SUPPLY
                    || state.getMode() != VesselMode.IDLE
                    || state.getAssignedVesselId() != null
                    || state.getDestination() != null) {
                continue;
            }

            double distanceSquared = distanceSquared(state.getPosition(), targetPosition);

            if (distanceSquared < nearestDistanceSquared) {
                nearestSupply = vessel;
                nearestDistanceSquared = distanceSquared;
            }
        }

        return nearestSupply;
    }

    private boolean hasValidAssignment(
            Vessel supplyVessel,
            Vessel navalVessel,
            VesselMode expectedNavalMode) {
        if (navalVessel == null || navalVessel.getType() != VesselType.NAVAL) {
            return false;
        }

        VesselState navalState = navalVessel.getState();
        Integer assignedSupplyId = navalState.getAssignedVesselId();

        return navalState.getMode() == expectedNavalMode
                && assignedSupplyId != null
                && assignedSupplyId == supplyVessel.getId();
    }

    private boolean hasValidAssignedSupply(
            List<Vessel> vessels,
            Vessel navalVessel,
            VesselMode expectedSupplyMode) {
        Vessel supplyVessel = findVesselById(
                vessels,
                navalVessel.getState().getAssignedVesselId());

        if (supplyVessel == null || supplyVessel.getType() != VesselType.SUPPLY) {
            return false;
        }

        Integer assignedNavalId = supplyVessel.getState().getAssignedVesselId();

        return supplyVessel.getState().getMode() == expectedSupplyMode
                && assignedNavalId != null
                && assignedNavalId == navalVessel.getId();
    }

    private void recoverInvalidAssignment(Vessel supplyVessel, Vessel navalVessel) {
        if (navalVessel != null && navalVessel.getType() == VesselType.NAVAL) {
            Integer assignedSupplyId = navalVessel.getState().getAssignedVesselId();

            if (assignedSupplyId != null && assignedSupplyId == supplyVessel.getId()) {
                if (navalVessel.getState().getFuel() > LOW_FUEL_THRESHOLD) {
                    navalVessel.getState().finishResupply();

                    if (navalVessel.getState().getMode() == VesselMode.IDLE) {
                        assignNextPort(navalVessel.getState());
                    }
                } else {
                    navalVessel.getState().requestResupply();
                }
            }
        }

        supplyVessel.getState().releaseSupport();
    }

    private Vessel findVesselById(List<Vessel> vessels, Integer vesselId) {
        if (vesselId == null) {
            return null;
        }

        for (Vessel vessel : vessels) {
            if (vessel.getId() == vesselId) {
                return vessel;
            }
        }

        return null;
    }

    private void assignNextPort(VesselState state) {
        List<Position> ports = service.getPorts();
        int destinationIndex = random.nextInt(ports.size());
        Position destination = ports.get(destinationIndex);

        if (destination.equals(state.getPosition())) {
            destination = ports.get((destinationIndex + 1) % ports.size());
        }

        state.setDestination(destination);
    }

    private double distanceSquared(Position first, Position second) {
        double latitudeDelta = first.latitude() - second.latitude();
        double longitudeDelta = first.longitude() - second.longitude();

        return latitudeDelta * latitudeDelta + longitudeDelta * longitudeDelta;
    }
}
