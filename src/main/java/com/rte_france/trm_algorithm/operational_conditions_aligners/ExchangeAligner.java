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
import java.util.Objects;

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

    public ExchangeAligner(BalanceComputationParameters balanceComputationParameters, LoadFlow.Runner loadFlowRunner, ComputationManager computationManager) {
        this.balanceComputationParameters = balanceComputationParameters;
        this.loadFlowRunner = loadFlowRunner;
        this.computationManager = computationManager;
        this.balanceComputationFactory = new BalanceComputationFactoryImpl();
    }

    public Status align(Network referenceNetwork, Network marketBasedNetwork, ZonalData<Scalable> marketZonalScalable) {
        LoadFlow.run(referenceNetwork, balanceComputationParameters.getLoadFlowParameters());
        LoadFlow.run(marketBasedNetwork, balanceComputationParameters.getLoadFlowParameters());

        if (referenceNetwork.getCountries().stream().noneMatch(country -> hasDifferentNetPosition(referenceNetwork, marketBasedNetwork, country))) {
            LOGGER.info("No significant net position difference. Exchange alignment ignored !");
            return Status.ALREADY_ALIGNED;
        }

        List<BalanceComputationArea> areas = createBalanceComputationAreas(referenceNetwork, marketZonalScalable);
        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);
        String variantId = marketBasedNetwork.getVariantManager().getWorkingVariantId();
        BalanceComputationResult result = balanceComputation.run(marketBasedNetwork, variantId, balanceComputationParameters).join();
        if (result.getStatus().equals(BalanceComputationResult.Status.FAILED)) {
            LOGGER.error("Balance computation failed");
            return Status.FAILED;
        }
        return Status.ALIGNED_WITH_BALANCE_ADJUSTMENT;
    }

    private static boolean hasDifferentNetPosition(Network referenceNetwork, Network marketBasedNetwork, Country country) {
        CountryAreaFactory countryAreaFactory = new CountryAreaFactory(country);
        return Math.abs(countryAreaFactory.create(referenceNetwork).getNetPosition() - countryAreaFactory.create(marketBasedNetwork).getNetPosition()) > NET_POSITION_EPSILON;
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
