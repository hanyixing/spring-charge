package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.service.ChargeDischargeOptimizationService;
import com.example.demo.service.DemandForecastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 储能设备集群充放电调度控制器
 */
@RestController
@RequestMapping("/api/energy-storage")
public class EnergyStorageController {

    @Autowired
    private ChargeDischargeOptimizationService optimizationService;

    @Autowired
    private DemandForecastService forecastService;

    /**
     * 执行充放电优化调度
     */
    @PostMapping("/optimize")
    public OptimizationResult optimizeChargeDischarge(
            @RequestBody OptimizationRequest request) {
        
        // 创建储能设备集群
        List<StorageDevice> devices = createStorageDevices(request.getDeviceCount());
        
        // 获取历史数据
        List<double[]> historicalData = request.getHistoricalData() != null ? 
                request.getHistoricalData() : 
                forecastService.generateMockHistoricalData(7);
        
        LocalDateTime startTime = request.getStartTime() != null ? 
                request.getStartTime() : LocalDateTime.now();
        
        // 执行优化
        return optimizationService.optimize(devices, startTime, historicalData);
    }

    /**
     * 获取需求预测
     */
    @GetMapping("/forecast")
    public List<DemandForecast> getDemandForecast(
            @RequestParam(defaultValue = "7") int historicalDays) {
        
        List<double[]> historicalData = forecastService.generateMockHistoricalData(historicalDays);
        return forecastService.generateForecast(LocalDateTime.now(), historicalData);
    }

    /**
     * 快速测试接口
     */
    @GetMapping("/test")
    public Map<String, Object> quickTest() {
        Map<String, Object> result = new HashMap<>();
        
        // 创建测试设备
        List<StorageDevice> devices = createStorageDevices(5);
        
        // 生成历史数据
        List<double[]> historicalData = forecastService.generateMockHistoricalData(7);
        
        // 执行优化
        long startTime = System.currentTimeMillis();
        OptimizationResult optimizationResult = optimizationService.optimize(
                devices, LocalDateTime.now(), historicalData);
        long endTime = System.currentTimeMillis();
        
        // 构建响应
        result.put("success", true);
        result.put("optimizationTimeMs", endTime - startTime);
        result.put("totalCost", String.format("%.2f 元", optimizationResult.getTotalCost()));
        result.put("totalRevenue", String.format("%.2f 元", optimizationResult.getTotalRevenue()));
        result.put("netProfit", String.format("%.2f 元", optimizationResult.getNetProfit()));
        result.put("totalChargeEnergy", String.format("%.2f kWh", optimizationResult.getTotalChargeEnergy()));
        result.put("totalDischargeEnergy", String.format("%.2f kWh", optimizationResult.getTotalDischargeEnergy()));
        result.put("initialSoc", String.format("%.2f%%", optimizationResult.getInitialTotalSoc() * 100));
        result.put("finalSoc", String.format("%.2f%%", optimizationResult.getFinalTotalSoc() * 100));
        result.put("constraintsSatisfied", optimizationResult.isConstraintsSatisfied());
        
        if (!optimizationResult.isConstraintsSatisfied()) {
            result.put("constraintViolations", optimizationResult.getConstraintViolations());
        }
        
        // 添加详细的调度计划
        List<Map<String, Object>> scheduleDetails = new ArrayList<>();
        if (optimizationResult.getTotalSchedule() != null) {
            for (PowerSchedule schedule : optimizationResult.getTotalSchedule()) {
                if (schedule.getTimeIndex() % 4 == 0) { // 每小时显示一次
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("timeIndex", schedule.getTimeIndex());
                    detail.put("time", schedule.getTimePoint().toLocalTime().toString());
                    detail.put("chargePower", String.format("%.2f kW", schedule.getChargePower()));
                    detail.put("dischargePower", String.format("%.2f kW", schedule.getDischargePower()));
                    detail.put("netPower", String.format("%.2f kW", schedule.getNetPower()));
                    detail.put("price", String.format("%.2f 元/kWh", schedule.getElectricityPrice()));
                    detail.put("soc", String.format("%.2f%%", schedule.getCumulativeSoc() * 100));
                    detail.put("cost", String.format("%.2f 元", schedule.getEstimatedCost()));
                    scheduleDetails.add(detail);
                }
            }
        }
        result.put("scheduleDetails", scheduleDetails);
        
        return result;
    }

    /**
     * 创建设备集群
     */
    private List<StorageDevice> createStorageDevices(int count) {
        List<StorageDevice> devices = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < count; i++) {
            StorageDevice device = new StorageDevice();
            device.setDeviceId("DEVICE_" + String.format("%03d", i + 1));
            device.setDeviceName("储能设备 " + (i + 1));
            
            // 设置设备参数 (模拟不同类型设备)
            double capacity = 500 + random.nextDouble() * 500; // 500-1000 kWh
            device.setRatedCapacity(capacity);
            device.setMaxChargePower(capacity * 0.5); // 0.5C充电
            device.setMaxDischargePower(capacity * 0.5); // 0.5C放电
            device.setMinChargePower(capacity * 0.05); // 最小5%功率
            device.setMinDischargePower(capacity * 0.05);
            device.setCurrentSoc(0.3 + random.nextDouble() * 0.4); // 初始SOC 30-70%
            device.setMaxSoc(0.95);
            device.setMinSoc(0.1);
            device.setChargeEfficiency(0.95);
            device.setDischargeEfficiency(0.95);
            device.setStatus(StorageDevice.DeviceStatus.IDLE);
            
            devices.add(device);
        }
        
        return devices;
    }

    /**
     * 优化请求对象
     */
    public static class OptimizationRequest {
        private Integer deviceCount = 5;
        private LocalDateTime startTime;
        private List<double[]> historicalData;

        public Integer getDeviceCount() {
            return deviceCount != null ? deviceCount : 5;
        }

        public void setDeviceCount(Integer deviceCount) {
            this.deviceCount = deviceCount;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public List<double[]> getHistoricalData() {
            return historicalData;
        }

        public void setHistoricalData(List<double[]> historicalData) {
            this.historicalData = historicalData;
        }
    }
}
