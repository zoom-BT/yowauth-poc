package yowyob.comops.api.pocapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reprise de la JacksonConfiguration du bootstrap réel : fournit l'ObjectMapper
 * attendu par les filtres web de kernel-core (qui ne s'appuient pas sur l'autoconfig).
 */
@Configuration
public class JacksonConfiguration {

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .findAndAddModules()
                .build();
    }
}
