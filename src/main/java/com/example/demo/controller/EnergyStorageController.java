package com.example.demo.controller;

import com.example.demo.model.ClusterScheduleResult;
import com.example.demo.model.EnergyStorageDevice;
import com.example.demo.model.PowerSchedule;
import com.example.demo.service.ChargeDischargeOptimizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/energy")
public class EnergyStorageController {
    
    @Autowired
    private ChargeDischargeOptimizationService optimizationService;
    
    @PostMapping("/optimize")
    public Map<String, Object> optimizeSchedule(@RequestBody Map<String, Object> request) {
        log.info("收到充放电优化请求");
        
        List<EnergyStorageDevice> devices = parseDevices(request);
        List<Double> historicalDemand = parseHistoricalDemand(request);
        
        ClusterScheduleResult result = optimizationService.optimizeClusterSchedule(devices, historicalDemand);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", result.getMessage());
        response.put("data", formatResult(result));
        
        return response;
    }
    
    @GetMapping("/demo")
    public Map<String, Object> demoOptimize() {
        log.info("执行演示优化");
        
        List<EnergyStorageDevice> devices = createDemoDevices();
        List<Double> historicalDemand = createDemoHistoricalDemand();
        
        ClusterScheduleResult result = optimizationService.optimizeClusterSchedule(devices, historicalDemand);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", result.getMessage());
        response.put("data", formatResult(result));
        
        return response;
    }
    
    @GetMapping("/devices")
    public Map<String, Object> getDemoDevices() {
        List<EnergyStorageDevice> devices = createDemoDevices();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("devices", devices);
        
        return response;
    }
    
    private List<EnergyStorageDevice> parseDevices(Map<String, Object> request) {
        List<EnergyStorageDevice> devices = new ArrayList<>();
        
        Object devicesObj = request.get("devices");
        if (devicesObj instanceof List) {
            for (Object obj : (List<?>) devicesObj) {
                if (obj instanceof Map) {
                    Map<?, ?> deviceMap = (Map<?, ?>) obj;
                    EnergyStorageDevice device = EnergyStorageDevice.builder()
                            .id(getLong(deviceMap.get("id")))
                            .name(getString(deviceMap.get("name")))
                            .maxPower(getDouble(deviceMap.get("maxPower")))
                            .minPower(getDouble(deviceMap.get("minPower")))
                            .currentSoc(getDouble(deviceMap.get("currentSoc")))
                            .maxCapacity(getDouble(deviceMap.get("maxCapacity")))
                            .efficiency(getDouble(deviceMap.get("efficiency"), 0.95))
                            .build();
                    devices.add(device);
                }
            }
        }
        
        if (devices.isEmpty()) {
            devices = createDemoDevices();
        }
        
        return devices;
    }
    
    private List<Double> parseHistoricalDemand(Map<String, Object> request) {
        List<Double> historicalDemand = new ArrayList<>();
        
        Object demandObj = request.get("historicalDemand");
        if (demandObj instanceof List) {
            for (Object obj : (List<?>) demandObj) {
                if (obj instanceof Number) {
                    historicalDemand.add(((Number) obj).doubleValue());
                }
            }
        }
        
        return historicalDemand;
    }
    
    private List<EnergyStorageDevice> createDemoDevices() {
        List<EnergyStorageDevice> devices = new ArrayList<>();
        
        devices.add(EnergyStorageDevice.builder()
                .id(1L)
                .name("储能设备A")
                .maxPower(100.0)
                .minPower(10.0)
                .currentSoc(200.0)
                .maxCapacity(400.0)
                .efficiency(0.95)
                .build());
        
        devices.add(EnergyStorageDevice.builder()
                .id(2L)
                .name("储能设备B")
                .maxPower(80.0)
                .minPower(8.0)
                .currentSoc(150.0)
                .maxCapacity(300.0)
                .efficiency(0.92)
                .build());
        
        devices.add(EnergyStorageDevice.builder()
                .id(3L)
                .name("储能设备C")
                .maxPower(120.0)
                .minPower(12.0)
                .currentSoc(250.0)
                .maxCapacity(500.0)
                .efficiency(0.93)
                .build());
        
        return devices;
    }
    
    private List<Double> createDemoHistoricalDemand() {
        List<Double> demand = new ArrayList<>();
        java.util.Random random = new java.util.Random(42);
        
        for (int i = 0; i < 96; i++) {
            int hour = i / 4;
            double baseDemand;
            
            if (hour >= 8 && hour < 12) {
                baseDemand = 50 + random.nextDouble() * 30;
            } else if (hour >= 14 && hour < 18) {
                baseDemand = 60 + random.nextDouble() * 40;
            } else if (hour >= 18 && hour < 22) {
                baseDemand = 80 + random.nextDouble() * 50;
            } else if (hour >= 22 || hour < 6) {
                baseDemand = 20 + random.nextDouble() * 20;
            } else {
                baseDemand = 30 + random.nextDouble() * 25;
            }
            
            demand.add(Math.round(baseDemand * 100.0) / 100.0);
        }
        
        return demand;
    }
    
    private Map<String, Object> formatResult(ClusterScheduleResult result) {
        Map<String, Object> data = new HashMap<>();
        
        data.put("totalCost", result.getTotalCost());
        data.put("totalDemandMet", result.getTotalDemandMet());
        data.put("feasible", result.isFeasible());
        data.put("clusterPower", result.getClusterPower());
        data.put("clusterSoc", result.getClusterSoc());
        data.put("demandList", result.getDemandList());
        data.put("priceList", result.getPriceList());
        
        List<Map<String, Object>> deviceSchedules = new ArrayList<>();
        for (PowerSchedule schedule : result.getDeviceSchedules()) {
            Map<String, Object> scheduleMap = new HashMap<>();
            scheduleMap.put("deviceId", schedule.getDeviceId());
            scheduleMap.put("deviceName", schedule.getDeviceName());
            scheduleMap.put("chargePower", schedule.getChargePower());
            scheduleMap.put("dischargePower", schedule.getDischargePower());
            scheduleMap.put("socList", schedule.getSocList());
            scheduleMap.put("totalCost", schedule.getTotalCost());
            deviceSchedules.add(scheduleMap);
        }
        data.put("deviceSchedules", deviceSchedules);
        
        data.put("summary", generateSummary(result));
        
        return data;
    }
    
    private Map<String, Object> generateSummary(ClusterScheduleResult result) {
        Map<String, Object> summary = new HashMap<>();
        
        List<Double> clusterPower = result.getClusterPower();
        List<Double> demandList = result.getDemandList();
        
        double maxPower = clusterPower.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double minPower = clusterPower.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double avgPower = clusterPower.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        int peakDischargeSlot = 0;
        double peakDischarge = 0;
        for (int i = 0; i < clusterPower.size(); i++) {
            if (clusterPower.get(i) > peakDischarge) {
                peakDischarge = clusterPower.get(i);
                peakDischargeSlot = i;
            }
        }
        
        int peakChargeSlot = 0;
        double peakCharge = 0;
        for (int i = 0; i < clusterPower.size(); i++) {
            if (clusterPower.get(i) < peakCharge) {
                peakCharge = clusterPower.get(i);
                peakChargeSlot = i;
            }
        }
        
        summary.put("maxDischargePower", maxPower);
        summary.put("maxChargePower", Math.abs(minPower));
        summary.put("avgPower", avgPower);
        summary.put("peakDischargeTime", formatSlotToTime(peakDischargeSlot));
        summary.put("peakChargeTime", formatSlotToTime(peakChargeSlot));
        summary.put("totalDevices", result.getDeviceSchedules().size());
        summary.put("demandSatisfactionRate", String.format("%.2f%%", result.getTotalDemandMet() * 100));
        
        return summary;
    }
    
    private String formatSlotToTime(int slot) {
        int hour = slot / 4;
        int minute = (slot % 4) * 15;
        return String.format("%02d:%02d", hour, minute);
    }
    
    private Long getLong(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return 0L;
    }
    
    private Double getDouble(Object obj) {
        return getDouble(obj, 0.0);
    }
    
    private Double getDouble(Object obj, double defaultValue) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        return defaultValue;
    }
    
    private String getString(Object obj) {
        return obj != null ? obj.toString() : "";
    }
}
