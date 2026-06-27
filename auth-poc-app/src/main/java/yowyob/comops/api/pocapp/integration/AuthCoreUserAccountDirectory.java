package yowyob.comops.api.pocapp.integration;

import yowyob.comops.api.actor.application.port.out.UserAccountDirectory;
import yowyob.comops.api.auth.application.port.out.UserAccountRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Adaptateur d'intégration repris du bootstrap réel : relie le port actor
 * {@link UserAccountDirectory} au repository UserAccount d'auth-core (présent dans le slice).
 */
@Component
public class AuthCoreUserAccountDirectory implements UserAccountDirectory {

    private final UserAccountRepository userAccountRepository;

    public AuthCoreUserAccountDirectory(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public Mono<UserAccountLink> findByUserId(UUID tenantId, UUID userId) {
        return userAccountRepository.findById(tenantId, userId)
                .map(userAccount -> new UserAccountLink(userAccount.id(), userAccount.actorId(), userAccount.username(),
                        userAccount.email()));
    }
}
