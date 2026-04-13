package com.example.demo.service;

import com.example.demo.model.ChargingUser;
import com.example.demo.model.PowerAllocationResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PowerAllocationService {
    private static final int TOTAL_SLOTS = 96;
    private static final double SLOT_HOURS = 0.25;
    private double stationMaxPower;
    private Map<String, ChargingUser> activeUsers;
    private int currentSlot;

    public PowerAllocationService() {
        this.stationMaxPower = 500.0;
        this.activeUsers = new HashMap<>();
        this.currentSlot = 0;
    }

    public void setStationMaxPower(double power) {
        this.stationMaxPower = power;
    }

    public void addUser(ChargingUser user) {
        activeUsers.put(user.getUserId(), user);
    }

    public void removeUser(String userId) {
        activeUsers.remove(userId);
    }

    public List<ChargingUser> getActiveUsers() {
        return new ArrayList<>(activeUsers.values());
    }

    public PowerAllocationResult allocatePower() {
        List<ChargingUser> validUsers = activeUsers.values().stream()
                .filter(ChargingUser::isActive)
                .collect(Collectors.toList());

        if (validUsers.isEmpty()) {
            return new PowerAllocationResult(true, "没有活跃用户");
        }

        Map<String, List<Double>> userPowerMap = new HashMap<>();
        List<Double> totalPower = new ArrayList<>(Collections.nCopies(TOTAL_SLOTS, 0.0));

        for (ChargingUser user : validUsers) {
            userPowerMap.put(user.getUserId(), new ArrayList<>(Collections.nCopies(TOTAL_SLOTS, 0.0)));
        }

        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            int finalSlot = slot;
            List<ChargingUser> slotUsers = validUsers.stream()
                    .filter(u -> finalSlot >= u.getStartSlot() && finalSlot < u.getEndSlot())
                    .collect(Collectors.toList());

            if (slotUsers.isEmpty()) continue;

            double[] remainingEnergy = new double[slotUsers.size()];
            for (int i = 0; i < slotUsers.size(); i++) {
                ChargingUser u = slotUsers.get(i);
                remainingEnergy[i] = u.getTargetEnergy() - calculateChargedEnergy(u, userPowerMap.get(u.getUserId()), finalSlot);
            }

            int remainingSlotsInWindow = 0;
            for (ChargingUser u : slotUsers) {
                int slotsLeft = u.getEndSlot() - finalSlot;
                remainingSlotsInWindow = Math.max(remainingSlotsInWindow, slotsLeft);
            }

            double totalRequiredPower = 0;
            double[] targetPower = new double[slotUsers.size()];
            for (int i = 0; i < slotUsers.size(); i++) {
                int slotsLeft = slotUsers.get(i).getEndSlot() - finalSlot;
                targetPower[i] = remainingEnergy[i] / (slotsLeft * SLOT_HOURS);
                targetPower[i] = Math.max(0, targetPower[i]);
                totalRequiredPower += targetPower[i];
            }

            double scaleFactor = 1.0;
            if (totalRequiredPower > stationMaxPower) {
                scaleFactor = stationMaxPower / totalRequiredPower;
            }

            double slotTotalPower = 0;
            for (int i = 0; i < slotUsers.size(); i++) {
                ChargingUser u = slotUsers.get(i);
                double allocatedPower = targetPower[i] * scaleFactor;
                userPowerMap.get(u.getUserId()).set(finalSlot, allocatedPower);
                slotTotalPower += allocatedPower;
            }
            totalPower.set(finalSlot, slotTotalPower);
        }

        Map<String, Double> userTotalEnergy = new HashMap<>();
        for (ChargingUser user : validUsers) {
            List<Double> powerProfile = userPowerMap.get(user.getUserId());
            double totalEnergy = powerProfile.stream().mapToDouble(p -> p * SLOT_HOURS).sum();
            userTotalEnergy.put(user.getUserId(), totalEnergy);
        }

        PowerAllocationResult result = new PowerAllocationResult(true, "功率分配完成");
        result.setUserPowerProfile(userPowerMap);
        result.setStationTotalPower(totalPower);
        result.setMaxStationPower(stationMaxPower);
        result.setUserTotalEnergy(userTotalEnergy);

        return result;
    }

    private double calculateChargedEnergy(ChargingUser user, List<Double> powerProfile, int upToSlot) {
        double energy = 0;
        for (int i = user.getStartSlot(); i < upToSlot; i++) {
            energy += powerProfile.get(i) * SLOT_HOURS;
        }
        return energy;
    }

    public String formatAllocationResult(PowerAllocationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n============================================\n");
        sb.append("          充电站功率分配结果报告\n");
        sb.append("============================================\n");
        sb.append(String.format("充电站总功率上限: %.2f kW\n", result.getMaxStationPower()));
        sb.append(String.format("时间槽数量: %d (每槽15分钟)\n\n", TOTAL_SLOTS));

        sb.append("-------------- 用户充电总结 --------------\n");
        Map<String, Double> userEnergy = result.getUserTotalEnergy();
        for (Map.Entry<String, Double> entry : userEnergy.entrySet()) {
            ChargingUser user = activeUsers.get(entry.getKey());
            if (user != null) {
                sb.append(String.format("用户 %s:\n", entry.getKey()));
                sb.append(String.format("  目标电量: %.2f kWh\n", user.getTargetEnergy()));
                sb.append(String.format("  实际分配: %.2f kWh\n", entry.getValue()));
                sb.append(String.format("  时间窗口: 第%d-%d槽 (%.1f小时)\n",
                        user.getStartSlot(), user.getEndSlot(),
                        (user.getEndSlot() - user.getStartSlot()) * 0.25));
                double difference = entry.getValue() - user.getTargetEnergy();
                sb.append(String.format("  差值: %.2f kWh (%s)\n",
                        Math.abs(difference),
                        difference >= -0.01 ? "已满足" : "未满足"));
            }
        }

        sb.append("\n----------- 各时间槽总功率分布 -----------\n");
        List<Double> totalPower = result.getStationTotalPower();
        boolean overload = false;
        for (int i = 0; i < TOTAL_SLOTS; i += 4) {
            double hourPower = 0;
            for (int j = 0; j < 4 && i + j < TOTAL_SLOTS; j++) {
                hourPower += totalPower.get(i + j);
                if (totalPower.get(i + j) > stationMaxPower) {
                    overload = true;
                }
            }
            sb.append(String.format("%02d:00-%02d:00 平均: %.2f kW  ",
                    i / 4, (i + 4) / 4, hourPower / 4));
            if ((i / 4 + 1) % 4 == 0) sb.append("\n");
        }

        sb.append("\n\n------------- 功率约束检查 -------------\n");
        double maxUsedPower = totalPower.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        sb.append(String.format("实际峰值功率: %.2f kW\n", maxUsedPower));
        sb.append(String.format("充电站上限: %.2f kW\n", stationMaxPower));
        sb.append(String.format("是否超载: %s\n", maxUsedPower <= stationMaxPower + 0.01 ? "否 ✓" : "是 ✗"));

        boolean allGoalsMet = true;
        for (Map.Entry<String, Double> entry : userEnergy.entrySet()) {
            ChargingUser user = activeUsers.get(entry.getKey());
            if (user != null && entry.getValue() < user.getTargetEnergy() - 0.01) {
                allGoalsMet = false;
                break;
            }
        }
        sb.append(String.format("所有用户目标达成: %s\n", allGoalsMet ? "是 ✓" : "否 ✗"));
        sb.append("============================================\n");

        return sb.toString();
    }
}
