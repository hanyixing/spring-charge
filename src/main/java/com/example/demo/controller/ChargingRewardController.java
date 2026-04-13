package com.example.demo.controller;

import com.example.demo.dto.ChargingRequest;
import com.example.demo.dto.ChargingRewardResponse;
import com.example.demo.service.ChargingRewardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/charging")
public class ChargingRewardController {

    private final ChargingRewardService chargingRewardService;

    public ChargingRewardController(ChargingRewardService chargingRewardService) {
        this.chargingRewardService = chargingRewardService;
    }

    @PostMapping("/reward")
    public Map<String, Object> calculateReward(@RequestBody ChargingRequest request) {
        log.info("计算充电奖励: startPoint={}, endPoint={}, chargingKwh={}, rewardPerKwh={}, days={}",
                request.getStartPoint(), request.getEndPoint(),
                request.getChargingKwh(), request.getRewardPerKwh(), request.getDays());

        ChargingRewardResponse response = chargingRewardService.calculateReward(
                request.getStartPoint(),
                request.getEndPoint(),
                request.getChargingKwh(),
                request.getRewardPerKwh(),
                request.getDays()
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", response);
        result.put("message", "奖励计算成功");

        return result;
    }

    @GetMapping("/reward")
    public Map<String, Object> calculateRewardGet(
            @RequestParam Integer startPoint,
            @RequestParam Integer endPoint,
            @RequestParam Double chargingKwh,
            @RequestParam(defaultValue = "0.5") Double rewardPerKwh,
            @RequestParam(required = false) Integer days) {

        ChargingRewardResponse response = chargingRewardService.calculateReward(
                startPoint, endPoint, chargingKwh, rewardPerKwh, days
        );

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", response);
        return result;
    }

    @GetMapping("/timeslots")
    public Map<String, Object> getAllTimeSlots() {
        List<ChargingRewardService.TimeSlotInfo> slots = chargingRewardService.getAllTimeSlots();

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", slots);
        result.put("total", slots.size());
        result.put("message", "共96个时间点，谷时为奖励时段");

        return result;
    }

    @GetMapping("/timeslot")
    public Map<String, Object> getTimeSlotInfo(@RequestParam Integer point) {
        Map<String, Object> data = new HashMap<>();
        data.put("point", point);
        data.put("timeRange", chargingRewardService.pointToTimeRange(point));
        data.put("timeSlotType", chargingRewardService.getTimeSlotType(point).getName());
        data.put("isRewardPeriod", chargingRewardService.isRewardPeriod(point));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", data);
        return result;
    }

    @GetMapping("/demo")
    public Map<String, Object> demo() {
        log.info("执行充电奖励演示");

        List<Map<String, Object>> demoCases = new ArrayList<>();
        demoCases.add(createDemoCase("谷时充电案例", 0, 8, 10.0, 0.5, null));
        demoCases.add(createDemoCase("峰时充电案例", 32, 40, 10.0, 0.5, null));
        demoCases.add(createDemoCase("跨时段充电案例", 20, 36, 10.0, 0.5, null));
        demoCases.add(createDemoCase("全天充电案例", 0, 96, 50.0, 0.5, null));
        demoCases.add(createDemoCase("跨天充电案例(22:00-次日06:00)", 88, 24, 10.0, 0.5, null));
        demoCases.add(createDemoCase("跨天充电案例(23:00-次日07:00)", 92, 28, 20.0, 0.5, null));
        demoCases.add(createDemoCase("跨两天充电案例(今日23:00-第三日24:00)", 92, 96, 100.0, 0.5, 2));
        demoCases.add(createDemoCase("跨三天充电案例(今日23:00-第四日07:00)", 92, 28, 200.0, 0.5, 3));

        Map<String, Object> algorithm = new HashMap<>();
        algorithm.put("description", "充电奖励算法");
        algorithm.put("totalPoints", 96);
        algorithm.put("valleyPeriod", "23:00-07:00 (奖励时段)");
        algorithm.put("peakPeriod", "08:00-12:00, 17:00-21:00 (无奖励)");
        algorithm.put("rewardRule", "奖励金额 = 充电量(kWh) × 奖励单价(元/kWh) × (奖励时段占比)");
        algorithm.put("crossDaySupport", "支持跨多天充电，通过days参数指定跨越天数");

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", demoCases);
        result.put("algorithm", algorithm);

        return result;
    }

    private Map<String, Object> createDemoCase(String name, Integer start, Integer end,
                                                Double kwh, Double reward, Integer days) {
        ChargingRewardResponse response = chargingRewardService.calculateReward(start, end, kwh, reward, days);
        Map<String, Object> caseData = new HashMap<>();
        caseData.put("caseName", name);
        caseData.put("startPoint", start);
        caseData.put("endPoint", end);
        caseData.put("startTime", response.getStartTime());
        caseData.put("endTime", response.getEndTime());
        caseData.put("chargingKwh", kwh);
        caseData.put("rewardPerKwh", reward);
        caseData.put("totalReward", response.getTotalReward());
        caseData.put("timeSlotType", response.getTimeSlotType());
        caseData.put("isRewardPeriod", response.getIsRewardPeriod());
        caseData.put("isCrossDay", response.getIsCrossDay());
        caseData.put("days", response.getDays());
        caseData.put("totalPoints", response.getTotalPoints());
        caseData.put("rewardPoints", response.getRewardPoints());
        return caseData;
    }
}
