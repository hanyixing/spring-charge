package com.example.demo;

import com.example.demo.config.RewardConfig;
import com.example.demo.model.ChargeRewardResult;
import com.example.demo.model.ChargeSession;
import com.example.demo.service.ChargeRewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ChargeRewardServiceTest {

    @Autowired
    private ChargeRewardService rewardService;

    @Autowired
    private RewardConfig rewardConfig;

    @BeforeEach
    void setUp() {
        rewardConfig.setRewardTimeSlots(Arrays.asList(0,1,2,3,4,5,6,7,8,9,88,89,90,91,92,93,94,95));
        rewardConfig.setRewardRatePerKwh(new BigDecimal("0.5"));
    }

    @Test
    void testValleyTimeCharge_FullReward() {
        ChargeSession session = new ChargeSession();
        session.setUserId("user_001");
        session.setStartTime(LocalDateTime.of(2024, 4, 13, 0, 0));
        session.setEndTime(LocalDateTime.of(2024, 4, 13, 1, 0));
        session.setEnergyKwh(new BigDecimal("20.0"));

        ChargeRewardResult result = rewardService.calculateReward(session);

        System.out.println("=== 测试1: 谷时充电(00:00-01:00) ===");
        System.out.println("用户ID: " + result.getUserId());
        System.out.println("总奖励: " + result.getTotalReward() + "元");
        System.out.println("奖励电量: " + result.getRewardedEnergy() + "kWh");
        System.out.println("无奖励电量: " + result.getNonRewardedEnergy() + "kWh");
        System.out.println("时段详情:");
        result.getTimeSlotRewards().forEach(slot -> 
            System.out.printf("  [%s] %s %s - 电量: %.2fkWh, 奖励: %.2f元%n", 
                slot.isRewardPeriod() ? "奖励时段" : "非奖励时段",
                slot.getDate(),
                slot.getTimeRange(), 
                slot.getEnergy(), 
                slot.getReward())
        );

        assertEquals(0, result.getNonRewardedEnergy().compareTo(BigDecimal.ZERO));
        assertTrue(result.getTotalReward().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testPeakTimeCharge_NoReward() {
        ChargeSession session = new ChargeSession();
        session.setUserId("user_002");
        session.setStartTime(LocalDateTime.of(2024, 4, 13, 12, 0));
        session.setEndTime(LocalDateTime.of(2024, 4, 13, 13, 0));
        session.setEnergyKwh(new BigDecimal("20.0"));

        ChargeRewardResult result = rewardService.calculateReward(session);

        System.out.println("\n=== 测试2: 峰时充电(12:00-13:00) ===");
        System.out.println("用户ID: " + result.getUserId());
        System.out.println("总奖励: " + result.getTotalReward() + "元");
        System.out.println("奖励电量: " + result.getRewardedEnergy() + "kWh");
        System.out.println("无奖励电量: " + result.getNonRewardedEnergy() + "kWh");

        assertEquals(0, result.getTotalReward().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.getRewardedEnergy().compareTo(BigDecimal.ZERO));
    }

    @Test
    void testCrossDayCharge_OneDayBoundary() {
        ChargeSession session = new ChargeSession();
        session.setUserId("user_003");
        session.setStartTime(LocalDateTime.of(2024, 4, 13, 23, 30));
        session.setEndTime(LocalDateTime.of(2024, 4, 14, 0, 30));
        session.setEnergyKwh(new BigDecimal("40.0"));

        ChargeRewardResult result = rewardService.calculateReward(session);

        System.out.println("\n=== 测试3: 跨天充电(23:30-次日00:30) ===");
        System.out.println("用户ID: " + result.getUserId());
        System.out.println("总时段数: " + result.getTimeSlotRewards().size());
        System.out.println("总奖励: " + result.getTotalReward() + "元");
        System.out.println("奖励电量: " + result.getRewardedEnergy() + "kWh");
        System.out.println("无奖励电量: " + result.getNonRewardedEnergy() + "kWh");
        System.out.println("时段详情:");
        result.getTimeSlotRewards().forEach(slot -> 
            System.out.printf("  [%s] %s %s - 电量: %.2fkWh, 奖励: %.2f元%n", 
                slot.isRewardPeriod() ? "奖励时段" : "非奖励时段",
                slot.getDate(),
                slot.getTimeRange(), 
                slot.getEnergy(), 
                slot.getReward())
        );

        assertEquals(5, result.getTimeSlotRewards().size());
        assertEquals(0, result.getTotalReward().compareTo(new BigDecimal("20.00")));
        assertTrue(result.getTotalReward().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testMultiDayCharge_ThreeDays() {
        ChargeSession session = new ChargeSession();
        session.setUserId("user_004");
        session.setStartTime(LocalDateTime.of(2024, 4, 13, 22, 0));
        session.setEndTime(LocalDateTime.of(2024, 4, 15, 2, 0));
        session.setEnergyKwh(new BigDecimal("200.0"));

        ChargeRewardResult result = rewardService.calculateReward(session);

        System.out.println("\n=== 测试4: 连续多天充电(4月13日22:00 - 4月15日02:00) ===");
        System.out.println("用户ID: " + result.getUserId());
        System.out.println("总时段数: " + result.getTimeSlotRewards().size());
        System.out.println("总奖励: " + result.getTotalReward() + "元");
        System.out.println("奖励电量: " + result.getRewardedEnergy() + "kWh");
        System.out.println("无奖励电量: " + result.getNonRewardedEnergy() + "kWh");
        System.out.println("时段详情(显示跨天部分):");
        result.getTimeSlotRewards().stream()
            .filter(s -> s.getTimeSlotIndex() == 0 || s.getTimeSlotIndex() >= 88)
            .forEach(slot -> 
                System.out.printf("  [%s] %s %s - slot:%d%n", 
                    slot.isRewardPeriod() ? "奖励时段" : "非奖励时段",
                    slot.getDate(),
                    slot.getTimeRange(),
                    slot.getTimeSlotIndex())
            );

        assertTrue(result.getTimeSlotRewards().size() > 96, "多天充电时段数应超过96");
        assertTrue(result.getTotalReward().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testPeakValleyMixCharge() {
        ChargeSession session = new ChargeSession();
        session.setUserId("user_005");
        session.setStartTime(LocalDateTime.of(2024, 4, 13, 20, 0));
        session.setEndTime(LocalDateTime.of(2024, 4, 14, 4, 0));
        session.setEnergyKwh(new BigDecimal("120.0"));

        ChargeRewardResult result = rewardService.calculateReward(session);

        System.out.println("\n=== 测试5: 峰谷混合充电(20:00-次日04:00) ===");
        System.out.println("用户ID: " + result.getUserId());
        System.out.println("总时段数: " + result.getTimeSlotRewards().size());
        System.out.println("总奖励: " + result.getTotalReward() + "元");
        System.out.println("奖励电量: " + result.getRewardedEnergy() + "kWh");
        System.out.println("无奖励电量: " + result.getNonRewardedEnergy() + "kWh");
        
        long rewardSlotCount = result.getTimeSlotRewards().stream()
            .filter(ChargeRewardResult.TimeSlotReward::isRewardPeriod)
            .count();
        long nonRewardSlotCount = result.getTimeSlotRewards().stream()
            .filter(s -> !s.isRewardPeriod())
            .count();
            
        System.out.println("奖励时段数: " + rewardSlotCount);
        System.out.println("非奖励时段数: " + nonRewardSlotCount);

        assertTrue(rewardSlotCount > 0, "应有奖励时段");
        assertTrue(nonRewardSlotCount > 0, "应有非奖励时段");
        assertTrue(result.getNonRewardedEnergy().compareTo(BigDecimal.ZERO) > 0, "非奖励时段充电电量应大于0");
    }

    @Test
    void testRewardPeriods() {
        System.out.println("\n=== 奖励时段配置 ===");
        rewardService.getAllRewardPeriods().forEach(p -> System.out.println("  " + p));
        System.out.println("奖励标准: 0.5元/kWh");
        System.out.println("算法目标: 引导用户在谷时(00:00-02:15, 22:00-24:00)充电，降低电网峰时压力");
    }
}
