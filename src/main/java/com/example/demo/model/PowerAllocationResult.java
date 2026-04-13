package com.example.demo.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class PowerAllocationResult {
    private boolean success;
    private String message;
    private Map<String, List<Double>> userPowerProfile;
    private List<Double> stationTotalPower;
    private double maxStationPower;
    private Map<String, Double> userTotalEnergy;

    public PowerAllocationResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
