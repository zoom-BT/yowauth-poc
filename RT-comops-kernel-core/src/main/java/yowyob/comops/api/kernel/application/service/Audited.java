package yowyob.comops.api.kernel.application.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a use case method (returning {@code Mono<?>} or {@code Flux<?>}) as audit-worthy.
 *
 * <p>On successful completion of the reactive pipeline, the {@code AuditAspect} records a
 * {@link yowyob.comops.api.kernel.domain.model.SystemAuditEntry} with:
 * <ul>
 *   <li>{@code action} = {@link #action()} (required)</li>
 *   <li>{@code targetType} = {@link #targetType()} (required)</li>
 *   <li>{@code targetId} = the {@code id()} of the result if available via reflection, else the
 *       first UUID-typed argument, else {@code "n/a"}</li>
 *   <li>{@code payloadSummary} = method signature + truncated arguments</li>
 *   <li>tenant, organization, actor and correlation fields are auto-resolved from
 *       {@link ReactiveRequestContextHolder}</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    String action();

    String targetType();

    /** When true (default) failed operations are also audited with action suffixed by "_FAILED". */
    boolean recordFailure() default true;
}
