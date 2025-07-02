/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.balances_adjustment.balance_computation.BalanceComputationResult;
import com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position.EmptyExchangeAndNetPosition;
import com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position.ExchangeAndNetPositionInterface;
import com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position.NetPositionInterface;

import java.util.Objects;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class ExchangeAlignerResult {
    private final ExchangeAndNetPositionInterface referenceExchangeAndNetPosition;
    private final ExchangeAndNetPositionInterface initialMarketBasedExchangeAndNetPosition;
    private final NetPositionInterface targetNetPositions;
    private final BalanceComputationResult balanceComputationResult;
    private final ExchangeAndNetPositionInterface newMarketBasedExchangeAndNetPosition;
    private final ExchangeAlignerStatus status;

    public ExchangeAlignerResult(Builder builder) {
        referenceExchangeAndNetPosition = builder.referenceExchangeAndNetPosition;
        initialMarketBasedExchangeAndNetPosition = builder.initialMarketBasedExchangeAndNetPosition;
        targetNetPositions = builder.targetNetPositions;
        balanceComputationResult = builder.balanceComputationResult;
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

    public NetPositionInterface getTargetNetPositions() {
        return targetNetPositions;
    }

    public BalanceComputationResult getBalanceComputationResult() {
        return balanceComputationResult;
    }

    public ExchangeAndNetPositionInterface getNewMarketBasedExchangeAndNetPosition() {
        return newMarketBasedExchangeAndNetPosition;
    }

    public ExchangeAlignerStatus getStatus() {
        return status;
    }

    public double getInitialMaxAbsoluteExchangeDifference() {
        return referenceExchangeAndNetPosition.getMaxAbsoluteExchangeDifference(initialMarketBasedExchangeAndNetPosition);
    }

    public double getNewMaxAbsoluteExchangeDifference() {
        return referenceExchangeAndNetPosition.getMaxAbsoluteExchangeDifference(newMarketBasedExchangeAndNetPosition);
    }

    public static final class Builder {
        private ExchangeAndNetPositionInterface referenceExchangeAndNetPosition;
        private ExchangeAndNetPositionInterface initialMarketBasedExchangeAndNetPosition;
        private NetPositionInterface targetNetPositions;
        private BalanceComputationResult balanceComputationResult;
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

        public Builder addTargetNetPosition(NetPositionInterface targetNetPositions) {
            this.targetNetPositions = targetNetPositions;
            return this;
        }

        public Builder addBalanceComputationResult(BalanceComputationResult balanceComputationResult) {
            this.balanceComputationResult = balanceComputationResult;
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

        public ExchangeAlignerResult build() {
            Objects.requireNonNull(referenceExchangeAndNetPosition, "referenceExchangeAndNetPosition must not be null");
            Objects.requireNonNull(initialMarketBasedExchangeAndNetPosition, "initialMarketBasedExchangeAndNetPosition must not be null");
            Objects.requireNonNull(targetNetPositions, "targetNetPositions must not be null");
            Objects.requireNonNull(status, "status must not be null");
            return new ExchangeAlignerResult(this);
        }
    }

}
