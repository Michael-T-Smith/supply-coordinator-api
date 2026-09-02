package com.michaelsmith.supply_coordinator_api.web;

import java.util.List;
import java.util.HashMap;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.michaelsmith.supply_coordinator_api.vessel.Vessel;
import com.michaelsmith.supply_coordinator_api.vessel.VesselService;

@RestController
public class VesselController {
    
    private final VesselService service;
    VesselController() {
        this.service = new VesselService();
    }

    @GetMapping("/healthcheck")
    public HashMap<String, Object> healthcheck() {
    
        HashMap<String, Object> data = new HashMap<>();
        data.put("status", 200);
        data.put("message", "Server Reached"); 
        VesselService service = new VesselService();
        service.createVessels();
        return data;
   } 

   
   @GetMapping("/api/vessels")
   public List<Vessel> loadVessel() {
    List<Vessel> vessels = service.getVessels();
    return vessels;
   }


}
