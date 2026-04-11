package com.example.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ChargeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testEndpoint() throws Exception {
        mockMvc.perform(get("/api/charge/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.success").value(true))
                .andExpect(jsonPath("$.result.totalEnergy").exists())
                .andExpect(jsonPath("$.result.totalCost").exists())
                .andExpect(jsonPath("$.chargingTimePoints").exists());
    }

    @Test
    void testOptimizeCharge() throws Exception {
        String requestJson = "{"
                + "\"currentBattery\": 20,"
                + "\"targetBattery\": 90,"
                + "\"startTimeIndex\": 0,"
                + "\"endTimeIndex\": 60"
                + "}";

        mockMvc.perform(post("/api/charge/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalEnergy").value(35.0));
    }

    @Test
    void testGetDefaultPrice() throws Exception {
        mockMvc.perform(get("/api/charge/price/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prices").isArray())
                .andExpect(jsonPath("$.prices.length()").value(96));
    }

    @Test
    void testOptimizeChargeInvalidInput() throws Exception {
        String requestJson = "{"
                + "\"currentBattery\": 150,"
                + "\"targetBattery\": 80,"
                + "\"startTimeIndex\": 0,"
                + "\"endTimeIndex\": 47"
                + "}";

        mockMvc.perform(post("/api/charge/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testOptimizeChargeImpossible() throws Exception {
        String requestJson = "{"
                + "\"currentBattery\": 0,"
                + "\"targetBattery\": 100,"
                + "\"startTimeIndex\": 0,"
                + "\"endTimeIndex\": 5"
                + "}";

        mockMvc.perform(post("/api/charge/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }
}
