/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.balances_adjustment.balance_computation.*;
import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.balances_adjustment.util.NetworkAreaFactory;
import com.powsybl.computation.ComputationManager;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.openrao.commons.EICode;
import com.rte_france.trm_algorithm.TrmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class ExchangeAligner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeAligner.class);
    public static final double NET_POSITION_EPSILON = 1e-3;
    private final BalanceComputationParameters balanceComputationParameters;
    private final ComputationManager computationManager;
    private final LoadFlow.Runner loadFlowRunner;
    private final BalanceComputationFactory balanceComputationFactory;

    public enum Status {
        ALIGNED_WITH_BALANCE_ADJUSTMENT,
        ALREADY_ALIGNED,
        FAILED,
    }

    public static final class Result {
        private final Map<Country, Double> referenceNetPositions;
        private final Map<Country, Double> initialMarketBasedNetPositions;
        private final Map<Country, Double> newMarketBasedNetPositions;
        private final BalanceComputationResult balanceComputationResult;
        private final Status status;

        private Result(Map<Country, Double> referenceNetPositions,
                       Map<Country, Double> initialMarketBasedNetPositions,
                       Map<Country, Double> newMarketBasedNetPositions,
                       BalanceComputationResult balanceComputationResult,
                       Status status) {
            this.referenceNetPositions = referenceNetPositions;
            this.initialMarketBasedNetPositions = initialMarketBasedNetPositions;
            this.newMarketBasedNetPositions = newMarketBasedNetPositions;
            this.balanceComputationResult = balanceComputationResult;
            this.status = status;
        }

        public static Builder getBuilder() {
            return new Builder();
        }

        public Map<Country, Double> getReferenceNetPositions() {
            return referenceNetPositions;
        }

        public Map<Country, Double> getInitialMarketBasedNetPositions() {
            return initialMarketBasedNetPositions;
        }

        public Map<Country, Double> getNewMarketBasedNetPositions() {
            return newMarketBasedNetPositions;
        }

        public BalanceComputationResult getBalanceComputationResult() {
            return balanceComputationResult;
        }

        public Status getStatus() {
            return status;
        }

        public static final class Builder {
            private Map<Country, Double> initialMarketBasedNetPositions;
            private Map<Country, Double> referenceNetPositions;
            private Map<Country, Double> newMarketBasedNetPositions;
            private BalanceComputationResult balanceComputationResult;
            private Status status;

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

            public Builder addNewMarketBasedNetPositions(Map<Country, Double> newMarketBasedNetPositions) {
                this.newMarketBasedNetPositions = newMarketBasedNetPositions;
                return this;
            }

            public Builder addBalanceComputationResult(BalanceComputationResult balanceComputationResult) {
                this.balanceComputationResult = balanceComputationResult;
                return this;
            }

            public Builder addExchangeAlignerStatus(Status status) {
                this.status = status;
                return this;
            }

            public Result build() {
                Objects.requireNonNull(initialMarketBasedNetPositions, "initialMarketBasedNetPositions must not be null");
                Objects.requireNonNull(referenceNetPositions, "referenceNetPositions must not be null");
                Objects.requireNonNull(status, "status must not be null");
                return new Result(referenceNetPositions,
                    initialMarketBasedNetPositions,
                    newMarketBasedNetPositions,
                    balanceComputationResult,
                    status);
            }
        }

    }

    public ExchangeAligner(BalanceComputationParameters balanceComputationParameters, LoadFlow.Runner loadFlowRunner, ComputationManager computationManager) {
        this.balanceComputationParameters = balanceComputationParameters;
        this.loadFlowRunner = loadFlowRunner;
        this.computationManager = computationManager;
        this.balanceComputationFactory = new BalanceComputationFactoryImpl();
    }

    public Result align(Network referenceNetwork, Network marketBasedNetwork, ZonalData<Scalable> marketZonalScalable) {
        LOGGER.info("Aligning exchanges");
        Result.Builder builder = Result.getBuilder();
        LoadFlow.run(referenceNetwork, balanceComputationParameters.getLoadFlowParameters());
        LoadFlow.run(marketBasedNetwork, balanceComputationParameters.getLoadFlowParameters());
        Map<Country, Double> initialMarketBasedNetPositions = getNetPositions(marketBasedNetwork);
        Map<Country, Double> referenceNetPositions = getNetPositions(referenceNetwork);
        builder.addInitialMarketBasedNetPositions(initialMarketBasedNetPositions)
            .addReferenceNetPositions(referenceNetPositions);

        if (referenceNetPositions.keySet().stream().noneMatch(country -> hasDifferentNetPosition(country, referenceNetPositions, initialMarketBasedNetPositions))) {
            LOGGER.info("No significant net position difference. Exchange alignment ignored !");
            return builder.addExchangeAlignerStatus(Status.ALREADY_ALIGNED).build();
        }

        List<BalanceComputationArea> areas = createBalanceComputationAreas(referenceNetwork, marketZonalScalable);
        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);
        String variantId = marketBasedNetwork.getVariantManager().getWorkingVariantId();
        BalanceComputationResult result = balanceComputation.run(marketBasedNetwork, variantId, balanceComputationParameters).join();
        builder.addNewMarketBasedNetPositions(getNetPositions(marketBasedNetwork))
            .addBalanceComputationResult(result);
        if (result.getStatus().equals(BalanceComputationResult.Status.FAILED)) {
            LOGGER.error("Balance computation failed");
            return builder.addExchangeAlignerStatus(Status.FAILED).build();
        }
        return builder.addExchangeAlignerStatus(Status.ALIGNED_WITH_BALANCE_ADJUSTMENT).build();
    }

    private static Map<Country, Double> getNetPositions(Network network) {
        return network.getCountries().stream()
            .collect(Collectors.toMap(
                Function.identity(),
                country -> new CountryAreaFactory(country).create(network).getNetPosition()));
    }

    private static boolean hasDifferentNetPosition(Country country, Map<Country, Double> referenceNetPositions, Map<Country, Double> initialMarketBasedNetPositions) {
        return !initialMarketBasedNetPositions.containsKey(country) || Math.abs(referenceNetPositions.get(country) - initialMarketBasedNetPositions.get(country)) > NET_POSITION_EPSILON;
    }

    private static List<BalanceComputationArea> createBalanceComputationAreas(Network referenceNetwork, ZonalData<Scalable> zonalData) {
        return referenceNetwork.getCountries().stream().map(country -> {
            String name = country.getName();
            NetworkAreaFactory networkAreaFactory = new CountryAreaFactory(country);
            String areaCode = new EICode(country).getAreaCode();
            Scalable scalable = zonalData.getData(areaCode);
            if (Objects.isNull(scalable)) {
                throw new TrmException("Scalable not found: " + areaCode);
            }
            double referenceNetPosition = networkAreaFactory.create(referenceNetwork).getNetPosition();
            return new BalanceComputationArea(name, networkAreaFactory, scalable, referenceNetPosition);
        }).toList();
    }
}
