package yowyob.comops.api.kernel.adapter.in.web;

import yowyob.comops.api.kernel.config.JwtTokenService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class JwkSetController {

    private final JwtTokenService jwtTokenService;

    public JwkSetController(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> jwks() {
        return Mono.fromSupplier(() -> jwtTokenService.isEnabled()
                ? ResponseEntity.ok(jwtTokenService.getPublicJwkSet().toJSONObject())
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error", "JWT signing key set is not configured.")));
    }
}
