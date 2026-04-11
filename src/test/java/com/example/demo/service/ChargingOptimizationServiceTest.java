package com.example.demo.service;

import com.example.demo.model.ChargingRequest;
import com.example.demo.model.ChargingResult;
import com.example.demo.model.ElectricityPriceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ChargingOptimizationServiceTest {

    private ChargingOptimizationService service;

    @BeforeEach
    void setUp() {
        service = new ChargingOptimizationService();
    }

    @Test
    @DisplayName("测试默认电价配置")
    void testDefaultPriceConfig() {
        ElectricityPriceConfig config = service.getPriceConfig();
        
        assertNotNull(config);
        assertEquals(0, new BigDecimal("1.2").compareTo(config.getSharpPeakPrice()));
        assertEquals(0, new BigDecimal("0.9").compareTo(config.getPeakPrice()));
        assertEquals(0, new BigDecimal("0.6").compareTo(config.getFlatPrice()));
        assertEquals(0, new BigDecimal("0.3").compareTo(config.getValleyPrice()));
    }

    @Test
    @DisplayName("测试设置电价配置")
    void testSetPriceConfig() {
        ElectricityPriceConfig newConfig = new ElectricityPriceConfig(
            new BigDecimal("1.5"),
            new BigDecimal("1.0"),
            new BigDecimal("0.7"),
            new BigDecimal("0.25")
        );
        
        service.setPriceConfig(newConfig);
        ElectricityPriceConfig config = service.getPriceConfig();
        
        assertEquals(0, new BigDecimal("1.5").compareTo(config.getSharpPeakPrice()));
        assertEquals(0, new BigDecimal("1.0").compareTo(config.getPeakPrice()));
        assertEquals(0, new BigDecimal("0.7").compareTo(config.getFlatPrice()));
        assertEquals(0, new BigDecimal("0.25").compareTo(config.getValleyPrice()));
    }

    @Test
    @DisplayName("测试获取96点电价")
    void testGet96PointPrices() {
        BigDecimal[] prices = service.get96PointPrices();
        
        assertEquals(96, prices.length);
        
        assertEquals(0, new BigDecimal("0.3").compareTo(prices[0]));
        assertEquals(0, new BigDecimal("0.3").compareTo(prices[23]));
        assertEquals(0, new BigDecimal("0.9").compareTo(prices[32]));
        assertEquals(0, new BigDecimal("1.2").compareTo(prices[76]));
    }

    @Test
    @DisplayName("测试获取电价类型")
    void testGetPriceTypeByPoint() {
        assertEquals("低谷", service.getPriceTypeByPoint(0));
        assertEquals("低谷", service.getPriceTypeByPoint(23));
        assertEquals("平价", service.getPriceTypeByPoint(28));
        assertEquals("高峰", service.getPriceTypeByPoint(32));
        assertEquals("尖峰", service.getPriceTypeByPoint(76));
        assertEquals("平价", service.getPriceTypeByPoint(84));
    }

    @Test
    @DisplayName("测试时间点转换")
    void testGetTimeSlotByPoint() {
        assertEquals("00:00", service.getTimeSlotByPoint(0));
        assertEquals("00:15", service.getTimeSlotByPoint(1));
        assertEquals("01:00", service.getTimeSlotByPoint(4));
        assertEquals("12:00", service.getTimeSlotByPoint(48));
        assertEquals("23:45", service.getTimeSlotByPoint(95));
    }

    @Test
    @DisplayName("测试正常充电优化 - 全天充电")
    void testOptimizeCharging_FullDay() {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(95);
        request.setCurrentSoc(new BigDecimal("20"));
        request.setTargetSoc(new BigDecimal("80"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        ChargingResult result = service.optimizeCharging(request);
        
        assertNotNull(result);
        assertEquals("充电优化成功", result.getMessage());
        assertNotNull(result.getChargingPoints());
        assertFalse(result.getChargingPoints().isEmpty());
        
        assertEquals(0, new BigDecimal("36.0000").compareTo(result.getTotalEnergy()));
        assertTrue(result.getTotalCost().compareTo(BigDecimal.ZERO) > 0);
        
        for (ChargingResult.ChargingPoint point : result.getChargingPoints()) {
            assertEquals("低谷", point.getPriceType());
        }
    }

    @Test
    @DisplayName("测试充电优化 - 部分时段充电")
    void testOptimizeCharging_PartialTime() {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(40);
        request.setCurrentSoc(new BigDecimal("30"));
        request.setTargetSoc(new BigDecimal("70"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        ChargingResult result = service.optimizeCharging(request);
        
        assertNotNull(result);
        assertEquals("充电优化成功", result.getMessage());
        assertEquals(0, new BigDecimal("24.0000").compareTo(result.getTotalEnergy()));
        
        assertTrue(result.getActualStartPoint().intValue() >= 0);
        assertTrue(result.getActualEndPoint().intValue() <= 40);
    }

    @Test
    @DisplayName("测试充电优化 - 时间不足")
    void testOptimizeCharging_InsufficientTime() {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(32);
        request.setEndPoint(44);
        request.setCurrentSoc(new BigDecimal("30"));
        request.setTargetSoc(new BigDecimal("70"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        ChargingResult result = service.optimizeCharging(request);
        
        assertNotNull(result);
        assertTrue(result.getMessage().contains("充电时间不足"));
        assertTrue(result.getChargingPoints().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalEnergy()));
    }

    @Test
    @DisplayName("测试充电优化 - 无需充电")
    void testOptimizeCharging_NoNeedToCharge() {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(95);
        request.setCurrentSoc(new BigDecimal("80"));
        request.setTargetSoc(new BigDecimal("80"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        ChargingResult result = service.optimizeCharging(request);
        
        assertNotNull(result);
        assertTrue(result.getMessage().contains("无需充电"));
        assertTrue(result.getChargingPoints().isEmpty());
    }

    @Test
    @DisplayName("测试充电优化 - 当前电量超过目标")
    void testOptimizeCharging_CurrentExceedsTarget() {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(95);
        request.setCurrentSoc(new BigDecimal("90"));
        request.setTargetSoc(new BigDecimal("80"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        ChargingResult result = service.optimizeCharging(request);
        
        assertNotNull(result);
        assertTrue(result.getMessage().contains("无需充电"));
    }

    @Test
    @DisplayName("测试参数验证 - 开始点为空")
    void testValidateRequest_StartPointNull() {
        ChargingRequest request = new ChargingRequest();
        request.setEndPoint(95);
        request.setCurrentSoc(new BigDecimal("20"));
        request.setTargetSoc(new BigDecimal("80"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            service.optimizeCharging(request);
        });
    }

    @Test
    @DisplayName("测试参数验证 - 开始点超出范围")
    void testValidateRequest_StartPointOutOfRange() {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(-1);
        request.setEndPoint(95);
        request.setCurrentSoc(new BigDecimal("20"));
        request.setTargetSoc(new BigDecimal("80"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            service.optimizeCharging(request);
        });
    }

    @Test
    @DisplayName("测试参数验证 - 开始点大于结束点")
    void testValidateRequest_StartPointGreaterThanEndPoint() {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(50);
        request.setEndPoint(30);
        request.setCurrentSoc(new BigDecimal("20"));
        request.setTargetSoc(new BigDecimal("80"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            service.optimizeCharging(request);
        });
    }

    @Test
    @DisplayName("测试参数验证 - SOC超出范围")
    void testValidateRequest_SocOutOfRange() {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(95);
        request.setCurrentSoc(new BigDecimal("120"));
        request.setTargetSoc(new BigDecimal("80"));
        
        assertThrows(IllegalArgumentException.class, () -> {
            service.optimizeCharging(request);
        });
    }

    @Test
    @DisplayName("测试充电功率限制")
    void testChargingPowerLimit() {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(95);
        request.setCurrentSoc(new BigDecimal("20"));
        request.setTargetSoc(new BigDecimal("80"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("3.5"));
        
        ChargingResult result = service.optimizeCharging(request);
        
        assertNotNull(result);
        for (ChargingResult.ChargingPoint point : result.getChargingPoints()) {
            assertTrue(point.getPower().compareTo(new BigDecimal("3.5")) <= 0);
        }
    }

    @Test
    @DisplayName("测试充电成本计算正确性")
    void testChargingCostCalculation() {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(10);
        request.setCurrentSoc(new BigDecimal("50"));
        request.setTargetSoc(new BigDecimal("60"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        ChargingResult result = service.optimizeCharging(request);
        
        assertNotNull(result);
        
        BigDecimal expectedEnergy = new BigDecimal("6.0");
        assertEquals(0, expectedEnergy.compareTo(result.getTotalEnergy()));
        
        BigDecimal expectedCost = expectedEnergy.multiply(new BigDecimal("0.3"));
        assertEquals(0, expectedCost.compareTo(result.getTotalCost()));
    }

    @Test
    @DisplayName("测试低谷时段优先充电")
    void testValleyPeriodPriority() {
        ChargingRequest request = new ChargingRequest();
        request.setStartPoint(0);
        request.setEndPoint(95);
        request.setCurrentSoc(new BigDecimal("50"));
        request.setTargetSoc(new BigDecimal("70"));
        request.setBatteryCapacity(new BigDecimal("60"));
        request.setMaxChargingPower(new BigDecimal("7"));
        
        ChargingResult result = service.optimizeCharging(request);
        
        assertNotNull(result);
        
        for (ChargingResult.ChargingPoint point : result.getChargingPoints()) {
            assertEquals("低谷", point.getPriceType());
            assertEquals(0, new BigDecimal("0.3").compareTo(point.getPrice()));
        }
    }
}
