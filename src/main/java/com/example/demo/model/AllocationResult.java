package com.example.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllocationResult {
    private int timeSlot;
    private double totalPower;
    private Map<String, Double> userAllocations;
    private double remainingPower;
    
    public String getTimeRange() {
        int startHour = timeSlot / 4;
        int startMinute = (timeSlot % 4) * 15;
        int endMinute = startMinute + 15;
        int endHour = startHour;
        if (endMinute >= 60) {
            endMinute = 0;
            endHour++;
        }
        return String.format("%02d:%02d-%02d:%02d", startHour, startMinute, endHour, endMinute);
    }
}
