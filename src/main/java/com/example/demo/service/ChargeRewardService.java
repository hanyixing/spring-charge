package com.example.demo.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChargeRewardService {

    private static final int POINTS_PER_DAY = 96;
    private static final int MINUTES_PER_POINT = 15;
    private static final BigDecimal DEFAULT_REWARD_RATE = new BigDecimal("0.5");

    private List<TimeSlotReward> rewardSlots = new ArrayList<>();

    @Data
    public static class TimeSlotReward {
        private int startPoint;
        private int endPoint;
        private LocalTime startTime;
        private LocalTime endTime;
        private BigDecimal rewardRate;
        private String description;

        public TimeSlotReward(int startPoint, int endPoint, BigDecimal rewardRate, String description) {
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            this.rewardRate = rewardRate;
            this.description = description;
            this.startTime = pointToTime(startPoint);
            this.endTime = pointToTime(endPoint);
        }
    }

    @Data
    public static class ChargeRecord {
        private int point;
        private LocalDate date;
        private double kwh;
        private BigDecimal reward;
        private boolean isRewardSlot;
        private String timeRange;

        public ChargeRecord(int point, LocalDate date, double kwh, BigDecimal reward, boolean isRewardSlot) {
            this.point = point;
            this.date = date;
            this.kwh = kwh;
            this.reward = reward;
            this.isRewardSlot = isRewardSlot;
            this.timeRange = getTimeRangeByPoint(point);
        }
    }

    @Data
    public static class ChargeSession {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private double totalKwh;
        private String userId;
        private String chargeId;

        public ChargeSession(String userId, String chargeId, LocalDateTime startTime, LocalDateTime endTime, double totalKwh) {
            this.userId = userId;
            this.chargeId = chargeId;
            this.startTime = startTime;
            this.endTime = endTime;
            this.totalKwh = totalKwh;
        }
    }

    @Data
    public static class ChargeSegment {
        private LocalDate date;
        private int point;
        private double kwh;
        private String timeRange;

        public ChargeSegment(LocalDate date, int point, double kwh) {
            this.date = date;
            this.point = point;
            this.kwh = kwh;
            this.timeRange = getTimeRangeByPoint(point);
        }
    }

    @Data
    public static class DailyRewardResult {
        private LocalDate date;
        private List<ChargeRecord> records = new ArrayList<>();
        private double totalKwh = 0;
        private BigDecimal totalReward = BigDecimal.ZERO;
        private double rewardKwh = 0;
        private double nonRewardKwh = 0;

        public void addRecord(ChargeRecord record) {
            records.add(record);
            totalKwh += record.getKwh();
            totalReward = totalReward.add(record.getReward());
            if (record.isRewardSlot()) {
                rewardKwh += record.getKwh();
            } else {
                nonRewardKwh += record.getKwh();
            }
        }
    }

    @Data
    public static class MultiDayRewardResult {
        private String userId;
        private String chargeId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<DailyRewardResult> dailyResults = new ArrayList<>();
        private double totalKwh = 0;
        private BigDecimal totalReward = BigDecimal.ZERO;
        private double rewardKwh = 0;
        private double nonRewardKwh = 0;
        private int totalPoints = 0;

        public void addDailyResult(DailyRewardResult result) {
            dailyResults.add(result);
            totalKwh += result.getTotalKwh();
            totalReward = totalReward.add(result.getTotalReward());
            rewardKwh += result.getRewardKwh();
            nonRewardKwh += result.getNonRewardKwh();
            totalPoints += result.getRecords().size();
        }
    }

    @PostConstruct
    public void init() {
        initRewardSlots();
        log.info("充电奖励服务初始化完成，共配置 {} 个奖励时段", rewardSlots.size());
    }

    private void initRewardSlots() {
        rewardSlots.clear();
        rewardSlots.add(new TimeSlotReward(0, 16, new BigDecimal("0.8"), "深夜谷时 (00:00-04:00)"));
        rewardSlots.add(new TimeSlotReward(72, 88, new BigDecimal("0.6"), "晚间谷时 (18:00-22:00)"));
    }

    public static LocalTime pointToTime(int point) {
        int totalMinutes = point * MINUTES_PER_POINT;
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;
        return LocalTime.of(hour, minute);
    }

    public static int timeToPoint(LocalTime time) {
        return (time.getHour() * 60 + time.getMinute()) / MINUTES_PER_POINT;
    }

    public static String getTimeRangeByPoint(int point) {
        LocalTime time = pointToTime(point);
        LocalTime endTime = time.plusMinutes(MINUTES_PER_POINT);
        return String.format("%02d:%02d-%02d:%02d", time.getHour(), time.getMinute(), endTime.getHour(), endTime.getMinute());
    }

    public boolean isRewardSlot(int point) {
        for (TimeSlotReward slot : rewardSlots) {
            if (point >= slot.getStartPoint() && point < slot.getEndPoint()) {
                return true;
            }
        }
        return false;
    }

    public BigDecimal getRewardRate(int point) {
        for (TimeSlotReward slot : rewardSlots) {
            if (point >= slot.getStartPoint() && point < slot.getEndPoint()) {
                return slot.getRewardRate();
            }
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateReward(int point, double kwh) {
        if (kwh <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = getRewardRate(point);
        return rate.multiply(BigDecimal.valueOf(kwh)).setScale(2, RoundingMode.HALF_UP);
    }

    public List<ChargeSegment> splitChargeSession(ChargeSession session) {
        List<ChargeSegment> segments = new ArrayList<>();

        if (session == null || session.getStartTime() == null || session.getEndTime() == null) {
            log.error("充电会话信息无效");
            return segments;
        }

        if (session.getStartTime().isAfter(session.getEndTime())) {
            log.error("充电开始时间不能晚于结束时间");
            return segments;
        }

        if (session.getTotalKwh() <= 0) {
            log.error("充电电量必须大于0");
            return segments;
        }

        LocalDateTime current = session.getStartTime();
        LocalDateTime end = session.getEndTime();

        long totalMinutes = java.time.Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
        if (totalMinutes <= 0) {
            totalMinutes = MINUTES_PER_POINT;
        }

        double kwhPerMinute = session.getTotalKwh() / totalMinutes;

        while (current.isBefore(end)) {
            LocalDate currentDate = current.toLocalDate();
            int currentPoint = timeToPoint(current.toLocalTime());

            LocalDateTime nextPointStart = getNextPointStartTime(current);
            LocalDateTime segmentEnd = nextPointStart.isAfter(end) ? end : nextPointStart;

            long segmentMinutes = java.time.Duration.between(current, segmentEnd).toMinutes();
            if (segmentMinutes <= 0) {
                segmentMinutes = 1;
            }

            double segmentKwh = kwhPerMinute * segmentMinutes;

            segments.add(new ChargeSegment(currentDate, currentPoint, segmentKwh));

            current = segmentEnd;
        }
        
        if (end.getMinute() == 0 && end.getSecond() == 0 && end.getNano() == 0) {
            LocalDate endDate = end.toLocalDate();
            int endPoint = timeToPoint(end.toLocalTime());
            if (segments.isEmpty() || 
                segments.get(segments.size() - 1).getPoint() != endPoint ||
                !segments.get(segments.size() - 1).getDate().equals(endDate)) {
                segments.add(new ChargeSegment(endDate, endPoint, 0.0));
            }
        }

        return segments;
    }

    private LocalDateTime getNextPointStartTime(LocalDateTime current) {
        int currentPoint = timeToPoint(current.toLocalTime());
        int nextPoint = currentPoint + 1;
        LocalDate nextDate = current.toLocalDate();
        
        if (nextPoint >= POINTS_PER_DAY) {
            nextPoint = 0;
            nextDate = nextDate.plusDays(1);
        }
        
        return LocalDateTime.of(nextDate, pointToTime(nextPoint));
    }

    public MultiDayRewardResult calculateMultiDayReward(ChargeSession session) {
        MultiDayRewardResult result = new MultiDayRewardResult();
        result.setUserId(session.getUserId());
        result.setChargeId(session.getChargeId());
        result.setStartTime(session.getStartTime());
        result.setEndTime(session.getEndTime());

        List<ChargeSegment> segments = splitChargeSession(session);

        if (segments.isEmpty()) {
            log.warn("充电会话没有产生任何分段数据");
            return result;
        }

        LocalDate currentDate = null;
        DailyRewardResult dailyResult = null;

        for (ChargeSegment segment : segments) {
            if (currentDate == null || !currentDate.equals(segment.getDate())) {
                if (dailyResult != null) {
                    result.addDailyResult(dailyResult);
                }
                currentDate = segment.getDate();
                dailyResult = new DailyRewardResult();
                dailyResult.setDate(currentDate);
            }

            boolean isReward = isRewardSlot(segment.getPoint());
            BigDecimal reward = calculateReward(segment.getPoint(), segment.getKwh());
            dailyResult.addRecord(new ChargeRecord(segment.getPoint(), segment.getDate(), segment.getKwh(), reward, isReward));
        }

        if (dailyResult != null) {
            result.addDailyResult(dailyResult);
        }

        log.info("跨天充电奖励计算完成: 用户={}, 充电ID={}, 总电量={}kWh, 总奖励={}元, 跨{}天",
                session.getUserId(), session.getChargeId(), result.getTotalKwh(), result.getTotalReward(), result.getDailyResults().size());

        return result;
    }

    public DailyRewardResult calculateDailyReward(double[] hourlyKwh) {
        DailyRewardResult result = new DailyRewardResult();
        result.setDate(LocalDate.now());

        if (hourlyKwh == null || hourlyKwh.length != POINTS_PER_DAY) {
            log.error("输入数据无效，需要提供96个点的充电数据");
            return result;
        }

        for (int point = 0; point < POINTS_PER_DAY; point++) {
            double kwh = hourlyKwh[point];
            boolean isReward = isRewardSlot(point);
            BigDecimal reward = calculateReward(point, kwh);
            result.addRecord(new ChargeRecord(point, LocalDate.now(), kwh, reward, isReward));
        }

        return result;
    }

    public List<TimeSlotReward> getRewardSlots() {
        return new ArrayList<>(rewardSlots);
    }

    public String getPointDescription(int point) {
        LocalTime time = pointToTime(point);
        LocalTime endTime = time.plusMinutes(MINUTES_PER_POINT);
        String timeRange = String.format("%02d:%02d-%02d:%02d", time.getHour(), time.getMinute(), endTime.getHour(), endTime.getMinute());

        if (isRewardSlot(point)) {
            BigDecimal rate = getRewardRate(point);
            return String.format("点%d [%s] 奖励时段 (%.2f元/kWh)", point, timeRange, rate);
        } else {
            return String.format("点%d [%s] 非奖励时段", point, timeRange);
        }
    }
}
