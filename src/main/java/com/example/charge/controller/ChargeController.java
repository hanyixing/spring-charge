package com.example.charge.controller;

import com.example.charge.model.ChargeRequest;
import com.example.charge.model.ChargeResult;
import com.example.charge.model.ElectricityPrice;
import com.example.charge.service.ChargeOptimizationService;
import com.example.charge.service.PriceConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/charge")
public class ChargeController {

    @Autowired
    private ChargeOptimizationService chargeOptimizationService;

    @Autowired
    private PriceConfigService priceConfigService;

    @PostMapping("/optimize")
    public ResponseEntity<ChargeResult> optimizeCharge(@RequestBody ChargeRequest request) {
        if (request.getPriceList() == null || request.getPriceList().isEmpty()) {
            request.setPriceList(priceConfigService.getAllPrices());
        }
        ChargeResult result = chargeOptimizationService.calculateOptimalCharge(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/prices/default")
    public ResponseEntity<List<ElectricityPrice>> getDefaultPrices() {
        return ResponseEntity.ok(priceConfigService.getAllPrices());
    }
}
