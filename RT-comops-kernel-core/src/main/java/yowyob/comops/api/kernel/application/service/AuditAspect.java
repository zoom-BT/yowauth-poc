package yowyob.comops.api.kernel.application.service;

import yowyob.comops.api.kernel.application.port.in.RecordSystemAuditUseCase;
import yowyob.comops.api.kernel.domain.model.TenantContext;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class AuditAspect {

    private static final Logger LOG = LoggerFactory.getLogger(AuditAspect.class);
    private static final int MAX_PAYLOAD_LENGTH = 1000;

    private final ObjectProvider<RecordSystemAuditUseCase> recordSystemAuditProvider;

    public AuditAspect(ObjectProvider<RecordSystemAuditUseCase> recordSystemAuditProvider) {
        this.recordSystemAuditProvider = recordSystemAuditProvider;
    }

    @Around("@annotation(yowyob.comops.api.kernel.application.service.Audited)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Audited annotation = method.getAnnotation(Audited.class);
        Object proceeded = joinPoint.proceed();

        if (proceeded instanceof Mono<?> mono) {
            return mono.flatMap(value -> recordSuccess(annotation, joinPoint, value).thenReturn(value))
                    .onErrorResume(error -> recordFailureIfNeeded(annotation, joinPoint, error)
                            .then(Mono.error(error)));
        }
        if (proceeded instanceof Flux<?> flux) {
            return flux.collectList()
                    .flatMapMany(values -> recordSuccess(annotation, joinPoint,
                                    values.isEmpty() ? null : values.get(0))
                            .thenMany(Flux.fromIterable(values)))
                    .onErrorResume(error -> recordFailureIfNeeded(annotation, joinPoint, error)
                            .thenMany(Flux.error(error)));
        }
        // Non-reactive: record synchronously, errors propagate normally
        recordSuccess(annotation, joinPoint, proceeded).subscribe();
        return proceeded;
    }

    private Mono<Void> recordSuccess(Audited annotation, ProceedingJoinPoint joinPoint, Object result) {
        return recordEntry(annotation.action(), annotation.targetType(), joinPoint, result);
    }

    private Mono<Void> recordFailureIfNeeded(Audited annotation, ProceedingJoinPoint joinPoint, Throwable error) {
        if (!annotation.recordFailure()) {
            return Mono.empty();
        }
        return recordEntry(annotation.action() + "_FAILED", annotation.targetType(), joinPoint, error);
    }

    private Mono<Void> recordEntry(String action, String targetType, ProceedingJoinPoint joinPoint, Object result) {
        RecordSystemAuditUseCase useCase = recordSystemAuditProvider.getIfAvailable();
        if (useCase == null) {
            return Mono.empty();
        }
        return ReactiveRequestContextHolder.getRequiredContext()
                .onErrorResume(e -> Mono.empty())
                .switchIfEmpty(Mono.fromCallable(() -> new TenantContext(null, null, null, null, null)))
                .flatMap(ctx -> {
                    if (ctx.tenantId() == null) {
                        return Mono.empty();
                    }
                    String targetId = resolveTargetId(joinPoint.getArgs(), result);
                    String payloadSummary = buildPayloadSummary(joinPoint, result);
                    return useCase.record(ctx.tenantId(), ctx.organizationId(), ctx.userId(),
                                    action, targetType, targetId, payloadSummary)
                            .doOnError(e -> LOG.warn("Failed to record audit entry for action={}", action, e))
                            .onErrorResume(e -> Mono.empty());
                });
    }

    private String resolveTargetId(Object[] args, Object result) {
        if (result != null) {
            try {
                Method idMethod = result.getClass().getMethod("id");
                Object value = idMethod.invoke(result);
                if (value != null) {
                    return value.toString();
                }
            } catch (ReflectiveOperationException ignored) {
                // not an entity with id()
            }
        }
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof UUID uuid) {
                    return uuid.toString();
                }
            }
        }
        return "n/a";
    }

    private String buildPayloadSummary(ProceedingJoinPoint joinPoint, Object result) {
        String method = joinPoint.getSignature().toShortString();
        String argSummary = Arrays.stream(joinPoint.getArgs())
                .map(this::truncateArg)
                .collect(Collectors.joining(", ", "[", "]"));
        String resultSummary = result == null ? "null" : truncateArg(result.getClass().getSimpleName());
        String full = method + " args=" + argSummary + " result=" + resultSummary;
        return full.length() > MAX_PAYLOAD_LENGTH ? full.substring(0, MAX_PAYLOAD_LENGTH) : full;
    }

    private String truncateArg(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof CharSequence
                || arg instanceof Number
                || arg instanceof UUID
                || arg instanceof Boolean
                || arg instanceof Enum<?>) {
            String s = arg.toString();
            return s.length() > 120 ? s.substring(0, 120) + "..." : s;
        }
        return arg.getClass().getSimpleName();
    }
}
