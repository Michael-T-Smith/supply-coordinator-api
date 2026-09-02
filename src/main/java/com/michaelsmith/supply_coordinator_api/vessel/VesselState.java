package com.michaelsmith.supply_coordinator_api.vessel;

public record VesselState (
    int id,
    VesselType type,
    String name,
    int fuel,  
    int food,
    double latitude, 
    double longitude
){}
