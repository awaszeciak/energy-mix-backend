package com.codibly.energymix.dto;

import java.time.OffsetDateTime;

public record OptimalChargingWindow(
        OffsetDateTime start,
        OffsetDateTime end,
        double averageCleanEnergyPercentage
) {
}
