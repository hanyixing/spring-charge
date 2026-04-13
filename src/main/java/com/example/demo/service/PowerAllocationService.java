package com.example.demo.service;

import com.example.demo.model.AllocationResult;
import com.example.demo.model.ChargingStation;
import com.example.demo.model.ChargingUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class PowerAllocationService {
    
    private static final int TOTAL_SLOTS = 96;
    private static final double SLOT_DURATION_HOURS = 0.25;
    
    private ChargingStation station;
    
    public PowerAllocationService() {
        this.station = ChargingStation.create("STATION-001", 100.0);
    }
    
    public void setStationPower(double totalPower) {
        this.station.setTotalPower(totalPower);
        reallocate();
    }
    
    public ChargingUser addUser(double currentEnergy, double targetEnergy,
                                 int startTimeSlot, int endTimeSlot,
                                 double maxChargingPower) {
        ChargingUser user = ChargingUser.create(currentEnergy, targetEnergy, 
                                                 startTimeSlot, endTimeSlot, maxChargingPower);
        station.addUser(user);
        log.info("用户加入充电队列: userId={}, 需充电量={}kWh, 时间范围=[{},{}]",
                user.getUserId(), user.getRequiredEnergy(), startTimeSlot, endTimeSlot);
        reallocate();
        return user;
    }
    
    public void removeUser(String userId) {
        ChargingUser user = station.getUser(userId);
        if (user != null) {
            station.removeUser(userId);
            log.info("用户取消充电: userId={}", userId);
            reallocate();
        }
    }
    
    public void updateUserEnergy(String userId, double currentEnergy) {
        ChargingUser user = station.getUser(userId);
        if (user != null) {
            user.setCurrentEnergy(currentEnergy);
            if (currentEnergy >= user.getTargetEnergy()) {
                user.setActive(false);
                log.info("用户充电完成: userId={}", userId);
            }
            reallocate();
        }
    }
    
    public synchronized void reallocate() {
        clearAllocations();
        List<ChargingUser> activeUsers = station.getActiveUsers();
        if (activeUsers.isEmpty()) {
            log.info("当前无活跃用户");
            return;
        }
        Map<String, Double> remainingEnergy = new HashMap<>();
        for (ChargingUser user : activeUsers) {
            remainingEnergy.put(user.getUserId(), user.getRequiredEnergy());
        }
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            allocateSlot(slot, activeUsers, remainingEnergy);
        }
        logAllocationSummary();
    }
    
    private void clearAllocations() {
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            station.getUserPowerAllocation().get(i).clear();
        }
        Arrays.fill(station.getPowerSchedule(), 0);
    }
    
    private void allocateSlot(int slot, List<ChargingUser> activeUsers, 
                              Map<String, Double> remainingEnergy) {
        List<ChargingUser> eligibleUsers = new ArrayList<>();
        for (ChargingUser user : activeUsers) {
            if (slot >= user.getStartTimeSlot() && slot <= user.getEndTimeSlot()) {
                double remaining = remainingEnergy.get(user.getUserId());
                if (remaining > 0.001) {
                    eligibleUsers.add(user);
                }
            }
        }
        if (eligibleUsers.isEmpty()) {
            return;
        }
        double availablePower = station.getTotalPower();
        Map<String, Double> allocations = new HashMap<>();
        for (ChargingUser user : eligibleUsers) {
            double minPower = user.getMinRequiredPower();
            double fairPower = availablePower / eligibleUsers.size();
            double maxPower = user.getMaxChargingPower();
            double remaining = remainingEnergy.get(user.getUserId());
            double maxPossibleFromEnergy = remaining / SLOT_DURATION_HOURS;
            double allocated = Math.min(Math.min(fairPower, maxPower), maxPossibleFromEnergy);
            allocated = Math.max(allocated, Math.min(minPower, maxPower));
            allocations.put(user.getUserId(), allocated);
        }
        double totalAllocated = allocations.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalAllocated > availablePower) {
            double scale = availablePower / totalAllocated;
            for (String userId : allocations.keySet()) {
                allocations.put(userId, allocations.get(userId) * scale);
            }
            totalAllocated = availablePower;
        }
        for (Map.Entry<String, Double> entry : allocations.entrySet()) {
            String userId = entry.getKey();
            double power = entry.getValue();
            station.getUserPowerAllocation().get(slot).put(userId, power);
            double energyCharged = power * SLOT_DURATION_HOURS;
            remainingEnergy.put(userId, remainingEnergy.get(userId) - energyCharged);
        }
        station.getPowerSchedule()[slot] = totalAllocated;
    }
    
    public AllocationResult getAllocationAtSlot(int slot) {
        if (slot < 0 || slot >= TOTAL_SLOTS) {
            return null;
        }
        AllocationResult result = new AllocationResult();
        result.setTimeSlot(slot);
        result.setTotalPower(station.getPowerSchedule()[slot]);
        result.setUserAllocations(new HashMap<>(station.getUserPowerAllocation().get(slot)));
        result.setRemainingPower(station.getTotalPower() - station.getPowerSchedule()[slot]);
        return result;
    }
    
    public List<AllocationResult> getAllAllocations() {
        List<AllocationResult> results = new ArrayList<>();
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            results.add(getAllocationAtSlot(slot));
        }
        return results;
    }
    
    public Map<String, Object> getUserSchedule(String userId) {
        Map<String, Object> schedule = new LinkedHashMap<>();
        ChargingUser user = station.getUser(userId);
        if (user == null) {
            return schedule;
        }
        List<Map<String, Object>> slots = new ArrayList<>();
        double totalEnergy = 0;
        for (int slot = user.getStartTimeSlot(); slot <= user.getEndTimeSlot(); slot++) {
            Double power = station.getUserPowerAllocation().get(slot).get(userId);
            if (power != null && power > 0) {
                Map<String, Object> slotInfo = new LinkedHashMap<>();
                slotInfo.put("slot", slot);
                slotInfo.put("timeRange", formatSlotTime(slot));
                slotInfo.put("power", power);
                double energy = power * SLOT_DURATION_HOURS;
                slotInfo.put("energy", energy);
                totalEnergy += energy;
                slots.add(slotInfo);
            }
        }
        schedule.put("userId", userId);
        schedule.put("currentEnergy", user.getCurrentEnergy());
        schedule.put("targetEnergy", user.getTargetEnergy());
        schedule.put("requiredEnergy", user.getRequiredEnergy());
        schedule.put("scheduledEnergy", totalEnergy);
        schedule.put("slots", slots);
        return schedule;
    }
    
    private String formatSlotTime(int slot) {
        int hour = slot / 4;
        int minute = (slot % 4) * 15;
        return String.format("%02d:%02d", hour, minute);
    }
    
    private void logAllocationSummary() {
        log.info("========== 功率分配摘要 ==========");
        log.info("充电站总功率: {} kW", station.getTotalPower());
        log.info("活跃用户数: {}", station.getActiveUsers().size());
        for (ChargingUser user : station.getActiveUsers()) {
            Map<String, Object> schedule = getUserSchedule(user.getUserId());
            log.info("用户 {}: 需充电 {} kWh, 计划充电 {} kWh",
                    user.getUserId(),
                    String.format("%.2f", user.getRequiredEnergy()),
                    String.format("%.2f", schedule.get("scheduledEnergy")));
        }
        double maxPower = Arrays.stream(station.getPowerSchedule()).max().orElse(0);
        double avgPower = Arrays.stream(station.getPowerSchedule()).average().orElse(0);
        log.info("峰值功率: {} kW", String.format("%.2f", maxPower));
        log.info("平均功率: {} kW", String.format("%.2f", avgPower));
        log.info("=================================");
    }
    
    public ChargingStation getStation() {
        return station;
    }
    
    public List<ChargingUser> getAllUsers() {
        return new ArrayList<>(station.getUsers().values());
    }
}
