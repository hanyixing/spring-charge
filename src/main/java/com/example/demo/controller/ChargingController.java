package com.example.demo.controller;

import com.example.demo.model.AllocationResult;
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
    
    @PostMapping("/station/power")
    public Map<String, Object> setStationPower(@RequestBody Map<String, Object> request) {
        double totalPower = Double.parseDouble(request.get("totalPower").toString());
        powerAllocationService.setStationPower(totalPower);
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "充电站功率设置成功");
        response.put("totalPower", totalPower);
        return response;
    }
    
    @PostMapping("/user/add")
    public Map<String, Object> addUser(@RequestBody Map<String, Object> request) {
        double currentEnergy = Double.parseDouble(request.get("currentEnergy").toString());
        double targetEnergy = Double.parseDouble(request.get("targetEnergy").toString());
        int startTimeSlot = Integer.parseInt(request.get("startTimeSlot").toString());
        int endTimeSlot = Integer.parseInt(request.get("endTimeSlot").toString());
        double maxChargingPower = Double.parseDouble(request.getOrDefault("maxChargingPower", "22").toString());
        
        ChargingUser user = powerAllocationService.addUser(currentEnergy, targetEnergy, 
                                                           startTimeSlot, endTimeSlot, maxChargingPower);
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "用户添加成功");
        response.put("userId", user.getUserId());
        response.put("currentEnergy", user.getCurrentEnergy());
        response.put("targetEnergy", user.getTargetEnergy());
        response.put("requiredEnergy", user.getRequiredEnergy());
        response.put("startTimeSlot", startTimeSlot);
        response.put("endTimeSlot", endTimeSlot);
        response.put("timeRange", formatSlotRange(startTimeSlot, endTimeSlot));
        return response;
    }
    
    @DeleteMapping("/user/{userId}")
    public Map<String, Object> removeUser(@PathVariable String userId) {
        powerAllocationService.removeUser(userId);
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "用户已取消充电");
        response.put("userId", userId);
        return response;
    }
    
    @PutMapping("/user/{userId}/energy")
    public Map<String, Object> updateUserEnergy(@PathVariable String userId, 
                                                 @RequestBody Map<String, Object> request) {
        double currentEnergy = Double.parseDouble(request.get("currentEnergy").toString());
        powerAllocationService.updateUserEnergy(userId, currentEnergy);
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "用户电量已更新");
        response.put("userId", userId);
        response.put("currentEnergy", currentEnergy);
        return response;
    }
    
    @GetMapping("/users")
    public Map<String, Object> getAllUsers() {
        List<ChargingUser> users = powerAllocationService.getAllUsers();
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", users.size());
        response.put("users", users);
        return response;
    }
    
    @GetMapping("/user/{userId}/schedule")
    public Map<String, Object> getUserSchedule(@PathVariable String userId) {
        return powerAllocationService.getUserSchedule(userId);
    }
    
    @GetMapping("/allocation/{slot}")
    public Map<String, Object> getAllocationAtSlot(@PathVariable int slot) {
        AllocationResult result = powerAllocationService.getAllocationAtSlot(slot);
        
        Map<String, Object> response = new LinkedHashMap<>();
        if (result != null) {
            response.put("slot", result.getTimeSlot());
            response.put("timeRange", result.getTimeRange());
            response.put("totalPower", result.getTotalPower());
            response.put("remainingPower", result.getRemainingPower());
            response.put("userAllocations", result.getUserAllocations());
        }
        return response;
    }
    
    @GetMapping("/allocation/all")
    public Map<String, Object> getAllAllocations() {
        List<AllocationResult> results = powerAllocationService.getAllAllocations();
        
        List<Map<String, Object>> allocations = new ArrayList<>();
        for (AllocationResult result : results) {
            if (result.getTotalPower() > 0) {
                Map<String, Object> alloc = new LinkedHashMap<>();
                alloc.put("slot", result.getTimeSlot());
                alloc.put("timeRange", result.getTimeRange());
                alloc.put("totalPower", result.getTotalPower());
                alloc.put("remainingPower", result.getRemainingPower());
                alloc.put("userAllocations", result.getUserAllocations());
                allocations.add(alloc);
            }
        }
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total", allocations.size());
        response.put("allocations", allocations);
        return response;
    }
    
    @GetMapping("/station/info")
    public Map<String, Object> getStationInfo() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("stationId", powerAllocationService.getStation().getStationId());
        response.put("totalPower", powerAllocationService.getStation().getTotalPower());
        response.put("totalUsers", powerAllocationService.getStation().getUsers().size());
        response.put("activeUsers", powerAllocationService.getStation().getActiveUsers().size());
        return response;
    }
    
    @PostMapping("/test/demo")
    public Map<String, Object> runDemo() {
        log.info("========== 开始演示充电站功率分配算法 ==========");
        
        powerAllocationService.setStationPower(100.0);
        
        log.info("场景1: 添加3个用户");
        ChargingUser user1 = powerAllocationService.addUser(10, 50, 0, 32, 22);
        ChargingUser user2 = powerAllocationService.addUser(20, 60, 16, 48, 22);
        ChargingUser user3 = powerAllocationService.addUser(5, 40, 8, 40, 22);
        
        Map<String, Object> result1 = new LinkedHashMap<>();
        result1.put("scenario", "初始状态 - 3个用户");
        result1.put("user1", powerAllocationService.getUserSchedule(user1.getUserId()));
        result1.put("user2", powerAllocationService.getUserSchedule(user2.getUserId()));
        result1.put("user3", powerAllocationService.getUserSchedule(user3.getUserId()));
        
        log.info("场景2: 用户2取消充电");
        powerAllocationService.removeUser(user2.getUserId());
        
        Map<String, Object> result2 = new LinkedHashMap<>();
        result2.put("scenario", "用户2取消后 - 剩余2个用户");
        result2.put("user1", powerAllocationService.getUserSchedule(user1.getUserId()));
        result2.put("user3", powerAllocationService.getUserSchedule(user3.getUserId()));
        
        log.info("场景3: 新用户4加入充电队列");
        ChargingUser user4 = powerAllocationService.addUser(0, 80, 24, 64, 30);
        
        Map<String, Object> result3 = new LinkedHashMap<>();
        result3.put("scenario", "用户4加入后 - 3个用户");
        result3.put("user1", powerAllocationService.getUserSchedule(user1.getUserId()));
        result3.put("user3", powerAllocationService.getUserSchedule(user3.getUserId()));
        result3.put("user4", powerAllocationService.getUserSchedule(user4.getUserId()));
        
        log.info("场景4: 用户1充电进度更新");
        powerAllocationService.updateUserEnergy(user1.getUserId(), 35);
        
        Map<String, Object> result4 = new LinkedHashMap<>();
        result4.put("scenario", "用户1电量更新后");
        result4.put("user1", powerAllocationService.getUserSchedule(user1.getUserId()));
        result4.put("user3", powerAllocationService.getUserSchedule(user3.getUserId()));
        result4.put("user4", powerAllocationService.getUserSchedule(user4.getUserId()));
        
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "演示完成");
        response.put("stationInfo", getStationInfo());
        response.put("scenario1", result1);
        response.put("scenario2", result2);
        response.put("scenario3", result3);
        response.put("scenario4", result4);
        response.put("allAllocations", getAllAllocations());
        
        log.info("========== 演示结束 ==========");
        return response;
    }
    
    private String formatSlotRange(int startSlot, int endSlot) {
        return formatSlotTime(startSlot) + " - " + formatSlotTime(endSlot + 1);
    }
    
    private String formatSlotTime(int slot) {
        int hour = slot / 4;
        int minute = (slot % 4) * 15;
        return String.format("%02d:%02d", hour, minute);
    }
}
