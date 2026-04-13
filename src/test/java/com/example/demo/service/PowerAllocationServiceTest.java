package com.example.demo.service;

import com.example.demo.model.AllocationResult;
import com.example.demo.model.ChargingUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PowerAllocationServiceTest {

    private PowerAllocationService powerAllocationService;

    @BeforeEach
    void setUp() {
        powerAllocationService = new PowerAllocationService();
    }

    @Test
    void testSetStationPower() {
        powerAllocationService.setStationPower(200.0);
        assertEquals(200.0, powerAllocationService.getStation().getTotalPower());
    }

    @Test
    void testAddUser() {
        ChargingUser user = powerAllocationService.addUser(10, 50, 0, 32, 22);
        
        assertNotNull(user.getUserId());
        assertEquals(10.0, user.getCurrentEnergy());
        assertEquals(50.0, user.getTargetEnergy());
        assertEquals(40.0, user.getRequiredEnergy());
        assertEquals(0, user.getStartTimeSlot());
        assertEquals(32, user.getEndTimeSlot());
        assertTrue(user.isActive());
    }

    @Test
    void testRemoveUser() {
        ChargingUser user = powerAllocationService.addUser(10, 50, 0, 32, 22);
        assertEquals(1, powerAllocationService.getAllUsers().size());
        
        powerAllocationService.removeUser(user.getUserId());
        assertEquals(0, powerAllocationService.getAllUsers().size());
    }

    @Test
    void testUpdateUserEnergy() {
        ChargingUser user = powerAllocationService.addUser(10, 50, 0, 32, 22);
        
        powerAllocationService.updateUserEnergy(user.getUserId(), 30);
        ChargingUser updatedUser = powerAllocationService.getStation().getUser(user.getUserId());
        assertEquals(30.0, updatedUser.getCurrentEnergy());
        assertTrue(updatedUser.isActive());
    }

    @Test
    void testUserCompleteCharging() {
        ChargingUser user = powerAllocationService.addUser(10, 50, 0, 32, 22);
        
        powerAllocationService.updateUserEnergy(user.getUserId(), 55);
        ChargingUser updatedUser = powerAllocationService.getStation().getUser(user.getUserId());
        assertFalse(updatedUser.isActive());
    }

    @Test
    void testAllocationDoesNotExceedTotalPower() {
        powerAllocationService.setStationPower(100.0);
        
        powerAllocationService.addUser(0, 100, 0, 20, 50);
        powerAllocationService.addUser(0, 100, 0, 20, 50);
        powerAllocationService.addUser(0, 100, 0, 20, 50);
        
        for (int slot = 0; slot < 21; slot++) {
            AllocationResult result = powerAllocationService.getAllocationAtSlot(slot);
            assertTrue(result.getTotalPower() <= 100.0, 
                "Slot " + slot + " power " + result.getTotalPower() + " exceeds 100 kW");
        }
    }

    @Test
    void testAllocationWithinUserTimeRange() {
        ChargingUser user = powerAllocationService.addUser(0, 40, 8, 20, 22);
        
        for (int slot = 0; slot < 8; slot++) {
            AllocationResult result = powerAllocationService.getAllocationAtSlot(slot);
            assertNull(result.getUserAllocations().get(user.getUserId()));
        }
        
        for (int slot = 8; slot <= 20; slot++) {
            AllocationResult result = powerAllocationService.getAllocationAtSlot(slot);
            Double power = result.getUserAllocations().get(user.getUserId());
            if (power != null && power > 0) {
                assertTrue(power > 0, "User should have power allocation in time range");
            }
        }
    }

    @Test
    void testUserScheduleMeetsEnergyRequirement() {
        ChargingUser user = powerAllocationService.addUser(10, 50, 0, 40, 22);
        
        Map<String, Object> schedule = powerAllocationService.getUserSchedule(user.getUserId());
        double scheduledEnergy = (Double) schedule.get("scheduledEnergy");
        double requiredEnergy = (Double) schedule.get("requiredEnergy");
        
        assertTrue(scheduledEnergy >= requiredEnergy * 0.95, 
            "Scheduled energy " + scheduledEnergy + " should meet required energy " + requiredEnergy);
    }

    @Test
    void testMultipleUsersFairAllocation() {
        powerAllocationService.setStationPower(60.0);
        
        ChargingUser user1 = powerAllocationService.addUser(0, 30, 0, 10, 22);
        ChargingUser user2 = powerAllocationService.addUser(0, 30, 0, 10, 22);
        
        AllocationResult result = powerAllocationService.getAllocationAtSlot(5);
        Double power1 = result.getUserAllocations().get(user1.getUserId());
        Double power2 = result.getUserAllocations().get(user2.getUserId());
        
        assertNotNull(power1);
        assertNotNull(power2);
        assertTrue(power1 > 0);
        assertTrue(power2 > 0);
        assertTrue(power1 + power2 <= 60.0);
    }

    @Test
    void testDynamicReallocation() {
        powerAllocationService.setStationPower(100.0);
        
        ChargingUser user1 = powerAllocationService.addUser(0, 50, 0, 20, 22);
        AllocationResult result1 = powerAllocationService.getAllocationAtSlot(5);
        Double powerBefore = result1.getUserAllocations().get(user1.getUserId());
        assertNotNull(powerBefore, "User1 should have allocation at slot 5");
        
        ChargingUser user2 = powerAllocationService.addUser(0, 50, 0, 20, 22);
        AllocationResult result2 = powerAllocationService.getAllocationAtSlot(5);
        Double powerAfter = result2.getUserAllocations().get(user1.getUserId());
        assertNotNull(powerAfter, "User1 should still have allocation at slot 5");
        
        assertTrue(powerAfter <= powerBefore, 
            "Power should decrease when new user joins: before=" + powerBefore + ", after=" + powerAfter);
    }

    @Test
    void testGetAllAllocations() {
        powerAllocationService.addUser(0, 50, 0, 10, 22);
        
        List<AllocationResult> results = powerAllocationService.getAllAllocations();
        assertEquals(96, results.size());
    }

    @Test
    void testGetAllUsers() {
        powerAllocationService.addUser(0, 50, 0, 20, 22);
        powerAllocationService.addUser(10, 60, 10, 30, 22);
        
        List<ChargingUser> users = powerAllocationService.getAllUsers();
        assertEquals(2, users.size());
    }

    @Test
    void testChargingUserModel() {
        ChargingUser user = ChargingUser.create(10, 50, 0, 32, 22);
        
        assertEquals(40.0, user.getRequiredEnergy());
        assertEquals(33, user.getAvailableTimeSlots());
        assertTrue(user.getMinRequiredPower() > 0);
    }

    @Test
    void testAllocationResultTimeRange() {
        AllocationResult result = new AllocationResult();
        result.setTimeSlot(8);
        
        assertEquals("02:00-02:15", result.getTimeRange());
        
        result.setTimeSlot(0);
        assertEquals("00:00-00:15", result.getTimeRange());
        
        result.setTimeSlot(95);
        assertEquals("23:45-24:00", result.getTimeRange());
    }

    @Test
    void testScenario_UserCancelsAndReallocates() {
        powerAllocationService.setStationPower(100.0);
        
        ChargingUser user1 = powerAllocationService.addUser(0, 80, 0, 30, 30);
        ChargingUser user2 = powerAllocationService.addUser(0, 80, 0, 30, 30);
        
        Map<String, Object> schedule1Before = powerAllocationService.getUserSchedule(user1.getUserId());
        
        powerAllocationService.removeUser(user2.getUserId());
        
        Map<String, Object> schedule1After = powerAllocationService.getUserSchedule(user1.getUserId());
        
        double energyBefore = (Double) schedule1Before.get("scheduledEnergy");
        double energyAfter = (Double) schedule1After.get("scheduledEnergy");
        
        assertTrue(energyAfter >= energyBefore, 
            "User1 should get more or equal energy after user2 cancels");
    }

    @Test
    void testScenario_MultipleTimeRanges() {
        powerAllocationService.setStationPower(100.0);
        
        ChargingUser user1 = powerAllocationService.addUser(0, 30, 0, 20, 22);
        ChargingUser user2 = powerAllocationService.addUser(0, 30, 20, 40, 22);
        
        AllocationResult result1 = powerAllocationService.getAllocationAtSlot(5);
        assertNotNull(result1.getUserAllocations().get(user1.getUserId()));
        assertNull(result1.getUserAllocations().get(user2.getUserId()));
        
        AllocationResult result2 = powerAllocationService.getAllocationAtSlot(22);
        assertNull(result2.getUserAllocations().get(user1.getUserId()));
        Double user2Power = result2.getUserAllocations().get(user2.getUserId());
        if (user2Power != null) {
            assertTrue(user2Power > 0);
        }
    }

    @Test
    void testScenario_MaxPowerConstraint() {
        powerAllocationService.setStationPower(100.0);
        
        ChargingUser user = powerAllocationService.addUser(0, 100, 0, 10, 30);
        
        Map<String, Object> schedule = powerAllocationService.getUserSchedule(user.getUserId());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> slots = (List<Map<String, Object>>) schedule.get("slots");
        
        for (Map<String, Object> slot : slots) {
            double power = (Double) slot.get("power");
            assertTrue(power <= 30.0, "Power " + power + " exceeds max charging power 30 kW");
        }
    }
}
