/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.algorithm;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class TrmAlgorithm {

    private final LoadFlowParameters loadFlowParameters;

    public TrmAlgorithm(LoadFlowParameters loadFlowParameters) {
        this.loadFlowParameters = loadFlowParameters;
    }

    public Map<String, Double> run(Network referenceNetwork, Network marketBasedNetwork) {
        Set<Branch> referenceNetworkElements = CneSelector.getNetworkElements(referenceNetwork);
        Set<Branch> marketBasedNetworkElements = CneSelector.getNetworkElements(marketBasedNetwork);

        checkIdenticalNetworkElements(referenceNetworkElements, marketBasedNetworkElements, "Market-based");
        checkIdenticalNetworkElements(marketBasedNetworkElements, referenceNetworkElements, "Real-time");

        OperationalConditionAligner.align(referenceNetwork, marketBasedNetwork);
        FlowExtractor flowExtractor = new FlowExtractor(loadFlowParameters);
        Map<String, Double> referenceFlows = flowExtractor.extract(referenceNetwork, referenceNetworkElements);
        Map<String, Double> marketBasedFlows = flowExtractor.extract(marketBasedNetwork, marketBasedNetworkElements);
        double ptdf = 1.;
        return referenceFlows.keySet().stream().collect(Collectors.toMap(
            Function.identity(),
            key -> (marketBasedFlows.get(key) - referenceFlows.get(key)) / ptdf
        ));
    }

    private static void checkIdenticalNetworkElements(Set<Branch> networkElement1, Set<Branch> networkElement2, String networkMsg) {
        Set<String> extraReferenceElement = networkElement1.stream().map(Identifiable::getId).collect(Collectors.toSet());
        extraReferenceElement.removeAll(networkElement2.stream().map(Identifiable::getId).collect(Collectors.toSet()));

        if (!extraReferenceElement.isEmpty()) {
            throw new TrmException(String.format("%s network doesn't contain the following elements: %s.", networkMsg, extraReferenceElement));
        }
    }
}
