/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class LoadMapShapeAligner implements OperationalConditionAligner {

    private final Predicate<Load> loadPredicate;

    public LoadMapShapeAligner() {
        loadPredicate = load -> true;
    }

    public LoadMapShapeAligner(Country... countries) {
        this(Set.of(countries));
    }

    public LoadMapShapeAligner(Set<Country> countries) {
        loadPredicate = load -> countries.contains(getInjectionCountry(load));
    }

    @Override
    public void align(Network referenceNetwork, Network marketBasedNetwork) {
        Set<String> loadsIds = getAllLoadsInCommon(referenceNetwork, marketBasedNetwork);
        Map<Country, Double> totalLoadsReference = getTotalLoadByCountry(loadsIds, referenceNetwork);
        Map<Country, Double> totalLoadsMarketBased = getTotalLoadByCountry(loadsIds, marketBasedNetwork);

        loadsIds.stream().map(marketBasedNetwork::getLoad).forEach(
                load -> {
                    Country loadCountry = getInjectionCountry(load);
                    load.setP0(referenceNetwork.getLoad(load.getId()).getP0() * totalLoadsMarketBased.get(loadCountry) / totalLoadsReference.get(loadCountry));
                }
        );
    }

    private Set<String> getAllLoadsInCommon(Network referenceNetwork, Network marketBasedNetwork) {
        return referenceNetwork.getLoadStream()
                .filter(loadPredicate)
                .map(Identifiable::getId)
                .filter(id -> marketBasedNetwork.getLoad(id) != null)
                .collect(Collectors.toSet());
    }

    private static Country getInjectionCountry(Injection<?> injection) {
        return injection.getTerminal().getVoltageLevel().getSubstation().flatMap(Substation::getCountry).orElse(null);
    }

    private static Map<Country, Double> getTotalLoadByCountry(Set<String> loadsIds, Network network) {
        Map<Country, Double> totalLoad = new EnumMap<>(Country.class);
        loadsIds.stream().map(network::getLoad).forEach(
                load -> {
                    Country loadCountry = getInjectionCountry(load);
                    totalLoad.put(loadCountry, totalLoad.getOrDefault(loadCountry, 0.) + load.getP0());
                }
        );
        return totalLoad;
    }
}
