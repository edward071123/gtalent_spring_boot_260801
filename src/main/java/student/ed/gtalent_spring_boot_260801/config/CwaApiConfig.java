package student.ed.gtalent_spring_boot_260801.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CwaApiConfig {

    @Bean("cwaRestClient")
    public RestClient cwaRestClient() {
        return RestClient.builder()
                .baseUrl("https://opendata.cwa.gov.tw")
                .build();
    }
}
