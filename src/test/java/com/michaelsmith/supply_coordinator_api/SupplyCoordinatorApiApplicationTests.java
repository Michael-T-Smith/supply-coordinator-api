package com.michaelsmith.supply_coordinator_api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SupplyCoordinatorApiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void vesselsEndpointSerializesTheSharedFleet() throws Exception {
        mockMvc.perform(get("/api/vessels"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(18)))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].name").value("NAVAL-01"))
                .andExpect(jsonPath("$[0].type").value("NAVAL"))
                .andExpect(jsonPath("$[0].state.food").isNumber())
                .andExpect(jsonPath("$[0].state.fuel").isNumber())
                .andExpect(jsonPath("$[0].state.speed").isNumber())
                .andExpect(jsonPath("$[0].state.mode").isString())
                .andExpect(jsonPath("$[0].state.position.latitude").isNumber())
                .andExpect(jsonPath("$[0].state.position.longitude").isNumber())
                .andExpect(jsonPath("$[0].state.destination").hasJsonPath())
                .andExpect(jsonPath("$[0].state.assignedVesselId").hasJsonPath())
                .andExpect(jsonPath("$[15].name").value("SUPPLY-01"))
                .andExpect(jsonPath("$[15].type").value("SUPPLY"))
                .andExpect(jsonPath("$[15].state.position.latitude").isNumber())
                .andExpect(jsonPath("$[15].state.position.longitude").isNumber())
                .andExpect(jsonPath("$[15].state.assignedVesselId").hasJsonPath());
    }

}
