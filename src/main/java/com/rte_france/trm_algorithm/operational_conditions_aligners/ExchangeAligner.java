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
import com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position.ExchangeAndNetPosition;
import com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position.TargetNetPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.rte_france.trm_algorithm.operational_conditions_aligners.ExchangeAlignerStatus.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class ExchangeAligner implements OperationalConditionAligner {
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

    private List<BalanceComputationArea> createBalanceComputationAreas(TargetNetPosition targetNetPositions) {
        return targetNetPositions.getCountries().stream().sorted(Comparator.comparing(Country::getName)).map(country -> { // sorted for consistency in tests
            String name = country.getName();
            NetworkAreaFactory networkAreaFactory = new CountryAreaFactory(country);
            String areaCode = new EICode(country).getAreaCode();
            Scalable scalable = marketZonalScalable.getData(areaCode);
            if (Objects.isNull(scalable)) {
                throw new TrmException("Scalable not found: " + areaCode);
            }
            return new BalanceComputationArea(name, networkAreaFactory, scalable, targetNetPositions.getNetPosition(country));
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

        LoadFlow.run(referenceNetwork, balanceComputationParameters.getLoadFlowParameters());
        LoadFlow.run(marketBasedNetwork, balanceComputationParameters.getLoadFlowParameters());

        ExchangeAndNetPosition referenceExchangeAndNetPosition = new ExchangeAndNetPosition(referenceNetwork);
        ExchangeAndNetPosition initialMarketBasedExchangeAndNetPosition = new ExchangeAndNetPosition(marketBasedNetwork);
        TargetNetPosition targetNetPositions = new TargetNetPosition(referenceExchangeAndNetPosition, initialMarketBasedExchangeAndNetPosition);

        ExchangeAlignerResult.Builder builder = ExchangeAlignerResult.builder()
            .addReferenceExchangeAndNetPosition(referenceExchangeAndNetPosition)
            .addInitialMarketBasedExchangeAndNetPositions(initialMarketBasedExchangeAndNetPosition)
            .addTargetNetPosition(targetNetPositions);

        if (referenceExchangeAndNetPosition.getMaxAbsoluteExchangeDifference(initialMarketBasedExchangeAndNetPosition) < EXCHANGE_EPSILON) {
            LOGGER.info("No significant exchange difference. Exchange alignment ignored !");
            result = builder.addExchangeAlignerStatus(ALREADY_ALIGNED).build();
            return;
        }

        BalanceComputationResult balanceComputationResult = align(marketBasedNetwork, targetNetPositions);
        ExchangeAndNetPosition newMarketBasedExchangeAndNetPosition = new ExchangeAndNetPosition(marketBasedNetwork);

        builder.addBalanceComputationResult(balanceComputationResult)
            .addNewMarketBasedExchangeAndNetPositions(newMarketBasedExchangeAndNetPosition);

        if (balanceComputationResult.getStatus().equals(BalanceComputationResult.Status.FAILED)) {
            LOGGER.error("Balance computation failed");
            result = builder.addExchangeAlignerStatus(NOT_ALIGNED).build();
            return;
        }
        if (referenceExchangeAndNetPosition.getMaxAbsoluteExchangeDifference(newMarketBasedExchangeAndNetPosition) > EXCHANGE_EPSILON) {
            LOGGER.error("Net positions have reached their targets but exchange are not aligned. This may be explained by the NTC hypothesis");
            result = builder.addExchangeAlignerStatus(TARGET_NET_POSITION_REACHED_BUT_EXCHANGE_NOT_ALIGNED).build();
            return;
        }
        result = builder.addExchangeAlignerStatus(ALIGNED_WITH_BALANCE_ADJUSTMENT).build();
    }

    private BalanceComputationResult align(Network marketBasedNetwork, TargetNetPosition targetNetPositions) {
        List<BalanceComputationArea> areas = createBalanceComputationAreas(targetNetPositions);
        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);
        String variantId = marketBasedNetwork.getVariantManager().getWorkingVariantId();
        return balanceComputation.run(marketBasedNetwork, variantId, balanceComputationParameters).join();
    }
}
