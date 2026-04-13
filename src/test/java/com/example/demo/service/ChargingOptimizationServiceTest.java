package com.example.demo.service;

import com.example.demo.entity.ChargingPlan;
import com.example.demo.entity.ChargingRequest;
import com.example.demo.entity.ElectricityPrice;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 充电优化服务测试类
 */
@SpringBootTest
public class ChargingOptimizationServiceTest {

    @Autowired
    private ChargingOptimizationService chargingService;

    @Autowired
    private ElectricityPriceService priceService;

    /**
     * 测试场景1：全天充电，应该优先选择谷时
     */
    @Test
    public void testOptimalCharging_FullDay() {
        ChargingRequest request = ChargingRequest.builder()
                .currentSoc(20.0)
                .targetSoc(80.0)
                .startTimePoint(0)
                .endTimePoint(96)
                .batteryCapacity(60.0)
                .maxChargingPower(7.0)
                .build();

        ChargingPlan plan = chargingService.calculateOptimalChargingPlan(request);

        assertTrue(plan.isSuccess(), "充电计划应该成功");
        assertEquals(36.0, plan.getRequiredEnergy(), 0.01, "需要充电量应为36kWh");

        // 验证优先在谷时充电（谷时电价最低0.3元）
        double expectedMinCost = 36.0 * 0.3; // 全部在谷时充电
        assertEquals(expectedMinCost, plan.getTotalCost(), 0.01, "总成本应为10.8元");

        System.out.println("=== 测试场景1：全天充电 ===");
        System.out.println("需要电量: " + plan.getRequiredEnergy() + " kWh");
        System.out.println("总成本: " + plan.getTotalCost() + " 元");
        System.out.println("平均电价: " + (plan.getTotalCost() / plan.getRequiredEnergy()) + " 元/kWh");
        System.out.println("充电时长: " + plan.getChargingDuration() + " 分钟");
        printSegments(plan.getSegments());
    }

    /**
     * 测试场景2：仅在峰时充电
     */
    @Test
    public void testOptimalCharging_PeakHoursOnly() {
        // 10:00-15:00 (40-60点) 是峰时和尖时
        ChargingRequest request = ChargingRequest.builder()
                .currentSoc(30.0)
                .targetSoc(50.0)
                .startTimePoint(40)
                .endTimePoint(60)
                .batteryCapacity(60.0)
                .maxChargingPower(7.0)
                .build();

        ChargingPlan plan = chargingService.calculateOptimalChargingPlan(request);

        assertTrue(plan.isSuccess(), "充电计划应该成功");
        assertEquals(12.0, plan.getRequiredEnergy(), 0.01, "需要充电量应为12kWh");

        System.out.println("\n=== 测试场景2：峰时充电（10:00-15:00） ===");
        System.out.println("需要电量: " + plan.getRequiredEnergy() + " kWh");
        System.out.println("总成本: " + plan.getTotalCost() + " 元");
        System.out.println("平均电价: " + (plan.getTotalCost() / plan.getRequiredEnergy()) + " 元/kWh");
        printSegments(plan.getSegments());
    }

    /**
     * 测试场景3：跨时段充电，验证算法选择低价时段
     */
    @Test
    public void testOptimalCharging_CrossPeriods() {
        // 从谷时到平时 (00:00-10:00)
        ChargingRequest request = ChargingRequest.builder()
                .currentSoc(10.0)
                .targetSoc(40.0)
                .startTimePoint(0)
                .endTimePoint(40)
                .batteryCapacity(60.0)
                .maxChargingPower(7.0)
                .build();

        ChargingPlan plan = chargingService.calculateOptimalChargingPlan(request);

        assertTrue(plan.isSuccess(), "充电计划应该成功");
        assertEquals(18.0, plan.getRequiredEnergy(), 0.01, "需要充电量应为18kWh");

        // 应该优先在谷时(0.3元)充满，而不是平时(0.6元)
        double maxEnergyInValley = 32 * 7.0 * 0.25; // 谷时最大可充电量
        assertTrue(maxEnergyInValley >= plan.getRequiredEnergy(), "谷时应该足够充满");

        // 平均电价应该接近0.3元
        double avgPrice = plan.getTotalCost() / plan.getRequiredEnergy();
        assertEquals(0.3, avgPrice, 0.01, "平均电价应接近谷时电价");

        System.out.println("\n=== 测试场景3：跨时段充电（00:00-10:00） ===");
        System.out.println("需要电量: " + plan.getRequiredEnergy() + " kWh");
        System.out.println("总成本: " + plan.getTotalCost() + " 元");
        System.out.println("平均电价: " + avgPrice + " 元/kWh");
        printSegments(plan.getSegments());
    }

    /**
     * 测试场景4：时间窗口不足以完成充电
     */
    @Test
    public void testOptimalCharging_InsufficientTime() {
        ChargingRequest request = ChargingRequest.builder()
                .currentSoc(10.0)
                .targetSoc(90.0)
                .startTimePoint(0)
                .endTimePoint(4) // 只有1小时
                .batteryCapacity(60.0)
                .maxChargingPower(7.0)
                .build();

        ChargingPlan plan = chargingService.calculateOptimalChargingPlan(request);

        assertFalse(plan.isSuccess(), "充电计划应该失败");
        assertNotNull(plan.getMessage(), "应该返回错误消息");

        System.out.println("\n=== 测试场景4：时间不足 ===");
        System.out.println("结果: " + plan.getMessage());
    }

    /**
     * 测试场景5：验证电价配置
     */
    @Test
    public void testElectricityPriceConfiguration() {
        List<ElectricityPrice> prices = priceService.getCurrentPrices();
        assertFalse(prices.isEmpty(), "电价配置不应为空");

        // 验证96个点的电价
        double[] priceArray = priceService.getPriceArray();
        assertEquals(96, priceArray.length, "应有96个点的电价");

        // 验证谷时电价
        double valleyPrice = priceService.getPriceValueAtPoint(0); // 00:00
        assertEquals(0.3, valleyPrice, 0.01, "谷时电价应为0.3元");

        // 验证尖时电价
        double peakPrice = priceService.getPriceValueAtPoint(50); // 12:30
        assertEquals(1.5, peakPrice, 0.01, "尖时电价应为1.5元");

        System.out.println("\n=== 测试场景5：电价配置验证 ===");
        System.out.println("谷时电价(00:00): " + priceService.getPriceValueAtPoint(0) + " 元");
        System.out.println("平时电价(08:00): " + priceService.getPriceValueAtPoint(32) + " 元");
        System.out.println("峰时电价(10:00): " + priceService.getPriceValueAtPoint(40) + " 元");
        System.out.println("尖时电价(12:00): " + priceService.getPriceValueAtPoint(48) + " 元");
    }

    private void printSegments(List<ChargingPlan.ChargingSegment> segments) {
        System.out.println("充电时段详情:");
        for (ChargingPlan.ChargingSegment segment : segments) {
            System.out.printf("  %s-%s: 功率=%.1fkW, 电量=%.2fkWh, 成本=%.2f元, 电价=%.2f元/kWh(%s)\n",
                    segment.getStartTime(), segment.getEndTime(),
                    segment.getPower(), segment.getEnergy(),
                    segment.getCost(), segment.getPrice(),
                    segment.getPriceType());
        }
    }
}
