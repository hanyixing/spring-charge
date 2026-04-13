package com.example.demo.service;

import com.example.demo.entity.ChargingStation;
import com.example.demo.entity.ChargingUser;
import com.example.demo.entity.PowerAllocationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class PowerAllocationService {

    public PowerAllocationResult allocatePower(ChargingStation station) {
        log.info("开始功率分配，充电站总功率: {} kW, 用户数: {}",
                station.getTotalPower(), station.getUsers().size());

        station.resetPowerUsage();

        List<ChargingUser> activeUsers = new ArrayList<>();
        for (ChargingUser user : station.getUsers()) {
            if (user.isActive() && user.getRemainingEnergy() > 0.01) {
                activeUsers.add(user);
                Arrays.fill(user.getAllocatedPower(), 0.0);
            }
        }

        if (activeUsers.isEmpty()) {
            return buildEmptyResult(station);
        }

        activeUsers.sort(Comparator.comparingInt(ChargingUser::getEndTimeSlot)
                .thenComparingDouble(ChargingUser::getRemainingEnergy).reversed());

        boolean allocationSuccessful = performAllocation(station, activeUsers);

        return buildResult(station, activeUsers, allocationSuccessful);
    }

    private boolean performAllocation(ChargingStation station, List<ChargingUser> users) {
        boolean allSatisfied = true;

        for (ChargingUser user : users) {
            double remainingEnergy = user.getRemainingEnergy();
            int startSlot = user.getStartTimeSlot();
            int endSlot = user.getEndTimeSlot();
            double maxPower = user.getMaxPower();

            if (remainingEnergy <= 0.01) continue;

            List<Integer> availableSlots = new ArrayList<>();
            for (int slot = startSlot; slot <= endSlot && slot < ChargingStation.TOTAL_SLOTS; slot++) {
                availableSlots.add(slot);
            }

            if (availableSlots.isEmpty()) {
                log.warn("用户 {} 没有可用时间段", user.getUserId());
                allSatisfied = false;
                continue;
            }

            double totalPossibleEnergy = 0.0;
            for (int slot : availableSlots) {
                double availablePower = Math.min(maxPower, station.getRemainingPower(slot));
                totalPossibleEnergy += availablePower * ChargingStation.SLOT_DURATION_HOURS;
            }

            if (totalPossibleEnergy < remainingEnergy * 0.99) {
                log.warn("用户 {} 无法完成充电目标，可用能量: {}, 需要能量: {}",
                        user.getUserId(), totalPossibleEnergy, remainingEnergy);
                allSatisfied = false;
            }

            double energyToAllocate = Math.min(remainingEnergy, totalPossibleEnergy);
            double avgPowerNeeded = energyToAllocate / (availableSlots.size() * ChargingStation.SLOT_DURATION_HOURS);

            for (int slot : availableSlots) {
                if (energyToAllocate <= 0.01) break;

                double remainingPower = station.getRemainingPower(slot);
                double powerToAllocate = Math.min(Math.min(avgPowerNeeded * 1.2, maxPower), remainingPower);
                powerToAllocate = Math.round(powerToAllocate * 100.0) / 100.0;

                if (powerToAllocate > 0.1) {
                    double energyInSlot = powerToAllocate * ChargingStation.SLOT_DURATION_HOURS;
                    energyInSlot = Math.min(energyInSlot, energyToAllocate);
                    powerToAllocate = energyInSlot / ChargingStation.SLOT_DURATION_HOURS;

                    user.getAllocatedPower()[slot] = powerToAllocate;
                    station.getPowerUsage()[slot] += powerToAllocate;
                    energyToAllocate -= energyInSlot;
                }
            }

            double totalAllocatedEnergy = 0.0;
            for (int slot = startSlot; slot <= endSlot; slot++) {
                totalAllocatedEnergy += user.getAllocatedPower()[slot] * ChargingStation.SLOT_DURATION_HOURS;
            }
            user.setCurrentEnergy(user.getTargetEnergy() - user.getRemainingEnergy() + totalAllocatedEnergy);

            log.debug("用户 {} 分配完成，分配能量: {}, 目标能量: {}",
                    user.getUserId(), totalAllocatedEnergy, user.getTargetEnergy());
        }

        return allSatisfied;
    }

    public void reallocateOnUserJoin(ChargingStation station, ChargingUser newUser) {
        log.info("新用户加入充电队列: {}", newUser.getUserId());
        station.addUser(newUser);
        allocatePower(station);
    }

    public void reallocateOnUserCancel(ChargingStation station, String userId) {
        log.info("用户取消充电: {}", userId);
        station.cancelCharging(userId);
        allocatePower(station);
    }

    private PowerAllocationResult buildEmptyResult(ChargingStation station) {
        return PowerAllocationResult.builder()
                .success(true)
                .message("没有活跃的用户需要充电")
                .totalStationPower(station.getTotalPower())
                .totalPowerUsage(new double[ChargingStation.TOTAL_SLOTS])
                .userAllocations(new ArrayList<>())
                .build();
    }

    private PowerAllocationResult buildResult(ChargingStation station, List<ChargingUser> users, boolean allSatisfied) {
        List<PowerAllocationResult.UserAllocationDetail> details = new ArrayList<>();

        for (ChargingUser user : users) {
            double actualEnergy = 0.0;
            for (int i = 0; i < ChargingStation.TOTAL_SLOTS; i++) {
                actualEnergy += user.getAllocatedPower()[i] * ChargingStation.SLOT_DURATION_HOURS;
            }

            PowerAllocationResult.UserAllocationDetail detail = PowerAllocationResult.UserAllocationDetail.builder()
                    .userId(user.getUserId())
                    .userName(user.getUserName())
                    .startTimeSlot(user.getStartTimeSlot())
                    .endTimeSlot(user.getEndTimeSlot())
                    .targetEnergy(user.getTargetEnergy())
                    .actualEnergy(Math.round(actualEnergy * 100.0) / 100.0)
                    .allocatedPower(user.getAllocatedPower().clone())
                    .maxPower(user.getMaxPower())
                    .satisfied(actualEnergy >= user.getTargetEnergy() * 0.99)
                    .build();

            details.add(detail);
        }

        Map<Integer, Double> timeSlotPowerMap = new LinkedHashMap<>();
        for (int i = 0; i < ChargingStation.TOTAL_SLOTS; i++) {
            if (station.getPowerUsage()[i] > 0.01) {
                timeSlotPowerMap.put(i, Math.round(station.getPowerUsage()[i] * 100.0) / 100.0);
            }
        }

        String message = allSatisfied ? "功率分配成功，所有用户充电目标可满足" : "功率分配完成，部分用户可能无法达到充电目标";

        return PowerAllocationResult.builder()
                .success(true)
                .message(message)
                .totalStationPower(station.getTotalPower())
                .totalPowerUsage(station.getPowerUsage().clone())
                .userAllocations(details)
                .timeSlotPowerMap(timeSlotPowerMap)
                .build();
    }
}
