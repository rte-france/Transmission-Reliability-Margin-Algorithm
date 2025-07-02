/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position.EmptyExchangeAndNetPosition;
import com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position.ExchangeAndNetPositionInterface;

import java.util.Objects;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class ItalyNorthExchangeAlignerResult {
    private final ExchangeAndNetPositionInterface referenceExchangeAndNetPosition;
    private final ExchangeAndNetPositionInterface initialMarketBasedExchangeAndNetPosition;
    private final ExchangeAndNetPositionInterface newMarketBasedExchangeAndNetPosition;
    private final ExchangeAlignerStatus status;

    public ItalyNorthExchangeAlignerResult(Builder builder) {
        referenceExchangeAndNetPosition = builder.referenceExchangeAndNetPosition;
        initialMarketBasedExchangeAndNetPosition = builder.initialMarketBasedExchangeAndNetPosition;
        newMarketBasedExchangeAndNetPosition = builder.newMarketBasedExchangeAndNetPosition;
        status = builder.status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ExchangeAndNetPositionInterface getReferenceExchangeAndNetPosition() {
        return referenceExchangeAndNetPosition;
    }

    public ExchangeAndNetPositionInterface getInitialMarketBasedExchangeAndNetPosition() {
        return initialMarketBasedExchangeAndNetPosition;
    }

    public ExchangeAndNetPositionInterface getNewMarketBasedExchangeAndNetPosition() {
        return newMarketBasedExchangeAndNetPosition;
    }

    public ExchangeAlignerStatus getStatus() {
        return status;
    }

    public static final class Builder {
        private ExchangeAndNetPositionInterface referenceExchangeAndNetPosition;
        private ExchangeAndNetPositionInterface initialMarketBasedExchangeAndNetPosition;
        private ExchangeAndNetPositionInterface newMarketBasedExchangeAndNetPosition = new EmptyExchangeAndNetPosition();
        private ExchangeAlignerStatus status;

        private Builder() {
            // Builder pattern
        }

        public Builder addReferenceExchangeAndNetPosition(ExchangeAndNetPositionInterface referenceExchangeAndNetPosition) {
            this.referenceExchangeAndNetPosition = referenceExchangeAndNetPosition;
            return this;
        }

        public Builder addInitialMarketBasedExchangeAndNetPositions(ExchangeAndNetPositionInterface initialMarketBasedExchangeAndNetPosition) {
            this.initialMarketBasedExchangeAndNetPosition = initialMarketBasedExchangeAndNetPosition;
            return this;
        }

        public Builder addNewMarketBasedExchangeAndNetPositions(ExchangeAndNetPositionInterface newMarketBasedExchangeAndNetPosition) {
            this.newMarketBasedExchangeAndNetPosition = newMarketBasedExchangeAndNetPosition;
            return this;
        }

        public Builder addExchangeAlignerStatus(ExchangeAlignerStatus status) {
            this.status = status;
            return this;
        }

        public ItalyNorthExchangeAlignerResult build() {
            Objects.requireNonNull(referenceExchangeAndNetPosition, "referenceExchangeAndNetPosition must not be null");
            Objects.requireNonNull(initialMarketBasedExchangeAndNetPosition, "initialMarketBasedExchangeAndNetPosition must not be null");
            Objects.requireNonNull(status, "status must not be null");
            return new ItalyNorthExchangeAlignerResult(this);
        }
    }

}
