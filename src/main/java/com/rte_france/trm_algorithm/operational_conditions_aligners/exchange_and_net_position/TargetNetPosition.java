/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position;

import com.powsybl.iidm.network.Country;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class TargetNetPosition implements NetPositionInterface {
    private static final double DEFAULT_NET_POSITION = 0.;

    private final Map<Country, Double> netPositions;

    public TargetNetPosition(ExchangeAndNetPosition referenceExchangeAndNetPosition, ExchangeAndNetPosition initialMarketBasedExchangeAndNetPosition) {
        netPositions = computeTargetNetPosition(referenceExchangeAndNetPosition, initialMarketBasedExchangeAndNetPosition);

    }

    public static Map<Country, Double> computeTargetNetPosition(ExchangeAndNetPosition referenceExchangeAndNetPosition, ExchangeAndNetPosition initialMarketBasedExchangeAndNetPosition) {
        return initialMarketBasedExchangeAndNetPosition.getCountries().stream()
            .collect(Collectors.toMap(
                Function.identity(), country -> computeTargetNetPosition(country, referenceExchangeAndNetPosition, initialMarketBasedExchangeAndNetPosition)
            ));
    }

    private static double computeTargetNetPosition(Country country, ExchangeAndNetPosition referenceExchangeAndNetPosition, ExchangeAndNetPosition initialMarketBasedExchangeAndNetPosition) {
        if (!referenceExchangeAndNetPosition.getCountries().contains(country)) {
            return initialMarketBasedExchangeAndNetPosition.getNetPosition(country);
        }
        return initialMarketBasedExchangeAndNetPosition.getNetPosition(country) + getTotalLeavingFlow(referenceExchangeAndNetPosition.getCountries(), country, referenceExchangeAndNetPosition) - getTotalLeavingFlow(referenceExchangeAndNetPosition.getCountries(), country, initialMarketBasedExchangeAndNetPosition);
    }

    private static double getTotalLeavingFlow(Set<Country> countries, Country country, ExchangeAndNetPosition exchangeAndNetPosition) {
        return countries.stream()
            .mapToDouble(otherCountry -> exchangeAndNetPosition.getExchange(country, otherCountry))
            .sum();
    }

    @Override
    public Set<Country> getCountries() {
        return Collections.unmodifiableSet(netPositions.keySet());
    }

    @Override
    public double getNetPosition(Country country) {
        return netPositions.getOrDefault(country, DEFAULT_NET_POSITION);
    }
}
