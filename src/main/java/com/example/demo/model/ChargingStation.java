package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChargingStation {
    private String stationId;
    private double totalPower;
    private int totalTimeSlots = 96;
    private double[] powerSchedule;
    private Map<String, ChargingUser> users;
    private Map<Integer, Map<String, Double>> userPowerAllocation;
    
    public static ChargingStation create(String stationId, double totalPower) {
        ChargingStation station = new ChargingStation();
        station.setStationId(stationId);
        station.setTotalPower(totalPower);
        station.setPowerSchedule(new double[96]);
        station.setUsers(new HashMap<>());
        station.setUserPowerAllocation(new HashMap<>());
        for (int i = 0; i < 96; i++) {
            station.getUserPowerAllocation().put(i, new HashMap<>());
        }
        return station;
    }
    
    public void addUser(ChargingUser user) {
        users.put(user.getUserId(), user);
    }
    
    public void removeUser(String userId) {
        users.remove(userId);
        for (Map<String, Double> slotAllocation : userPowerAllocation.values()) {
            slotAllocation.remove(userId);
        }
    }
    
    public ChargingUser getUser(String userId) {
        return users.get(userId);
    }
    
    public List<ChargingUser> getActiveUsers() {
        List<ChargingUser> activeUsers = new ArrayList<>();
        for (ChargingUser user : users.values()) {
            if (user.isActive()) {
                activeUsers.add(user);
            }
        }
        return activeUsers;
    }
    
    public double getUsedPowerAtSlot(int slot) {
        double total = 0;
        Map<String, Double> allocation = userPowerAllocation.get(slot);
        if (allocation != null) {
            for (Double power : allocation.values()) {
                total += power;
            }
        }
        return total;
    }
    
    public double getAvailablePowerAtSlot(int slot) {
        return totalPower - getUsedPowerAtSlot(slot);
    }
}
