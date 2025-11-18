/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position;

import com.powsybl.balances_adjustment.util.BorderBasedCountryArea;
import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class ExchangeAndNetPosition implements ExchangeAndNetPositionInterface {
    private static final double DEFAULT_EXCHANGE_FLOW = 0.;
    private static final double DEFAULT_NET_POSITION = 0.;

    private final Map<Country, Map<Country, Double>> exchanges;
    private final Map<Country, Double> netPositions;

    public ExchangeAndNetPosition(Network network) {
        exchanges = computeExchanges(createCountryAreas(network));
        netPositions = computeNetPositions(network);
    }

    private static Map<Country, Double> computeNetPositions(Network network) {
        return network.getCountries().stream()
            .collect(Collectors.toMap(
                Function.identity(),
                country -> new CountryAreaFactory(country).create(network).getNetPosition()));
    }

    private static Map<Country, BorderBasedCountryArea> createCountryAreas(Network network) {
        return network.getCountries().stream().collect(Collectors.toMap(Function.identity(), country -> new BorderBasedCountryArea(network, List.of(country))));
    }

    private static Map<Country, Map<Country, Double>> computeExchanges(Map<Country, BorderBasedCountryArea> countryAreaMap) {
        Set<Country> countries = countryAreaMap.keySet();
        return countries.stream()
            .collect(Collectors.toMap(
                Function.identity(),
                countrySource -> computeExchangesOfCountrySource(countrySource, countries, countryAreaMap)
            ));
    }

    private static Map<Country, Double> computeExchangesOfCountrySource(Country countrySource, Set<Country> countries, Map<Country, BorderBasedCountryArea> countryAreaMap) {
        return countries.stream()
            .filter(countrySink -> countrySource != countrySink)
            .collect(Collectors.toMap(
                Function.identity(),
                countrySink -> getLeavingFlowToCountry(countrySource, countrySink, countryAreaMap)
            ));
    }

    private static double getLeavingFlowToCountry(Country countrySource, Country countrySink, Map<Country, BorderBasedCountryArea> countryAreaMap) {
        return countryAreaMap.get(countrySource).getLeavingFlowToCountry(countryAreaMap.get(countrySink));
    }

    @Override
    public Set<Country> getCountries() {
        return Collections.unmodifiableSet(exchanges.keySet());
    }

    @Override
    public double getExchange(Country countrySource, Country countrySink) {
        return exchanges.getOrDefault(countrySource, Collections.emptyMap()).getOrDefault(countrySink, DEFAULT_EXCHANGE_FLOW);
    }

    @Override
    public double getNetPosition(Country country) {
        return netPositions.getOrDefault(country, DEFAULT_NET_POSITION);
    }

    @Override
    public double getMaxAbsoluteExchangeDifference(ExchangeAndNetPositionInterface otherExchangeAndNetPosition) {
        return exchanges.keySet().stream()
            .mapToDouble(countrySource -> getMaxAbsoluteExchangeDifference(countrySource, otherExchangeAndNetPosition))
            .max().orElse(DEFAULT_EXCHANGE_FLOW);
    }

    @Override
    public double getExchangeToExterior(Country countrySource) {
        return getNetPosition(countrySource) - exchanges.getOrDefault(countrySource, Collections.emptyMap()).values().stream().mapToDouble(aDouble -> aDouble).sum();
    }

    private double getMaxAbsoluteExchangeDifference(Country countrySource, ExchangeAndNetPositionInterface otherExchangeAndNetPosition) {
        if (!otherExchangeAndNetPosition.getCountries().containsAll(getCountries())) {
            return Double.NaN;
        }
        return exchanges.get(countrySource).keySet().stream()
            .mapToDouble(countrySink -> Math.abs(exchanges.get(countrySource).get(countrySink) - otherExchangeAndNetPosition.getExchange(countrySource, countrySink)))
            .max().orElse(DEFAULT_EXCHANGE_FLOW);
    }
}
