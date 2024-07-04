/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.balances_adjustment.balance_computation.BalanceComputationResult;
import com.powsybl.iidm.network.Country;

import java.util.Map;
import java.util.Objects;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class ExchangeAlignerResult {
    private final Map<Country, Double> referenceNetPositions;
    private final Map<Country, Double> initialMarketBasedNetPositions;
    private final Map<Country, Map<Country, Double>> referenceExchanges;
    private final Map<Country, Map<Country, Double>> initialMarketBasedExchanges;
    private final Double initialMaxAbsoluteExchangeDifference;
    private final Map<Country, Double> targetNetPositions;
    private final BalanceComputationResult balanceComputationResult;
    private final Map<Country, Double> newMarketBasedNetPositions;
    private final Map<Country, Map<Country, Double>> newMarketBasedExchanges;
    private final Double newMaxAbsoluteExchangeDifference;
    private final ExchangeAligner.Status status;

    private ExchangeAlignerResult(Map<Country, Double> referenceNetPositions,
                                  Map<Country, Double> initialMarketBasedNetPositions,
                                  Map<Country, Map<Country, Double>> referenceExchanges,
                                  Map<Country, Map<Country, Double>> initialMarketBasedExchanges,
                                  Double initialMaxAbsoluteExchangeDifference,
                                  Map<Country, Double> targetNetPositions,
                                  BalanceComputationResult balanceComputationResult,
                                  Map<Country, Double> newMarketBasedNetPositions,
                                  Map<Country, Map<Country, Double>> newMarketBasedExchanges,
                                  Double newMaxAbsoluteExchangeDifference,
                                  ExchangeAligner.Status status) {
        this.referenceNetPositions = referenceNetPositions;
        this.initialMarketBasedNetPositions = initialMarketBasedNetPositions;
        this.referenceExchanges = referenceExchanges;
        this.initialMarketBasedExchanges = initialMarketBasedExchanges;
        this.initialMaxAbsoluteExchangeDifference = initialMaxAbsoluteExchangeDifference;
        this.targetNetPositions = targetNetPositions;
        this.balanceComputationResult = balanceComputationResult;
        this.newMarketBasedNetPositions = newMarketBasedNetPositions;
        this.newMarketBasedExchanges = newMarketBasedExchanges;
        this.newMaxAbsoluteExchangeDifference = newMaxAbsoluteExchangeDifference;
        this.status = status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<Country, Double> getReferenceNetPositions() {
        return referenceNetPositions;
    }

    public Map<Country, Double> getInitialMarketBasedNetPositions() {
        return initialMarketBasedNetPositions;
    }

    public Map<Country, Map<Country, Double>> getReferenceExchanges() {
        return referenceExchanges;
    }

    public Double getInitialMaxAbsoluteExchangeDifference() {
        return initialMaxAbsoluteExchangeDifference;
    }

    public Map<Country, Map<Country, Double>> getInitialMarketBasedExchanges() {
        return initialMarketBasedExchanges;
    }

    public Map<Country, Double> getTargetNetPositions() {
        return targetNetPositions;
    }

    public BalanceComputationResult getBalanceComputationResult() {
        return balanceComputationResult;
    }

    public Map<Country, Double> getNewMarketBasedNetPositions() {
        return newMarketBasedNetPositions;
    }

    public Map<Country, Map<Country, Double>> getNewMarketBasedExchanges() {
        return newMarketBasedExchanges;
    }

    public Double getNewMaxAbsoluteExchangeDifference() {
        return newMaxAbsoluteExchangeDifference;
    }

    public ExchangeAligner.Status getStatus() {
        return status;
    }

    public static final class Builder {
        private Map<Country, Double> referenceNetPositions;
        private Map<Country, Double> initialMarketBasedNetPositions;
        private Map<Country, Map<Country, Double>> referenceExchanges;
        private Map<Country, Map<Country, Double>> initialMarketBasedExchanges;
        private Double initialMaxAbsoluteExchangeDifference;
        private Map<Country, Double> targetNetPositions;
        private BalanceComputationResult balanceComputationResult;
        private Map<Country, Double> newMarketBasedNetPositions;
        private Map<Country, Map<Country, Double>> newMarketBasedExchanges;
        private Double newMaxAbsoluteExchangeDifference;
        private ExchangeAligner.Status status;

        private Builder() {
            // Builder pattern
        }

        public Builder addReferenceNetPositions(Map<Country, Double> referenceNetPositions) {
            this.referenceNetPositions = referenceNetPositions;
            return this;
        }

        public Builder addInitialMarketBasedNetPositions(Map<Country, Double> initialMarketBasedNetPositions) {
            this.initialMarketBasedNetPositions = initialMarketBasedNetPositions;
            return this;
        }

        public Builder addReferenceExchange(Map<Country, Map<Country, Double>> referenceExchanges) {
            this.referenceExchanges = referenceExchanges;
            return this;
        }

        public Builder addInitialMarketBasedExchanges(Map<Country, Map<Country, Double>> initialMarketBasedExchanges) {
            this.initialMarketBasedExchanges = initialMarketBasedExchanges;
            return this;
        }

        public Builder addInitialMaxAbsoluteExchangeDifference(double initialMaxAbsoluteExchangeDifference) {
            this.initialMaxAbsoluteExchangeDifference = initialMaxAbsoluteExchangeDifference;
            return this;
        }

        public Builder addTargetNetPosition(Map<Country, Double> targetNetPositions) {
            this.targetNetPositions = targetNetPositions;
            return this;
        }

        public Builder addBalanceComputationResult(BalanceComputationResult balanceComputationResult) {
            this.balanceComputationResult = balanceComputationResult;
            return this;
        }

        public Builder addNewMarketBasedNetPositions(Map<Country, Double> newMarketBasedNetPositions) {
            this.newMarketBasedNetPositions = newMarketBasedNetPositions;
            return this;
        }

        public Builder addNewMarketBasedExchanges(Map<Country, Map<Country, Double>> newMarketBasedExchanges) {
            this.newMarketBasedExchanges = newMarketBasedExchanges;
            return this;
        }

        public Builder addNewMaxAbsoluteExchangeDifference(double newMaxAbsoluteExchangeDifference) {
            this.newMaxAbsoluteExchangeDifference = newMaxAbsoluteExchangeDifference;
            return this;
        }

        public Builder addExchangeAlignerStatus(ExchangeAligner.Status status) {
            this.status = status;
            return this;
        }

        public ExchangeAlignerResult build() {
            Objects.requireNonNull(referenceNetPositions, "referenceNetPositions must not be null");
            Objects.requireNonNull(initialMarketBasedNetPositions, "initialMarketBasedNetPositions must not be null");
            Objects.requireNonNull(referenceExchanges, "referenceExchanges must not be null");
            Objects.requireNonNull(initialMarketBasedExchanges, "initialMarketBasedExchanges must not be null");
            Objects.requireNonNull(initialMaxAbsoluteExchangeDifference, "initialMaxAbsoluteExchangeDifference must not be null");
            Objects.requireNonNull(targetNetPositions, "targetNetPositions must not be null");
            Objects.requireNonNull(status, "status must not be null");
            return new ExchangeAlignerResult(referenceNetPositions,
                initialMarketBasedNetPositions,
                referenceExchanges,
                initialMarketBasedExchanges,
                initialMaxAbsoluteExchangeDifference,
                targetNetPositions,
                balanceComputationResult,
                newMarketBasedNetPositions,
                newMarketBasedExchanges,
                newMaxAbsoluteExchangeDifference,
                status
            );
        }
    }

}
