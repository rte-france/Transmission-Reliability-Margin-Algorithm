/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class ItalyNorthExchangeAligner implements OperationalConditionAligner {
    private static final double EXCHANGE_EPSILON = 1e-1;
    private static final Logger LOGGER = LoggerFactory.getLogger(ItalyNorthExchangeAligner.class);
    private Map<Country, Double> countriesBalances = new HashMap<>();

    public ItalyNorthExchangeAligner(Network network) {
        countriesBalances = computeCountriesBalances(network);
    }

    private Map<Country, Double> computeCountriesBalances(Network network) {
        LoadFlow.run(network);
        Map<Country, Double> countriesBalances = new HashMap<>();
        countriesBalances.put(Country.AT, new CountryAreaFactory(Country.AT).create(network).getNetPosition());
        countriesBalances.put(Country.CH, new CountryAreaFactory(Country.CH).create(network).getNetPosition());
        countriesBalances.put(Country.FR, new CountryAreaFactory(Country.FR).create(network).getNetPosition());
        countriesBalances.put(Country.SI, new CountryAreaFactory(Country.SI).create(network).getNetPosition());
        countriesBalances.put(Country.IT, new CountryAreaFactory(Country.IT).create(network).getNetPosition());
        countriesBalances.put(Country.DE, new CountryAreaFactory(Country.DE).create(network).getNetPosition());
        return countriesBalances;
    }

    public double getCountryBalance(Country country) {
        return countriesBalances.get(country);
    }

    @Override
    public void align(Network referenceNetwork, Network marketBasedNetwork) {
        LOGGER.info("Aligning exchanges");
    }
}
