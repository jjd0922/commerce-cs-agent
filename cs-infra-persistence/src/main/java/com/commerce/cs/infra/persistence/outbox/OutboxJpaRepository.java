package com.commerce.cs.infra.persistence.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxJpaRepository extends JpaRepository<OutboxMessage, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select message from OutboxMessage message where message.status = com.commerce.cs.infra.persistence.outbox.OutboxStatus.PENDING order by message.createdAt asc")
    List<OutboxMessage> findPendingForUpdate(Pageable pageable);
}
