package com.example.demo.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class OptimizationResult {
    private double totalCost;
    private List<TimeSlotPower> timeSlotPowers;
    private Map<String, List<Double>> devicePowerPerSlot;
    private Map<String, List<Double>> deviceSOCHistory;
    private boolean demandSatisfied;
    private boolean socWithinLimits;
    private String message;

    public OptimizationResult() {
    }
}
