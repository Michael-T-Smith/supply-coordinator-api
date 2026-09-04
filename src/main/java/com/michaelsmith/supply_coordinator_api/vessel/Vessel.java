package com.michaelsmith.supply_coordinator_api.vessel;

public class Vessel {
    private final int id;
    private final String name;
    private final VesselState state;
    private final VesselType type;

    public Vessel(
            int id,
            VesselType type,
            String name,
            int food,
            int fuel,
            double speed,
            Position position,
            Position destinationPosition) {
        this.id = id;
        this.name = name;
        this.type = type;
        VesselMode initialMode = destinationPosition == null ? VesselMode.IDLE : VesselMode.MOVING;
        this.state = new VesselState(food, fuel, position, destinationPosition, speed, initialMode);
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public VesselType getType() {
        return type;
    }

    public VesselState getState() {
        return state;
    }


    // Helper functions
    public void consumeResource(String type, int amount) {
        if ("food".equals(type)) {
            this.state.consumeFood(amount);
        } else {
            this.state.consumeFuel(amount);
        }
    }
}
