/*
 * Copyright 2017 Expero, Inc.
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

package com.experoinc.dropwizard.tinkerpop;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.util.Duration;
import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;
import org.apache.tinkerpop.gremlin.driver.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link HealthCheck} that validates the servers ability to connect to the TinkerPop server
 *
 * @author Chris Pounds
 */
public class TinkerPopHealthCheck extends HealthCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(TinkerPopHealthCheck.class);

    private final Client client;
    private final String validationGremlinQuery;
    private final Duration validationTimeout;

    public TinkerPopHealthCheck(
            Cluster cluster,
            String validationGremlinQuery,
            Duration validationTimeout) {

        this.client = cluster.connect();
        this.validationGremlinQuery = validationGremlinQuery;
        this.validationTimeout = validationTimeout;
    }

    @Override
    protected Result check() throws Exception {

        try {
            CompletableFuture<ResultSet> future = client.submitAsync(validationGremlinQuery);
            ResultSet result = future.get(validationTimeout.toMilliseconds(), TimeUnit.MILLISECONDS);

            return Result.healthy();

        } catch (TimeoutException ex) {
            String msg = String.format("Validation query was unable to complete after %d ms",
                    validationTimeout.toMilliseconds());

            LOGGER.error(msg);
            return Result.unhealthy(msg);
        } catch (Exception ex) {
            LOGGER.error("Unable to execute tinker pop health check", ex);
            return Result.unhealthy(ex.getMessage());
        }

    }
}
