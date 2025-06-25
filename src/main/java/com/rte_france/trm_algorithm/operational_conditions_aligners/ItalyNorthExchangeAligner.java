/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.farao_community.farao.dichotomy.shift.SplittingFactors;
import com.google.common.collect.ImmutableMap;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;

import com.rte_france.trm_algorithm.TrmUtils;
import com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position.ExchangeAndNetPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.powsybl.iidm.network.Country.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class ItalyNorthExchangeAligner implements OperationalConditionAligner {
    private static final double EXCHANGE_EPSILON = 1e-1;
    private static final Logger LOGGER = LoggerFactory.getLogger(ItalyNorthExchangeAligner.class);

    public ItalyNorthExchangeAligner() {
        UcteGlskDocument ucteGlskDocument = null;
    }

    @Override
    public void align(Network referenceNetwork, Network marketBasedNetwork) {
        LOGGER.info("Aligning exchanges");

        LoadFlow.run(referenceNetwork); // TODO: point at loadflow parameters
        LoadFlow.run(marketBasedNetwork);

        ExchangeAndNetPosition referenceExchangeAndNetPosition = new ExchangeAndNetPosition(referenceNetwork);
        ExchangeAndNetPosition initialMarketBasedExchangeAndNetPosition = new ExchangeAndNetPosition(marketBasedNetwork);

        ZonalData<Scalable> zonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);

        Map<String, Double>  splittingFactors = ImmutableMap.of(
                new CountryEICode(FR).getCode(), 0.4,
                new CountryEICode(AT).getCode(), 0.3,
                new CountryEICode(CH).getCode(), 0.1,
                new CountryEICode(SI).getCode(), 0.2
        );

        ShiftDispatcher shiftDispatcher = new SplittingFactors(splittingFactors);

        LinearScaler linearScaler = new LinearScaler(zonalScalable, shiftDispatcher);
        try {
            linearScaler.shiftNetwork(referenceExchangeAndNetPosition.getNetPosition(Country.IT), marketBasedNetwork);
            ExchangeAndNetPosition finalMarketBasedExchangeAndNetPosition = new ExchangeAndNetPosition(marketBasedNetwork);

            int i = 1;
        } catch (GlskLimitationException e) {
            throw new RuntimeException(e);
        } catch (ShiftingException e) {
            throw new RuntimeException(e);
        }
    }
}
