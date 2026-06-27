package yowyob.comops.api.auth.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record RegisterUserRequest(
        UUID actorId,
        @NotBlank String username,
        @Email @NotBlank String email,
        String phoneNumber,
        String password,
        @NotBlank String authProvider,
        String externalSubject) {
}
