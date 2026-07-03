package com.codibly.energymix.controller;


import com.codibly.energymix.dto.DailyEnergyMix;
import com.codibly.energymix.service.EnergyMixService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EnergyMixController {

    private final EnergyMixService energyMixService;

    public EnergyMixController(EnergyMixService energyMixService) {
        this.energyMixService = energyMixService;
    }

    @GetMapping("/api/energy-mix")
    public List<DailyEnergyMix> getEnergyMix() {
        return energyMixService.getThreeDayEnergyMix();
    }
}
