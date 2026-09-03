package com.michaelsmith.supply_coordinator_api.simulation;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.michaelsmith.supply_coordinator_api.vessel.Position;
import com.michaelsmith.supply_coordinator_api.vessel.Vessel;
import com.michaelsmith.supply_coordinator_api.vessel.VesselService;
import com.michaelsmith.supply_coordinator_api.vessel.VesselState;

@Component
public class SimulationEngine {
    private final VesselService service;
    private static final Position PORT_A = new Position (30.395148, -81.408010);
    private static final Position PORT_B = new Position(27.0, -65.0);
    private static final long MOVEMENT_DURATION = 15_000L;
    SimulationEngine(VesselService service) {
        this.service = service;
    }

    @Scheduled(fixedRate = 1000)
    public void tick() {
        long now = System.currentTimeMillis();

        for (Vessel vessel : service.getVessels()) {
            VesselState state = vessel.getState();

            if(!state.isMoving()) {
                startMovement(vessel, now);
                continue;
            }

            if (state.hasMovementFinished(now)) {
                state.finishMovement();
                //setup a random buffer timer for idle time.
                startMovement(vessel, now);
            }
        }
    }

    private void startMovement(Vessel vessel, long now){
        VesselState state = vessel.getState();

        Position destination;
        Position position = state.getPosition(); 
        if(position.equals(PORT_A)) {
            destination = PORT_B;
        } else if(position.equals(PORT_B)) {
            destination = PORT_A;
        } else {
            destination = new Position(27.0, -65.0);
        }

        state.move(destination, now, MOVEMENT_DURATION);
    }
}
