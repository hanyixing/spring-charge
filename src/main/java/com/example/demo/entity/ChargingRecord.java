package com.example.demo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChargingRecord {
    private String userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double energyKwh;
    private double rewardAmount;
    private LocalDateTime createTime;
}
