package com.example.demo.service;

import com.example.demo.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ChargingRewardService {
    
    private Map<Integer, TimeSlotConfig> timeSlotConfigs = new HashMap<>();
    
    private double rewardRatePerKwh = 0.5;
    
    @PostConstruct
    public void init() {
        initializeTimeSlots();
        log.info("充电奖励服务初始化完成，共配置{}个时间段", timeSlotConfigs.size());
    }
    
    private void initializeTimeSlots() {
        for (int i = 0; i < TimeSlotConfig.TOTAL_SLOTS; i++) {
            int startMinutes = i * TimeSlotConfig.SLOT_DURATION_MINUTES;
            int startHour = startMinutes / 60;
            int startMin = startMinutes % 60;
            int endMinutes = startMinutes + TimeSlotConfig.SLOT_DURATION_MINUTES;
            int endHour = endMinutes / 60;
            int endMin = endMinutes % 60;
            if (endHour >= 24) {
                endHour = 0;
            }
            
            String startTime = String.format("%02d:%02d", startHour, startMin);
            String endTime = String.format("%02d:%02d", endHour, endMin);
            
            String slotType = determineSlotType(startHour, startMin);
            double rewardRate = TimeSlotConfig.SLOT_TYPE_VALLEY.equals(slotType) ? rewardRatePerKwh : 0.0;
            
            TimeSlotConfig config = TimeSlotConfig.builder()
                    .slotIndex(i)
                    .startTime(startTime)
                    .endTime(endTime)
                    .slotType(slotType)
                    .rewardRate(rewardRate)
                    .build();
            
            timeSlotConfigs.put(i, config);
        }
    }
    
    private String determineSlotType(int hour, int minute) {
        if (hour >= 23 || hour < 7) {
            return TimeSlotConfig.SLOT_TYPE_VALLEY;
        } else if ((hour >= 8 && hour < 11) || (hour >= 18 && hour < 23)) {
            return TimeSlotConfig.SLOT_TYPE_PEAK;
        } else {
            return TimeSlotConfig.SLOT_TYPE_NORMAL;
        }
    }
    
    public int getTimeSlotIndex(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        int totalMinutes = time.getHour() * 60 + time.getMinute();
        return totalMinutes / TimeSlotConfig.SLOT_DURATION_MINUTES;
    }
    
    public TimeSlotConfig getTimeSlotConfig(int slotIndex) {
        return timeSlotConfigs.get(slotIndex);
    }
    
    public TimeSlotConfig getTimeSlotConfig(LocalDateTime dateTime) {
        int slotIndex = getTimeSlotIndex(dateTime);
        return timeSlotConfigs.get(slotIndex);
    }
    
    public ChargingRecord calculateReward(String userId, double chargingKwh, LocalDateTime chargingTime) {
        int slotIndex = getTimeSlotIndex(chargingTime);
        TimeSlotConfig config = timeSlotConfigs.get(slotIndex);
        
        double rewardAmount = chargingKwh * config.getRewardRate();
        
        return ChargingRecord.builder()
                .userId(userId)
                .chargingKwh(chargingKwh)
                .chargingStartTime(chargingTime)
                .chargingEndTime(chargingTime.plusMinutes(15))
                .timeSlotIndex(slotIndex)
                .rewardAmount(rewardAmount)
                .timeSlotType(config.getSlotType())
                .build();
    }
    
    public ChargingSession calculateSessionReward(String userId, LocalDateTime startTime, LocalDateTime endTime, double chargingPowerKw) {
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("结束时间不能早于开始时间");
        }
        
        if (chargingPowerKw <= 0) {
            throw new IllegalArgumentException("充电功率必须大于0");
        }
        
        String sessionId = UUID.randomUUID().toString();
        
        long totalMinutes = Duration.between(startTime, endTime).toMinutes();
        double totalChargingKwh = (totalMinutes / 60.0) * chargingPowerKw;
        
        List<SlotChargingDetail> slotDetails = calculateSlotDetails(startTime, endTime, chargingPowerKw);
        
        double totalRewardAmount = 0.0;
        double valleyChargingKwh = 0.0;
        double peakChargingKwh = 0.0;
        double normalChargingKwh = 0.0;
        int valleySlots = 0;
        int peakSlots = 0;
        int normalSlots = 0;
        
        for (SlotChargingDetail detail : slotDetails) {
            totalRewardAmount += detail.getRewardAmount();
            
            switch (detail.getSlotType()) {
                case TimeSlotConfig.SLOT_TYPE_VALLEY:
                    valleyChargingKwh += detail.getChargingKwh();
                    valleySlots++;
                    break;
                case TimeSlotConfig.SLOT_TYPE_PEAK:
                    peakChargingKwh += detail.getChargingKwh();
                    peakSlots++;
                    break;
                case TimeSlotConfig.SLOT_TYPE_NORMAL:
                    normalChargingKwh += detail.getChargingKwh();
                    normalSlots++;
                    break;
            }
        }
        
        return ChargingSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .startTime(startTime)
                .endTime(endTime)
                .chargingPowerKw(chargingPowerKw)
                .totalChargingKwh(totalChargingKwh)
                .totalRewardAmount(totalRewardAmount)
                .totalSlots(slotDetails.size())
                .valleySlots(valleySlots)
                .peakSlots(peakSlots)
                .normalSlots(normalSlots)
                .valleyChargingKwh(valleyChargingKwh)
                .peakChargingKwh(peakChargingKwh)
                .normalChargingKwh(normalChargingKwh)
                .build();
    }
    
    public List<SlotChargingDetail> calculateSlotDetails(LocalDateTime startTime, LocalDateTime endTime, double chargingPowerKw) {
        List<SlotChargingDetail> details = new ArrayList<>();
        
        LocalDateTime current = startTime;
        
        while (current.isBefore(endTime)) {
            int currentSlotIndex = getTimeSlotIndex(current);
            TimeSlotConfig config = timeSlotConfigs.get(currentSlotIndex);
            
            LocalDateTime slotEnd = current.withHour(0).withMinute(0).withSecond(0)
                    .plusMinutes((long) (currentSlotIndex + 1) * TimeSlotConfig.SLOT_DURATION_MINUTES);
            
            if (slotEnd.isAfter(endTime)) {
                slotEnd = endTime;
            }
            
            long minutesInSlot = Duration.between(current, slotEnd).toMinutes();
            double kwhInSlot = (minutesInSlot / 60.0) * chargingPowerKw;
            double rewardInSlot = kwhInSlot * config.getRewardRate();
            
            SlotChargingDetail detail = SlotChargingDetail.builder()
                    .slotIndex(currentSlotIndex)
                    .slotStartTime(current)
                    .slotEndTime(slotEnd)
                    .slotType(config.getSlotType())
                    .rewardRate(config.getRewardRate())
                    .chargingKwh(kwhInSlot)
                    .rewardAmount(rewardInSlot)
                    .chargingMinutes(minutesInSlot)
                    .build();
            
            details.add(detail);
            
            current = slotEnd;
        }
        
        return details;
    }
    
    public RewardResult calculateBatchRewards(String userId, List<ChargingRecord> chargingRecords) {
        double totalChargingKwh = 0.0;
        double totalRewardAmount = 0.0;
        double valleyChargingKwh = 0.0;
        double peakChargingKwh = 0.0;
        double normalChargingKwh = 0.0;
        
        List<ChargingRecord> processedRecords = new ArrayList<>();
        
        for (ChargingRecord record : chargingRecords) {
            ChargingRecord processed = calculateReward(
                    userId, 
                    record.getChargingKwh(), 
                    record.getChargingStartTime()
            );
            processedRecords.add(processed);
            
            totalChargingKwh += processed.getChargingKwh();
            totalRewardAmount += processed.getRewardAmount();
            
            switch (processed.getTimeSlotType()) {
                case TimeSlotConfig.SLOT_TYPE_VALLEY:
                    valleyChargingKwh += processed.getChargingKwh();
                    break;
                case TimeSlotConfig.SLOT_TYPE_PEAK:
                    peakChargingKwh += processed.getChargingKwh();
                    break;
                case TimeSlotConfig.SLOT_TYPE_NORMAL:
                    normalChargingKwh += processed.getChargingKwh();
                    break;
            }
        }
        
        return RewardResult.builder()
                .userId(userId)
                .totalChargingKwh(totalChargingKwh)
                .totalRewardAmount(totalRewardAmount)
                .valleyChargingKwh(valleyChargingKwh)
                .peakChargingKwh(peakChargingKwh)
                .normalChargingKwh(normalChargingKwh)
                .chargingRecords(processedRecords)
                .build();
    }
    
    public List<TimeSlotConfig> getAllTimeSlotConfigs() {
        List<TimeSlotConfig> configs = new ArrayList<>();
        for (int i = 0; i < TimeSlotConfig.TOTAL_SLOTS; i++) {
            configs.add(timeSlotConfigs.get(i));
        }
        return configs;
    }
    
    public void setRewardRatePerKwh(double rate) {
        this.rewardRatePerKwh = rate;
        initializeTimeSlots();
        log.info("奖励费率已更新为: {}元/kWh", rate);
    }
    
    public double getRewardRatePerKwh() {
        return rewardRatePerKwh;
    }
}
