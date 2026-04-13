package com.example.demo.service;

import com.example.demo.entity.ChargingPlan;
import com.example.demo.entity.ChargingPlan.ChargingSegment;
import com.example.demo.entity.ChargingRequest;
import com.example.demo.entity.ElectricityPrice;
import com.example.demo.entity.ElectricityPrice.PriceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 充电优化服务
 * 实现充电省钱算法，计算最优充电策略
 */
@Slf4j
@Service
public class ChargingOptimizationService {

    @Autowired
    private ElectricityPriceService priceService;

    // 每个时间点的时长（小时）= 15分钟 = 0.25小时
    private static final double TIME_SLOT_HOURS = 0.25;

    /**
     * 计算最优充电计划
     * 核心算法：在允许的时间范围内，优先在低价时段充电
     *
     * @param request 充电请求
     * @return 充电计划
     */
    public ChargingPlan calculateOptimalChargingPlan(ChargingRequest request) {
        log.info("开始计算最优充电计划: currentSoc={}, targetSoc={}, startPoint={}, endPoint={}",
                request.getCurrentSoc(), request.getTargetSoc(),
                request.getStartTimePoint(), request.getEndTimePoint());

        // 验证请求
        if (!request.isValid()) {
            return ChargingPlan.builder()
                    .success(false)
                    .message("请求参数无效：请检查电量范围和时间设置")
                    .build();
        }

        // 计算需要充电的电量
        double socDiff = request.getTargetSoc() - request.getCurrentSoc();
        double requiredEnergy = socDiff / 100.0 * request.getBatteryCapacity();

        // 计算可用时间窗口
        int startPoint = request.getStartTimePoint();
        int endPoint = request.getEndTimePoint();
        int availableSlots = endPoint - startPoint;

        // 计算最大可充电量
        double maxEnergy = availableSlots * request.getMaxChargingPower() * TIME_SLOT_HOURS;

        // 检查是否能在时间窗口内完成充电
        if (requiredEnergy > maxEnergy) {
            return ChargingPlan.builder()
                    .success(false)
                    .message(String.format(
                            "在设定的时间范围内无法完成充电。需要电量: %.2f kWh, 最大可充电量: %.2f kWh",
                            requiredEnergy, maxEnergy))
                    .requiredEnergy(requiredEnergy)
                    .build();
        }

        // 获取电价信息
        double[] prices = priceService.getPriceArray();
        PriceType[] priceTypes = priceService.getPriceTypeArray();

        // 创建时间槽列表，用于排序
        List<TimeSlot> timeSlots = new ArrayList<>();
        for (int i = startPoint; i < endPoint; i++) {
            timeSlots.add(new TimeSlot(i, prices[i], priceTypes[i]));
        }

        // 按电价从低到高排序（优先在低价时段充电）
        timeSlots.sort(Comparator.comparingDouble(TimeSlot::getPrice));

        // 计算最优充电策略
        Map<Integer, Double> powerDistribution = new HashMap<>();
        Map<Integer, Double> costDistribution = new HashMap<>();
        Map<Integer, PriceType> priceTypeDistribution = new HashMap<>();

        double remainingEnergy = requiredEnergy;
        double totalCost = 0.0;

        // 优先在低价时段充满
        for (TimeSlot slot : timeSlots) {
            if (remainingEnergy <= 0) {
                break;
            }

            // 当前槽位可充电量
            double slotMaxEnergy = request.getMaxChargingPower() * TIME_SLOT_HOURS;
            double slotEnergy = Math.min(remainingEnergy, slotMaxEnergy);
            double slotPower = slotEnergy / TIME_SLOT_HOURS;
            double slotCost = slotEnergy * slot.getPrice();

            powerDistribution.put(slot.getPoint(), slotPower);
            costDistribution.put(slot.getPoint(), slotCost);
            priceTypeDistribution.put(slot.getPoint(), slot.getPriceType());

            remainingEnergy -= slotEnergy;
            totalCost += slotCost;
        }

        // 计算充电时长
        int chargingSlots = powerDistribution.size();
        int chargingDuration = chargingSlots * 15; // 分钟

        // 生成时段详情
        List<ChargingSegment> segments = createChargingSegments(
                powerDistribution, priceTypeDistribution, prices, startPoint, endPoint);

        ChargingPlan plan = ChargingPlan.builder()
                .success(true)
                .message("充电计划计算成功")
                .requiredEnergy(requiredEnergy)
                .totalCost(totalCost)
                .chargingDuration(chargingDuration)
                .powerDistribution(powerDistribution)
                .costDistribution(costDistribution)
                .priceTypeDistribution(priceTypeDistribution)
                .segments(segments)
                .build();

        log.info("最优充电计划计算完成: 需要电量={}kWh, 总成本={}元, 充电时长={}分钟",
                requiredEnergy, totalCost, chargingDuration);

        return plan;
    }

    /**
     * 创建充电时段详情列表
     */
    private List<ChargingSegment> createChargingSegments(
            Map<Integer, Double> powerDistribution,
            Map<Integer, PriceType> priceTypeDistribution,
            double[] prices,
            int startPoint,
            int endPoint) {

        List<ChargingSegment> segments = new ArrayList<>();

        // 按时间点排序
        List<Integer> sortedPoints = new ArrayList<>(powerDistribution.keySet());
        Collections.sort(sortedPoints);

        if (sortedPoints.isEmpty()) {
            return segments;
        }

        // 合并连续时段
        int segmentStart = sortedPoints.get(0);
        int segmentEnd = segmentStart + 1;
        double currentPower = powerDistribution.get(segmentStart);
        PriceType currentType = priceTypeDistribution.get(segmentStart);

        for (int i = 1; i < sortedPoints.size(); i++) {
            int point = sortedPoints.get(i);
            double power = powerDistribution.get(point);
            PriceType type = priceTypeDistribution.get(point);

            // 如果功率和电价类型相同且连续，则合并
            if (point == segmentEnd && Math.abs(power - currentPower) < 0.001 && type == currentType) {
                segmentEnd = point + 1;
            } else {
                // 保存当前时段
                segments.add(createSegment(segmentStart, segmentEnd, currentPower,
                        prices[segmentStart], currentType));

                // 开始新时段
                segmentStart = point;
                segmentEnd = point + 1;
                currentPower = power;
                currentType = type;
            }
        }

        // 保存最后一个时段
        segments.add(createSegment(segmentStart, segmentEnd, currentPower,
                prices[segmentStart], currentType));

        return segments;
    }

    /**
     * 创建充电时段
     */
    private ChargingSegment createSegment(int startPoint, int endPoint, double power,
                                          double price, PriceType priceType) {
        int durationSlots = endPoint - startPoint;
        double energy = power * durationSlots * TIME_SLOT_HOURS;
        double cost = energy * price;

        return ChargingSegment.builder()
                .startPoint(startPoint)
                .endPoint(endPoint)
                .startTime(pointToTime(startPoint))
                .endTime(pointToTime(endPoint))
                .power(power)
                .energy(energy)
                .cost(cost)
                .priceType(priceType)
                .price(price)
                .build();
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

    /**
     * 计算平均充电成本
     */
    public double calculateAverageCost(ChargingRequest request) {
        ChargingPlan plan = calculateOptimalChargingPlan(request);
        if (!plan.isSuccess()) {
            return -1;
        }
        return plan.getTotalCost() / plan.getRequiredEnergy();
    }

    /**
     * 时间槽内部类
     */
    private static class TimeSlot {
        private final int point;
        private final double price;
        private final PriceType priceType;

        public TimeSlot(int point, double price, PriceType priceType) {
            this.point = point;
            this.price = price;
            this.priceType = priceType;
        }

        public int getPoint() {
            return point;
        }

        public double getPrice() {
            return price;
        }

        public PriceType getPriceType() {
            return priceType;
        }
    }
}
