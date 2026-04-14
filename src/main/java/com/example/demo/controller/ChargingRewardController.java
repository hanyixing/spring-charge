package com.example.demo.controller;

import com.example.demo.entity.ChargingPoint;
import com.example.demo.entity.ChargingRecord;
import com.example.demo.entity.RewardConfig;
import com.example.demo.entity.RewardResult;
import com.example.demo.service.ChargingRewardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/charging")
public class ChargingRewardController {

    @Autowired
    private ChargingRewardService chargingRewardService;
    
    @Autowired
    private RewardConfig rewardConfig;

    @PostMapping("/reward")
    public Map<String, Object> calculateReward(
            @RequestParam String userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam double energyKwh) {
        
        log.info("收到充电奖励计算请求: userId={}, startTime={}, endTime={}, energyKwh={}", 
            userId, startTime, endTime, energyKwh);
        
        RewardResult result = chargingRewardService.calculateRewardWithDetails(userId, startTime, endTime, energyKwh);
        ChargingRecord record = chargingRewardService.createChargingRecord(userId, startTime, endTime, energyKwh);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", result);
        response.put("record", record);
        
        return response;
    }

    @GetMapping("/points")
    public Map<String, Object> getAllPoints() {
        List<ChargingPoint> points = chargingRewardService.getChargingPoints();
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("data", points);
        
        return response;
    }

    @GetMapping("/reward-periods")
    public Map<String, Object> getRewardPeriods() {
        List<ChargingPoint> rewardPeriods = chargingRewardService.getRewardPeriods();
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("rewardRate", rewardConfig.getRewardRate());
        response.put("valleyPeriods", rewardConfig.getValleyPeriods());
        response.put("peakPeriods", rewardConfig.getPeakPeriods());
        response.put("data", rewardPeriods);
        
        return response;
    }

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "success");
        response.put("rewardRate", rewardConfig.getRewardRate());
        response.put("valleyPeriods", rewardConfig.getValleyPeriods());
        response.put("peakPeriods", rewardConfig.getPeakPeriods());
        
        return response;
    }

    @PostMapping("/simulate")
    public Map<String, Object> simulateCharging(@RequestParam String scenario) {
        Map<String, Object> response = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime;
        LocalDateTime endTime;
        double energyKwh;
        String userId = "USER001";
        String description;
        
        switch (scenario) {
            case "valley":
                // 谷时充电场景 - 23:00开始充电
                startTime = now.withHour(23).withMinute(0).withSecond(0);
                endTime = startTime.plusHours(4);
                energyKwh = 20.0;
                description = "谷时充电(23:00-03:00)，应获得奖励";
                break;
            case "peak":
                // 峰时充电场景 - 08:00开始充电
                startTime = now.withHour(8).withMinute(0).withSecond(0);
                endTime = startTime.plusHours(3);
                energyKwh = 15.0;
                description = "峰时充电(08:00-11:00)，不应获得奖励";
                break;
            case "mixed":
                // 混合时段充电 - 跨越谷时和平时
                startTime = now.withHour(22).withMinute(0).withSecond(0);
                endTime = startTime.plusHours(4);
                energyKwh = 25.0;
                description = "混合时段充电(22:00-02:00)，部分获得奖励";
                break;
            case "noon-valley":
                // 午间谷时充电
                startTime = now.withHour(12).withMinute(0).withSecond(0);
                endTime = startTime.plusHours(2);
                energyKwh = 10.0;
                description = "午间谷时充电(12:00-14:00)，应获得奖励";
                break;
            default:
                response.put("code", 400);
                response.put("message", "未知场景: " + scenario);
                return response;
        }
        
        RewardResult result = chargingRewardService.calculateRewardWithDetails(userId, startTime, endTime, energyKwh);
        
        response.put("code", 200);
        response.put("message", "success");
        response.put("scenario", scenario);
        response.put("description", description);
        response.put("data", result);
        
        return response;
    }
}
