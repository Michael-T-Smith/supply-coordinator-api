package com.michaelsmith.supply_coordinator_api.vessel;

public class Vessel {
    private final VesselState state;
    public Vessel(int id, VesselType type, String name, int food, int fuel, double latitude, double longitude) {
        this.state = new VesselState(id, type, name, food, fuel, latitude, longitude);
    }

    public VesselState getVesselState() {
        return this.state;
    }
}
