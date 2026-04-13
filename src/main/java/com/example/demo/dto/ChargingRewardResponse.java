package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChargingRewardResponse {
    private Integer startPoint;
    private Integer endPoint;
    private String startTime;
    private String endTime;
    private Double chargingKwh;
    private Double rewardPerKwh;
    private Double totalReward;
    private String timeSlotType;
    private Boolean isRewardPeriod;
    private Boolean isCrossDay;
    private Integer days;
    private Integer totalPoints;
    private Integer rewardPoints;
    private List<TimePointDetail> timePointDetails;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimePointDetail {
        private Integer point;
        private Integer day;
        private String timeRange;
        private String timeSlotType;
        private Boolean isRewardPeriod;
    }
}
