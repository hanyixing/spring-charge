package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterScheduleResult {
    
    private List<PowerSchedule> deviceSchedules;
    private double totalCost;
    private double totalDemandMet;
    private List<Double> clusterPower;
    private List<Double> clusterSoc;
    private List<Double> demandList;
    private List<Double> priceList;
    private boolean feasible;
    private String message;
}
