package com.example.demo.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ChargingResult {
    
    private List<ChargingPoint> chargingPoints;
    
    private BigDecimal totalEnergy;
    
    private BigDecimal totalCost;
    
    private Integer chargingDuration;
    
    private BigDecimal actualStartPoint;
    
    private BigDecimal actualEndPoint;
    
    private String message;

    @Data
    public static class ChargingPoint {
        private Integer pointIndex;
        private String timeSlot;
        private BigDecimal price;
        private String priceType;
        private BigDecimal power;
        private BigDecimal energy;
        private BigDecimal cost;

        public ChargingPoint(Integer pointIndex, String timeSlot, BigDecimal price, 
                            String priceType, BigDecimal power, BigDecimal energy, BigDecimal cost) {
            this.pointIndex = pointIndex;
            this.timeSlot = timeSlot;
            this.price = price;
            this.priceType = priceType;
            this.power = power;
            this.energy = energy;
            this.cost = cost;
        }
    }
}
