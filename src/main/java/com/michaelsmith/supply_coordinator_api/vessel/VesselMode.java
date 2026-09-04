package com.michaelsmith.supply_coordinator_api.vessel;

// What state a vessel can be in
public enum VesselMode {
    IDLE,
    MOVING,
    RESUPPLY_REQUESTED,
    WAITING_FOR_SUPPLY,
    RESPONDING,
    RESUPPLYING
}
