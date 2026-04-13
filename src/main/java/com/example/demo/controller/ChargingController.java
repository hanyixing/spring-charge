package com.example.demo.controller;

import com.example.demo.model.ChargingUser;
import com.example.demo.model.PowerAllocationResult;
import com.example.demo.service.PowerAllocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/charging")
public class ChargingController {

    @Autowired
    private PowerAllocationService allocationService;

    @PostMapping("/station/power")
    public ResponseEntity<String> setStationPower(@RequestParam double maxPower) {
        allocationService.setStationMaxPower(maxPower);
        return ResponseEntity.ok("充电站总功率设置为: " + maxPower + " kW");
    }

    @PostMapping("/users")
    public ResponseEntity<String> addUser(@RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        double targetEnergy = ((Number) request.get("targetEnergy")).doubleValue();
        LocalDateTime startTime = LocalDateTime.parse((String) request.get("startTime"));
        LocalDateTime endTime = LocalDateTime.parse((String) request.get("endTime"));

        ChargingUser user = new ChargingUser(userId, targetEnergy, startTime, endTime);
        allocationService.addUser(user);
        return ResponseEntity.ok("用户 " + userId + " 已加入充电队列");
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<String> removeUser(@PathVariable String userId) {
        allocationService.removeUser(userId);
        return ResponseEntity.ok("用户 " + userId + " 已取消充电");
    }

    @GetMapping("/users")
    public ResponseEntity<List<ChargingUser>> getActiveUsers() {
        return ResponseEntity.ok(allocationService.getActiveUsers());
    }

    @PostMapping("/allocate")
    public ResponseEntity<String> allocatePower() {
        PowerAllocationResult result = allocationService.allocatePower();
        String formattedResult = allocationService.formatAllocationResult(result);
        System.out.println(formattedResult);
        return ResponseEntity.ok(formattedResult);
    }

    @PostMapping("/test")
    public ResponseEntity<String> runTest() {
        allocationService.setStationMaxPower(500);

        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();

        allocationService.addUser(new ChargingUser("User001", 60,
                today.plusHours(8), today.plusHours(12)));

        allocationService.addUser(new ChargingUser("User002", 80,
                today.plusHours(9), today.plusHours(15)));

        allocationService.addUser(new ChargingUser("User003", 50,
                today.plusHours(10), today.plusHours(14)));

        allocationService.addUser(new ChargingUser("User004", 100,
                today.plusHours(18), today.plusHours(22)));

        PowerAllocationResult result = allocationService.allocatePower();
        String formattedResult = allocationService.formatAllocationResult(result);
        System.out.println(formattedResult);
        return ResponseEntity.ok(formattedResult);
    }
}
