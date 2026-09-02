package com.michaelsmith.supply_coordinator_api;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    
    @GetMapping("/")
    public static String root(Model model) { 
        return "index";
    }    
}
