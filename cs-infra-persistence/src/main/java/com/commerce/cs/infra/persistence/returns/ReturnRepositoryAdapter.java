package com.commerce.cs.infra.persistence.returns;

import com.commerce.cs.application.returns.ReturnRepository;
import com.commerce.cs.domain.returns.Return;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReturnRepositoryAdapter implements ReturnRepository {

    private final ReturnJpaRepository returnJpaRepository;

    @Override
    public Optional<Return> findByIdempotencyKey(String idempotencyKey) {
        return returnJpaRepository.findByIdempotencyKey(idempotencyKey)
            .map(ReturnEntity::toDomain);
    }

    @Override
    public void save(Return returnRequest, String idempotencyKey) {
        returnJpaRepository.save(ReturnEntity.from(returnRequest, idempotencyKey));
    }
}
