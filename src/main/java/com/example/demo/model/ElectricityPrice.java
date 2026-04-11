package com.example.demo.model;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ElectricityPrice {
    private List<BigDecimal> prices;

    public ElectricityPrice() {
        this.prices = new ArrayList<>(96);
        for (int i = 0; i < 96; i++) {
            this.prices.add(BigDecimal.ZERO);
        }
    }

    public void setPrice(int index, BigDecimal price) {
        if (index >= 0 && index < 96) {
            this.prices.set(index, price);
        }
    }

    public BigDecimal getPrice(int index) {
        if (index >= 0 && index < 96) {
            return this.prices.get(index);
        }
        return BigDecimal.ZERO;
    }
}
