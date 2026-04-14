package com.example.demo.controller;

import com.example.demo.model.EnergyStorageDevice;
import com.example.demo.model.OptimizationResult;
import com.example.demo.service.EnergyOptimizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/energy")
public class EnergyOptimizationController {

    @Autowired
    private EnergyOptimizationService optimizationService;

    @GetMapping("/optimize")
    public ResponseEntity<OptimizationResult> optimize() {
        List<EnergyStorageDevice> devices = optimizationService.createSampleDevices();
        OptimizationResult result = optimizationService.optimizeChargeDischarge(devices);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/optimize/custom")
    public ResponseEntity<OptimizationResult> optimizeCustom(@RequestBody List<EnergyStorageDevice> devices) {
        OptimizationResult result = optimizationService.optimizeChargeDischarge(devices);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/devices")
    public ResponseEntity<List<EnergyStorageDevice>> getSampleDevices() {
        return ResponseEntity.ok(optimizationService.createSampleDevices());
    }
}
