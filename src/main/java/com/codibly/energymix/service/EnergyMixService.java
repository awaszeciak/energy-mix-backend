package com.codibly.energymix.service;


import com.codibly.energymix.client.CarbonIntensityClient;
import com.codibly.energymix.dto.DailyEnergyMix;
import com.codibly.energymix.dto.FuelPercentage;
import com.codibly.energymix.dto.carbonapi.FuelMix;
import com.codibly.energymix.dto.carbonapi.GenerationInterval;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EnergyMixService {

    private static final Set<String> CLEAN_FUELS = Set.of("biomass", "nuclear", "hydro", "wind", "solar");

    private final CarbonIntensityClient carbonIntensityClient;

    public EnergyMixService(CarbonIntensityClient carbonIntensityClient) {
        this.carbonIntensityClient = carbonIntensityClient;
    }

    public List<DailyEnergyMix> getThreeDayEnergyMix() {
        LocalDate today = LocalDate.now();
        List<GenerationInterval> intervals = carbonIntensityClient.getGenerationMix(today, today.plusDays(2));

        Map<LocalDate, List<GenerationInterval>> groupedByDay = groupByDate(intervals);

        List<DailyEnergyMix> result = new ArrayList<>();
        for (LocalDate day = today; !day.isAfter(today.plusDays(2)); day = day.plusDays(1)) {
            List<GenerationInterval> dayIntervals = groupedByDay.getOrDefault(day, List.of());
            result.add(aggregateDay(day, dayIntervals));
        }
        return result;
    }

    private Map<LocalDate, List<GenerationInterval>> groupByDate(List<GenerationInterval> intervals) {
        return intervals.stream()
                .collect(Collectors.groupingBy(this::extractDate));
    }

    private LocalDate extractDate(GenerationInterval interval) {
        return OffsetDateTime.parse(interval.from()).toLocalDate();
    }

    private DailyEnergyMix aggregateDay(LocalDate date,  List<GenerationInterval> dayIntervals) {
        if (dayIntervals.isEmpty()) {
            return new DailyEnergyMix(date, List.of(), 0.0);
        }

        Map<String, List<Double>> percByFuel = new HashMap<>();
        for (GenerationInterval interval : dayIntervals) {
            for (FuelMix fuelMix : interval.generationmix()) {
                percByFuel.computeIfAbsent(fuelMix.fuel(), k -> new ArrayList<>()).add(fuelMix.perc());
            }
        }

        List<FuelPercentage> breakdown = percByFuel.entrySet().stream()
                .map(e -> new FuelPercentage(e.getKey(), average(e.getValue())))
                .sorted(Comparator.comparing(FuelPercentage::fuel))
                .toList();

        double cleanEnergyPercentage = breakdown.stream()
                .filter(fp -> CLEAN_FUELS.contains(fp.fuel()))
                .mapToDouble(FuelPercentage::percentage)
                .sum();

        return new DailyEnergyMix(date, breakdown, round(cleanEnergyPercentage));
    }

    private double average(List<Double> values) {
        return round(values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
