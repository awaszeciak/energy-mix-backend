package com.codibly.energymix.service;

import com.codibly.energymix.client.CarbonIntensityClient;
import com.codibly.energymix.dto.DailyEnergyMix;
import com.codibly.energymix.dto.FuelPercentage;
import com.codibly.energymix.dto.carbonapi.FuelMix;
import com.codibly.energymix.dto.carbonapi.GenerationInterval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class EnergyMixServiceTest {

    @Mock
    private CarbonIntensityClient carbonIntensityClient;

    private EnergyMixService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new EnergyMixService(carbonIntensityClient);
    }

    @Test
    void shouldAverageFuelPercentagesForDayWithMultipleIntervals() {
        LocalDate today = LocalDate.now();

        GenerationInterval interval1 = new GenerationInterval(
                today + "T00:00Z", today + "T00:30Z",
                List.of(new FuelMix("gas", 60.0), new FuelMix("wind", 40.0))
        );

        GenerationInterval interval2 = new GenerationInterval(
                today + "T00:00Z", today + "T01:00",
                List.of(new FuelMix("gas", 40.0), new FuelMix("wind", 60.0))
        );

        when(carbonIntensityClient.getGenerationMix(any(), any()))
                .thenReturn(List.of(interval1, interval2));

        List<DailyEnergyMix> result = service.getThreeDayEnergyMix();

        DailyEnergyMix todayMix = result.get(0);

        assertThat(todayMix.date()).isEqualTo(today);
        assertThat(todayMix.fuelBreakdown()).containsExactlyInAnyOrder(
                new FuelPercentage("gas", 50.0),
                new FuelPercentage("wind", 50.0)
        );

        assertThat(todayMix.cleanEnergyPercentage()).isEqualTo(50.0);
    }

    @Test
    void shouldReturnThreeDaysEvenWhenApiReturnsPartialData() {
        LocalDate today = LocalDate.now();

        GenerationInterval onlyToday = new GenerationInterval(
                today + "T00:00Z", today + "T00:30Z",
                List.of(new FuelMix("solar", 100.0))
        );

        when(carbonIntensityClient.getGenerationMix(any(), any()))
                .thenReturn(List.of(onlyToday));

        List<DailyEnergyMix> result = service.getThreeDayEnergyMix();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).date()).isEqualTo(today);
        assertThat(result.get(1).date()).isEqualTo(today.plusDays(1));
        assertThat(result.get(2).date()).isEqualTo(today.plusDays(2));

        assertThat(result.get(1).fuelBreakdown()).isEmpty();
        assertThat(result.get(1).cleanEnergyPercentage()).isEqualTo(0.0);
    }

    @Test
    void shouldSumAllCleanFuelsNotJustOne() {
        LocalDate today = LocalDate.now();

        GenerationInterval interval = new GenerationInterval(
                today + "T00:00Z", today + "T00:30Z",
                List.of(
                        new FuelMix("biomass", 10.0),
                        new FuelMix("nuclear", 15.0),
                        new FuelMix("hydro", 5.0),
                        new FuelMix("wind", 20.0),
                        new FuelMix("solar", 10.0),
                        new FuelMix("gas", 40.0)
                )
        );

        when(carbonIntensityClient.getGenerationMix(any(), any()))
                .thenReturn(List.of(interval));

        List<DailyEnergyMix> result = service.getThreeDayEnergyMix();

        assertThat(result.get(0).cleanEnergyPercentage()).isEqualTo(60.0);
    }
}
