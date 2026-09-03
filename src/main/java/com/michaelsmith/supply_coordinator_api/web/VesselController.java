package com.michaelsmith.supply_coordinator_api.web;

import java.util.ArrayList;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.michaelsmith.supply_coordinator_api.vessel.Vessel;
import com.michaelsmith.supply_coordinator_api.vessel.VesselService;

@RestController
public class VesselController {
    
    private final VesselService service;
    VesselController(VesselService service) {
        this.service = service;
    }
   
   @GetMapping("/api/vessels")
   public ArrayList<Vessel> loadVessel() {
    return service.getVessels();
   }


}
