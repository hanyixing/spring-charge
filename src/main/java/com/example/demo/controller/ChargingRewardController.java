package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.ChargingRewardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/charging")
public class ChargingRewardController {
    
    @Autowired
    private ChargingRewardService chargingRewardService;
    
    @GetMapping("/timeslots")
    public Map<String, Object> getAllTimeSlots() {
        log.info("获取所有时间段配置");
        
        List<TimeSlotConfig> configs = chargingRewardService.getAllTimeSlotConfigs();
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalSlots", configs.size());
        response.put("rewardRatePerKwh", chargingRewardService.getRewardRatePerKwh());
        response.put("timeslots", configs);
        
        return response;
    }
    
    @GetMapping("/timeslots/valley")
    public Map<String, Object> getValleyTimeSlots() {
        log.info("获取谷时时间段（奖励时段）");
        
        List<TimeSlotConfig> allConfigs = chargingRewardService.getAllTimeSlotConfigs();
        List<TimeSlotConfig> valleySlots = new ArrayList<>();
        
        for (TimeSlotConfig config : allConfigs) {
            if (TimeSlotConfig.SLOT_TYPE_VALLEY.equals(config.getSlotType())) {
                valleySlots.add(config);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("valleySlotCount", valleySlots.size());
        response.put("rewardRatePerKwh", chargingRewardService.getRewardRatePerKwh());
        response.put("valleyTimeslots", valleySlots);
        
        return response;
    }
    
    @PostMapping("/calculate")
    public Map<String, Object> calculateReward(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        double chargingKwh = ((Number) request.get("chargingKwh")).doubleValue();
        String chargingTimeStr = (String) request.get("chargingTime");
        
        LocalDateTime chargingTime = LocalDateTime.parse(chargingTimeStr);
        
        log.info("计算充电奖励 - 用户: {}, 充电量: {}kWh, 时间: {}", userId, chargingKwh, chargingTime);
        
        ChargingRecord record = chargingRewardService.calculateReward(userId, chargingKwh, chargingTime);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("record", record);
        
        return response;
    }
    
    @PostMapping("/session")
    public Map<String, Object> calculateSessionReward(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        String startTimeStr = (String) request.get("startTime");
        String endTimeStr = (String) request.get("endTime");
        double chargingPowerKw = ((Number) request.get("chargingPowerKw")).doubleValue();
        
        LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
        LocalDateTime endTime = LocalDateTime.parse(endTimeStr);
        
        log.info("计算充电会话奖励 - 用户: {}, 开始: {}, 结束: {}, 功率: {}kW", 
                userId, startTime, endTime, chargingPowerKw);
        
        ChargingSession session = chargingRewardService.calculateSessionReward(userId, startTime, endTime, chargingPowerKw);
        
        List<SlotChargingDetail> slotDetails = chargingRewardService.calculateSlotDetails(startTime, endTime, chargingPowerKw);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("session", session);
        response.put("slotDetails", slotDetails);
        
        return response;
    }
    
    @PostMapping("/calculate/batch")
    public Map<String, Object> calculateBatchRewards(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        List<Map<String, Object>> chargingList = (List<Map<String, Object>>) request.get("chargingList");
        
        log.info("批量计算充电奖励 - 用户: {}, 记录数: {}", userId, chargingList.size());
        
        List<ChargingRecord> records = new ArrayList<>();
        for (Map<String, Object> item : chargingList) {
            double kwh = ((Number) item.get("chargingKwh")).doubleValue();
            String timeStr = (String) item.get("chargingTime");
            LocalDateTime time = LocalDateTime.parse(timeStr);
            
            ChargingRecord record = ChargingRecord.builder()
                    .chargingKwh(kwh)
                    .chargingStartTime(time)
                    .build();
            records.add(record);
        }
        
        RewardResult result = chargingRewardService.calculateBatchRewards(userId, records);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("result", result);
        
        return response;
    }
    
    @PostMapping("/reward-rate")
    public Map<String, Object> setRewardRate(@RequestBody Map<String, Object> request) {
        double rate = ((Number) request.get("rate")).doubleValue();
        
        log.info("设置奖励费率: {}元/kWh", rate);
        
        chargingRewardService.setRewardRatePerKwh(rate);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("newRate", rate);
        response.put("message", "奖励费率已更新");
        
        return response;
    }
    
    @GetMapping("/test")
    public Map<String, Object> testAlgorithm() {
        log.info("执行算法测试");
        
        String userId = "TEST_USER_001";
        List<ChargingRecord> testRecords = new ArrayList<>();
        
        testRecords.add(ChargingRecord.builder()
                .chargingKwh(10.0)
                .chargingStartTime(LocalDateTime.of(2024, 1, 1, 0, 0))
                .build());
        
        testRecords.add(ChargingRecord.builder()
                .chargingKwh(15.0)
                .chargingStartTime(LocalDateTime.of(2024, 1, 1, 3, 30))
                .build());
        
        testRecords.add(ChargingRecord.builder()
                .chargingKwh(20.0)
                .chargingStartTime(LocalDateTime.of(2024, 1, 1, 9, 0))
                .build());
        
        testRecords.add(ChargingRecord.builder()
                .chargingKwh(12.0)
                .chargingStartTime(LocalDateTime.of(2024, 1, 1, 14, 0))
                .build());
        
        testRecords.add(ChargingRecord.builder()
                .chargingKwh(18.0)
                .chargingStartTime(LocalDateTime.of(2024, 1, 1, 19, 0))
                .build());
        
        testRecords.add(ChargingRecord.builder()
                .chargingKwh(25.0)
                .chargingStartTime(LocalDateTime.of(2024, 1, 1, 23, 30))
                .build());
        
        RewardResult result = chargingRewardService.calculateBatchRewards(userId, testRecords);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("testDescription", "测试充电奖励算法：包含谷时、峰时、平时充电");
        response.put("rewardRatePerKwh", chargingRewardService.getRewardRatePerKwh());
        response.put("result", result);
        
        return response;
    }
    
    @GetMapping("/test/cross-slot")
    public Map<String, Object> testCrossSlotCharging() {
        log.info("测试跨时间段充电场景");
        
        String userId = "TEST_USER_CROSS_SLOT";
        List<Map<String, Object>> testCases = new ArrayList<>();
        
        LocalDateTime startTime1 = LocalDateTime.of(2024, 1, 1, 6, 45);
        LocalDateTime endTime1 = LocalDateTime.of(2024, 1, 1, 7, 30);
        double power1 = 10.0;
        ChargingSession session1 = chargingRewardService.calculateSessionReward(userId, startTime1, endTime1, power1);
        List<SlotChargingDetail> details1 = chargingRewardService.calculateSlotDetails(startTime1, endTime1, power1);
        
        Map<String, Object> case1 = new HashMap<>();
        case1.put("caseName", "跨谷时到平时（6:45-7:30）");
        case1.put("startTime", startTime1.toString());
        case1.put("endTime", endTime1.toString());
        case1.put("chargingPowerKw", power1);
        case1.put("session", session1);
        case1.put("slotDetails", details1);
        testCases.add(case1);
        
        LocalDateTime startTime2 = LocalDateTime.of(2024, 1, 1, 22, 30);
        LocalDateTime endTime2 = LocalDateTime.of(2024, 1, 1, 23, 30);
        double power2 = 15.0;
        ChargingSession session2 = chargingRewardService.calculateSessionReward(userId, startTime2, endTime2, power2);
        List<SlotChargingDetail> details2 = chargingRewardService.calculateSlotDetails(startTime2, endTime2, power2);
        
        Map<String, Object> case2 = new HashMap<>();
        case2.put("caseName", "跨峰时到谷时（22:30-23:30）");
        case2.put("startTime", startTime2.toString());
        case2.put("endTime", endTime2.toString());
        case2.put("chargingPowerKw", power2);
        case2.put("session", session2);
        case2.put("slotDetails", details2);
        testCases.add(case2);
        
        LocalDateTime startTime3 = LocalDateTime.of(2024, 1, 1, 23, 0);
        LocalDateTime endTime3 = LocalDateTime.of(2024, 1, 2, 1, 0);
        double power3 = 20.0;
        ChargingSession session3 = chargingRewardService.calculateSessionReward(userId, startTime3, endTime3, power3);
        List<SlotChargingDetail> details3 = chargingRewardService.calculateSlotDetails(startTime3, endTime3, power3);
        
        Map<String, Object> case3 = new HashMap<>();
        case3.put("caseName", "跨天充电（23:00-次日01:00）");
        case3.put("startTime", startTime3.toString());
        case3.put("endTime", endTime3.toString());
        case3.put("chargingPowerKw", power3);
        case3.put("session", session3);
        case3.put("slotDetails", details3);
        testCases.add(case3);
        
        LocalDateTime startTime4 = LocalDateTime.of(2024, 1, 1, 10, 30);
        LocalDateTime endTime4 = LocalDateTime.of(2024, 1, 1, 14, 30);
        double power4 = 8.0;
        ChargingSession session4 = chargingRewardService.calculateSessionReward(userId, startTime4, endTime4, power4);
        List<SlotChargingDetail> details4 = chargingRewardService.calculateSlotDetails(startTime4, endTime4, power4);
        
        Map<String, Object> case4 = new HashMap<>();
        case4.put("caseName", "跨峰时到平时（10:30-14:30）");
        case4.put("startTime", startTime4.toString());
        case4.put("endTime", endTime4.toString());
        case4.put("chargingPowerKw", power4);
        case4.put("session", session4);
        case4.put("slotDetails", details4);
        testCases.add(case4);
        
        LocalDateTime startTime5 = LocalDateTime.of(2024, 1, 1, 5, 0);
        LocalDateTime endTime5 = LocalDateTime.of(2024, 1, 1, 10, 0);
        double power5 = 12.0;
        ChargingSession session5 = chargingRewardService.calculateSessionReward(userId, startTime5, endTime5, power5);
        List<SlotChargingDetail> details5 = chargingRewardService.calculateSlotDetails(startTime5, endTime5, power5);
        
        Map<String, Object> case5 = new HashMap<>();
        case5.put("caseName", "跨谷时到峰时（5:00-10:00）");
        case5.put("startTime", startTime5.toString());
        case5.put("endTime", endTime5.toString());
        case5.put("chargingPowerKw", power5);
        case5.put("session", session5);
        case5.put("slotDetails", details5);
        testCases.add(case5);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("testDescription", "测试跨时间段和跨天充电场景");
        response.put("rewardRatePerKwh", chargingRewardService.getRewardRatePerKwh());
        response.put("testCases", testCases);
        
        return response;
    }
}
