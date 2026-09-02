package com.michaelsmith.supply_coordinator_api.vessel;

import java.util.ArrayList;

public class VesselService {
    public ArrayList<Vessel> vessels = new ArrayList<Vessel>();
    
    public VesselService() {
        createVessels();
    }

    public void createVessels() { 
        Vessel eisenhower = new Vessel(1, VesselType.NAVAL, "USS Dwight D. Eisenflower", 75, 100 ,24.2, -72.2);
        Vessel supplier_1 = new Vessel(2, VesselType.SUPPLY, "Rock & Stock", 75, 100 ,15.2, -52.2);

    
        vessels.add(eisenhower);
        vessels.add(supplier_1);

   } 

   public ArrayList<Vessel> getVessels() {
    return vessels;
   }
}
