package yowyob.comops.api.auth.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserAccountSpringDataRepository extends ReactiveCrudRepository<UserAccountEntity, UUID> {

    Mono<Boolean> existsByTenantIdAndUsernameIgnoreCase(UUID tenantId, String username);

    Mono<UserAccountEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
            SELECT *
            FROM auth_core.user_account
            WHERE tenant_id = :tenantId
              AND lower(username) = lower(:username)
            LIMIT 1
            """)
    Mono<UserAccountEntity> findByTenantIdAndUsernameIgnoreCase(UUID tenantId, String username);

    @Query("""
            SELECT *
            FROM auth_core.user_account
            WHERE tenant_id = :tenantId
              AND lower(email) = lower(:email)
            LIMIT 1
            """)
    Mono<UserAccountEntity> findByTenantIdAndEmailIgnoreCase(UUID tenantId, String email);

    Mono<UserAccountEntity> findByTenantIdAndPhoneNumber(UUID tenantId, String phoneNumber);

    @Query("""
            SELECT *
            FROM auth_core.user_account
            WHERE lower(username) = lower(:username)
            """)
    Flux<UserAccountEntity> findAllByUsernameIgnoreCase(String username);

    @Query("""
            SELECT *
            FROM auth_core.user_account
            WHERE lower(email) = lower(:email)
            """)
    Flux<UserAccountEntity> findAllByEmailIgnoreCase(String email);

    Flux<UserAccountEntity> findAllByPhoneNumber(String phoneNumber);
}
