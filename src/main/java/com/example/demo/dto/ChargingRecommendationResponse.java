package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChargingRecommendationResponse {
    
    private String userId;
    
    private List<Double> recommendedCurve;
    
    private double totalCost;
    
    private double peakValleyScore;
    
    private double habitMatchScore;
    
    private double totalEnergy;
    
    private String algorithm;
    
    private long computationTimeMs;
    
    private double initialSoc;
    
    private double finalSoc;
    
    private List<Double> socCurve;
    
    private double socMinConstraint;
    
    private double socMaxConstraint;
}
