/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openshift.booster.service;

import java.time.Duration;

import com.nike.fastbreak.CircuitBreaker;
import com.nike.fastbreak.CircuitBreakerImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service invoking name-service via REST and guarded by Fastbreak.
 */
@Service
public class NameService {

    private final String nameHost = System.getProperty("name.host", "http://springboot-cb-name");
    private final RestTemplate restTemplate = new RestTemplate();
    private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;

    private CircuitBreaker<String> nameCircuitBreaker = CircuitBreakerImpl.<String>newBuilder()
        .withMaxConsecutiveFailuresAllowed(20)
        .withCallTimeout(Duration.ofMillis(1000))
        .withId("NameService")
        .build()
        .onOpen(() -> state = CircuitBreakerState.OPEN)
        .onClose(() -> state = CircuitBreakerState.CLOSED);

    public String getName() throws Exception {
        return nameCircuitBreaker.executeBlockingCall(this::getNameInternal);
    }

    private String getNameInternal() {
        return restTemplate.getForObject(nameHost + "/api/name", String.class);
    }

    private String getFallbackName() {
        return "Fallback";
    }

    CircuitBreakerState getState() throws Exception {
        return state;
    }
}
