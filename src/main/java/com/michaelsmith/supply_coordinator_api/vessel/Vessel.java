package com.michaelsmith.supply_coordinator_api.vessel;

public class Vessel {
    private final VesselState state;
    private final String name;
    private final VesselType type;

    public Vessel(VesselType type, String name, int food, int fuel, double latitude, double longitude) {
        this.name = name;
        this.type = type;
        this.state = new VesselState(food, fuel, latitude, longitude, VesselMode.IDLE);
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
    
    public void consumeResource(String type, int amount) {
        if ("food".equals(type)) {
            this.state.consumeFood(amount);
        } else {
            this.state.consumeFuel(amount);
        }
    }
}
