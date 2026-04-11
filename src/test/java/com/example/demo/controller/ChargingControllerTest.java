package com.example.demo.controller;

import com.example.demo.model.ChargingRequest;
import com.example.demo.model.ElectricityPriceConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ChargingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("测试获取电价配置")
    void testGetPriceConfig() throws Exception {
        mockMvc.perform(get("/api/charging/config/prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sharpPeakPrice").value(1.2))
                .andExpect(jsonPath("$.data.peakPrice").value(0.9))
                .andExpect(jsonPath("$.data.flatPrice").value(0.6))
                .andExpect(jsonPath("$.data.valleyPrice").value(0.3));
    }

    @Test
    @DisplayName("测试设置电价配置")
    void testSetPriceConfig() throws Exception {
        ElectricityPriceConfig config = new ElectricityPriceConfig(
            new BigDecimal("1.5"),
            new BigDecimal("1.0"),
            new BigDecimal("0.7"),
            new BigDecimal("0.25")
        );
        
        mockMvc.perform(post("/api/charging/config/prices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sharpPeakPrice").value(1.5))
                .andExpect(jsonPath("$.data.peakPrice").value(1.0));
    }

    @Test
    @DisplayName("测试获取96点电价")
    void testGetPrices() throws Exception {
        mockMvc.perform(get("/api/charging/prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.prices").isArray())
                .andExpect(jsonPath("$.prices.length()").value(96))
                .andExpect(jsonPath("$.priceTypes").isArray())
                .andExpect(jsonPath("$.priceTypes.length()").value(96))
                .andExpect(jsonPath("$.timeSlots").isArray())
                .andExpect(jsonPath("$.timeSlots.length()").value(96));
    }

    @Test
    @DisplayName("测试充电优化 - 正常请求")
    void testOptimizeCharging_Normal() throws Exception {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(95);
        request.setCurrentSoc(new BigDecimal("20"));
        request.setTargetSoc(new BigDecimal("80"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        mockMvc.perform(post("/api/charging/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalEnergy").value(36.0000))
                .andExpect(jsonPath("$.data.chargingPoints").isArray())
                .andExpect(jsonPath("$.data.chargingPoints.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.chargingPoints[0].priceType").value("低谷"));
    }

    @Test
    @DisplayName("测试充电优化 - 时间不足")
    void testOptimizeCharging_InsufficientTime() throws Exception {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(32);
        request.setEndPoint(44);
        request.setCurrentSoc(new BigDecimal("30"));
        request.setTargetSoc(new BigDecimal("70"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        mockMvc.perform(post("/api/charging/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("充电时间不足")));
    }

    @Test
    @DisplayName("测试充电优化 - 无需充电")
    void testOptimizeCharging_NoNeedToCharge() throws Exception {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(95);
        request.setCurrentSoc(new BigDecimal("80"));
        request.setTargetSoc(new BigDecimal("80"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        mockMvc.perform(post("/api/charging/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("无需充电")));
    }

    @Test
    @DisplayName("测试充电优化 - 参数验证失败")
    void testOptimizeCharging_ValidationError() throws Exception {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(-1);
        request.setEndPoint(95);
        request.setCurrentSoc(new BigDecimal("20"));
        request.setTargetSoc(new BigDecimal("80"));
        
        mockMvc.perform(post("/api/charging/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("测试充电优化 - 开始点大于结束点")
    void testOptimizeCharging_StartGreaterThanEnd() throws Exception {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(50);
        request.setEndPoint(30);
        request.setCurrentSoc(new BigDecimal("20"));
        request.setTargetSoc(new BigDecimal("80"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        mockMvc.perform(post("/api/charging/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("测试充电优化 - SOC超出范围")
    void testOptimizeCharging_SocOutOfRange() throws Exception {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(95);
        request.setCurrentSoc(new BigDecimal("120"));
        request.setTargetSoc(new BigDecimal("80"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        mockMvc.perform(post("/api/charging/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("测试测试接口")
    void testTestCharging() throws Exception {
        mockMvc.perform(post("/api/charging/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.testRequest").exists())
                .andExpect(jsonPath("$.testRequest.startPoint").value(0))
                .andExpect(jsonPath("$.testRequest.endPoint").value(95))
                .andExpect(jsonPath("$.testRequest.currentSoc").value(20))
                .andExpect(jsonPath("$.testRequest.targetSoc").value(80));
    }

    @Test
    @DisplayName("测试充电优化 - 验证充电点数据结构")
    void testOptimizeCharging_VerifyChargingPointStructure() throws Exception {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(10);
        request.setCurrentSoc(new BigDecimal("50"));
        request.setTargetSoc(new BigDecimal("60"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        mockMvc.perform(post("/api/charging/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chargingPoints[0].pointIndex").exists())
                .andExpect(jsonPath("$.data.chargingPoints[0].timeSlot").exists())
                .andExpect(jsonPath("$.data.chargingPoints[0].price").exists())
                .andExpect(jsonPath("$.data.chargingPoints[0].priceType").exists())
                .andExpect(jsonPath("$.data.chargingPoints[0].power").exists())
                .andExpect(jsonPath("$.data.chargingPoints[0].energy").exists())
                .andExpect(jsonPath("$.data.chargingPoints[0].cost").exists());
    }

    @Test
    @DisplayName("测试充电优化 - 验证总成本计算")
    void testOptimizeCharging_VerifyTotalCost() throws Exception {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(10);
        request.setCurrentSoc(new BigDecimal("50"));
        request.setTargetSoc(new BigDecimal("60"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        MvcResult result = mockMvc.perform(post("/api/charging/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        
        mockMvc.perform(post("/api/charging/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEnergy").value(6.0000))
                .andExpect(jsonPath("$.data.totalCost").value(1.8000));
    }

    @Test
    @DisplayName("测试充电优化 - 不同电池容量")
    void testOptimizeCharging_DifferentBatteryCapacity() throws Exception {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(95);
        request.setCurrentSoc(new BigDecimal("20"));
        request.setTargetSoc(new BigDecimal("80"));
        request.setBatteryCapacity(new BigDecimal("80"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        mockMvc.perform(post("/api/charging/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalEnergy").value(48.0000));
    }

    @Test
    @DisplayName("测试充电优化 - 不同充电功率")
    void testOptimizeCharging_DifferentChargingPower() throws Exception {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(95);
        request.setCurrentSoc(new BigDecimal("20"));
        request.setTargetSoc(new BigDecimal("80"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("3.5"));
        
        mockMvc.perform(post("/api/charging/optimize")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chargingPoints.length()").value(greaterThan(20)));
    }
}
