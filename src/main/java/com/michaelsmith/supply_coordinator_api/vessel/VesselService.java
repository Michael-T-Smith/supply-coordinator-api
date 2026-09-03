package com.michaelsmith.supply_coordinator_api.vessel;

import java.util.ArrayList;

import org.springframework.stereotype.Service;

@Service
public class VesselService {
    private ArrayList<Vessel> vessels = new ArrayList<Vessel>();
    
    public VesselService() {
        createVessels();
    }

    public void createVessels() { 
        Vessel eisenhower = new Vessel(VesselType.NAVAL, "USS Dwight D. Eisenflower", 75, 95 ,24.2, -72.2);
        Vessel supplier_1 = new Vessel(VesselType.SUPPLY, "Rock & Stock", 100, 100 ,15.2, -52.2);

    
        vessels.add(eisenhower);
        vessels.add(supplier_1);

   } 

   public ArrayList<Vessel> getVessels() {
    return vessels;
   }

}
