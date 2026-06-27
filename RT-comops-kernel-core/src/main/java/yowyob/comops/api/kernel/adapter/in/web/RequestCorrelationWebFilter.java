package yowyob.comops.api.kernel.adapter.in.web;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import yowyob.comops.api.kernel.application.service.ReactiveRequestContextHolder;
import yowyob.comops.api.kernel.domain.model.RequestCorrelation;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RequestCorrelationWebFilter implements WebFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String CLIENT_ID_HEADER = "X-Client-Id";

    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_CLIENT_ID = "clientId";
    public static final String MDC_REMOTE_IP = "remoteIp";
    public static final String MDC_HTTP_METHOD = "httpMethod";
    public static final String MDC_REQUEST_PATH = "requestPath";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String incomingRequestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        String requestId = (incomingRequestId == null || incomingRequestId.isBlank() || incomingRequestId.length() > 128)
                ? UUID.randomUUID().toString()
                : incomingRequestId.trim();

        String clientId = request.getHeaders().getFirst(CLIENT_ID_HEADER);
        String remoteIp = resolveRemoteIp(request);
        String method = request.getMethod() == null ? "UNKNOWN" : request.getMethod().name();
        String path = request.getURI().getRawPath();

        RequestCorrelation correlation = new RequestCorrelation(
                requestId,
                clientId,
                remoteIp,
                method,
                path,
                Instant.now());

        exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);

        return chain.filter(exchange)
                .contextWrite(ctx -> ReactiveRequestContextHolder.withRequestCorrelation(ctx, correlation))
                .doOnSubscribe(s -> applyMdc(correlation))
                .doFinally(signal -> clearMdc());
    }

    private void applyMdc(RequestCorrelation correlation) {
        MDC.put(MDC_REQUEST_ID, correlation.requestId());
        if (correlation.clientApplicationId() != null) {
            MDC.put(MDC_CLIENT_ID, correlation.clientApplicationId());
        }
        if (correlation.remoteIp() != null) {
            MDC.put(MDC_REMOTE_IP, correlation.remoteIp());
        }
        MDC.put(MDC_HTTP_METHOD, correlation.httpMethod());
        if (correlation.requestPath() != null) {
            MDC.put(MDC_REQUEST_PATH, correlation.requestPath());
        }
    }

    private void clearMdc() {
        MDC.remove(MDC_REQUEST_ID);
        MDC.remove(MDC_CLIENT_ID);
        MDC.remove(MDC_REMOTE_IP);
        MDC.remove(MDC_HTTP_METHOD);
        MDC.remove(MDC_REQUEST_PATH);
    }

    private String resolveRemoteIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = (comma == -1 ? forwarded : forwarded.substring(0, comma)).trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddress() == null
                ? null
                : request.getRemoteAddress().getAddress().getHostAddress();
    }
}
