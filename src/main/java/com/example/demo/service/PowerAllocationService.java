package com.example.demo.service;

import com.example.demo.model.ChargingStation;
import com.example.demo.model.ChargingUser;
import com.example.demo.model.TimeSlotPower;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PowerAllocationService {
    
    private static final int TOTAL_SLOTS = 96;
    private static final double SLOT_DURATION_HOURS = 0.25;

    public ChargingStation createStation(String name, double totalPower) {
        log.info("创建充电站: {}, 总功率: {} kW", name, totalPower);
        return ChargingStation.create(name, totalPower);
    }

    public void addUser(ChargingStation station, ChargingUser user) {
        station.getUsers().add(user);
        log.info("用户 {} 加入充电队列, 目标电量: {} kWh, 时间范围: [{}, {}]",
                user.getUserName(), user.getTargetEnergy(), 
                user.getStartTimeSlot(), user.getEndTimeSlot());
        reallocate(station);
    }

    public void removeUser(ChargingStation station, String userId) {
        Optional<ChargingUser> userOpt = station.getUsers().stream()
                .filter(u -> u.getUserId().equals(userId))
                .findFirst();
        
        if (userOpt.isPresent()) {
            ChargingUser user = userOpt.get();
            user.setActive(false);
            log.info("用户 {} 离开充电队列, 已充电: {} kWh", user.getUserName(), user.getCurrentEnergy());
            
            for (int i = 0; i < TOTAL_SLOTS; i++) {
                station.getTimeSlotPowers()[i].removeUser(userId);
            }
            reallocate(station);
        }
    }

    public void reallocate(ChargingStation station) {
        log.info("开始重新分配功率...");
        
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            station.getTimeSlotPowers()[i].getUserPowers().clear();
        }
        
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            allocateForTimeSlot(station, slot);
        }
        
        log.info("功率分配完成");
    }

    private void allocateForTimeSlot(ChargingStation station, int slot) {
        List<ChargingUser> usersInSlot = station.getUsersInTimeRange(slot);
        
        if (usersInSlot.isEmpty()) {
            return;
        }
        
        usersInSlot.sort((u1, u2) -> {
            double urgency1 = u1.getUrgency(station.getCurrentTimeSlot());
            double urgency2 = u2.getUrgency(station.getCurrentTimeSlot());
            return Double.compare(urgency2, urgency1);
        });
        
        double remainingPower = station.getTotalPower();
        TimeSlotPower tsp = station.getTimeSlotPowers()[slot];
        
        for (ChargingUser user : usersInSlot) {
            double requiredPower = calculateRequiredPower(user, station.getCurrentTimeSlot());
            double allocatedPower = Math.min(Math.min(requiredPower, user.getMaxPower()), remainingPower);
            allocatedPower = Math.max(allocatedPower, user.getMinPower());
            
            if (allocatedPower <= remainingPower && allocatedPower >= user.getMinPower()) {
                tsp.setUserPower(user.getUserId(), allocatedPower);
                remainingPower -= allocatedPower;
            } else if (remainingPower >= user.getMinPower()) {
                tsp.setUserPower(user.getUserId(), remainingPower);
                remainingPower = 0;
            }
        }
    }

    private double calculateRequiredPower(ChargingUser user, int currentSlot) {
        int remainingSlots = user.getRemainingTimeSlots(currentSlot);
        if (remainingSlots <= 0) {
            return user.getMaxPower();
        }
        double remainingEnergy = user.getRemainingEnergy();
        double requiredPower = remainingEnergy / (remainingSlots * SLOT_DURATION_HOURS);
        return Math.min(requiredPower, user.getMaxPower());
    }

    public void updateTimeSlot(ChargingStation station, int newTimeSlot) {
        int oldSlot = station.getCurrentTimeSlot();
        station.setCurrentTimeSlot(newTimeSlot);
        
        if (oldSlot < newTimeSlot) {
            for (int slot = oldSlot; slot < newTimeSlot; slot++) {
                applyChargingForSlot(station, slot);
            }
        }
        
        reallocate(station);
        log.info("时间槽更新: {} -> {}", oldSlot, newTimeSlot);
    }

    private void applyChargingForSlot(ChargingStation station, int slot) {
        TimeSlotPower tsp = station.getTimeSlotPowers()[slot];
        
        for (ChargingUser user : station.getUsers()) {
            if (!user.isActive()) continue;
            
            Double power = tsp.getUserPower(user.getUserId());
            if (power != null && power > 0) {
                double energyCharged = power * SLOT_DURATION_HOURS;
                user.setCurrentEnergy(user.getCurrentEnergy() + energyCharged);
                log.debug("时间槽 {}: 用户 {} 充电 {} kWh (功率 {} kW)", 
                        slot, user.getUserName(), energyCharged, power);
            }
        }
    }

    public Map<String, Object> getAllocationResult(ChargingStation station) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stationId", station.getStationId());
        result.put("stationName", station.getStationName());
        result.put("totalPower", station.getTotalPower());
        result.put("currentTimeSlot", station.getCurrentTimeSlot());
        
        List<Map<String, Object>> userList = new ArrayList<>();
        for (ChargingUser user : station.getUsers()) {
            Map<String, Object> userInfo = new LinkedHashMap<>();
            userInfo.put("userId", user.getUserId());
            userInfo.put("userName", user.getUserName());
            userInfo.put("targetEnergy", user.getTargetEnergy());
            userInfo.put("currentEnergy", Math.round(user.getCurrentEnergy() * 100.0) / 100.0);
            userInfo.put("remainingEnergy", Math.round(user.getRemainingEnergy() * 100.0) / 100.0);
            userInfo.put("isActive", user.isActive());
            userInfo.put("isCompleted", user.isCompleted());
            userInfo.put("timeRange", user.getStartTimeSlot() + "-" + user.getEndTimeSlot());
            userList.add(userInfo);
        }
        result.put("users", userList);
        
        List<Map<String, Object>> powerSchedule = new ArrayList<>();
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            TimeSlotPower tsp = station.getTimeSlotPowers()[i];
            double total = tsp.getTotalPower();
            if (total > 0) {
                Map<String, Object> slotInfo = new LinkedHashMap<>();
                slotInfo.put("slot", i);
                slotInfo.put("totalPower", Math.round(total * 100.0) / 100.0);
                slotInfo.put("userPowers", tsp.getUserPowers());
                powerSchedule.add(slotInfo);
            }
        }
        result.put("powerSchedule", powerSchedule);
        
        return result;
    }

    public List<Map<String, Object>> getDetailedSchedule(ChargingStation station) {
        List<Map<String, Object>> schedule = new ArrayList<>();
        
        for (int i = 0; i < TOTAL_SLOTS; i++) {
            TimeSlotPower tsp = station.getTimeSlotPowers()[i];
            Map<String, Object> slotInfo = new LinkedHashMap<>();
            slotInfo.put("slot", i);
            slotInfo.put("time", slotToTime(i));
            slotInfo.put("totalPower", Math.round(tsp.getTotalPower() * 100.0) / 100.0);
            
            Map<String, Object> userPowerDetails = new LinkedHashMap<>();
            for (ChargingUser user : station.getUsers()) {
                Double power = tsp.getUserPower(user.getUserId());
                if (power != null) {
                    userPowerDetails.put(user.getUserName(), Math.round(power * 100.0) / 100.0);
                }
            }
            slotInfo.put("userPowers", userPowerDetails);
            schedule.add(slotInfo);
        }
        
        return schedule;
    }

    private String slotToTime(int slot) {
        int totalMinutes = slot * 15;
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}
