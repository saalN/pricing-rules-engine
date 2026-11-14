package com.priceforge.audit.infrastructure.in.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.priceforge.audit.application.port.in.RecordAuditEntryUseCase;
import com.priceforge.sharedkernel.OutboxEventHandler;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AuditOutboxConfigTest {

    @Test
    void registersExactlyOneHandlerPerAuditedEventType() {
        new ApplicationContextRunner()
            .withBean(AuditEntryTranslator.class)
            .withBean(RecordAuditEntryUseCase.class, () -> mock(RecordAuditEntryUseCase.class))
            .withUserConfiguration(AuditOutboxConfig.class)
            .run(context -> {
                Map<String, OutboxEventHandler> handlers = context.getBeansOfType(OutboxEventHandler.class);

                assertThat(handlers.values().stream().map(OutboxEventHandler::eventType).collect(Collectors.toList()))
                    .containsExactlyInAnyOrderElementsOf(AuditEntryTranslator.AUDITED_EVENT_TYPES);
            });
    }
}
