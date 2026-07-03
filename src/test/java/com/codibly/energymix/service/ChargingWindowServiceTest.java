package com.codibly.energymix.service;

import com.codibly.energymix.client.CarbonIntensityClient;
import com.codibly.energymix.dto.OptimalChargingWindow;
import com.codibly.energymix.dto.carbonapi.FuelMix;
import com.codibly.energymix.dto.carbonapi.GenerationInterval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ChargingWindowServiceTest {

    @Mock
    private CarbonIntensityClient carbonIntensityClient;

    private ChargingWindowService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ChargingWindowService(carbonIntensityClient);
    }

    private GenerationInterval interval(LocalDate day, int hour, int minute, double cleanPerc) {
        OffsetDateTime from = day.atStartOfDay().atOffset(java.time.ZoneOffset.UTC).plusHours(hour).plusMinutes(minute);
        OffsetDateTime to = from.plusMinutes(30);

        return new GenerationInterval(
                from.toString(),
                to.toString(),
                List.of(new FuelMix("wind", cleanPerc), new FuelMix("gas", 100 - cleanPerc))
        );
    }

    @Test
    void shouldFindWindowWithHighestAverageCleanEnergy() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<GenerationInterval> intervals = new ArrayList<>();

        intervals.add(interval(tomorrow, 0,0,10.0));
        intervals.add(interval(tomorrow, 0, 30, 10.0));
        intervals.add(interval(tomorrow, 1, 0, 90.0));
        intervals.add(interval(tomorrow, 1, 30, 90.0));
        intervals.add(interval(tomorrow, 2, 0, 20.0));
        intervals.add(interval(tomorrow, 2, 30, 20.0));

        when(carbonIntensityClient.getGenerationMix(any(), any())).thenReturn(intervals);

        OptimalChargingWindow result = service.findOptimalWindow(1);

        assertThat(result.averageCleanEnergyPercentage()).isEqualTo(90.0);
        assertThat(result.start()).isEqualTo(tomorrow.atStartOfDay().atOffset(java.time.ZoneOffset.UTC).plusHours(1));
    }

    @Test
    void shouldFindWindowThatCrossesMidnight() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate dayAfter = tomorrow.plusDays(1);

        List<GenerationInterval> intervals = new ArrayList<>();

        intervals.add(interval(tomorrow, 22, 0, 5.0));
        intervals.add(interval(tomorrow, 22, 30, 5.0));
        intervals.add(interval(tomorrow,23,0,95.0));
        intervals.add(interval(tomorrow, 23, 30, 95.0));
        intervals.add(interval(dayAfter, 0, 0, 95.0));
        intervals.add(interval(dayAfter, 0, 30, 95.0));
        intervals.add(interval(dayAfter, 1, 0, 5.0));
        intervals.add(interval(dayAfter, 1, 30, 5.0));

        when(carbonIntensityClient.getGenerationMix(any(), any())).thenReturn(intervals);

        OptimalChargingWindow result = service.findOptimalWindow(2);

        assertThat(result.averageCleanEnergyPercentage()).isEqualTo(95.0);
        assertThat(result.start()).isEqualTo(
                tomorrow.atStartOfDay().atOffset(java.time.ZoneOffset.UTC).plusHours(23)
        );
        assertThat(result.end()).isEqualTo(
                dayAfter.atStartOfDay().atOffset(java.time.ZoneOffset.UTC).plusHours(1)
        );
    }

    @Test
    void shouldThrowWHenNotEnoughDataForRequestedWindow() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<GenerationInterval> intervals = List.of(
                interval(tomorrow, 0, 0, 50.0),
                interval(tomorrow, 0, 30, 50.0)
        );

        when(carbonIntensityClient.getGenerationMix(any(), any())).thenReturn(intervals);

        assertThatThrownBy(()->service.findOptimalWindow(6)).isInstanceOf(IllegalStateException.class);
    }
}
