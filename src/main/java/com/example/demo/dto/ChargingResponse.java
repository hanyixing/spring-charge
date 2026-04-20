package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargingResponse {

    private boolean success;
    private String message;
    private Double totalEnergy;
    private Double totalCost;
    private Integer chargingPoints;
    private List<ChargingPoint> chargingSchedule;
    private Double actualStartSoc;
    private Double actualEndSoc;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargingPoint {
        private Integer point;
        private Integer hour;
        private Integer minute;
        private String period;
        private Double price;
        private Double power;
        private Double energy;
        private Double cost;
        private Double soc;
    }
}
