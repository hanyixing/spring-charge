package com.example.demo.service;

import com.example.demo.entity.ElectricityPrice;
import com.example.demo.entity.ElectricityPrice.PriceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 电价配置服务
 * 管理尖峰平谷电价配置
 */
@Slf4j
@Service
public class ElectricityPriceService {

    /**
     * 默认电价配置列表
     */
    private List<ElectricityPrice> defaultPrices;

    /**
     * 当前使用的电价配置
     */
    private List<ElectricityPrice> currentPrices;

    @PostConstruct
    public void init() {
        // 初始化默认电价配置
        defaultPrices = createDefaultPrices();
        currentPrices = new ArrayList<>(defaultPrices);
        log.info("电价配置服务初始化完成，共{}个时段", currentPrices.size());
    }

    /**
     * 创建默认电价配置
     * 典型分时电价配置：
     * - 谷时：00:00-08:00 (0-32点)
     * - 平时：08:00-10:00 (32-40点), 15:00-19:00 (60-76点), 21:00-00:00 (84-96点)
     * - 峰时：10:00-15:00 (40-60点), 19:00-21:00 (76-84点)
     * - 尖时：根据地区可能不同，这里设为19:00-21:00 (76-84点) 或 10:00-12:00 (40-48点)
     */
    private List<ElectricityPrice> createDefaultPrices() {
        List<ElectricityPrice> prices = new ArrayList<>();

        // 谷时：00:00-08:00 (0-32点) - 电价最低
        prices.add(ElectricityPrice.builder()
                .type(PriceType.GU)
                .price(0.3)
                .startPoint(0)
                .endPoint(32)
                .build());

        // 平时：08:00-10:00 (32-40点)
        prices.add(ElectricityPrice.builder()
                .type(PriceType.PING)
                .price(0.6)
                .startPoint(32)
                .endPoint(40)
                .build());

        // 峰时：10:00-12:00 (40-48点)
        prices.add(ElectricityPrice.builder()
                .type(PriceType.FENG)
                .price(1.0)
                .startPoint(40)
                .endPoint(48)
                .build());

        // 尖时：12:00-14:00 (48-56点) - 电价最高
        prices.add(ElectricityPrice.builder()
                .type(PriceType.JIAN)
                .price(1.5)
                .startPoint(48)
                .endPoint(56)
                .build());

        // 峰时：14:00-15:00 (56-60点)
        prices.add(ElectricityPrice.builder()
                .type(PriceType.FENG)
                .price(1.0)
                .startPoint(56)
                .endPoint(60)
                .build());

        // 平时：15:00-19:00 (60-76点)
        prices.add(ElectricityPrice.builder()
                .type(PriceType.PING)
                .price(0.6)
                .startPoint(60)
                .endPoint(76)
                .build());

        // 尖时：19:00-21:00 (76-84点) - 电价最高
        prices.add(ElectricityPrice.builder()
                .type(PriceType.JIAN)
                .price(1.5)
                .startPoint(76)
                .endPoint(84)
                .build());

        // 平时：21:00-24:00 (84-96点)
        prices.add(ElectricityPrice.builder()
                .type(PriceType.PING)
                .price(0.6)
                .startPoint(84)
                .endPoint(96)
                .build());

        return prices;
    }

    /**
     * 获取当前电价配置
     */
    public List<ElectricityPrice> getCurrentPrices() {
        return new ArrayList<>(currentPrices);
    }

    /**
     * 更新电价配置
     */
    public void updatePrices(List<ElectricityPrice> newPrices) {
        this.currentPrices = new ArrayList<>(newPrices);
        log.info("电价配置已更新，共{}个时段", currentPrices.size());
    }

    /**
     * 重置为默认电价配置
     */
    public void resetToDefault() {
        this.currentPrices = new ArrayList<>(defaultPrices);
        log.info("电价配置已重置为默认");
    }

    /**
     * 获取指定时间点的电价
     *
     * @param point 时间点索引（0-95）
     * @return 电价配置
     */
    public ElectricityPrice getPriceAtPoint(int point) {
        for (ElectricityPrice price : currentPrices) {
            if (point >= price.getStartPoint() && point < price.getEndPoint()) {
                return price;
            }
        }
        // 默认返回谷时电价
        return ElectricityPrice.builder()
                .type(PriceType.GU)
                .price(0.3)
                .startPoint(point)
                .endPoint(point + 1)
                .build();
    }

    /**
     * 获取指定时间点的电价类型
     *
     * @param point 时间点索引（0-95）
     * @return 电价类型
     */
    public PriceType getPriceTypeAtPoint(int point) {
        ElectricityPrice price = getPriceAtPoint(point);
        return price != null ? price.getType() : PriceType.GU;
    }

    /**
     * 获取指定时间点的电价（元/kWh）
     *
     * @param point 时间点索引（0-95）
     * @return 电价
     */
    public double getPriceValueAtPoint(int point) {
        ElectricityPrice price = getPriceAtPoint(point);
        return price != null ? price.getPrice() : 0.3;
    }

    /**
     * 获取96个点的电价数组
     *
     * @return 电价数组
     */
    public double[] getPriceArray() {
        double[] prices = new double[96];
        for (int i = 0; i < 96; i++) {
            prices[i] = getPriceValueAtPoint(i);
        }
        return prices;
    }

    /**
     * 获取96个点的电价类型数组
     *
     * @return 电价类型数组
     */
    public PriceType[] getPriceTypeArray() {
        PriceType[] types = new PriceType[96];
        for (int i = 0; i < 96; i++) {
            types[i] = getPriceTypeAtPoint(i);
        }
        return types;
    }
}
