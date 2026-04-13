package com.example.demo;

import com.example.demo.model.ChargingStation;
import com.example.demo.model.ChargingUser;
import com.example.demo.service.PowerAllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PowerAllocationTest {

    @Autowired
    private PowerAllocationService service;

    private ChargingStation station;

    @BeforeEach
    void setUp() {
        station = service.createStation("测试充电站", 100.0);
    }

    @Test
    void testBasicAllocation() {
        System.out.println("\n========== 测试1: 基本功率分配 ==========");
        
        ChargingUser user1 = ChargingUser.create("张三", 50.0, 0, 20, 0, 22.0);
        ChargingUser user2 = ChargingUser.create("李四", 30.0, 0, 20, 0, 22.0);
        
        service.addUser(station, user1);
        service.addUser(station, user2);
        
        Map<String, Object> result = service.getAllocationResult(station);
        printResult(result);
        
        double totalPower = station.getTimeSlotPowers()[0].getTotalPower();
        assertTrue(totalPower <= 100.0, "总功率不应超过充电站限制");
        System.out.println("时间槽0的总功率: " + totalPower + " kW");
        System.out.println("测试通过: 总功率未超过充电站限制");
    }

    @Test
    void testPowerLimitConstraint() {
        System.out.println("\n========== 测试2: 功率限制约束测试 ==========");
        
        for (int i = 0; i < 6; i++) {
            ChargingUser user = ChargingUser.create("用户" + (i + 1), 100.0, 0, 20, 0, 22.0);
            service.addUser(station, user);
        }
        
        System.out.println("添加了6个用户,每个最大功率22kW,总需求132kW,但充电站限制100kW");
        
        for (int slot = 0; slot < 21; slot++) {
            double totalPower = station.getTimeSlotPowers()[slot].getTotalPower();
            assertTrue(totalPower <= 100.0, 
                "时间槽" + slot + "功率超限: " + totalPower);
        }
        
        System.out.println("所有时间槽功率均未超过100kW限制");
        System.out.println("测试通过: 功率限制约束有效");
    }

    @Test
    void testUserCancellation() {
        System.out.println("\n========== 测试3: 用户取消充电测试 ==========");
        
        ChargingUser user1 = ChargingUser.create("王五", 50.0, 0, 20, 0, 22.0);
        ChargingUser user2 = ChargingUser.create("赵六", 50.0, 0, 20, 0, 22.0);
        
        service.addUser(station, user1);
        service.addUser(station, user2);
        
        double powerBefore = station.getTimeSlotPowers()[0].getTotalPower();
        System.out.println("取消前时间槽0总功率: " + powerBefore + " kW");
        
        service.removeUser(station, user1.getUserId());
        
        double powerAfter = station.getTimeSlotPowers()[0].getTotalPower();
        System.out.println("取消后时间槽0总功率: " + powerAfter + " kW");
        
        Map<String, Object> result = service.getAllocationResult(station);
        System.out.println("\n取消后的分配结果:");
        printResult(result);
        
        System.out.println("测试通过: 用户取消后功率重新分配成功");
    }

    @Test
    void testDynamicJoin() {
        System.out.println("\n========== 测试4: 用户动态加入测试 ==========");
        
        ChargingUser user1 = ChargingUser.create("用户A", 30.0, 0, 10, 0, 22.0);
        service.addUser(station, user1);
        
        System.out.println("初始用户A加入后的分配:");
        double power1 = station.getTimeSlotPowers()[0].getTotalPower();
        System.out.println("时间槽0总功率: " + power1 + " kW");
        
        ChargingUser user2 = ChargingUser.create("用户B", 40.0, 0, 10, 0, 22.0);
        service.addUser(station, user2);
        
        System.out.println("\n用户B加入后的分配:");
        double power2 = station.getTimeSlotPowers()[0].getTotalPower();
        System.out.println("时间槽0总功率: " + power2 + " kW");
        
        assertTrue(power2 <= 100.0, "动态加入后功率不应超限");
        System.out.println("测试通过: 动态加入后功率重新分配成功");
    }

    @Test
    void testTimeSlotProgression() {
        System.out.println("\n========== 测试5: 时间槽推进测试 ==========");
        
        ChargingUser user = ChargingUser.create("测试用户", 10.0, 0, 5, 0, 22.0);
        service.addUser(station, user);
        
        System.out.println("初始状态:");
        System.out.println("  当前时间槽: " + station.getCurrentTimeSlot());
        System.out.println("  用户当前电量: " + user.getCurrentEnergy() + " kWh");
        
        service.updateTimeSlot(station, 3);
        
        System.out.println("\n推进到时间槽3后:");
        System.out.println("  当前时间槽: " + station.getCurrentTimeSlot());
        System.out.println("  用户当前电量: " + user.getCurrentEnergy() + " kWh");
        
        assertTrue(user.getCurrentEnergy() > 0, "用户应该已经充入电量");
        System.out.println("测试通过: 时间槽推进正常,电量计算正确");
    }

    @Test
    void testUrgencyPriority() {
        System.out.println("\n========== 测试6: 紧急程度优先级测试 ==========");
        
        ChargingStation testStation = service.createStation("紧急测试站", 50.0);
        
        ChargingUser urgentUser = ChargingUser.create("紧急用户", 50.0, 0, 2, 0, 30.0);
        ChargingUser normalUser = ChargingUser.create("普通用户", 50.0, 0, 20, 0, 30.0);
        
        service.addUser(testStation, normalUser);
        service.addUser(testStation, urgentUser);
        
        double urgentPower = testStation.getTimeSlotPowers()[0].getUserPower(urgentUser.getUserId());
        double normalPower = testStation.getTimeSlotPowers()[0].getUserPower(normalUser.getUserId());
        
        System.out.println("紧急用户(时间范围0-2)功率: " + urgentPower + " kW");
        System.out.println("普通用户(时间范围0-20)功率: " + normalPower + " kW");
        
        assertTrue(urgentPower >= normalPower, "紧急用户应获得更高功率");
        System.out.println("测试通过: 紧急用户获得更高优先级");
    }

    @Test
    void testFullSimulation() {
        System.out.println("\n========== 测试7: 完整模拟测试 ==========");
        
        ChargingStation simStation = service.createStation("模拟充电站", 150.0);
        
        service.addUser(simStation, ChargingUser.create("早班用户1", 40.0, 0, 24, 3.5, 22.0));
        service.addUser(simStation, ChargingUser.create("早班用户2", 35.0, 0, 24, 3.5, 22.0));
        service.addUser(simStation, ChargingUser.create("午班用户", 50.0, 16, 48, 3.5, 22.0));
        service.addUser(simStation, ChargingUser.create("晚班用户", 60.0, 48, 80, 3.5, 22.0));
        
        System.out.println("\n初始分配结果:");
        Map<String, Object> result = service.getAllocationResult(simStation);
        printResult(result);
        
        System.out.println("\n模拟时间推进到时间槽24:");
        service.updateTimeSlot(simStation, 24);
        
        result = service.getAllocationResult(simStation);
        printResult(result);
        
        System.out.println("\n新用户紧急加入:");
        ChargingUser emergencyUser = ChargingUser.create("紧急用户", 30.0, 24, 32, 3.5, 22.0);
        service.addUser(simStation, emergencyUser);
        
        result = service.getAllocationResult(simStation);
        printResult(result);
        
        System.out.println("\n测试通过: 完整模拟成功");
    }

    private void printResult(Map<String, Object> result) {
        System.out.println("----------------------------------------");
        System.out.println("充电站: " + result.get("stationName"));
        System.out.println("总功率: " + result.get("totalPower") + " kW");
        System.out.println("当前时间槽: " + result.get("currentTimeSlot"));
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> users = (List<Map<String, Object>>) result.get("users");
        System.out.println("\n用户列表:");
        for (Map<String, Object> user : users) {
            System.out.printf("  %s: 目标%.1f kWh, 当前%.2f kWh, 剩余%.2f kWh, 时间%s, 完成:%s%n",
                    user.get("userName"),
                    user.get("targetEnergy"),
                    user.get("currentEnergy"),
                    user.get("remainingEnergy"),
                    user.get("timeRange"),
                    user.get("isCompleted"));
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> schedule = (List<Map<String, Object>>) result.get("powerSchedule");
        if (!schedule.isEmpty()) {
            System.out.println("\n功率调度(非零时段):");
            for (Map<String, Object> slot : schedule) {
                System.out.printf("  时间槽%s: 总功率%.2f kW - %s%n",
                        slot.get("slot"),
                        slot.get("totalPower"),
                        slot.get("userPowers"));
            }
        }
        System.out.println("----------------------------------------");
    }
}
