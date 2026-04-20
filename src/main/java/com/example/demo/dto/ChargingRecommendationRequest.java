package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChargingRecommendationRequest {
    
    private String userId;
    
    private List<Double> historicalCurve;
    
    private List<Double> priceCurve;
    
    private double totalDemand;
    
    private double maxPower;
    
    private double minPower;
    
    private List<Double> gridLoadCurve;
    
    private double currentSoc;
    
    private double targetSoc;
    
    private double batteryCapacity;
    
    private double socMin;
    
    private double socMax;
    
    private int availableTimeSlots;
}
