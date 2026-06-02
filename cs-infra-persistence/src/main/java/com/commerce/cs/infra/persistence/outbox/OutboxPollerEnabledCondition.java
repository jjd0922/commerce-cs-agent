package com.commerce.cs.infra.persistence.outbox;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OutboxPollerEnabledCondition implements Condition {

    private static final String PROPERTY = "commerce.outbox.poller.enabled";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return context.getEnvironment().getProperty(PROPERTY, Boolean.class, true);
    }
}
