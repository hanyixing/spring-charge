package com.example.demo.controller;

import com.example.demo.service.ChargeRewardService;
import com.example.demo.service.ChargeRewardService.ChargeRecord;
import com.example.demo.service.ChargeRewardService.ChargeSegment;
import com.example.demo.service.ChargeRewardService.ChargeSession;
import com.example.demo.service.ChargeRewardService.DailyRewardResult;
import com.example.demo.service.ChargeRewardService.MultiDayRewardResult;
import com.example.demo.service.ChargeRewardService.TimeSlotReward;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/charge")
@RequiredArgsConstructor
public class ChargeRewardController {

    private final ChargeRewardService chargeRewardService;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Data
    public static class ChargeRequest {
        private double[] kwhData;
    }

    @Data
    public static class ChargeSessionRequest {
        private String userId;
        private String chargeId;
        private String startTime;
        private String endTime;
        private double totalKwh;
    }

    @Data
    public static class ChargeResponse {
        private boolean success;
        private String message;
        private DailyRewardResult data;
        private Summary summary;
    }

    @Data
    public static class MultiDayChargeResponse {
        private boolean success;
        private String message;
        private MultiDayRewardResult data;
        private MultiDaySummary summary;
    }

    @Data
    public static class Summary {
        private double totalKwh;
        private BigDecimal totalReward;
        private double rewardKwh;
        private double nonRewardKwh;
        private double rewardRatio;
        private double peakShiftRate;
    }

    @Data
    public static class MultiDaySummary {
        private String userId;
        private String chargeId;
        private String startTime;
        private String endTime;
        private int totalDays;
        private double totalKwh;
        private BigDecimal totalReward;
        private double rewardKwh;
        private double nonRewardKwh;
        private double rewardRatio;
        private int totalPoints;
        private List<DailySummary> dailySummaries;
    }

    @Data
    public static class DailySummary {
        private String date;
        private double totalKwh;
        private BigDecimal totalReward;
        private double rewardKwh;
        private double nonRewardKwh;
        private double rewardRatio;
        private int pointCount;
    }

    @GetMapping("/slots")
    public Map<String, Object> getRewardSlots() {
        List<TimeSlotReward> slots = chargeRewardService.getRewardSlots();
        List<Map<String, Object>> slotList = new ArrayList<>();

        for (TimeSlotReward slot : slots) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("startPoint", slot.getStartPoint());
            map.put("endPoint", slot.getEndPoint());
            map.put("startTime", slot.getStartTime().toString());
            map.put("endTime", slot.getEndTime().toString());
            map.put("rewardRate", slot.getRewardRate());
            map.put("description", slot.getDescription());
            slotList.add(map);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("slots", slotList);
        return response;
    }

    @GetMapping("/points")
    public Map<String, Object> getAllPoints() {
        List<Map<String, Object>> points = new ArrayList<>();

        for (int i = 0; i < 96; i++) {
            Map<String, Object> point = new LinkedHashMap<>();
            String timeRange = ChargeRewardService.getTimeRangeByPoint(i);
            boolean isReward = chargeRewardService.isRewardSlot(i);
            BigDecimal rate = chargeRewardService.getRewardRate(i);

            point.put("point", i);
            point.put("timeRange", timeRange);
            point.put("isRewardSlot", isReward);
            point.put("rewardRate", rate);
            points.add(point);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("points", points);
        return response;
    }

    @PostMapping("/calculate")
    public ChargeResponse calculateReward(@RequestBody ChargeRequest request) {
        ChargeResponse response = new ChargeResponse();

        if (request == null || request.getKwhData() == null) {
            response.setSuccess(false);
            response.setMessage("请求数据不能为空");
            return response;
        }

        if (request.getKwhData().length != 96) {
            response.setSuccess(false);
            response.setMessage("需要提供96个点的充电数据（每15分钟一个点）");
            return response;
        }

        try {
            DailyRewardResult result = chargeRewardService.calculateDailyReward(request.getKwhData());

            Summary summary = new Summary();
            summary.setTotalKwh(result.getTotalKwh());
            summary.setTotalReward(result.getTotalReward());
            summary.setRewardKwh(result.getRewardKwh());
            summary.setNonRewardKwh(result.getNonRewardKwh());
            summary.setRewardRatio(result.getTotalKwh() > 0 ? result.getRewardKwh() / result.getTotalKwh() * 100 : 0);
            summary.setPeakShiftRate(result.getTotalKwh() > 0 ? result.getRewardKwh() / result.getTotalKwh() : 0);

            response.setSuccess(true);
            response.setMessage("计算成功");
            response.setData(result);
            response.setSummary(summary);

            log.info("单日充电奖励计算完成: 日期={}, 总电量={}kWh, 奖励金额={}元, 谷时占比={}%",
                    result.getDate(), result.getTotalKwh(), result.getTotalReward(), String.format("%.2f", summary.getRewardRatio()));

        } catch (Exception e) {
            log.error("计算奖励失败", e);
            response.setSuccess(false);
            response.setMessage("计算失败: " + e.getMessage());
        }

        return response;
    }

    @PostMapping("/calculate-session")
    public MultiDayChargeResponse calculateSession(@RequestBody ChargeSessionRequest request) {
        MultiDayChargeResponse response = new MultiDayChargeResponse();

        if (request == null || request.getStartTime() == null || request.getEndTime() == null) {
            response.setSuccess(false);
            response.setMessage("请求数据不能为空");
            return response;
        }

        try {
            LocalDateTime startTime = LocalDateTime.parse(request.getStartTime(), DATE_TIME_FORMATTER);
            LocalDateTime endTime = LocalDateTime.parse(request.getEndTime(), DATE_TIME_FORMATTER);

            ChargeSession session = new ChargeSession(
                    request.getUserId(),
                    request.getChargeId(),
                    startTime,
                    endTime,
                    request.getTotalKwh()
            );

            MultiDayRewardResult result = chargeRewardService.calculateMultiDayReward(session);

            MultiDaySummary summary = new MultiDaySummary();
            summary.setUserId(result.getUserId());
            summary.setChargeId(result.getChargeId());
            summary.setStartTime(result.getStartTime().format(DATE_TIME_FORMATTER));
            summary.setEndTime(result.getEndTime().format(DATE_TIME_FORMATTER));
            summary.setTotalDays(result.getDailyResults().size());
            summary.setTotalKwh(result.getTotalKwh());
            summary.setTotalReward(result.getTotalReward());
            summary.setRewardKwh(result.getRewardKwh());
            summary.setNonRewardKwh(result.getNonRewardKwh());
            summary.setRewardRatio(result.getTotalKwh() > 0 ? result.getRewardKwh() / result.getTotalKwh() * 100 : 0);
            summary.setTotalPoints(result.getTotalPoints());

            List<DailySummary> dailySummaries = new ArrayList<>();
            for (DailyRewardResult daily : result.getDailyResults()) {
                DailySummary ds = new DailySummary();
                ds.setDate(daily.getDate().format(DATE_FORMATTER));
                ds.setTotalKwh(daily.getTotalKwh());
                ds.setTotalReward(daily.getTotalReward());
                ds.setRewardKwh(daily.getRewardKwh());
                ds.setNonRewardKwh(daily.getNonRewardKwh());
                ds.setRewardRatio(daily.getTotalKwh() > 0 ? daily.getRewardKwh() / daily.getTotalKwh() * 100 : 0);
                ds.setPointCount(daily.getRecords().size());
                dailySummaries.add(ds);
            }
            summary.setDailySummaries(dailySummaries);

            response.setSuccess(true);
            response.setMessage("计算成功");
            response.setData(result);
            response.setSummary(summary);

        } catch (Exception e) {
            log.error("计算跨天充电奖励失败", e);
            response.setSuccess(false);
            response.setMessage("计算失败: " + e.getMessage());
        }

        return response;
    }

    @GetMapping("/demo")
    public ChargeResponse demoCalculation() {
        double[] demoData = generateDemoData();
        ChargeRequest request = new ChargeRequest();
        request.setKwhData(demoData);
        return calculateReward(request);
    }

    @GetMapping("/demo-session")
    public MultiDayChargeResponse demoSession() {
        ChargeSessionRequest request = new ChargeSessionRequest();
        request.setUserId("USER001");
        request.setChargeId("CHG20250413001");
        request.setStartTime("2025-04-12 23:30:00");
        request.setEndTime("2025-04-13 02:30:00");
        request.setTotalKwh(50.0);
        return calculateSession(request);
    }

    private double[] generateDemoData() {
        double[] data = new double[96];
        Random random = new Random();

        for (int i = 0; i < 96; i++) {
            String timeRange = ChargeRewardService.getTimeRangeByPoint(i);
            int hour = ChargeRewardService.pointToTime(i).getHour();

            double baseKwh = 0.5 + random.nextDouble() * 1.5;

            if (hour >= 0 && hour < 6) {
                data[i] = baseKwh * 1.5;
            } else if (hour >= 10 && hour < 15) {
                data[i] = baseKwh * 0.5;
            } else if (hour >= 18 && hour < 22) {
                data[i] = baseKwh * 1.2;
            } else {
                data[i] = baseKwh;
            }
        }

        return data;
    }
}
