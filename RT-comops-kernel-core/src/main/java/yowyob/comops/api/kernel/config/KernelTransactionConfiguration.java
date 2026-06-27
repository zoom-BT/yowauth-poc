package yowyob.comops.api.kernel.config;

import yowyob.comops.api.kernel.application.port.out.ReactiveTransactionalExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
public class KernelTransactionConfiguration {

    @Bean
    @Profile("!test-memory")
    ReactiveTransactionalExecutor reactiveTransactionalExecutor(ReactiveTransactionManager transactionManager) {
        TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
        return new ReactiveTransactionalExecutor() {
            @Override
            public <T> reactor.core.publisher.Mono<T> transactional(reactor.core.publisher.Mono<T> publisher) {
                return transactionalOperator.transactional(publisher);
            }
        };
    }

    @Bean
    @Profile("test-memory")
    ReactiveTransactionalExecutor noOpReactiveTransactionalExecutor() {
        return new ReactiveTransactionalExecutor() {
            @Override
            public <T> reactor.core.publisher.Mono<T> transactional(reactor.core.publisher.Mono<T> publisher) {
                return publisher;
            }
        };
    }
}
