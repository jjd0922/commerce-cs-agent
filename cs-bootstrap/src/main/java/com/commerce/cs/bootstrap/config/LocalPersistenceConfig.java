package com.commerce.cs.bootstrap.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile("local")
@EntityScan("com.commerce.cs.infra.persistence")
@EnableJpaRepositories("com.commerce.cs.infra.persistence")
public class LocalPersistenceConfig {
}
