package com.example.demo.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RewardResult {
    private String userId;
    private double totalEnergy;
    private double totalReward;
    private double rewardRate;
    private List<Map<String, Object>> pointDetails;
    private String message;
}
