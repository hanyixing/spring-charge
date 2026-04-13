package com.example.demo.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ChargeRewardResult {
    private String userId;
    private BigDecimal totalReward;
    private BigDecimal rewardedEnergy;
    private BigDecimal nonRewardedEnergy;
    private List<TimeSlotReward> timeSlotRewards;

    @Data
    public static class TimeSlotReward {
        private String date;
        private int timeSlotIndex;
        private String timeRange;
        private boolean isRewardPeriod;
        private BigDecimal energy;
        private BigDecimal reward;
    }
}
