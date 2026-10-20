package student.ed.gtalent_spring_boot_260801.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import student.ed.gtalent_spring_boot_260801.response.WeatherRainProbabilityResponse;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private static final String CWA_DATASET_PATH = "/api/v1/rest/datastore/F-D0047-001";
    private static final String RAIN_PROBABILITY_ELEMENT = "3小時降雨機率";

    private final RestClient restClient;
    private final String authorization;

    public WeatherController(
            @Qualifier("cwaRestClient") RestClient restClient,
            @Value("${cwa.api.authorization}") String authorization) {
        this.restClient = restClient;
        this.authorization = authorization;
    }

    @GetMapping
    public List<WeatherRainProbabilityResponse> getWeather(
            @RequestParam(defaultValue = "頭城鎮") String locationName) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(CWA_DATASET_PATH)
                        .queryParam("Authorization", authorization)
                        .queryParam("format", "JSON")
                        .queryParam("LocationName", locationName)
                        .queryParam("ElementName", RAIN_PROBABILITY_ELEMENT)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);

        return extractRainProbabilities(response);
    }

    private List<WeatherRainProbabilityResponse> extractRainProbabilities(JsonNode response) {
        List<WeatherRainProbabilityResponse> result = new ArrayList<>();
        JsonNode locationsGroups = response.path("records").path("Locations");

        for (JsonNode locationsGroup : locationsGroups) {
            for (JsonNode location : locationsGroup.path("Location")) {
                for (JsonNode weatherElement : location.path("WeatherElement")) {
                    for (JsonNode time : weatherElement.path("Time")) {
                        String probability = time.path("ElementValue")
                                .path(0)
                                .path("ProbabilityOfPrecipitation")
                                .stringValue("");

                        if (!probability.isBlank()) {
                            result.add(new WeatherRainProbabilityResponse(
                                    time.path("StartTime").stringValue(""),
                                    time.path("EndTime").stringValue(""),
                                    Integer.valueOf(probability)));
                        }
                    }
                }
            }
        }

        return result;
    }
}
