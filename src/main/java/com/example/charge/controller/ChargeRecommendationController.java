package com.example.charge.controller;

import com.example.charge.model.ChargeRecommendationRequest;
import com.example.charge.model.ChargeRecommendationResponse;
import com.example.charge.model.ElectricityPrice;
import com.example.charge.service.ChargeRecommendationService;
import com.example.charge.service.ElectricityPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/charge")
@CrossOrigin(origins = "*")
public class ChargeRecommendationController {

    @Autowired
    private ChargeRecommendationService recommendationService;

    @Autowired
    private ElectricityPriceService priceService;

    @PostMapping("/recommend")
    public ResponseEntity<ChargeRecommendationResponse> recommend(@RequestBody ChargeRecommendationRequest request) {
        ChargeRecommendationResponse response = recommendationService.generateRecommendation(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/prices")
    public ResponseEntity<List<ElectricityPrice>> getPrices() {
        return ResponseEntity.ok(priceService.generateDayAheadPrices());
    }
}
