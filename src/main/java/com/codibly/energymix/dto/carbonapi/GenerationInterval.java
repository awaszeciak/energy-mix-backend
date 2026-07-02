package com.codibly.energymix.dto.carbonapi;

import java.util.List;

public record GenerationInterval(String from, String to, List<FuelMix> generationmix) {
}
