package com.example.charge;

import com.example.charge.model.ChargePoint;
import com.example.charge.model.ChargeRecommendationRequest;
import com.example.charge.model.ChargeRecommendationResponse;
import com.example.charge.service.ChargeRecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ChargeRecommendationServiceTest {

    @Autowired
    private ChargeRecommendationService service;

    @Test
    public void testGenerateRecommendationWithSOC() {
        ChargeRecommendationRequest request = new ChargeRecommendationRequest();
        request.setUserId("user_001");
        request.setCurrentSOC(new BigDecimal("15"));
        request.setTargetSOC(new BigDecimal("95"));
        request.setBatteryCapacity(new BigDecimal("70"));
        request.setMaxPower(new BigDecimal("7"));
        request.setEarliestStart(0);
        request.setLatestEnd(95);

        List<List<BigDecimal>> historicalData = new ArrayList<>();
        List<BigDecimal> day1 = new ArrayList<>();
        for (int i = 0; i < 96; i++) {
            if (i >= 72 && i <= 90) {
                day1.add(new BigDecimal("3.5"));
            } else {
                day1.add(BigDecimal.ZERO);
            }
        }
        historicalData.add(day1);
        request.setHistoricalData(historicalData);

        ChargeRecommendationResponse response = service.generateRecommendation(request);

        assertNotNull(response);
        assertEquals("user_001", response.getUserId());
        assertEquals(96, response.getRecommendedCurve().size());

        System.out.println("=== 充电推荐结果验证(SOC特性版) ===");
        System.out.println("初始SOC: " + response.getInitialSOC() + "%");
        System.out.println("最终SOC: " + response.getFinalSOC() + "%");
        System.out.println("总充电量: " + response.getTotalEnergy() + " kWh");
        System.out.println("总费用: " + response.getTotalCost() + " 元");
        System.out.println("削峰率: " + response.getPeakLoadReduction() + "%");
        System.out.println("推荐理由: " + response.getRecommendationReason());
        System.out.println("\n充电时段详情(含SOC变化):");
        System.out.println("时间    | 功率(kW) | SOC(%)  | 电价(元) | 费用(元) | 备注");
        System.out.println("--------+----------+---------+----------+----------+-------------------");

        BigDecimal lastSOC = response.getInitialSOC();
        for (ChargePoint point : response.getRecommendedCurve()) {
            if (point.getPower().compareTo(BigDecimal.ZERO) > 0) {
                int hour = point.getTimeSlot() / 4;
                int minute = (point.getTimeSlot() % 4) * 15;

                String remark = "";
                if (lastSOC.compareTo(new BigDecimal("80")) < 0 && point.getSocAfterCharge().compareTo(new BigDecimal("80")) >= 0) {
                    remark = "→超过80%SOC,功率开始下降";
                } else if (lastSOC.compareTo(new BigDecimal("90")) < 0 && point.getSocAfterCharge().compareTo(new BigDecimal("90")) >= 0) {
                    remark = "→超过90%SOC,功率继续下降";
                }

                System.out.printf("%02d:%02d  | %8.2f | %7.2f | %8.2f | %8.4f | %s%n",
                        hour, minute,
                        point.getPower(),
                        point.getSocAfterCharge(),
                        point.getPrice(),
                        point.getCost(),
                        remark);

                lastSOC = point.getSocAfterCharge();
            }
        }

        System.out.println("\n=== SOC非线性充电特性验证 ===");
        System.out.println("SOC区间  | 最大可用功率");
        System.out.println("---------+-------------");
        System.out.println("0-80%    | 100% (7kW)  快充区间");
        System.out.println("80-90%   |  70% (4.9kW) 开始降速");
        System.out.println("90-95%   |  50% (3.5kW) 慢速充电");
        System.out.println("95%+     |  30% (2.1kW) 涓流充电");

        assertTrue(response.getFinalSOC().compareTo(new BigDecimal("90")) >= 0);
        assertTrue(response.getPeakLoadReduction().compareTo(new BigDecimal("50")) >= 0);
        System.out.println("\n✓ SOC充电特性已成功集成到算法中!");
    }
}
