package com.example.demo.controller;

import com.example.demo.entity.ChargingPlan;
import com.example.demo.entity.ChargingRequest;
import com.example.demo.entity.ElectricityPrice;
import com.example.demo.service.ChargingOptimizationService;
import com.example.demo.service.ElectricityPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 充电优化控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/charging")
public class ChargingController {

    @Autowired
    private ChargingOptimizationService chargingService;

    @Autowired
    private ElectricityPriceService priceService;

    /**
     * 计算最优充电计划
     */
    @PostMapping("/plan")
    public Map<String, Object> calculateChargingPlan(@RequestBody ChargingRequest request) {
        log.info("收到充电计划计算请求: {}", request);

        long startTime = System.currentTimeMillis();
        ChargingPlan plan = chargingService.calculateOptimalChargingPlan(request);
        long endTime = System.currentTimeMillis();

        Map<String, Object> response = new HashMap<>();
        response.put("success", plan.isSuccess());
        response.put("message", plan.getMessage());
        response.put("calculationTimeMs", endTime - startTime);

        if (plan.isSuccess()) {
            Map<String, Object> data = new HashMap<>();
            data.put("requiredEnergy", String.format("%.2f", plan.getRequiredEnergy()));
            data.put("totalCost", String.format("%.2f", plan.getTotalCost()));
            data.put("averagePrice", String.format("%.3f", plan.getTotalCost() / plan.getRequiredEnergy()));
            data.put("chargingDuration", plan.getChargingDuration());
            data.put("chargingDurationHours", String.format("%.2f", plan.getChargingDuration() / 60.0));
            data.put("segments", plan.getSegments());

            // 功率分布详情
            Map<String, Object> powerDist = new LinkedHashMap<>();
            for (int i = request.getStartTimePoint(); i < request.getEndTimePoint(); i++) {
                Double power = plan.getPowerDistribution().get(i);
                if (power != null && power > 0) {
                    powerDist.put(String.format("point_%d_%s", i, pointToTime(i)),
                            String.format("%.2f kW", power));
                }
            }
            data.put("powerDistribution", powerDist);

            response.put("data", data);
        }

        return response;
    }

    /**
     * 获取当前电价配置
     */
    @GetMapping("/prices")
    public Map<String, Object> getCurrentPrices() {
        List<ElectricityPrice> prices = priceService.getCurrentPrices();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", prices);

        // 96点电价详情
        double[] priceArray = priceService.getPriceArray();
        ElectricityPrice.PriceType[] typeArray = priceService.getPriceTypeArray();

        List<Map<String, Object>> pointDetails = new ArrayList<>();
        for (int i = 0; i < 96; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("point", i);
            point.put("time", pointToTime(i));
            point.put("price", priceArray[i]);
            point.put("type", typeArray[i]);
            pointDetails.add(point);
        }
        response.put("pointDetails", pointDetails);

        return response;
    }

    /**
     * 更新电价配置
     */
    @PostMapping("/prices")
    public Map<String, Object> updatePrices(@RequestBody List<ElectricityPrice> prices) {
        priceService.updatePrices(prices);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "电价配置已更新");
        return response;
    }

    /**
     * 重置为默认电价配置
     */
    @PostMapping("/prices/reset")
    public Map<String, Object> resetPrices() {
        priceService.resetToDefault();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "电价配置已重置为默认值");
        return response;
    }

    /**
     * 测试接口 - 使用默认参数计算充电计划
     */
    @GetMapping("/test")
    public Map<String, Object> testChargingPlan() {
        // 默认测试参数
        ChargingRequest request = ChargingRequest.builder()
                .currentSoc(20.0)
                .targetSoc(80.0)
                .startTimePoint(0)
                .endTimePoint(96)
                .batteryCapacity(60.0)
                .maxChargingPower(7.0)
                .build();

        return calculateChargingPlan(request);
    }

    /**
     * 时间点索引转换为时间字符串（HH:mm）
     */
    private String pointToTime(int point) {
        int totalMinutes = point * 15;
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}
