package com.codibly.energymix.service;


import com.codibly.energymix.client.CarbonIntensityClient;
import com.codibly.energymix.dto.OptimalChargingWindow;
import com.codibly.energymix.dto.carbonapi.FuelMix;
import com.codibly.energymix.dto.carbonapi.GenerationInterval;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class ChargingWindowService {

    private static final Set<String> CLEAN_FUELS = Set.of("biomass", "nuclear", "hydro", "wind", "solar");
    private static final int INTERVALS_PER_HOUR = 2;

    private final CarbonIntensityClient carbonIntensityClient;

    public ChargingWindowService(CarbonIntensityClient carbonIntensityClient) {
        this.carbonIntensityClient = carbonIntensityClient;
    }

    public OptimalChargingWindow findOptimalWindow(int windowHours) {
        int intervalsNeeded = windowHours * INTERVALS_PER_HOUR;

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate dayAfterTomorrow = tomorrow.plusDays(1);

        List<GenerationInterval> intervals = carbonIntensityClient
                .getGenerationMix(tomorrow, dayAfterTomorrow)
                .stream()
                .sorted(Comparator.comparing(i -> OffsetDateTime.parse(i.from())))
                .toList();

        if (intervals.size() < intervalsNeeded) {
            throw new IllegalStateException("Not enough data to determine the window " + windowHours + " h");
        }

        double[] cleanPercPerInterval = intervals.stream()
                .mapToDouble(this::cleanPercentageOf)
                .toArray();

        double windowSum = 0;
        for (int i = 0; i < intervalsNeeded; i++) {
            windowSum += cleanPercPerInterval[i];
        }

        double bestAverage = windowSum / intervalsNeeded;
        int bestStartIndex = 0;

        for (int i = 1; i <= cleanPercPerInterval.length - intervalsNeeded; i++) {
            windowSum = windowSum - cleanPercPerInterval[i - 1] + cleanPercPerInterval[i + intervalsNeeded - 1];
            double average = windowSum / intervalsNeeded;
            if (average > bestAverage) {
                bestAverage = average;
                bestStartIndex = i;
            }
        }

        GenerationInterval startInterval = intervals.get(bestStartIndex);
        GenerationInterval endInterval = intervals.get(bestStartIndex + intervalsNeeded - 1);

        return new OptimalChargingWindow(
                OffsetDateTime.parse(startInterval.from()),
                OffsetDateTime.parse(endInterval.to()),
                round(bestAverage)
        );
    }

    private double cleanPercentageOf(GenerationInterval interval) {
        return interval.generationmix().stream()
                .filter(fm -> CLEAN_FUELS.contains(fm.fuel()))
                .mapToDouble(FuelMix::perc)
                .sum();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }


}
