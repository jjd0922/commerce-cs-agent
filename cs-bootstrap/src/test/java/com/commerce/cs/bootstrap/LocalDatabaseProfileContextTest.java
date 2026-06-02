package com.commerce.cs.bootstrap;

import com.commerce.cs.infra.persistence.order.OrderJpaRepository;
import com.commerce.cs.infra.persistence.outbox.OutboxJpaRepository;
import com.commerce.cs.infra.persistence.returns.ReturnJpaRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
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

@DisplayName("local DB profile bootstrap 컨텍스트")
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("local")
@SpringBootTest(
    classes = CommerceCsAgentApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.main.lazy-initialization=true",
        "spring.task.scheduling.enabled=false"
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

    @Test
    @DisplayName("MySQL datasource와 Flyway schema, JPA repository를 로딩한다")
    void loads_local_database_profile_context() {
        assertThat(dataSource).isNotNull();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        assertThat(orderJpaRepository.count()).isZero();
        assertThat(returnJpaRepository.count()).isZero();
        assertThat(outboxJpaRepository.count()).isZero();
    }
}
