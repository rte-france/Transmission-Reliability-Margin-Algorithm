/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.balances_adjustment.balance_computation.*;
import com.powsybl.balances_adjustment.util.CountryArea;
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
public class ExchangeAligner implements OperationalConditionAligner {
    public static final double DEFAULT_EXCHANGE_FLOW = 0.;
    private static final double EXCHANGE_EPSILON = 1e-1;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeAligner.class);
    private final BalanceComputationParameters balanceComputationParameters;
    private final LoadFlow.Runner loadFlowRunner;
    private final ComputationManager computationManager;
    private final ZonalData<Scalable> marketZonalScalable;
    private final BalanceComputationFactory balanceComputationFactory;
    private ExchangeAlignerResult result = null;

    public ExchangeAligner(BalanceComputationParameters balanceComputationParameters, LoadFlow.Runner loadFlowRunner, ComputationManager computationManager, ZonalData<Scalable> marketZonalScalable) {
        Objects.requireNonNull(balanceComputationParameters);
        Objects.requireNonNull(loadFlowRunner);
        Objects.requireNonNull(computationManager);
        Objects.requireNonNull(marketZonalScalable);
        this.balanceComputationParameters = balanceComputationParameters;
        this.loadFlowRunner = loadFlowRunner;
        this.computationManager = computationManager;
        this.marketZonalScalable = marketZonalScalable;
        this.balanceComputationFactory = new BalanceComputationFactoryImpl();
    }

    private static Map<Country, Double> getNetPositions(Network network) {
        return network.getCountries().stream()
            .collect(Collectors.toMap(
                Function.identity(),
                country -> new CountryAreaFactory(country).create(network).getNetPosition()));
    }

    private static Map<Country, CountryArea> createCountryAreas(Network network) {
        return network.getCountries().stream().collect(Collectors.toMap(Function.identity(), country -> new CountryAreaFactory(country).create(network)));
    }

    private static Map<Country, Map<Country, Double>> computeExchanges(Map<Country, CountryArea> countryAreaMap) {
        return countryAreaMap.keySet().stream()
            .collect(Collectors.toMap(
                Function.identity(),
                country1 -> countryAreaMap.keySet().stream()
                    .filter(country2 -> country1 != country2)
                    .collect(Collectors.toMap(
                        Function.identity(),
                        country2 -> countryAreaMap.get(country1).getLeavingFlowToCountry(countryAreaMap.get(country2))))
            ));
    }

    private static double computeTargetNetPosition(Network referenceNetwork, Map<Country, Double> initialMarketBasedNetPositions, Country country, Map<Country, Map<Country, Double>> referenceExchanges, Map<Country, Map<Country, Double>> initialMarketBasedExchanges) {
        if (!referenceNetwork.getCountries().contains(country)) {
            return initialMarketBasedNetPositions.get(country);
        }
        return initialMarketBasedNetPositions.get(country) + getTotalLeavingFlow(referenceNetwork, country, referenceExchanges) - getTotalLeavingFlow(referenceNetwork, country, initialMarketBasedExchanges);
    }

    private static double getTotalLeavingFlow(Network network, Country country, Map<Country, Map<Country, Double>> exchanges) {
        return network.getCountries().stream()
            .mapToDouble(otherCountry -> exchanges.get(country).getOrDefault(otherCountry, DEFAULT_EXCHANGE_FLOW))
            .sum();
    }

    private static double getMaxAbsoluteExchangeDifference(Map<Country, Map<Country, Double>> referenceExchanges, Map<Country, Map<Country, Double>> newMarketBasedExchanges) {
        return referenceExchanges.keySet().stream().mapToDouble(country1 -> getMaxAbsoluteExchangeDifference(country1, referenceExchanges, newMarketBasedExchanges)).max().orElse(DEFAULT_EXCHANGE_FLOW);
    }

    private static double getMaxAbsoluteExchangeDifference(Country country1, Map<Country, Map<Country, Double>> referenceExchanges, Map<Country, Map<Country, Double>> newMarketBasedExchanges) {
        return referenceExchanges.get(country1).keySet().stream().mapToDouble(country2 -> Math.abs(referenceExchanges.get(country1).get(country2) - newMarketBasedExchanges.get(country1).get(country2))).max().orElse(DEFAULT_EXCHANGE_FLOW);
    }

    private List<BalanceComputationArea> createBalanceComputationAreas(Network marketBasedNetwork, Map<Country, Double> targetNetPositions) {
        return marketBasedNetwork.getCountries().stream().map(country -> {
            String name = country.getName();
            NetworkAreaFactory networkAreaFactory = new CountryAreaFactory(country);
            String areaCode = new EICode(country).getAreaCode();
            Scalable scalable = marketZonalScalable.getData(areaCode);
            if (Objects.isNull(scalable)) {
                throw new TrmException("Scalable not found: " + areaCode);
            }
            return new BalanceComputationArea(name, networkAreaFactory, scalable, targetNetPositions.get(country));
        }).toList();
    }

    public ExchangeAlignerResult getResult() {
        return result;
    }

    @Override
    public void align(Network referenceNetwork, Network marketBasedNetwork) {
        LOGGER.info("Aligning exchanges");

        if (!marketBasedNetwork.getCountries().containsAll(referenceNetwork.getCountries())) {
            throw new TrmException(String.format("Market based network contains countries %s. It does not contain all reference network countries %s", marketBasedNetwork.getCountries(), referenceNetwork.getCountries()));
        }

        ExchangeAlignerResult.Builder builder = ExchangeAlignerResult.builder();
        LoadFlow.run(referenceNetwork, balanceComputationParameters.getLoadFlowParameters());
        LoadFlow.run(marketBasedNetwork, balanceComputationParameters.getLoadFlowParameters());

        Map<Country, Double> initialMarketBasedNetPositions = getNetPositions(marketBasedNetwork);
        Map<Country, CountryArea> marketBasedCountryAreas = createCountryAreas(marketBasedNetwork);
        Map<Country, Map<Country, Double>> referenceExchanges = computeExchanges(createCountryAreas(referenceNetwork));
        Map<Country, Map<Country, Double>> initialMarketBasedExchanges = computeExchanges(marketBasedCountryAreas);
        double initialMaxAbsoluteExchangeDifference = getMaxAbsoluteExchangeDifference(referenceExchanges, initialMarketBasedExchanges);

        Map<Country, Double> targetNetPositions = marketBasedNetwork.getCountries().stream()
            .collect(Collectors.toMap(
                Function.identity(), country -> computeTargetNetPosition(referenceNetwork, initialMarketBasedNetPositions, country, referenceExchanges, initialMarketBasedExchanges)
            ));

        builder.addReferenceNetPositions(getNetPositions(referenceNetwork))
            .addInitialMarketBasedNetPositions(initialMarketBasedNetPositions)
            .addReferenceExchange(referenceExchanges)
            .addInitialMarketBasedExchanges(initialMarketBasedExchanges)
            .addInitialMaxAbsoluteExchangeDifference(initialMaxAbsoluteExchangeDifference)
            .addTargetNetPosition(targetNetPositions);

        if (initialMaxAbsoluteExchangeDifference < EXCHANGE_EPSILON) {
            LOGGER.info("No significant exchange difference. Exchange alignment ignored !");
            result = builder.addExchangeAlignerStatus(Status.ALREADY_ALIGNED).build();
            return;
        }

        List<BalanceComputationArea> areas = createBalanceComputationAreas(marketBasedNetwork, targetNetPositions);
        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);
        String variantId = marketBasedNetwork.getVariantManager().getWorkingVariantId();
        BalanceComputationResult balanceComputationResult = balanceComputation.run(marketBasedNetwork, variantId, balanceComputationParameters).join();
        Map<Country, Map<Country, Double>> newMarketBasedExchanges = computeExchanges(marketBasedCountryAreas);
        double newMaxAbsoluteExchangeDifference = getMaxAbsoluteExchangeDifference(referenceExchanges, newMarketBasedExchanges);

        builder.addNewMarketBasedNetPositions(getNetPositions(marketBasedNetwork))
            .addNewMarketBasedExchanges(newMarketBasedExchanges)
            .addNewMaxAbsoluteExchangeDifference(newMaxAbsoluteExchangeDifference)
            .addBalanceComputationResult(balanceComputationResult);

        if (balanceComputationResult.getStatus().equals(BalanceComputationResult.Status.FAILED)) {
            LOGGER.error("Balance computation failed");
            result = builder.addExchangeAlignerStatus(Status.NOT_ALIGNED).build();
            return;
        }
        if (newMaxAbsoluteExchangeDifference > EXCHANGE_EPSILON) {
            LOGGER.error("Net positions have reached their targets but exchange are not aligned. This may be explained by the NTC hypothesis");
            result = builder.addExchangeAlignerStatus(Status.TARGET_NET_POSITION_REACHED_BUT_EXCHANGE_NOT_ALIGNED).build();
            return;
        }
        result = builder.addExchangeAlignerStatus(Status.ALIGNED_WITH_BALANCE_ADJUSTMENT).build();
    }

    public enum Status {
        ALIGNED_WITH_BALANCE_ADJUSTMENT,
        ALREADY_ALIGNED,
        NOT_ALIGNED,
        TARGET_NET_POSITION_REACHED_BUT_EXCHANGE_NOT_ALIGNED,
    }
}
