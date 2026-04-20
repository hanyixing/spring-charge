package com.example.charge.service;

import com.example.charge.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ChargeRecommendationService {

    @Autowired
    private ElectricityPriceService priceService;

    @Autowired
    private UserHabitAnalysisService habitService;

    private static final BigDecimal SLOT_ENERGY_FACTOR = new BigDecimal("0.25");
    private static final BigDecimal SOC_80 = new BigDecimal("80");
    private static final BigDecimal SOC_90 = new BigDecimal("90");
    private static final BigDecimal SOC_95 = new BigDecimal("95");

    public ChargeRecommendationResponse generateRecommendation(ChargeRecommendationRequest request) {
        List<ElectricityPrice> prices = priceService.generateDayAheadPrices();
        List<BigDecimal> habitCurve = habitService.analyzeUserHabit(request);
        int[] preferredWindow = habitService.getPreferredChargeWindow(habitCurve);

        BigDecimal currentSOC = request.getCurrentSOC() != null ? request.getCurrentSOC() : new BigDecimal("20");
        BigDecimal targetSOC = request.getTargetSOC() != null ? request.getTargetSOC() : new BigDecimal("95");
        BigDecimal batteryCapacity = request.getBatteryCapacity() != null ? request.getBatteryCapacity() : new BigDecimal("70");

        BigDecimal requiredEnergy = targetSOC.subtract(currentSOC)
                .divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
                .multiply(batteryCapacity);

        int earliestStart = Math.max(request.getEarliestStart(), 0);
        int latestEnd = Math.min(request.getLatestEnd() > 0 ? request.getLatestEnd() : 95, 95);

        List<Integer> validSlots = IntStream.rangeClosed(earliestStart, latestEnd)
                .boxed()
                .collect(Collectors.toList());

        List<TimeSlotScore> scoredSlots = calculateSlotScores(validSlots, prices, habitCurve);
        List<ChargePoint> recommendedCurve = allocateChargePowerWithSOC(
                scoredSlots,
                requiredEnergy,
                request.getMaxPower(),
                prices,
                currentSOC,
                batteryCapacity
        );

        BigDecimal finalSOC = calculateFinalSOC(recommendedCurve, currentSOC, batteryCapacity);

        BigDecimal totalCost = recommendedCurve.stream()
                .map(ChargePoint::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEnergy = recommendedCurve.stream()
                .map(ChargePoint::getPower)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(SLOT_ENERGY_FACTOR);

        BigDecimal peakLoadReduction = calculatePeakLoadReduction(recommendedCurve, prices);

        ChargeRecommendationResponse response = new ChargeRecommendationResponse();
        response.setUserId(request.getUserId());
        response.setInitialSOC(currentSOC.setScale(2, RoundingMode.HALF_UP));
        response.setFinalSOC(finalSOC.setScale(2, RoundingMode.HALF_UP));
        response.setRecommendedCurve(recommendedCurve);
        response.setTotalCost(totalCost.setScale(2, RoundingMode.HALF_UP));
        response.setTotalEnergy(totalEnergy.setScale(2, RoundingMode.HALF_UP));
        response.setPeakLoadReduction(peakLoadReduction.setScale(2, RoundingMode.HALF_UP));
        response.setRecommendationReason(buildRecommendationReason(prices, preferredWindow, currentSOC, targetSOC, batteryCapacity));

        return response;
    }

    private BigDecimal getMaxPowerForSOC(BigDecimal currentSOC, BigDecimal maxPower) {
        if (currentSOC.compareTo(SOC_80) < 0) {
            return maxPower;
        } else if (currentSOC.compareTo(SOC_90) < 0) {
            return maxPower.multiply(new BigDecimal("0.7"));
        } else if (currentSOC.compareTo(SOC_95) < 0) {
            return maxPower.multiply(new BigDecimal("0.5"));
        } else {
            return maxPower.multiply(new BigDecimal("0.3"));
        }
    }

    private List<TimeSlotScore> calculateSlotScores(List<Integer> slots, List<ElectricityPrice> prices, List<BigDecimal> habitCurve) {
        List<TimeSlotScore> scores = new ArrayList<>();

        for (Integer slot : slots) {
            ElectricityPrice price = prices.get(slot);
            BigDecimal habitWeight = habitCurve.get(slot);

            BigDecimal priceScore = new BigDecimal("2.0")
                    .divide(price.getPrice().add(new BigDecimal("0.1")), 4, RoundingMode.HALF_UP);

            BigDecimal habitScore = habitWeight.multiply(new BigDecimal("1.5"));

            BigDecimal isPeak = price.getPeriodType().equals("PEAK") ? new BigDecimal("0.3") : new BigDecimal("1.0");

            BigDecimal totalScore = priceScore.multiply(new BigDecimal("0.5"))
                    .add(habitScore.multiply(new BigDecimal("0.3")))
                    .multiply(isPeak);

            scores.add(new TimeSlotScore(slot, totalScore, price.getPrice()));
        }

        scores.sort(Comparator.comparing(TimeSlotScore::getScore).reversed());

        return scores;
    }

    private List<ChargePoint> allocateChargePowerWithSOC(
            List<TimeSlotScore> scoredSlots,
            BigDecimal targetEnergy,
            BigDecimal maxPower,
            List<ElectricityPrice> prices,
            BigDecimal initialSOC,
            BigDecimal batteryCapacity) {

        List<ChargePoint> curve = new ArrayList<>();
        BigDecimal remainingEnergy = targetEnergy;
        BigDecimal currentSOC = initialSOC;

        for (int i = 0; i < 96; i++) {
            curve.add(new ChargePoint(i, BigDecimal.ZERO, prices.get(i).getPrice(), BigDecimal.ZERO, initialSOC));
        }

        for (TimeSlotScore slot : scoredSlots) {
            if (remainingEnergy.compareTo(BigDecimal.ZERO) <= 0) {
                ChargePoint point = curve.get(slot.getTimeSlot());
                point.setSocAfterCharge(currentSOC.setScale(2, RoundingMode.HALF_UP));
                continue;
            }

            BigDecimal availableMaxPower = getMaxPowerForSOC(currentSOC, maxPower);
            BigDecimal maxEnergyThisSlot = availableMaxPower.multiply(SLOT_ENERGY_FACTOR);

            BigDecimal chargePower = remainingEnergy.compareTo(maxEnergyThisSlot) <= 0
                    ? remainingEnergy.divide(SLOT_ENERGY_FACTOR, 4, RoundingMode.HALF_UP)
                    : availableMaxPower;

            BigDecimal energyCharged = chargePower.multiply(SLOT_ENERGY_FACTOR);
            BigDecimal cost = energyCharged.multiply(slot.getPrice());

            BigDecimal socDelta = energyCharged.divide(batteryCapacity, 6, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            currentSOC = currentSOC.add(socDelta);

            ChargePoint point = curve.get(slot.getTimeSlot());
            point.setPower(chargePower.setScale(2, RoundingMode.HALF_UP));
            point.setCost(cost.setScale(4, RoundingMode.HALF_UP));
            point.setSocAfterCharge(currentSOC.setScale(2, RoundingMode.HALF_UP));

            remainingEnergy = remainingEnergy.subtract(energyCharged);
        }

        return curve;
    }

    private BigDecimal calculateFinalSOC(List<ChargePoint> curve, BigDecimal initialSOC, BigDecimal batteryCapacity) {
        BigDecimal totalEnergy = curve.stream()
                .map(ChargePoint::getPower)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(SLOT_ENERGY_FACTOR);

        BigDecimal socDelta = totalEnergy.divide(batteryCapacity, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        return initialSOC.add(socDelta);
    }

    private BigDecimal calculatePeakLoadReduction(List<ChargePoint> curve, List<ElectricityPrice> prices) {
        BigDecimal peakPower = BigDecimal.ZERO;
        BigDecimal nonPeakPower = BigDecimal.ZERO;

        for (ChargePoint point : curve) {
            ElectricityPrice price = prices.get(point.getTimeSlot());
            if (price.getPeriodType().equals("PEAK")) {
                peakPower = peakPower.add(point.getPower());
            } else {
                nonPeakPower = nonPeakPower.add(point.getPower());
            }
        }

        BigDecimal total = peakPower.add(nonPeakPower);
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal("100").subtract(peakPower.divide(total, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")));
    }

    private String buildRecommendationReason(List<ElectricityPrice> prices, int[] preferredWindow,
                                            BigDecimal currentSOC, BigDecimal targetSOC, BigDecimal batteryCapacity) {
        long peakSlots = prices.stream().filter(p -> p.getPeriodType().equals("PEAK")).count();
        long valleySlots = prices.stream().filter(p -> p.getPeriodType().equals("VALLEY")).count();

        return String.format("SOC%d%%→SOC%d%%(%.0fkWh电池),充电特性:0-80%%快充,80-90%%降为70%%,90%%以上继续降功率.优先谷价(%d个时段0.35元/度),避开高峰(%d个时段1.25元/度)",
                currentSOC.intValue(), targetSOC.intValue(), batteryCapacity,
                valleySlots, peakSlots);
    }

    private static class TimeSlotScore {
        private final int timeSlot;
        private final BigDecimal score;
        private final BigDecimal price;

        public TimeSlotScore(int timeSlot, BigDecimal score, BigDecimal price) {
            this.timeSlot = timeSlot;
            this.score = score;
            this.price = price;
        }

        public int getTimeSlot() { return timeSlot; }
        public BigDecimal getScore() { return score; }
        public BigDecimal getPrice() { return price; }
    }
}
