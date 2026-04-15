package com.example.demo;

import com.example.demo.model.ChargingCurve;
import com.example.demo.model.ChargingRecommendation;
import com.example.demo.model.ElectricityPrice;
import com.example.demo.model.UserChargingHabit;
import com.example.demo.service.ChargingRecommendationService;
import com.example.demo.service.MockDataService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

@SpringBootTest
public class ChargingAlgorithmTest {

    @Test
    public void testChargingRecommendation() {
        System.out.println("========== 用户充电推荐算法测试 ==========\n");

        // 初始化服务
        ChargingRecommendationService recommendationService = new ChargingRecommendationService();
        MockDataService mockDataService = new MockDataService();

        String userId = "USER_001";
        double targetEnergy = 40.0; // 目标充电40kWh

        System.out.println("测试用户: " + userId);
        System.out.println("目标充电量: " + targetEnergy + " kWh\n");

        // 1. 生成历史充电数据
        System.out.println("【步骤1】生成历史充电数据...");
        List<ChargingCurve> historyCurves = mockDataService.generateHistoryCurves(userId, 30);
        System.out.println("生成了 " + historyCurves.size() + " 条历史充电记录\n");

        // 显示第一条历史记录作为示例
        ChargingCurve firstCurve = historyCurves.get(0);
        System.out.println("历史记录示例:");
        System.out.println("  日期: " + firstCurve.getChargingDate());
        System.out.println("  开始时间: " + formatTimeSlot(firstCurve.getStartTimeSlot()));
        System.out.println("  结束时间: " + formatTimeSlot(firstCurve.getEndTimeSlot()));
        System.out.println("  总充电量: " + String.format("%.2f", firstCurve.getTotalEnergy()) + " kWh");
        System.out.println("  平均功率: " + String.format("%.2f", firstCurve.getAvgPower()) + " kW\n");

        // 2. 分析用户充电习惯
        System.out.println("【步骤2】分析用户充电习惯...");
        UserChargingHabit habit = recommendationService.analyzeUserHabit(userId, historyCurves);
        System.out.println("用户习惯分析结果:");
        System.out.println("  历史充电次数: " + habit.getTotalChargingCount());
        System.out.println("  平均充电量: " + String.format("%.2f", habit.getAvgChargingEnergy()) + " kWh");
        System.out.println("  平均充电时长: " + String.format("%.2f", habit.getAvgChargingDuration()) + " 小时");
        System.out.println("  平均充电功率: " + String.format("%.2f", habit.getAvgChargingPower()) + " kW");
        System.out.println("  最大充电功率: " + String.format("%.2f", habit.getMaxChargingPower()) + " kW");
        System.out.println("  习惯稳定性评分: " + String.format("%.1f", habit.getHabitStabilityScore()) + "/100\n");

        // 显示首选充电时间段
        System.out.println("  首选充电时间段:");
        if (habit.getPreferredStartTimeRanges() != null) {
            for (UserChargingHabit.TimeRange range : habit.getPreferredStartTimeRanges()) {
                System.out.println("    - " + range.getDescription() + " (概率: " +
                        String.format("%.1f", range.getProbability() * 100) + "%)");
            }
        }
        System.out.println();

        // 3. 获取电价信息
        System.out.println("【步骤3】获取分时电价信息...");
        ElectricityPrice price = mockDataService.getElectricityPrice(LocalDate.now());
        System.out.println("电价信息:");
        System.out.println("  峰时电价: " + price.getPeakPrice() + " 元/kWh (10:00-18:00)");
        System.out.println("  平时电价: " + price.getFlatPrice() + " 元/kWh (其他时段)");
        System.out.println("  谷时电价: " + price.getValleyPrice() + " 元/kWh (23:00-07:00)\n");

        // 4. 生成充电推荐
        System.out.println("【步骤4】生成智能充电推荐...");
        long startTime = System.currentTimeMillis();
        ChargingRecommendation recommendation = recommendationService.generateRecommendation(
                userId, habit, targetEnergy, price);
        long endTime = System.currentTimeMillis();

        System.out.println("推荐生成耗时: " + (endTime - startTime) + " ms\n");

        // 5. 输出推荐结果
        System.out.println("========== 充电推荐结果 ==========\n");
        System.out.println("推荐ID: " + recommendation.getRecommendationId());
        System.out.println("推荐时间: " + recommendation.getGenerateTime());
        System.out.println("目标日期: " + recommendation.getTargetDate());
        System.out.println();

        System.out.println("【充电计划】");
        System.out.println("  建议开始时间: " + formatTimeSlot(recommendation.getRecommendedStartSlot()));
        System.out.println("  建议结束时间: " + formatTimeSlot(recommendation.getRecommendedEndSlot()));
        System.out.println("  预计总充电量: " + String.format("%.2f", recommendation.getEstimatedTotalEnergy()) + " kWh");
        System.out.println("  预计总成本: " + String.format("%.2f", recommendation.getEstimatedTotalCost()) + " 元");
        System.out.println("  平均电价: " + String.format("%.3f",
                recommendation.getEstimatedTotalCost() / recommendation.getEstimatedTotalEnergy()) + " 元/kWh\n");

        System.out.println("【优化评分】");
        System.out.println("  成本优化评分: " + String.format("%.1f", recommendation.getCostOptimizationScore()) + "/100");
        System.out.println("  削峰平谷评分: " + String.format("%.1f", recommendation.getPeakShavingScore()) + "/100");
        System.out.println("  习惯匹配评分: " + String.format("%.1f", recommendation.getHabitMatchScore()) + "/100");
        System.out.println("  综合评分: " + String.format("%.1f", recommendation.getOverallScore()) + "/100\n");

        System.out.println("【推荐说明】");
        System.out.println("  " + recommendation.getRecommendationDescription());
        System.out.println();

        // 6. 输出96点充电曲线（简化显示，每4个点显示一个）
        System.out.println("【96点充电功率曲线】（每15分钟一个点，单位：kW）");
        System.out.println("时间\t\t功率(kW)\t电价(元/kWh)\t电价类型");
        System.out.println("--------------------------------------------------------");

        double[] powerData = recommendation.getRecommendedPower();
        double[] priceData = recommendation.getElectricityPrice();

        for (int i = 0; i < 96; i += 4) { // 每小时显示一个点
            String timeLabel = formatTimeSlot(i);
            double power = powerData[i];
            double priceVal = priceData[i];
            String priceType = price.getPriceType(i);

            String bar = generatePowerBar(power);
            System.out.printf("%s\t%.2f\t\t%.2f\t\t%s %s%n",
                    timeLabel, power, priceVal, priceType, bar);
        }

        System.out.println("\n========== 测试完成 ==========");
    }

    private static String formatTimeSlot(int slot) {
        int hour = slot / 4;
        int minute = (slot % 4) * 15;
        return String.format("%02d:%02d", hour, minute);
    }

    private static String generatePowerBar(double power) {
        int length = (int) (power / 7.0 * 20); // 最大20个字符
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 20; i++) {
            if (i < length) {
                sb.append("█");
            } else {
                sb.append(" ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
