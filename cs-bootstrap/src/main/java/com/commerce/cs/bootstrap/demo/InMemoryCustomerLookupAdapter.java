package com.commerce.cs.bootstrap.demo;

import com.commerce.cs.application.verification.CustomerIdentity;
import com.commerce.cs.application.verification.CustomerLookupPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"demo", "local"})
public class InMemoryCustomerLookupAdapter implements CustomerLookupPort {

    private final Map<String, CustomerIdentity> customers = new ConcurrentHashMap<>();

    public InMemoryCustomerLookupAdapter() {
        save(new CustomerIdentity("user-1", "customer@example.com", "5678"));
    }

    @Override
    public Optional<CustomerIdentity> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(customers.get(email.toLowerCase()));
    }

    private void save(CustomerIdentity customerIdentity) {
        customers.put(customerIdentity.email().toLowerCase(), customerIdentity);
    }
}
