package com.commerce.cs.bootstrap;

import com.commerce.cs.api.chat.ChatController;
import com.commerce.cs.api.session.SessionController;
import com.commerce.cs.application.chat.ChatUseCase;
import com.commerce.cs.application.idempotency.IdempotencyStore;
import com.commerce.cs.application.llm.LlmClient;
import com.commerce.cs.application.lock.DistributedLock;
import com.commerce.cs.application.outbox.OutboxPort;
import com.commerce.cs.application.rag.QueryResultCachePort;
import com.commerce.cs.application.rag.VectorSearchPort;
import com.commerce.cs.application.returns.ReturnUseCase;
import com.commerce.cs.application.verification.VerificationUseCase;
import com.commerce.cs.bootstrap.demo.DemoAnthropicGateway;
import com.commerce.cs.bootstrap.demo.DemoOutboxExternalPublisher;
import com.commerce.cs.bootstrap.demo.InMemoryIdempotencyStore;
import com.commerce.cs.bootstrap.demo.InMemoryOutboxPort;
import com.commerce.cs.bootstrap.demo.InMemoryQueryResultCacheAdapter;
import com.commerce.cs.bootstrap.demo.InMemoryVectorSearchAdapter;
import com.commerce.cs.bootstrap.demo.LocalDistributedLock;
import com.commerce.cs.infra.llm.client.AnthropicGateway;
import com.commerce.cs.infra.persistence.outbox.OutboxExternalPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Bootstrap 애플리케이션 컨텍스트")
@SpringBootTest(
    classes = CommerceCsAgentApplication.class,
    properties = "spring.main.web-application-type=none"
)
@ActiveProfiles("demo")
class CommerceCsAgentApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("주요 API, application, demo infra Bean을 로딩한다")
    void loads_application_context_with_runtime_wiring() {
        assertThat(applicationContext.getBean(ChatController.class)).isNotNull();
        assertThat(applicationContext.getBean(SessionController.class)).isNotNull();
        assertThat(applicationContext.getBean(ChatUseCase.class)).isNotNull();
        assertThat(applicationContext.getBean(VerificationUseCase.class)).isNotNull();
        assertThat(applicationContext.getBean(ReturnUseCase.class)).isNotNull();
        assertThat(applicationContext.getBean(LlmClient.class)).isNotNull();
        assertThat(applicationContext.getBean(IdempotencyStore.class)).isNotNull();
        assertThat(applicationContext.getBean(DistributedLock.class)).isNotNull();
        assertThat(applicationContext.getBean(QueryResultCachePort.class)).isNotNull();
        assertThat(applicationContext.getBean(VectorSearchPort.class)).isNotNull();
        assertThat(applicationContext.getBean(AnthropicGateway.class)).isNotNull();
        assertThat(applicationContext.getBean(OutboxPort.class)).isNotNull();
        assertThat(applicationContext.getBean(OutboxExternalPublisher.class)).isNotNull();
        assertThat(applicationContext.getBean(Clock.class)).isNotNull();
    }

    @Test
    @DisplayName("demo profile에서 demo adapter Bean을 로딩한다")
    void loads_demo_profile_adapters() {
        assertThat(applicationContext.getBean(IdempotencyStore.class))
            .isInstanceOf(InMemoryIdempotencyStore.class);
        assertThat(applicationContext.getBean(DistributedLock.class))
            .isInstanceOf(LocalDistributedLock.class);
        assertThat(applicationContext.getBean(OutboxPort.class))
            .isInstanceOf(InMemoryOutboxPort.class);
        assertThat(applicationContext.getBean(OutboxExternalPublisher.class))
            .isInstanceOf(DemoOutboxExternalPublisher.class);
        assertThat(applicationContext.getBean(AnthropicGateway.class))
            .isInstanceOf(DemoAnthropicGateway.class);
        assertThat(applicationContext.getBean(QueryResultCachePort.class))
            .isInstanceOf(InMemoryQueryResultCacheAdapter.class);
        assertThat(applicationContext.getBean(VectorSearchPort.class))
            .isInstanceOf(InMemoryVectorSearchAdapter.class);
    }
}
