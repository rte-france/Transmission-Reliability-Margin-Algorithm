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
public class GeneratorMapShapeAligner implements OperationalConditionAligner {

    private final Predicate<Generator> generatorPredicate;

    public GeneratorMapShapeAligner() {
        generatorPredicate = gen -> true;
    }

    public GeneratorMapShapeAligner(Country... countries) {
        this(Set.of(countries));
    }

    public GeneratorMapShapeAligner(Set<Country> countries) {
        generatorPredicate = gen -> countries.contains(getInjectionCountry(gen));
    }

    @Override
    public void align(Network referenceNetwork, Network marketBasedNetwork) {
        Set<String> gensIds = getAllGensInCommon(referenceNetwork, marketBasedNetwork);
        Map<Country, Double> totalGenerationsReference = getTotalGenerationByCountry(gensIds, referenceNetwork);
        Map<Country, Double> totalGenerationsMarketBased = getTotalGenerationByCountry(gensIds, marketBasedNetwork);

        gensIds.stream().map(marketBasedNetwork::getGenerator).forEach(
                gen -> {
                    Country genCountry = getInjectionCountry(gen);
                    gen.setTargetP(referenceNetwork.getGenerator(gen.getId()).getTargetP() * totalGenerationsMarketBased.get(genCountry) / totalGenerationsReference.get(genCountry));
                }
        );
    }

    private Set<String> getAllGensInCommon(Network referenceNetwork, Network marketBasedNetwork) {
        return referenceNetwork.getGeneratorStream()
                .filter(generatorPredicate)
                .map(Identifiable::getId)
                .filter(id -> marketBasedNetwork.getGenerator(id) != null)
                .collect(Collectors.toSet());
    }

    private static Country getInjectionCountry(Injection<?> injection) {
        return injection.getTerminal().getVoltageLevel().getSubstation().flatMap(Substation::getCountry).orElse(null);
    }

    private static Map<Country, Double> getTotalGenerationByCountry(Set<String> generatorIds, Network network) {
        Map<Country, Double> totalGeneration = new EnumMap<>(Country.class);
        generatorIds.stream().map(network::getGenerator).forEach(
                gen -> {
                    Country genCountry = getInjectionCountry(gen);
                    totalGeneration.put(genCountry, totalGeneration.getOrDefault(genCountry, 0.) + gen.getTargetP());
                }
        );
        return totalGeneration;
    }
}
