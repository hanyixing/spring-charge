package com.example.demo;

import com.example.demo.model.BatterySOC;
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
public class SOCAlgorithmTest {

    @Test
    public void testSOCChargingRecommendation() {
        System.out.println("\n========== 用户充电推荐算法测试（SOC约束版） ==========\n");

        // 初始化服务
        ChargingRecommendationService recommendationService = new ChargingRecommendationService();
        MockDataService mockDataService = new MockDataService();

        String userId = "USER_001";

        // 定义SOC场景
        double currentSOC = 25.0;      // 当前25%电量
        double targetSOC = 85.0;       // 目标85%电量
        double batteryCapacity = 60.0; // 60kWh电池
        String departureTime = "20:00"; // 预计20:00离开

        System.out.println("测试用户: " + userId);
        System.out.println("电池容量: " + batteryCapacity + " kWh");
        System.out.println("当前SOC: " + currentSOC + "%");
        System.out.println("目标SOC: " + targetSOC + "%");
        System.out.println("预计离开时间: " + departureTime);
        System.out.println();

        // 1. 创建电池SOC对象
        System.out.println("【步骤1】初始化电池SOC参数...");
        int departureSlot = 80; // 20:00 = slot 80
        BatterySOC batterySOC = BatterySOC.builder()
                .totalCapacity(batteryCapacity)
                .currentSOC(currentSOC)
                .targetSOC(targetSOC)
                .minAllowedSOC(10.0)
                .maxAllowedSOC(90.0)
                .chargingEfficiency(0.92)
                .expectedDepartureSlot(departureSlot)
                .build();

        double requiredEnergy = batterySOC.calculateRequiredEnergy();
        System.out.println("需要充电能量: " + String.format("%.2f", requiredEnergy) + " kWh");
        System.out.println("SOC差值: " + String.format("%.1f", batterySOC.getSOCDifference()) + "%\n");

        // 2. 生成历史充电数据
        System.out.println("【步骤2】生成历史充电数据...");
        List<ChargingCurve> historyCurves = mockDataService.generateHistoryCurves(userId, 30);
        System.out.println("生成了 " + historyCurves.size() + " 条历史充电记录\n");

        // 3. 分析用户充电习惯
        System.out.println("【步骤3】分析用户充电习惯...");
        UserChargingHabit habit = recommendationService.analyzeUserHabit(userId, historyCurves);
        System.out.println("用户习惯分析结果:");
        System.out.println("  历史充电次数: " + habit.getTotalChargingCount());
        System.out.println("  平均充电量: " + String.format("%.2f", habit.getAvgChargingEnergy()) + " kWh");
        System.out.println("  平均充电时长: " + String.format("%.2f", habit.getAvgChargingDuration()) + " 小时");
        System.out.println("  习惯稳定性评分: " + String.format("%.1f", habit.getHabitStabilityScore()) + "/100\n");

        // 4. 获取电价信息
        System.out.println("【步骤4】获取分时电价信息...");
        ElectricityPrice price = mockDataService.getElectricityPrice(LocalDate.now());
        System.out.println("电价信息:");
        System.out.println("  峰时电价: " + price.getPeakPrice() + " 元/kWh (10:00-18:00)");
        System.out.println("  平时电价: " + price.getFlatPrice() + " 元/kWh (其他时段)");
        System.out.println("  谷时电价: " + price.getValleyPrice() + " 元/kWh (23:00-07:00)\n");

        // 5. 生成SOC约束的充电推荐
        System.out.println("【步骤5】生成SOC约束充电推荐...");
        long startTime = System.currentTimeMillis();
        ChargingRecommendation recommendation = recommendationService.generateRecommendationWithSOC(
                userId, habit, batterySOC, price);
        long endTime = System.currentTimeMillis();

        System.out.println("推荐生成耗时: " + (endTime - startTime) + " ms\n");

        // 6. 输出推荐结果
        System.out.println("========== SOC约束充电推荐结果 ==========\n");
        System.out.println("推荐ID: " + recommendation.getRecommendationId());
        System.out.println("推荐时间: " + recommendation.getGenerateTime());
        System.out.println();

        System.out.println("【SOC信息】");
        System.out.println("  初始SOC: " + String.format("%.1f", recommendation.getInitialSOC()) + "%");
        System.out.println("  目标SOC: " + String.format("%.1f", recommendation.getTargetSOC()) + "%");
        System.out.println("  电池容量: " + recommendation.getBatteryCapacity() + " kWh");
        System.out.println("  预计离开时间: " + formatTimeSlot(recommendation.getExpectedDepartureSlot()));
        System.out.println("  SOC达成率: " + String.format("%.1f", recommendation.getSocAchievementRate()) + "%");
        System.out.println("  是否能在离开前完成: " + (recommendation.isCanCompleteBeforeDeparture() ? "✓ 是" : "✗ 否"));
        System.out.println();

        System.out.println("【充电计划】");
        System.out.println("  建议开始时间: " + formatTimeSlot(recommendation.getRecommendedStartSlot()));
        System.out.println("  建议结束时间: " + formatTimeSlot(recommendation.getRecommendedEndSlot()));
        System.out.println("  SOC达到目标时间: " + formatTimeSlot(recommendation.getCompletionSlot()));
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

        // 7. 输出96点充电功率和SOC曲线
        System.out.println("【96点充电功率与SOC曲线】（每15分钟一个点）");
        System.out.println("时间\t\t功率(kW)\tSOC(%)\t电价(元/kWh)\t类型");
        System.out.println("--------------------------------------------------------");

        double[] powerData = recommendation.getRecommendedPower();
        double[] socCurve = recommendation.getSocCurve();
        double[] priceData = recommendation.getElectricityPrice();

        for (int i = 0; i < 96; i += 2) { // 每30分钟显示一个点
            String timeLabel = formatTimeSlot(i);
            double power = powerData[i];
            double soc = socCurve[i];
            double priceVal = priceData[i];
            String priceType = price.getPriceType(i);

            String powerBar = generatePowerBar(power);
            String socBar = generateSOCBar(soc);

            System.out.printf("%s\t%.2f\t%.1f\t%.2f\t\t%s%n",
                    timeLabel, power, soc, priceVal, priceType);
        }

        System.out.println("\n========== SOC约束测试完成 ==========");
    }

    private static String formatTimeSlot(int slot) {
        int hour = slot / 4;
        int minute = (slot % 4) * 15;
        return String.format("%02d:%02d", hour, minute);
    }

    private static String generatePowerBar(double power) {
        int length = (int) (power / 7.0 * 10);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            sb.append(i < length ? "█" : " ");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String generateSOCBar(double soc) {
        int length = (int) (soc / 100.0 * 10);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            sb.append(i < length ? "▓" : " ");
        }
        sb.append("]");
        return sb.toString();
    }
}
