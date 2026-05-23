package com.commerce.cs.application.returns;

import com.commerce.cs.domain.returns.Return;

import java.util.Optional;

public interface ReturnRepository {

    Optional<Return> findByIdempotencyKey(String idempotencyKey);

    void save(Return returnRequest, String idempotencyKey);
}
