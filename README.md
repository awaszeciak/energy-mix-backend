# Energy Mix Backend

Backend service presenting the UK's energy generation mix and calculating the optimal time window for electric vehicle charging based on clean energy share.

## Tech stack

- Java 21
- Spring Boot 3.3
- Maven

## Data source

[Carbon Intensity API](https://carbon-intensity.github.io/api-definitions/) - a public API providing the UK's energy generation mix in 30-minute intervals.

## Endpoints

### `GET /api/energy-mix`

Returns the energy mix for three days: today, tomorrow, and the day after tomorrow.

For each day: the average shere of each energy source (calculated from 30-minute intervals) and the clean energy percentage (sum of shares from biomass, nuclear, hydro, wind, solar).

Example response:
```json
[
  {
    "date": "2026-07-05",
    "fuelBreakdown": [
      { "fuel": "biomass", "percentage": 4.2 },
      { "fuel": "gas", "percentage": 30.1}
    ],
    "cleanEnergyPercentage": 62.4
  }
]
```


### `GET /api/optimal-window?hours={1-6}`
Determines the time window of the given length (in full hours, 1-6) with the highest forecasted clean energy share.

The window may start on one day and end on the next (crossing midnight) - the algorithm operates on a single, chronologically sorted list of intervals spanning both days, with no split by calendar day.

Example response:
```json
{
  "start": "2026-07-06T12:30:00Z",
  "end": "2026-07-06T15:30:00Z",
  "averageCleanEnergyPercentage": 88.9
}
```

## Architecture
 
    client/         CarbonIntensityClient: fetches raw data from the external API
    dto/            response DTOs for both endpoints
    dto/carbonapi/  DTOs mapping the raw Carbon Intensity API response
    service/        business logic: mix aggregation (EnergyMixService),
                    optimal window algorithm (ChargingWindowService)
    controller/     REST endpoints
    config/         WebClient and CORS configuration

The optimal window algorithm uses a **sliding window** technique - linear time complexity O(n) relative to the number of intervals, instead of recalculating the average from scratch for every possible window.

## Running locally

```
./mvnw spring-boot:run
```

The application starts on port `8081` by default (configurable via the `PORT` environment variable).

## Tests

```
./mvnw test
```

Unit tests cover the energy mix aggregation logic and the optimal window algorithm (including the case of a window crossing mifnight).

## Depolyment

Deployed on [Render](https://render.com) as a Docker Web Service.

Production URL: https://energy-mix-backend-0ts0.onrender.com

> Note: Render's free tier spins down the service after ~15 minutes of inactivity - the first request after a period of inactivity may take 30-60 seconds.
