package com.michaelsmith.supply_coordinator_api;

import java.util.HashMap;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class APIController {
   
    @GetMapping("/healthcheck")
    public HashMap<String, Object> healthcheck() {
    
        HashMap<String, Object> data = new HashMap<>();
        data.put("status", 200);
        data.put("message", "Server Reached"); 
    
        return data;
   } 

   





}
