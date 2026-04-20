package com.example.charge.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ChargeRecommendationRequest {
    private String userId;
    private BigDecimal currentSOC;
    private BigDecimal targetSOC;
    private BigDecimal batteryCapacity;
    private List<List<BigDecimal>> historicalData;
    private int earliestStart;
    private int latestEnd;
    private BigDecimal maxPower;
}
