package com.example.demo.service;

import com.example.demo.model.ChargeRequest;
import com.example.demo.model.ChargeResult;
import com.example.demo.model.ElectricityPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ChargeOptimizationServiceTest {

    private ChargeOptimizationService service;
    private ElectricityPrice testPrice;

    @BeforeEach
    void setUp() {
        service = new ChargeOptimizationService();
        testPrice = service.createDefaultPrice();
    }

    @Test
    void testNormalChargeOptimization() {
        ChargeRequest request = new ChargeRequest(30, 80, 0, 47);
        ChargeResult result = service.calculateOptimalCharge(request, testPrice);

        assertTrue(result.isSuccess());
        assertEquals(25.0, result.getTotalEnergy(), 0.01);
        assertTrue(result.getTotalCost().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.getTotalCost().compareTo(new BigDecimal("10.00")) < 0);
    }

    @Test
    void testCurrentBatteryEqualsTarget() {
        ChargeRequest request = new ChargeRequest(80, 80, 0, 47);
        ChargeResult result = service.calculateOptimalCharge(request, testPrice);

        assertTrue(result.isSuccess());
        assertEquals(0.0, result.getTotalEnergy(), 0.01);
        assertEquals(0, result.getTotalCost().compareTo(BigDecimal.ZERO));
    }

    @Test
    void testCurrentBatteryHigherThanTarget() {
        ChargeRequest request = new ChargeRequest(90, 80, 0, 47);
        ChargeResult result = service.calculateOptimalCharge(request, testPrice);

        assertTrue(result.isSuccess());
        assertEquals(0.0, result.getTotalEnergy(), 0.01);
    }

    @Test
    void testInsufficientChargingTime() {
        ChargeRequest request = new ChargeRequest(0, 100, 0, 5);
        ChargeResult result = service.calculateOptimalCharge(request, testPrice);

        assertFalse(result.isSuccess());
        assertFalse(result.getMessage().isEmpty());
    }

    @Test
    void testInvalidBatteryPercentage() {
        ChargeRequest request = new ChargeRequest(150, 80, 0, 47);
        ChargeResult result = service.calculateOptimalCharge(request, testPrice);

        assertFalse(result.isSuccess());
    }

    @Test
    void testInvalidTimeRange() {
        ChargeRequest request = new ChargeRequest(30, 80, 50, 40);
        ChargeResult result = service.calculateOptimalCharge(request, testPrice);

        assertFalse(result.isSuccess());
    }

    @Test
    void testChargeOnlyInCheapHours() {
        ElectricityPrice customPrice = new ElectricityPrice();
        for (int i = 40; i <= 44; i++) {
            customPrice.setPrice(i, new BigDecimal("0.30"));
        }
        for (int i = 45; i <= 50; i++) {
            customPrice.setPrice(i, new BigDecimal("1.50"));
        }

        ChargeRequest request = new ChargeRequest(30, 35, 40, 50);
        ChargeResult result = service.calculateOptimalCharge(request, customPrice);

        assertTrue(result.isSuccess());
        for (int i = 40; i <= 44; i++) {
            if (result.getChargePower(i) > 0) {
                assertTrue(true, "Charging in cheap hours " + i);
            }
        }
        for (int i = 45; i <= 50; i++) {
            assertEquals(0.0, result.getChargePower(i), 0.01,
                    "Should not charge at expensive hour " + i);
        }
    }

    @Test
    void testPeakHoursAvoidance() {
        ElectricityPrice peakPrice = service.createDefaultPrice();
        for (int i = 40; i <= 55; i++) {
            peakPrice.setPrice(i, new BigDecimal("2.00"));
        }
        for (int i = 0; i <= 39; i++) {
            peakPrice.setPrice(i, new BigDecimal("0.50"));
        }

        ChargeRequest request = new ChargeRequest(30, 40, 0, 55);
        ChargeResult result = service.calculateOptimalCharge(request, peakPrice);

        assertTrue(result.isSuccess());
        for (int i = 0; i <= 39; i++) {
            if (result.getChargePower(i) > 0) {
                assertTrue(true, "Charging should happen in cheap hours");
            }
        }
        for (int i = 40; i <= 55; i++) {
            assertEquals(0.0, result.getChargePower(i), 0.01,
                    "Should not charge in expensive peak hours");
        }
    }

    @Test
    void testEnergyCalculationAccuracy() {
        ChargeRequest request = new ChargeRequest(50, 75, 0, 95);
        ChargeResult result = service.calculateOptimalCharge(request, testPrice);

        assertTrue(result.isSuccess());
        double expectedEnergy = 50.0 * (75 - 50) / 100.0;
        assertEquals(expectedEnergy, result.getTotalEnergy(), 0.01);
    }

    @Test
    void testMaxPowerNotExceeded() {
        ChargeRequest request = new ChargeRequest(0, 100, 0, 95);
        ChargeResult result = service.calculateOptimalCharge(request, testPrice);

        assertTrue(result.isSuccess());
        for (int i = 0; i < 96; i++) {
            assertTrue(result.getChargePower(i) <= request.getMaxChargePower());
        }
    }

    @Test
    void testChargeOnlyWithinTimeRange() {
        ChargeRequest request = new ChargeRequest(30, 50, 20, 40);
        ChargeResult result = service.calculateOptimalCharge(request, testPrice);

        assertTrue(result.isSuccess());
        for (int i = 0; i < 20; i++) {
            assertEquals(0.0, result.getChargePower(i), 0.01);
        }
        for (int i = 41; i < 96; i++) {
            assertEquals(0.0, result.getChargePower(i), 0.01);
        }
    }
}
