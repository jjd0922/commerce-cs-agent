package com.commerce.cs.application.verification;

import java.util.Optional;

public interface CustomerLookupPort {

    Optional<CustomerIdentity> findByEmail(String email);
}
