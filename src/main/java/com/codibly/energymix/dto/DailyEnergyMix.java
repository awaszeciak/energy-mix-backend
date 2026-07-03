package com.codibly.energymix.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyEnergyMix(
        LocalDate date,
        List<FuelPercentage> fuelBreakdown,
        double cleanEnergyPercentage
) {
}
