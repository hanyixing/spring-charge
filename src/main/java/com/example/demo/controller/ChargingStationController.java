package com.example.demo.controller;

import com.example.demo.entity.ChargingStation;
import com.example.demo.entity.ChargingUser;
import com.example.demo.entity.PowerAllocationResult;
import com.example.demo.service.PowerAllocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/charging")
@RequiredArgsConstructor
public class ChargingStationController {

    private final PowerAllocationService powerAllocationService;

    private ChargingStation station;

    @PostConstruct
    public void init() {
        station = new ChargingStation("ST001", "充电站一号", 500.0);
        log.info("充电站初始化完成，总功率: {} kW", station.getTotalPower());
    }

    @GetMapping("/station/info")
    public Map<String, Object> getStationInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("stationId", station.getStationId());
        info.put("stationName", station.getStationName());
        info.put("totalPower", station.getTotalPower());
        info.put("userCount", station.getUsers().size());
        return info;
    }

    @PostMapping("/user/add")
    public Map<String, Object> addUser(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        String userName = (String) request.get("userName");
        int startTimeSlot = (Integer) request.get("startTimeSlot");
        int endTimeSlot = (Integer) request.get("endTimeSlot");
        double targetEnergy = ((Number) request.get("targetEnergy")).doubleValue();
        double maxPower = ((Number) request.getOrDefault("maxPower", 60.0)).doubleValue();

        ChargingUser user = ChargingUser.builder()
                .userId(userId)
                .userName(userName)
                .startTimeSlot(startTimeSlot)
                .endTimeSlot(endTimeSlot)
                .targetEnergy(targetEnergy)
                .currentEnergy(0.0)
                .maxPower(maxPower)
                .active(true)
                .build();

        powerAllocationService.reallocateOnUserJoin(station, user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "用户添加成功");
        response.put("userId", userId);
        return response;
    }

    @PostMapping("/user/cancel")
    public Map<String, Object> cancelCharging(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        powerAllocationService.reallocateOnUserCancel(station, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "用户充电已取消");
        response.put("userId", userId);
        return response;
    }

    @GetMapping("/allocation")
    public PowerAllocationResult getAllocation() {
        return powerAllocationService.allocatePower(station);
    }

    @PostMapping("/allocate")
    public PowerAllocationResult allocate() {
        return powerAllocationService.allocatePower(station);
    }

    @GetMapping("/users")
    public Map<String, Object> getUsers() {
        Map<String, Object> response = new HashMap<>();
        response.put("users", station.getUsers());
        response.put("count", station.getUsers().size());
        return response;
    }

    @PostMapping("/reset")
    public Map<String, Object> resetStation() {
        station.getUsers().clear();
        station.resetPowerUsage();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "充电站已重置");
        return response;
    }
}
