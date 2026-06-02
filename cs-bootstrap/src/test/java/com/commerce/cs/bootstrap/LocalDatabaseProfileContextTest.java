package com.commerce.cs.bootstrap;

import com.commerce.cs.api.chat.ChatController;
import com.commerce.cs.api.session.SessionController;
import com.commerce.cs.application.chat.ChatUseCase;
import com.commerce.cs.application.idempotency.IdempotencyStore;
import com.commerce.cs.application.llm.LlmClient;
import com.commerce.cs.application.lock.DistributedLock;
import com.commerce.cs.application.order.OrderRepository;
import com.commerce.cs.application.outbox.OutboxPort;
import com.commerce.cs.application.rag.QueryResultCachePort;
import com.commerce.cs.application.rag.VectorSearchPort;
import com.commerce.cs.application.returns.ReturnRepository;
import com.commerce.cs.application.returns.ReturnUseCase;
import com.commerce.cs.application.verification.CustomerLookupPort;
import com.commerce.cs.application.verification.VerificationUseCase;
import com.commerce.cs.bootstrap.demo.DemoAnthropicGateway;
import com.commerce.cs.bootstrap.demo.InMemoryCustomerLookupAdapter;
import com.commerce.cs.bootstrap.demo.InMemoryIdempotencyStore;
import com.commerce.cs.bootstrap.demo.InMemoryQueryResultCacheAdapter;
import com.commerce.cs.bootstrap.demo.InMemorySessionManager;
import com.commerce.cs.bootstrap.demo.InMemoryVectorSearchAdapter;
import com.commerce.cs.bootstrap.demo.LocalDistributedLock;
import com.commerce.cs.bootstrap.local.LocalOutboxExternalPublisher;
import com.commerce.cs.infra.llm.client.AnthropicGateway;
import com.commerce.cs.infra.persistence.order.OrderRepositoryAdapter;
import com.commerce.cs.infra.persistence.order.OrderJpaRepository;
import com.commerce.cs.infra.persistence.outbox.OutboxAdapter;
import com.commerce.cs.infra.persistence.outbox.OutboxExternalPublisher;
import com.commerce.cs.infra.persistence.outbox.OutboxJpaRepository;
import com.commerce.cs.infra.persistence.returns.ReturnRepositoryAdapter;
import com.commerce.cs.infra.persistence.returns.ReturnJpaRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("local DB profile bootstrap 而⑦뀓?ㅽ듃")
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("local")
@SpringBootTest(
    classes = CommerceCsAgentApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.task.scheduling.enabled=false",
        "commerce.outbox.poller.enabled=false"
    }
)
class LocalDatabaseProfileContextTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("commerce_cs")
        .withUsername("commerce")
        .withPassword("commerce");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private OrderJpaRepository orderJpaRepository;

    @Autowired
    private ReturnJpaRepository returnJpaRepository;

    @Autowired
    private OutboxJpaRepository outboxJpaRepository;

    @Autowired
    private ChatController chatController;

    @Autowired
    private SessionController sessionController;

    @Autowired
    private ChatUseCase chatUseCase;

    @Autowired
    private VerificationUseCase verificationUseCase;

    @Autowired
    private ReturnUseCase returnUseCase;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private IdempotencyStore idempotencyStore;

    @Autowired
    private DistributedLock distributedLock;

    @Autowired
    private CustomerLookupPort customerLookupPort;

    @Autowired
    private QueryResultCachePort queryResultCachePort;

    @Autowired
    private VectorSearchPort vectorSearchPort;

    @Autowired
    private AnthropicGateway anthropicGateway;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReturnRepository returnRepository;

    @Autowired
    private OutboxPort outboxPort;

    @Autowired
    private OutboxExternalPublisher outboxExternalPublisher;

    @Test
    @DisplayName("local profile ?꾩껜 runtime context瑜?濡쒕뵫?쒕떎")
    void loads_local_runtime_context() {
        assertThat(chatController).isNotNull();
        assertThat(sessionController).isNotNull();
        assertThat(chatUseCase).isNotNull();
        assertThat(verificationUseCase).isNotNull();
        assertThat(returnUseCase).isNotNull();
        assertThat(llmClient).isNotNull();

        assertThat(dataSource).isNotNull();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        assertThat(orderJpaRepository.count()).isZero();
        assertThat(returnJpaRepository.count()).isZero();
        assertThat(outboxJpaRepository.count()).isZero();
    }

    @Test
    @DisplayName("local profile? MySQL adapter? local-safe adapter瑜??④퍡 ?ъ슜?쒕떎")
    void loads_local_profile_adapters() {
        assertThat(orderRepository).isInstanceOf(OrderRepositoryAdapter.class);
        assertThat(returnRepository).isInstanceOf(ReturnRepositoryAdapter.class);
        assertThat(outboxPort).isInstanceOf(OutboxAdapter.class);
        assertThat(outboxExternalPublisher).isInstanceOf(LocalOutboxExternalPublisher.class);

        assertThat(idempotencyStore).isInstanceOf(InMemoryIdempotencyStore.class);
        assertThat(distributedLock).isInstanceOf(LocalDistributedLock.class);
        assertThat(customerLookupPort).isInstanceOf(InMemoryCustomerLookupAdapter.class);
        assertThat(queryResultCachePort).isInstanceOf(InMemoryQueryResultCacheAdapter.class);
        assertThat(vectorSearchPort).isInstanceOf(InMemoryVectorSearchAdapter.class);
        assertThat(anthropicGateway).isInstanceOf(DemoAnthropicGateway.class);
        assertThat(chatUseCase).extracting("sessionManager").isInstanceOf(InMemorySessionManager.class);
    }
}
