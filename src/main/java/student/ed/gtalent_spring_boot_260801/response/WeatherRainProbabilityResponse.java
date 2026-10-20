package student.ed.gtalent_spring_boot_260801.response;

public class WeatherRainProbabilityResponse {

    private final String startTime;
    private final String endTime;
    private final Integer rainProbability;

    public WeatherRainProbabilityResponse(
            String startTime,
            String endTime,
            Integer rainProbability) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.rainProbability = rainProbability;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public Integer getRainProbability() {
        return rainProbability;
    }
}
