package com.commerce.cs.bootstrap.demo;

import com.commerce.cs.application.returns.ReturnRepository;
import com.commerce.cs.domain.returns.Return;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryReturnRepository implements ReturnRepository {

    private final Map<String, Return> returnsByIdempotencyKey = new ConcurrentHashMap<>();

    @Override
    public Optional<Return> findByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(returnsByIdempotencyKey.get(idempotencyKey));
    }

    @Override
    public void save(Return returnRequest, String idempotencyKey) {
        returnsByIdempotencyKey.put(idempotencyKey, returnRequest);
    }
}
