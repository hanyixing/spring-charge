package com.example.charge.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ChargeRecommendationResponse {
    private String userId;
    private BigDecimal initialSOC;
    private BigDecimal finalSOC;
    private List<ChargePoint> recommendedCurve;
    private BigDecimal totalCost;
    private BigDecimal totalEnergy;
    private BigDecimal peakLoadReduction;
    private String recommendationReason;
}
