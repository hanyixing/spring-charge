package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotPower {
    
    private int slotIndex;
    @Builder.Default
    private Map<String, Double> userPowers = new HashMap<>();
    
    public double getTotalPower() {
        return userPowers.values().stream().mapToDouble(Double::doubleValue).sum();
    }
    
    public void setUserPower(String userId, double power) {
        userPowers.put(userId, power);
    }
    
    public Double getUserPower(String userId) {
        return userPowers.get(userId);
    }
    
    public void removeUser(String userId) {
        userPowers.remove(userId);
    }
}
