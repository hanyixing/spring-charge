package com.example.demo.controller;

import com.example.demo.model.ChargeRewardResult;
import com.example.demo.model.ChargeSession;
import com.example.demo.service.ChargeRewardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/charge")
public class ChargeRewardController {

    @Autowired
    private ChargeRewardService rewardService;

    @PostMapping("/reward/calculate")
    public ResponseEntity<ChargeRewardResult> calculateReward(@RequestBody ChargeSession session) {
        ChargeRewardResult result = rewardService.calculateReward(session);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/reward/periods")
    public ResponseEntity<Map<String, Object>> getRewardPeriods() {
        Map<String, Object> response = new HashMap<>();
        List<String> periods = rewardService.getAllRewardPeriods();
        response.put("rewardPeriods", periods);
        response.put("totalPeriods", periods.size());
        response.put("description", "谷时充电奖励时段（00:00-02:15 和 22:00-24:00）");
        response.put("rewardRate", "0.5元/kWh");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reward/test")
    public ResponseEntity<Map<String, Object>> runTest() {
        Map<String, Object> response = new HashMap<>();

        ChargeSession session1 = new ChargeSession();
        session1.setUserId("user_001");
        session1.setStartTime(LocalDateTime.of(2024, 4, 13, 0, 0));
        session1.setEndTime(LocalDateTime.of(2024, 4, 13, 1, 0));
        session1.setEnergyKwh(new BigDecimal("20.0"));
        ChargeRewardResult result1 = rewardService.calculateReward(session1);
        response.put("test1_谷时充电_00:00-01:00", result1);

        ChargeSession session2 = new ChargeSession();
        session2.setUserId("user_002");
        session2.setStartTime(LocalDateTime.of(2024, 4, 13, 12, 0));
        session2.setEndTime(LocalDateTime.of(2024, 4, 13, 13, 0));
        session2.setEnergyKwh(new BigDecimal("20.0"));
        ChargeRewardResult result2 = rewardService.calculateReward(session2);
        response.put("test2_峰时充电_12:00-13:00", result2);

        ChargeSession session3 = new ChargeSession();
        session3.setUserId("user_003");
        session3.setStartTime(LocalDateTime.of(2024, 4, 13, 23, 30));
        session3.setEndTime(LocalDateTime.of(2024, 4, 14, 0, 30));
        session3.setEnergyKwh(new BigDecimal("40.0"));
        ChargeRewardResult result3 = rewardService.calculateReward(session3);
        response.put("test3_跨时段充电_23:30-00:30", result3);

        return ResponseEntity.ok(response);
    }
}
