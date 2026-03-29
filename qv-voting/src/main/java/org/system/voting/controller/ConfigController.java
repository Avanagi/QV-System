package org.system.voting.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Value("${app.max-budget:100.0}")
    private double maxBudget;

    @Value("${app.poll-cost:50.00}")
    private double pollCost;

    @GetMapping
    public Map<String, Object> getAppConfig() {
        return Map.of(
                "maxBudget", maxBudget,
                "pollCost", pollCost
        );
    }
}