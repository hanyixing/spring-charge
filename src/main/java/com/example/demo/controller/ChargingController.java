package com.example.demo.controller;

import com.example.demo.model.ChargingStation;
import com.example.demo.model.ChargingUser;
import com.example.demo.service.PowerAllocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/charging")
public class ChargingController {
    
    @Autowired
    private PowerAllocationService powerAllocationService;
    
    private Map<String, ChargingStation> stations = new HashMap<>();

    @PostMapping("/station")
    public Map<String, Object> createStation(@RequestBody Map<String, Object> params) {
        String name = (String) params.getOrDefault("name", "充电站");
        double totalPower = ((Number) params.getOrDefault("totalPower", 100.0)).doubleValue();
        
        ChargingStation station = powerAllocationService.createStation(name, totalPower);
        stations.put(station.getStationId(), station);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("stationId", station.getStationId());
        result.put("message", "充电站创建成功");
        return result;
    }

    @PostMapping("/station/{stationId}/user")
    public Map<String, Object> addUser(
            @PathVariable String stationId,
            @RequestBody Map<String, Object> params) {
        
        ChargingStation station = stations.get(stationId);
        if (station == null) {
            return errorResult("充电站不存在");
        }
        
        String userName = (String) params.getOrDefault("userName", "用户" + System.currentTimeMillis());
        double targetEnergy = ((Number) params.getOrDefault("targetEnergy", 50.0)).doubleValue();
        int startTimeSlot = ((Number) params.getOrDefault("startTimeSlot", 0)).intValue();
        int endTimeSlot = ((Number) params.getOrDefault("endTimeSlot", 95)).intValue();
        double minPower = ((Number) params.getOrDefault("minPower", 0.0)).doubleValue();
        double maxPower = ((Number) params.getOrDefault("maxPower", 22.0)).doubleValue();
        
        ChargingUser user = ChargingUser.create(userName, targetEnergy, startTimeSlot, endTimeSlot, minPower, maxPower);
        powerAllocationService.addUser(station, user);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("userId", user.getUserId());
        result.put("message", "用户添加成功");
        return result;
    }

    @DeleteMapping("/station/{stationId}/user/{userId}")
    public Map<String, Object> removeUser(
            @PathVariable String stationId,
            @PathVariable String userId) {
        
        ChargingStation station = stations.get(stationId);
        if (station == null) {
            return errorResult("充电站不存在");
        }
        
        powerAllocationService.removeUser(station, userId);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", "用户已移除");
        return result;
    }

    @PutMapping("/station/{stationId}/timeslot/{timeSlot}")
    public Map<String, Object> updateTimeSlot(
            @PathVariable String stationId,
            @PathVariable int timeSlot) {
        
        ChargingStation station = stations.get(stationId);
        if (station == null) {
            return errorResult("充电站不存在");
        }
        
        powerAllocationService.updateTimeSlot(station, timeSlot);
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("currentTimeSlot", timeSlot);
        result.put("message", "时间槽已更新");
        return result;
    }

    @GetMapping("/station/{stationId}")
    public Map<String, Object> getStationInfo(@PathVariable String stationId) {
        ChargingStation station = stations.get(stationId);
        if (station == null) {
            return errorResult("充电站不存在");
        }
        
        return powerAllocationService.getAllocationResult(station);
    }

    @GetMapping("/station/{stationId}/schedule")
    public Map<String, Object> getDetailedSchedule(@PathVariable String stationId) {
        ChargingStation station = stations.get(stationId);
        if (station == null) {
            return errorResult("充电站不存在");
        }
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stationName", station.getStationName());
        result.put("totalPower", station.getTotalPower());
        result.put("currentTimeSlot", station.getCurrentTimeSlot());
        result.put("schedule", powerAllocationService.getDetailedSchedule(station));
        return result;
    }

    @GetMapping("/stations")
    public Map<String, Object> listStations() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("count", stations.size());
        result.put("stations", stations.keySet());
        return result;
    }

    private Map<String, Object> errorResult(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("error", message);
        return result;
    }
}
