package com.codibly.energymix.dto.carbonapi;

import java.util.List;

public record CarbonApiResponse(List<GenerationInterval> data) {
}
