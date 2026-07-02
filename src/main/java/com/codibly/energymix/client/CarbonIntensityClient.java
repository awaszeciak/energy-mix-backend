package com.codibly.energymix.client;


import com.codibly.energymix.dto.carbonapi.CarbonApiResponse;
import com.codibly.energymix.dto.carbonapi.GenerationInterval;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class CarbonIntensityClient {

    private final WebClient webClient;

    public CarbonIntensityClient(@Qualifier("carbonIntensityWebClient") WebClient webClient) {
        this.webClient = webClient;
    }


    public List<GenerationInterval> getGenerationMix(LocalDate from, LocalDate to) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");
        String fromStr = from.atStartOfDay().format(formatter);
        String toStr = to.plusDays(1).atStartOfDay().format(formatter);

        String path = "/generation/" + fromStr + "/" + toStr;

        CarbonApiResponse response = webClient.get()
                .uri(path)
                .retrieve()
                .bodyToMono(CarbonApiResponse.class)
                .block();

        if (response == null || response.data() == null) {
            throw new IllegalArgumentException("Lack of data from Carbon Intensity API for " + fromStr + " - " + toStr);
        }

        return response.data();
    }




}
